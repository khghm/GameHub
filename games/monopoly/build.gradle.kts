plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }

    jvm("server")

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            // Compose using BOM
            implementation(project.dependencies.platform(libs.compose.bom))
            implementation("androidx.compose.ui:ui")
            implementation("androidx.compose.material3:material3")
            implementation("androidx.compose.runtime:runtime")
            implementation("androidx.compose.foundation:foundation")
            implementation("androidx.compose.material3:material3:1.2.1")
            implementation("androidx.compose.ui:ui:1.5.0")
            implementation("androidx.compose.animation:animation:1.5.0")
            // در commonMain dependencies
            implementation("androidx.compose.material:material-icons-extended:1.6.0")  // یا نسخه متناسب با compose شما
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.gamehub.games.monopoly"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}