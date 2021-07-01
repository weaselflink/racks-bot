@file:Suppress("LocalVariableName")

rootProject.name = "numbsi-bot"

pluginManagement {
    val kotlin_version: String by settings
    val versions_version: String by settings
    plugins {
        kotlin("jvm") version kotlin_version
        id("com.github.ben-manes.versions") version versions_version
    }
}

