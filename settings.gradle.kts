import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "idea-openscad"

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.1.20"
        id("org.jetbrains.intellij.platform") version "2.18.1"
        id("org.jetbrains.changelog") version "2.5.0"
        id("org.jetbrains.kotlinx.kover") version "0.9.1"
        id("org.jetbrains.qodana") version "2025.2.1"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.18.1"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()

        intellijPlatform {
            defaultRepositories()
        }
    }
}
