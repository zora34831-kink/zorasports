
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.zora.sports"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zora.sports"
        minSdk = 23
        targetSdk = 35

        versionCode = 3
        versionName = "1.1.0"

        buildConfigField(
            "String",
            "FOOTBALL_API_KEY",
            "\"${project.findProperty("FOOTBALL_API_KEY") ?: ""}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    // Jetpack Compose
    val composeBom =
        platform("androidx.compose:compose-bom:2025.11.00")

    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Retrofit API
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:5.3.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
