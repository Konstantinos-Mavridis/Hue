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

    val execFiles = coverageModules.flatMap { modulePath ->
        val sub = project(modulePath)
        val buildDir = sub.layout.buildDirectory.get().asFile
        fileTree(buildDir) { include("**/*.exec") }
    }

    sourceDirectories.setFrom(sourceDirs)
    executionData.setFrom(execFiles)

    // classDirectories is set at configuration time via project.files(Callable).
    //
    // The Callable is evaluated lazily at execution time — after testDebugUnitTest
    // has run and the library JARs exist. The setFrom() call itself happens at
    // configuration time (before the property is finalized), so Gradle's
    // "value is final" constraint is never violated.
    //
    // In AGP 9.x / K2, compileDebugKotlin writes Kotlin classes into:
    //   intermediates/compile_library_classes_jar/debug/<taskName>/classes.jar
    // JaCoCo requires individual .class entries; zipTree() unpacks each JAR.
    classDirectories.setFrom(
        project.files(java.util.concurrent.Callable {
            coverageModules.flatMap { modulePath ->
                val sub = project(modulePath)
                val buildDir = sub.layout.buildDirectory.get().asFile

                // Unpack compiled Kotlin library JARs (AGP 9.x / K2 path)
                val unpackedKotlin = fileTree(buildDir) {
                    include("intermediates/compile_library_classes_jar/**/*.jar")
                }.map { jar ->
                    zipTree(jar).matching { exclude(jacocoAggregateExcludes) }
                }

                // Java source compilation outputs (Room DAOs, Hilt stubs, etc.)
                val javaClasses = fileTree(buildDir) {
                    include("intermediates/javac/debug/**/*.class")
                    exclude(jacocoAggregateExcludes)
                }

                // Older AGP fallback — individual Kotlin class files
                val legacyKotlinClasses = fileTree(buildDir) {
                    include("tmp/kotlin-classes/debug/**/*.class")
                    exclude(jacocoAggregateExcludes)
                }

                unpackedKotlin + listOf(javaClasses, legacyKotlinClasses)
            }
        })
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    // Diagnostic: visible in CI logs so path issues can be diagnosed quickly.
    doFirst {
        val execCount = executionData.files.size
        val classCount = classDirectories.asFileTree.files.size
        logger.lifecycle("[jacocoFullReport] exec files  : $execCount")
        executionData.files.sortedBy { it.absolutePath }
            .forEach { logger.lifecycle("  exec  → $it (${it.length()} B)") }
        logger.lifecycle("[jacocoFullReport] class files : $classCount")
        classDirectories.asFileTree.files.take(20).sortedBy { it.absolutePath }
            .forEach { logger.lifecycle("  class → $it") }
        if (classCount > 20) logger.lifecycle("  ... and ${classCount - 20} more")
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
