plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.reactivate.caraudiopro"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.reactivate.caraudiopro"
        minSdk = 28
        targetSdk = 33
        versionCode = 13
        versionName = "4.5"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = false }
}
