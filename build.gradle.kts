// Root build file for Auralis
// AGP 9.4.0 — built-in Kotlin 2.2.10 (do NOT apply org.jetbrains.kotlin.android)

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
