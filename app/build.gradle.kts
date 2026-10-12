import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// The private release key lives OUTSIDE this project (so it never ends up in a backup or a shared
// folder by accident): ~/Documents/budgeit-signing/. BACK THAT FOLDER UP. If the key is lost, nobody
// can ever update the app over an installed copy.
val releaseKeyFile = File(System.getProperty("user.home"), "Documents/budgeit-signing/keystore.properties")

android {
    namespace = "com.rishabh.budgetapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rishabh.budgetapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Only exists when the key file is found, so the project still builds on a computer without it.
        if (releaseKeyFile.exists()) {
            create("release") {
                val keyProperties = Properties().apply { releaseKeyFile.inputStream().use { load(it) } }
                storeFile = file(keyProperties.getProperty("storeFile"))
                storePassword = keyProperties.getProperty("storePassword")
                keyAlias = keyProperties.getProperty("keyAlias")
                keyPassword = keyProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Shrinking removes unused code and resources, which makes the installed app much smaller.
            isMinifyEnabled = true
            isShrinkResources = true
            // Sign with our own private key when it is available (see releaseKeyFile above). Without it,
            // fall back to the debug key so the build still works (fine for testing, NOT for sharing).
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.glance.appwidget)

    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)

    // Tests that run on the emulator (database and screen tests). They are not part of the real app.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.ui.test.manifest)
}
