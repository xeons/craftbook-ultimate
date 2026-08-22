pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
    }
    plugins {
        id("org.spongepowered.gradle.vanilla") version "0.3.2"
        id("org.spongepowered.gradle.plugin") version "2.3.0"
    }
}

rootProject.name = "craftbook-ultimate"

include("core", "paper", "sponge")

project(":core").name = "craftbook-ultimate-core"
project(":paper").name = "craftbook-ultimate-paper"
project(":sponge").name = "craftbook-ultimate-sponge"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://libraries.minecraft.net/") {
            name = "minecraft"
        }
    }
}
