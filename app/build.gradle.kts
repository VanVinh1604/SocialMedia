plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.socialmedia"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.socialmedia.project"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions { jvmTarget = "11" }

    buildFeatures {  viewBinding = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")

    // Firebase BOM quản lý version
    // Thử cách này - version cụ thể, KHÔNG dùng BOM
    implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")
    implementation("com.google.firebase:firebase-analytics-ktx:22.1.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.glide)
    kapt(libs.glide.compiler)
    implementation(libs.gson)

    // Extra
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
}
