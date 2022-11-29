import org.jetbrains.kotlin.gradle.plugin.mpp.Framework.BitcodeEmbeddingMode.BITCODE

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.rickclephas.kmp.nativecoroutines") version "0.13.1"
}

kotlin {
    jvm() {
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
    ios()

    // val xcf = XCFramework("SampleCommon")

    listOf(
        iosArm64(),
        iosX64(),
        watchosArm32(),
        watchosArm64(),
        watchosX64(),
        tvosArm64(),
        tvosX64(),
        macosX64(),
        macosArm64(),
        iosSimulatorArm64(),
        watchosSimulatorArm64(),
        tvosSimulatorArm64(),
    ).forEach {
        it.binaries {
            framework {
                export(project(":bipak-core"))
                isStatic = false
                baseName = "SampleCommon"
                transitiveExport = true
                // xcf.add(this)
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":bipak-core"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
                implementation("app.cash.turbine:turbine:0.8.0")
            }
        }
        val iosMain by getting
    }

    cocoapods {
        // Configure fields required by CocoaPods.
        version = "1.0"
        summary = "BiPaK"
        homepage = "https://github.com/nicolashaan/BiPaK"
        ios.deploymentTarget = "13"
        osx.deploymentTarget = "11"
        podfile = project.file("../bipak-sample-swiftui/Podfile")

        framework {
            // Required properties
            // Framework name configuration. Use this property instead of deprecated 'frameworkName'
            baseName = "SampleCommon"

            // Optional properties
            // Dynamic framework support
            isStatic = false
            // Dependency export
            export(project(":bipak-core"))
            transitiveExport = false // This is default.
            // Bitcode embedding
            embedBitcode(BITCODE)
        }
    }
}
