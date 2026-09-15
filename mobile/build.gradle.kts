import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
}

// 時計側と同じく、キーは local.properties → 環境変数 YOUTUBE_API_KEY の順で探す
val youtubeApiKey: String =
    run {
        val props = Properties()
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use(props::load)
        props.getProperty("YOUTUBE_API_KEY") ?: System.getenv("YOUTUBE_API_KEY") ?: ""
    }

android {
    namespace = "com.kazuya.weartube.mobile"
    compileSdk = 37

    defaultConfig {
        // Data Layer で通信するため、時計版と同じ applicationId・同じ署名にする
        applicationId = "com.kazuya.weartube"
        minSdk = 26
        targetSdk = 35
        val ciRun = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = ciRun
        versionName = "0.1.$ciRun"

        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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

dependencies {
    implementation(project(":shared"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
