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
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    repositories {
        mavenCentral()
    }
//    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
//        debug.set(true)
//    }
}

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory.set(projectDir.resolve("docs"))
}
