plugins {
    java
}

description = "Paper 26.x platform bindings for CraftBook Ultimate."

val core = project(":craftbook-ultimate-core")

dependencies {
    implementation(core)
    compileOnly(rootProject.libs.paper.api)

    testImplementation(platform(rootProject.libs.junit.bom))
    testImplementation(rootProject.libs.junit.jupiter)
    testImplementation(rootProject.libs.assertj)
    testImplementation(rootProject.libs.paper.api)
    testRuntimeOnly(rootProject.libs.junit.platform.launcher)
}

tasks.processResources {
    filesMatching("paper-plugin.yml") {
        expand("version" to project.version)
    }
}

// The server supplies Adventure and JSpecify, so the only thing the jar has to carry beyond its
// own classes is the platform-independent core module.
tasks.jar {
    archiveBaseName.set("CraftBookUltimate")
    from(core.sourceSets.main.map { it.output })
}
