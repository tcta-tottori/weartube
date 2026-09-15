import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
}

// YouTube Data API キーはソースに書かない（CLAUDE.md）。
// local.properties の YOUTUBE_API_KEY → 環境変数 YOUTUBE_API_KEY（CI の secret）の順で探し、無ければ空文字。
val youtubeApiKey: String =
    run {
        val props = Properties()
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use(props::load)
        props.getProperty("YOUTUBE_API_KEY") ?: System.getenv("YOUTUBE_API_KEY") ?: ""
    }

android {
    namespace = "com.kazuya.weartube"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kazuya.weartube"
        minSdk = 30
        targetSdk = 35
        // CI では GITHUB_RUN_NUMBER をビルド番号にする（設定画面で見分ける）
        val ciRun = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = ciRun
        versionName = "0.1.$ciRun"

        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
    }

    // CI で毎回鍵が変わると上書きインストールできないため、リポジトリの固定鍵で署名する（keystore/README.md）
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
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear.input)
    implementation(libs.wear.tooling.preview)

    // YouTube Data API v3（design.md 5.2: Retrofit + kotlinx.serialization）
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    // サムネイル表示
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
