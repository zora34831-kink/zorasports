
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.zora.sports"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zora.sports"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.3.0"

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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Jetpack Compose
    implementation(
        platform("androidx.compose:compose-bom:2025.11.00")
    )
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Android Core
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:35.2.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    // Media3 Player
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.6.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.6.1")

    // Coil Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
