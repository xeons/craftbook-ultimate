rootProject.name = "craftbook-ultimate"

include("core", "paper")

project(":core").name = "craftbook-ultimate-core"
project(":paper").name = "craftbook-ultimate-paper"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}
