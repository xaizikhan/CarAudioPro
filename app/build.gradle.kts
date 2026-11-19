plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.reactivate.caraudiopro"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.reactivate.caraudiopro"
        minSdk = 28          // DynamicsProcessing API
        targetSdk = 33       // Android 13
        versionCode = 2
        versionName = "0.2-poc"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = false }
}
