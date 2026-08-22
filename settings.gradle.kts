import org.spongepowered.gradle.vanilla.repository.MinecraftRepositoryExtension

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "sponge"
        }
    }
    plugins {
        id("org.spongepowered.gradle.vanilla") version "0.3.2"
    }
}

plugins {
    id("org.spongepowered.gradle.vanilla")
}

rootProject.name = "craftbook-ultimate"

include("core", "paper", "sponge")

project(":core").name = "craftbook-ultimate-core"
project(":paper").name = "craftbook-ultimate-paper"
project(":sponge").name = "craftbook-ultimate-sponge"

// VanillaGradle would otherwise add its repositories to the one project that asks for Minecraft,
// and a project declaring any repository of its own ignores the ones settled here — which loses
// Paper's and Sponge's. Registering Minecraft's alongside the rest keeps one list.
extensions.configure(MinecraftRepositoryExtension::class) {
    injectRepositories(false)
}

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
