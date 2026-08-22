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
    }
}
