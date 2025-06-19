plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.rehman.docscan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rehman.docscan"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.4"

        // Update what's new notes in distribution/whatsNew/whatsnew-en-US

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Go to Gradle.properties and set RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD.
        create("release") {
            if (project.hasProperty("RELEASE_STORE_FILE")) {
                storeFile = file("$rootDir/${project.property("RELEASE_STORE_FILE")}")
            }
            storePassword = project.property("RELEASE_STORE_PASSWORD") as String
            keyAlias = project.property("RELEASE_KEY_ALIAS") as String
            keyPassword = project.property("RELEASE_KEY_PASSWORD") as String
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "DocScan Debug")
            signingConfig = signingConfigs.getByName("debug")

        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            resValue("string", "app_name", "DocScan")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase crashlytics for app
    implementation(libs.firebase.crashlytics)

    //Splash screen android 11+
    implementation(libs.androidx.core.splashscreen)

    // Lottie
    implementation(libs.lottie)

    // Responsive
    implementation(libs.sdp.android)
    implementation(libs.ssp.android)


    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Ml Kit document scanner
    implementation(libs.play.services.mlkit.document.scanner)

    //Glide for images
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    // Swipe refresh layout
    implementation(libs.androidx.swiperefreshlayout)

    //Image Viewer
    implementation(libs.stfalconimageviewer)

    // Play store in app update
    implementation(libs.app.update.ktx)


}