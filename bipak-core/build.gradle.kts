plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

kotlin {
    explicitApi()

    android()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }

    js() {
        nodejs()
        browser()
    }

    listOf(
        iosArm32(),
        iosArm64(),
        iosX64(),
        watchosArm32(),
        watchosArm64(),
        watchosX86(),
        watchosX64(),
        tvosArm64(),
        tvosX64(),
        macosX64("macOS"),
        macosArm64("macOSArm"),
        iosSimulatorArm64(),
        watchosSimulatorArm64(),
        tvosSimulatorArm64(),
    ).forEach {
        it.binaries {
            framework {
                baseName = "BiPaK"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutinesVersion}")
                implementation("app.cash.turbine:turbine:0.8.0")
            }
        }
    }
}

android {
    compileSdk = Versions.androidCompileSdk
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = Versions.androidMinSdk
        targetSdk = Versions.androidTargetSdk
    }
    namespace = "fr.haan.bipak"
}

version = project.properties["VERSION_NAME"] ?: "SNAPSHOT"
group = "fr.haan.bipak"

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications)
}

val javadocJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Javadoc JAR"
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaHtml"))
}

publishing {
    publications.withType(MavenPublication::class) {

        artifact(javadocJar)

        pom {
            name.set("BiPaK Core")
            description.set("Core library of BiPaK is a Kotlin multiplatform paging library.")
            url.set("https://github.com/nicolashaan/bipak")
            licenses {
                license {
                    name.set("BiPaK License")
                    url.set("https://github.com/nicolashaan/bipak/blob/main/LICENCE.md")
                }
            }
            developers {
                developer {
                    id.set("nicolashaan")
                    name.set("Nicolas Haan")
                }
            }

            scm {
                connection.set("scm:git:github.com/nicolashaan/bipak.git")
                developerConnection.set("scm:git:ssh://github.com/nicolashaan/bipak.git")
                url.set("https://github.com/nicolashaan/bipak/tree/main")
            }
        }
    }
}
