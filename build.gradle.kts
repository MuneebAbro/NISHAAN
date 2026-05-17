// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.navigation.safeargs) apply false
}