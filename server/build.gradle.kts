plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}



dependencies {
    implementation(project(":shared"))
    implementation(project(":games:tictactoe"))
    implementation(project(":games:uno"))
    implementation("io.ktor:ktor-server-core:3.0.1")
    implementation("io.ktor:ktor-server-netty:3.0.1")
    implementation("io.ktor:ktor-server-websockets:3.0.1")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
    implementation("io.ktor:ktor-server-cors:3.0.1")
    // Observability & Resilience
    implementation("io.ktor:ktor-server-metrics-micrometer:3.0.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.1")
    implementation("io.github.resilience4j:resilience4j-kotlin:2.2.0")
    implementation("io.github.resilience4j:resilience4j-micrometer:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
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
    implementation("io.ktor:ktor-server-auth:3.0.1")
    implementation("io.ktor:ktor-server-auth-jwt:3.0.1")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.jetbrains.exposed:exposed-core:0.52.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.52.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.52.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.52.0")
    implementation("org.flywaydb:flyway-core:10.17.1")
    implementation("io.lettuce:lettuce-core:6.4.1.RELEASE")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("com.h2database:h2:2.3.232")
    implementation("io.ktor:ktor-server-status-pages:3.0.1")
    // Koin for Dependency Injection
    implementation("io.insert-koin:koin-ktor:4.1.1")
    implementation("io.insert-koin:koin-logger-slf4j:4.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.ktor:ktor-client-mock:3.0.1")
    testImplementation("io.ktor:ktor-client-okhttp:3.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.gamehub.server.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}