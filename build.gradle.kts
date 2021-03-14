import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.github.ben-manes.versions")
}

group = "de.stefanbissell.numbsi"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ocraft:ocraft-s2client-bot:0.4.7")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
