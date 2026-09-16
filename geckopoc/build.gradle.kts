plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.kazuya.weartube.poc"
    compileSdk = 37

    defaultConfig {
        // 本体（com.kazuya.weartube）とは別アプリ。本体を軽いままにするため分けている
        applicationId = "com.kazuya.weartube.poc"
        minSdk = 30
        targetSdk = 35
        val ciRun = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = ciRun
        versionName = "0.1.$ciRun"
    }

    // GeckoView の native ライブラリは ABI ごとに 100MB 近い。両方入れると時計に入らないので 1 つずつ配る
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.geckoview)
}
