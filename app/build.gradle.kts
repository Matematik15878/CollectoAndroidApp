plugins {
    kotlin("plugin.serialization") version "2.1.0"
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
}

android {
    namespace = "com.collecto.collectoandroidapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.collecto.collectoandroidapp"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders.putIfAbsent("appAuthRedirectScheme", "com.collecto.collectoandroidapp")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.gson)
    implementation(libs.appauth)
    implementation(libs.material)
    implementation(libs.secretsmanager)
    implementation(libs.cognitoidentity)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.http.client.engine.crt)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.http.client.engine.okhttp)

    implementation("io.ktor:ktor-client-android:3.0.2")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.3"))

    implementation("com.github.bumptech.glide:glide:4.13.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.13.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}