buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        // TODO: Can we do better to share versions with settings.gradle.kts?
        val kotlinVersion: String = project.properties["kotlinVersion"] as String
        val androidPluginVersion: String = project.properties["androidPluginVersion"] as String
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.android.tools.build:gradle:$androidPluginVersion")
    }
}

plugins {
    id("org.jlleitschuh.gradle.ktlint") apply true
    id("org.jetbrains.dokka")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory.set(projectDir.resolve("docs"))
}

nexusPublishing {
    repositories {
        sonatype {
            stagingProfileId.set(project.properties["sonatypeStagingProfileId"].toString())
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}