import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.upagain"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.upagain"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties().apply {
            val propertiesFile = rootProject.file("local.properties")
            if (propertiesFile.exists()) {
                load(FileInputStream(propertiesFile))
            }
        }

        // 2. Inject the variable into BuildConfig
        buildConfigField("String", "API_BASE_URL", properties.getProperty("API_BASE_URL"))
        buildConfigField("String", "MAP_API_KEY", properties.getProperty("MAP_API_KEY"))
        buildConfigField("String", "FRONTEND_BASE_URL", properties.getProperty("FRONTEND_BASE_URL"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("com.auth0.android:jwtdecode:2.0.2")
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    // Pour les Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    // 2. Coil (Image Loading)
    implementation(libs.coil.compose)

    // 3. OkHttp (Networking)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // 4. EncryptedSharedPreferences (Security)
    implementation(libs.androidx.security.crypto)
}