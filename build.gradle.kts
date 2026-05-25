// Top-level build file – configuration shared across subprojects lives here.
plugins {
    alias(libs.plugins.android.application)     apply false
    alias(libs.plugins.android.library)         apply false
    alias(libs.plugins.kotlin.android)          apply false
    alias(libs.plugins.kotlin.kapt)             apply false
    alias(libs.plugins.hilt)                    apply false
    alias(libs.plugins.ksp)                     apply false
}
