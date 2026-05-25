plugins {
    alias(libs.plugins.android.application)     apply false
    alias(libs.plugins.android.library)         apply false
    alias(libs.plugins.kotlin.android)          apply false
    alias(libs.plugins.kotlin.kapt)             apply false
    alias(libs.plugins.hilt)                    apply false
    alias(libs.plugins.ksp)                     apply false
    jacoco
}

// ── Aggregate JaCoCo coverage across all modules ──────────────────────────

val jacocoAggregateExcludes = listOf(
    // Android-generated
    "**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "**/*Test*.*", "**/test/**", "**/androidTest/**",
    // DI boilerplate
    "**/*_HiltModules*", "**/*_Factory*", "**/*_MembersInjector*",
    "**/*Module_*", "**/*Component*",
    // Compose-generated
    "**/*ComposableSingletons*",
    // Room-generated
    "**/*_Impl*", "**/*Dao_Impl*", "**/*Database_Impl*",
    // Data-binding / ViewBinding
    "**/*Binding*",
    // Sealed classes / enums (trivially covered)
    "**/*Kt*",
    // Navigation-generated
    "**/*Directions*", "**/*Args*"
)

// Modules that carry testable business logic
val coverageModules = listOf(
    ":core:core-color",
    ":domain",
    ":data:pantone-db",
    ":feature:feature-matching",
    ":feature:feature-history",
    ":feature:feature-capture"
)

tasks.register<JacocoReport>("jacocoFullReport") {
    group = "verification"
    description = "Generates aggregated JaCoCo coverage report across all modules."

    dependsOn(subprojects.mapNotNull { sub ->
        sub.tasks.findByName("testDebugUnitTest")
    })

    val sourceDirs = coverageModules.flatMap { modulePath ->
        val sub = project(modulePath)
        listOf(
            File("${sub.projectDir}/src/main/kotlin"),
            File("${sub.projectDir}/src/main/java")
        )
    }.filter { it.exists() }

    val classDirs = coverageModules.flatMap { modulePath ->
        val sub = project(modulePath)
        fileTree("${sub.buildDir}/tmp/kotlin-classes/debug") { exclude(jacocoAggregateExcludes) } +
        fileTree("${sub.buildDir}/intermediates/javac/debug") { exclude(jacocoAggregateExcludes) }
    }

    val execFiles = coverageModules.flatMap { modulePath ->
        val sub = project(modulePath)
        fileTree("${sub.buildDir}/jacoco") { include("**/*.exec") }
    }

    sourceDirectories.setFrom(sourceDirs)
    classDirectories.setFrom(classDirs)
    executionData.setFrom(execFiles)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    group = "verification"
    description = "Fails build if overall instruction coverage is below 70%."
    dependsOn("jacocoFullReport")

    val coverageReport = tasks.named<JacocoReport>("jacocoFullReport").get()
    executionData(coverageReport.executionData)
    sourceDirectories.setFrom(coverageReport.sourceDirectories)
    classDirectories.setFrom(coverageReport.classDirectories)

    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value   = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}
