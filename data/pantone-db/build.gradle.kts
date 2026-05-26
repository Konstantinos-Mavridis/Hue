plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    jacoco
}

android {
    namespace = "com.hue.data.pantone"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug { enableUnitTestCoverage = true }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.extensions.configure<JacocoTaskExtension> {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:core-color"))

    implementation(libs.core.ktx)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    implementation(libs.coroutines.android)
    implementation(libs.gson)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Optional Pantone Connect API
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.arch.core.testing)
}
