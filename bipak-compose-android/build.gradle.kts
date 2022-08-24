plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
}

repositories {
    mavenCentral()
    google()
}

android {
    compileSdk = Versions.androidCompileSdk
    namespace = "fr.haan.bipak.compose.android"

    defaultConfig {
        minSdk = Versions.androidMinSdk
        targetSdk = Versions.androidTargetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xexplicit-api=warning"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.2.0"
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(project(":bipak-core"))

    implementation("androidx.core:core-ktx:${Versions.androidxCore}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutinesVersion}")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("com.google.android.material:material:1.6.1")

    implementation("androidx.compose.foundation:foundation:1.1.1")
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
    publications {

        register<MavenPublication>("release") {
            artifact(javadocJar)

            pom {
                name.set("BiPaK Android")
                description.set("Android Jetpack Compose support for BiPaK")
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
}
