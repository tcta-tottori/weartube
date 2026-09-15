plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.kazuya.weartube.shared"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)
    api(libs.datastore.preferences)

    // YouTube Data API v3
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)

    // Wearable Data Layer（スマホ ⇔ 時計の設定・お気に入り同期）
    api(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
}
