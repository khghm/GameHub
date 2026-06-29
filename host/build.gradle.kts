plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.gamehub.host"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gamehub.host"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SERVER_IP", "\"192.168.213.154\"") // مقدار پیش‌فرض
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":games:tictactoe"))
    implementation(project(":games:uno"))
    implementation(project(":games:connectfour"))
    implementation(project(":games:ludo"))
    implementation(project(":games:monopoly"))
    implementation(project(":games:chess"))
    implementation(project(":games:farkle"))
    implementation(project(":games:esmofamil"))
    implementation(project(":games:backgammon"))
    implementation(project(":games:abalone"))
    implementation(project(":games:spades-baloot"))
    implementation(project(":games:othello"))
    implementation(project(":games:baltazar"))
    implementation(project(":games:bridge"))
    implementation(project(":games:checkers"))
    implementation(project(":games:blokus"))
    implementation(project(":games:yahtzee"))
    implementation(project(":games:nard"))
    implementation(project(":games:hex"))
    implementation(project(":games:battleship"))
    implementation(project(":games:match-monster"))
    implementation(project(":games:soccer-striker"))
    // Compose using BOM
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx")
// Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("io.ktor:ktor-client-okhttp:3.0.1")
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.security:security-crypto:1.0.0")
}