plugins {
    alias(libs.plugins.android.application)     apply false
    alias(libs.plugins.android.library)         apply false
    alias(libs.plugins.kotlin.compose)          apply false
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

    // Never pull a stale 0-coverage result from the build cache.
    outputs.cacheIf { false }

    val testTasks = coverageModules.mapNotNull { modulePath ->
        project(modulePath).tasks.findByName("testDebugUnitTest")
    }
    dependsOn(testTasks)

    val sourceDirs = coverageModules.flatMap { modulePath ->
        val sub = project(modulePath)
        listOf(
            File("${sub.projectDir}/src/main/kotlin"),
            File("${sub.projectDir}/src/main/java")
        )
    }.filter { it.exists() }

    val classDirs = coverageModules.flatMap { modulePath ->
        val sub = project(modulePath)
        val buildDir = sub.layout.buildDirectory.get().asFile
        fileTree(buildDir) {
            include(
                // AGP 9.x / K2: Kotlin classes are compiled straight into this library JAR
                "intermediates/compile_library_classes_jar/debug/classes.jar",
                // Java source files (Room DAOs, etc.) — individual class files
                "intermediates/javac/debug/**/*.class",
                "intermediates/javac/debug/classes/**/*.class",
                // Older AGP fallback paths
                "tmp/kotlin-classes/debug/**/*.class",
                "intermediates/kotlinc/debug/**/*.class",
                "intermediates/kotlin_classes/debug/**/*.class"
            )
            exclude(jacocoAggregateExcludes)
        }
    }

    // Broad exec-file search: covers all known AGP output paths and handles
    // AGP version differences in where the .exec file is written.
    val execFiles = coverageModules.flatMap { modulePath ->
        val sub = project(modulePath)
        val buildDir = sub.layout.buildDirectory.get().asFile
        fileTree(buildDir) { include("**/*.exec") }
    }

    sourceDirectories.setFrom(sourceDirs)
    classDirectories.setFrom(classDirs)
    executionData.setFrom(execFiles)

    // Diagnostic: print what was found so CI logs show the actual paths.
    doFirst {
        val execCount = executionData.files.size
        val classEntries = classDirectories.asFileTree.files
        logger.lifecycle("[jacocoFullReport] exec files  : $execCount")
        executionData.files.sortedBy { it.absolutePath }
            .forEach { logger.lifecycle("  exec → $it (${it.length()} B)") }
        logger.lifecycle("[jacocoFullReport] class entries: ${classEntries.size}")
        classEntries.sortedBy { it.absolutePath }
            .forEach { logger.lifecycle("  class → $it (${it.length()} B)") }
    }

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

    outputs.cacheIf { false }

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
