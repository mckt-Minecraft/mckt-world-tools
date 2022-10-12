import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.7.20"
    id("io.ktor.plugin") version "2.1.1"
}

group = "io.github.mckt-minecraft"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("io.github.mcktminecraft.worldtools.MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "io.github.mcktminecraft.worldtools.MainKt"
        attributes["Multi-Release"] = true
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.kyori:adventure-nbt:4.11.0")

    implementation("it.unimi.dsi:fastutil-core:8.5.9")

    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")

    implementation("com.squareup.okio:okio:3.2.0")

    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}