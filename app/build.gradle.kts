plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.huyhoa.coclio"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.huyhoa.coclio"
        minSdk = 23
        targetSdk = 35
        versionCode = providers.gradleProperty("VERSION_CODE").orElse("1").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").orElse("1.0.0").get()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
