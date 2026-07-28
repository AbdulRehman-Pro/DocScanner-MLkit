package com.rehman.docscan.core

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.rehman.docscan.BuildConfig

object BillingManager : PurchasesUpdatedListener {

    private const val TAG = "BillingManager"

    private var appContext: Context? = null
    private var billingClient: BillingClient? = null
    private var premiumProductDetails: ProductDetails? = null
    private var isConnecting = false

    private val readyCallbacks = mutableListOf<(Boolean) -> Unit>()
    private var purchaseCallback: ((PurchaseResult) -> Unit)? = null

    sealed class PurchaseResult {
        data class Success(val isRestored: Boolean = false) : PurchaseResult()
        data class Error(val message: String) : PurchaseResult()
        data object Cancelled : PurchaseResult()
    }

    fun initialize(context: Context, onPremiumSynced: ((Boolean) -> Unit)? = null) {
        appContext = context.applicationContext
        ensureConnection(context.applicationContext) { isReady ->
            if (!isReady) {
                onPremiumSynced?.invoke(Prefs.isPremiumUnlocked())
                return@ensureConnection
            }

            queryPremiumProductDetails()
            syncPremiumStatus(onPremiumSynced)
        }
    }

    fun getPremiumPrice(onResult: (String?) -> Unit) {
        val context = appContext
        if (context == null) {
            onResult(null)
            return
        }

        ensureConnection(context) { isReady ->
            if (!isReady) {
                onResult(null)
                return@ensureConnection
            }

            premiumProductDetails?.let {
                onResult(it.oneTimePurchaseOfferDetails?.formattedPrice)
                return@ensureConnection
            }

            queryPremiumProductDetails { productDetails ->
                onResult(productDetails?.oneTimePurchaseOfferDetails?.formattedPrice)
            }
        }
    }

    fun launchPremiumPurchase(activity: Activity, onResult: (PurchaseResult) -> Unit) {
        purchaseCallback = onResult

        ensureConnection(activity.applicationContext) { isReady ->
            if (!isReady) {
                completePurchase(PurchaseResult.Error("Billing is currently unavailable."))
                return@ensureConnection
            }

            val productDetails = premiumProductDetails
            if (productDetails != null) {
                launchBillingFlow(activity, productDetails)
            } else {
                queryPremiumProductDetails { details ->
                    if (details == null) {
                        completePurchase(
                            PurchaseResult.Error(
                                "Premium product was not found. Verify the Play Console product ID."
                            )
                        )
                    } else {
                        launchBillingFlow(activity, details)
                    }
                }
            }
        }
    }

    fun restorePurchases(onResult: (PurchaseResult) -> Unit) {
        syncPremiumStatus { isPremium ->
            if (isPremium) {
                onResult(PurchaseResult.Success(isRestored = true))
            } else {
                onResult(PurchaseResult.Error("No premium purchase was found for this account."))
            }
        }
    }

    fun syncPremiumStatus(onResult: ((Boolean) -> Unit)? = null) {
        val context = appContext
        if (context == null) {
            onResult?.invoke(Prefs.isPremiumUnlocked())
            return
        }

        ensureConnection(context) { isReady ->
            if (!isReady) {
                onResult?.invoke(Prefs.isPremiumUnlocked())
                return@ensureConnection
            }

            billingClient?.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ) { billingResult, purchases ->
                if (billingResult.responseCode != BillingResponseCode.OK) {
                    Log.w(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
                    onResult?.invoke(Prefs.isPremiumUnlocked())
                    return@queryPurchasesAsync
                }

                handlePurchases(purchases, onResult)
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                val result = purchases.orEmpty()
                if (result.isEmpty()) {
                    completePurchase(PurchaseResult.Cancelled)
                    return
                }

                handlePurchases(result) { isPremium ->
                    if (isPremium) {
                        completePurchase(PurchaseResult.Success())
                    } else {
                        completePurchase(PurchaseResult.Error("Premium purchase was not completed."))
                    }
                }
            }

            BillingResponseCode.USER_CANCELED -> completePurchase(PurchaseResult.Cancelled)

            else -> {
                completePurchase(
                    PurchaseResult.Error(
                        billingResult.debugMessage.ifBlank { "Unable to complete purchase." }
                    )
                )
            }
        }
    }

    private fun ensureConnection(context: Context, onReady: (Boolean) -> Unit) {
        val client = billingClient
        if (client?.isReady == true) {
            onReady(true)
            return
        }

        readyCallbacks += onReady

        if (isConnecting) return

        isConnecting = true
        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                val isReady = billingResult.responseCode == BillingResponseCode.OK
                if (!isReady) {
                    Log.w(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }

                readyCallbacks.toList().forEach { callback -> callback(isReady) }
                readyCallbacks.clear()
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    private fun queryPremiumProductDetails(onResult: ((ProductDetails?) -> Unit)? = null) {
        val client = billingClient
        if (client?.isReady != true) {
            onResult?.invoke(null)
            return
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildConfig.PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingResponseCode.OK) {
                Log.w(TAG, "Failed to query product details: ${billingResult.debugMessage}")
                onResult?.invoke(null)
                return@queryProductDetailsAsync
            }

            premiumProductDetails = productDetailsList.firstOrNull()
            onResult?.invoke(premiumProductDetails)
        }
    }

    private fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    private fun handlePurchases(
        purchases: List<Purchase>,
        onHandled: ((Boolean) -> Unit)? = null
    ) {
        val hasPremium = purchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(BuildConfig.PREMIUM_PRODUCT_ID)
        }

        Prefs.setPremiumUnlocked(hasPremium)
        purchases.forEach(::acknowledgeIfNeeded)
        onHandled?.invoke(hasPremium)
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (
            purchase.purchaseState != Purchase.PurchaseState.PURCHASED ||
            purchase.isAcknowledged ||
            !purchase.products.contains(BuildConfig.PREMIUM_PRODUCT_ID)
        ) {
            return
        }

        billingClient?.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        ) { billingResult ->
            if (billingResult.responseCode != BillingResponseCode.OK) {
                Log.w(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    private fun completePurchase(result: PurchaseResult) {
        purchaseCallback?.invoke(result)
        purchaseCallback = null
    }
}
