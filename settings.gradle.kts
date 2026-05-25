pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Hue"

include(":app")
include(":core:core-color")
include(":core:core-design")
include(":data:pantone-db")
include(":domain")
include(":feature:feature-capture")
include(":feature:feature-matching")
include(":feature:feature-season")
include(":feature:feature-history")
