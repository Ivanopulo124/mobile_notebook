plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ivanopulo.planner"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ivanopulo.planner"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.0"

        // Public client ids, not secrets; a build without one hides that sync provider.
        val dropboxAppKey = providers.gradleProperty("PLANNER_DROPBOX_APP_KEY").getOrElse("")
        val yandexClientId = providers.gradleProperty("PLANNER_YANDEX_CLIENT_ID").getOrElse("")
        buildConfigField("String", "DROPBOX_APP_KEY", "\"$dropboxAppKey\"")
        buildConfigField("String", "YANDEX_CLIENT_ID", "\"$yandexClientId\"")
        manifestPlaceholders["dropboxAppKey"] = dropboxAppKey
        // The Yandex ID SDK manifest expects this exact placeholder name.
        manifestPlaceholders["YANDEX_CLIENT_ID"] = yandexClientId
    }

    // The release key lives outside the repo; machines without it still build, just unsigned.
    val releaseStoreFile = providers.gradleProperty("PLANNER_RELEASE_STORE_FILE").orNull
    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.gradleProperty("PLANNER_RELEASE_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("PLANNER_RELEASE_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("PLANNER_RELEASE_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.yandex.authsdk)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
