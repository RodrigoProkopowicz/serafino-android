plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Procesa `app/google-services.json` y activa Firebase (Auth + Analytics). Si el archivo
    // no está, el build sigue (el plugin solo advierte); en runtime se detecta con FirebaseApp.
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.serafino.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rodrigoprokopowicz.serafino"
        minSdk = 26
        targetSdk = 37
        // En sync con la versión de marketing de iOS (auditoría pre-producción 1.0.3).
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(project(":architecture"))
    implementation(project(":domain"))
    implementation(project(":designsystem"))
    implementation(project(":data"))
    implementation(project(":feature:recipes"))
    implementation(project(":feature:store"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    // BackendLoyaltyProvider expone un OkHttpClient inyectable en su constructor; el composition
    // root lo referencia (rama gated), así que okhttp debe estar en el compile classpath del app.
    implementation(libs.okhttp)

    // Firebase real: Auth (Sign in with Apple vía OAuthProvider) + Analytics (GA4). El BoM fija
    // versiones compatibles. `play-services` da `Task.await()` para puentear a corrutinas.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.kotlinx.coroutines.play.services)
    // Sign in with Google (Credential Manager).
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
