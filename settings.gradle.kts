pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// Disabled: causes toolchain download issues
// plugins {
//     id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
// }

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AutoPlayer"
include(":app")
