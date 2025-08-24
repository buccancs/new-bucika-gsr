plugins {
    kotlin("jvm")
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.topdon.bucika"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.8.0")
    
    // JSON processing
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.1")
    
    // WebSocket server
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    
    // mDNS discovery
    implementation("javax.jmdns:jmdns:3.5.12")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.8")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

javafx {
    version = "21"
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("com.topdon.bucika.pc.BucikaOrchestratorKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}