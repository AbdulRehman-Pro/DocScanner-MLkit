package com.rehman.docscan.ui.mainScreen.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.rehman.docscan.R
import com.rehman.docscan.adapter.ImagesAdapter
import com.rehman.docscan.core.Utils
import com.rehman.docscan.databinding.FragmentExploreBinding
import com.rehman.docscan.interfaces.AdapterClickInterface
import com.rehman.docscan.interfaces.SnackBarListener
import com.stfalcon.imageviewer.StfalconImageViewer

class ExploreFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, AdapterClickInterface {

    private lateinit var binding: FragmentExploreBinding
    private lateinit var imagesAdapter: ImagesAdapter

    private lateinit var viewer: StfalconImageViewer<Uri>

    private var callback: SnackBarListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ensure the host activity implements the interface
        if (context is SnackBarListener) {
            callback = context
        } else {
            throw RuntimeException("$context must implement MyFragmentCallback")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentExploreBinding.inflate(inflater, container, false)
        binding.swipeRefresh.setOnRefreshListener(this)
        binding.swipeRefresh.setColorSchemeResources(
            R.color.color_secondary,
            R.color.color_on_primary,
            R.color.color_secondary
        )
        return binding.root
    }


    override fun onResume() {
        super.onResume()
        getImagesList()
    }

    private fun getImagesList() {
        val imageUris = Utils.getImagesFromMediaStore(requireContext())
        if (imageUris.isEmpty()) {
            binding.noFileLayout.visibility = View.VISIBLE
            binding.imagesRV.visibility = View.GONE
        } else {
            initRecyclerview(imageUris)
        }
    }

    private fun initRecyclerview(list: ArrayList<Uri>) {
        binding.noFileLayout.visibility = View.GONE
        binding.imagesRV.visibility = View.VISIBLE
        imagesAdapter = ImagesAdapter(requireContext(), this, list)
        binding.imagesRV.adapter = imagesAdapter

    }

    override fun onRefresh() {
        binding.swipeRefresh.isRefreshing = false
        getImagesList()
    }

    override fun itemClick(imageUris: ArrayList<Uri>, position: Int, itemImage: ImageView) {

        viewer = StfalconImageViewer.Builder<Uri>(context, imageUris) { view, uri ->
            Glide.with(requireContext()).load(uri).into(view)
        }.withStartPosition(position)
            .withTransitionFrom(itemImage)
            .withImageChangeListener { newPosition ->
                val newTransitionView = findRecyclerViewImageViewAt(newPosition)
                if (newTransitionView != null) {
                    viewer.updateTransitionImage(newTransitionView)
                }
            }
            .withHiddenStatusBar(false)
            .show()
    }

    private fun findRecyclerViewImageViewAt(position: Int): ImageView? {
        val viewHolder = binding.imagesRV.findViewHolderForAdapterPosition(position)
        return viewHolder?.itemView?.findViewById(R.id.itemImage)
    }


    override fun shareClick(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Check out this image, scanned by DocScan.")
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Doc Share"))
    }

    override fun deleteClick(uri: Uri, position: Int) {
        if (Utils.deleteFileFromUri(requireContext(), uri)) {
            callback?.showSnackBar(
                "Image deleted successfully.",
                R.color.color_on_primary,
                R.color.color_on_secondary,
                2000L
            )
            imagesAdapter.itemRemoved(uri, position)
            if (imagesAdapter.itemCount == 0) {
                binding.noFileLayout.visibility = View.VISIBLE
                binding.imagesRV.visibility = View.GONE
            }
        } else {
            callback?.showSnackBar(
                "Failed to delete image.",
                R.color.color_error,
                R.color.color_text_primary,
                2000L
            )

        }
    }


}