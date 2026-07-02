// Top-level build file. Plugins are declared here (apply false) and applied per-module.

// AGP 9 trae Kotlin INTEGRADO (ya no se aplica `org.jetbrains.kotlin.android`); la versión
// del compilador se fija subiendo el Kotlin Gradle Plugin en el classpath del buildscript.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}
