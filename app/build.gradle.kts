plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.alan.citascritapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alan.citascritapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.pdfbox.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation ("androidx.datastore:datastore-preferences:1.0.0")
    implementation ("io.coil-kt:coil-compose:2.2.2")
    implementation ("androidx.core:core-ktx:1.12.0")
    implementation ("androidx.work:work-runtime-ktx:2.9.0")
    implementation ("androidx.core:core-splashscreen:1.0.1")
    implementation ("androidx.compose.material3:material3:1.2.0")
    implementation ("com.google.android.material:material:1.1.0")
    implementation ("com.google.accompanist:accompanist-swiperefresh:0.33.2-alpha")
    implementation ("io.github.vanpra.compose-material-dialogs:core:0.9.0")
    implementation ("io.github.vanpra.compose-material-dialogs:datetime:0.9.0")
    implementation ("com.jakewharton.threetenabp:threetenabp:1.4.4")
    implementation ("androidx.compose.material:material:1.6.1")
    implementation ("androidx.compose.foundation:foundation:1.6.0")
    implementation ("io.coil-kt:coil-compose:2.5.0")
    implementation ("io.coil-kt:coil-gif:2.5.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

}