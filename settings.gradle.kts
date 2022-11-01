pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        val kotlinVersion: String by settings
        val androidPluginVersion: String by settings
        id("com.android.application") version androidPluginVersion
        id("com.android.library") version androidPluginVersion
        id("org.jetbrains.kotlin.android") version kotlinVersion
        id("kotlin-android") version kotlinVersion
        kotlin("multiplatform") version kotlinVersion
        id("kotlin-gradle-plugin") version kotlinVersion
        id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
        id("org.jetbrains.dokka") version kotlinVersion
        id("maven-publish")
        id("signing")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

rootProject.name = "bipak-project"
include(":bipak-core")
include(":bipak-android")
include(":bipak-compose-android")
include("samples:bipak-sample-android")
include("samples:bipak-sample-shared")
