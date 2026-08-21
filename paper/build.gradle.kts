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

// The catalogue is the only honest source for what chips exist, and it touches no server API, so
// the page is written by running it rather than by anybody keeping a list in step by hand.
tasks.register<JavaExec>("generateIcDocs") {
    group = "documentation"
    description = "Writes docs/ics.md from the IC catalogue."
    mainClass.set("com.xeonproductions.craftbookultimate.paper.docs.GenerateIcDocs")
    classpath = sourceSets["main"].runtimeClasspath + configurations["compileClasspath"]
    args(rootProject.file("docs/ics.md").absolutePath)
}

// A test reads the generated catalogue page, so a change to it has to make that test run again.
// Without this Gradle sees only unchanged sources and skips the one check that would notice the
// page has drifted.
tasks.test {
    inputs.file(rootProject.file("docs/ics.md")).withPropertyName("icDocs")
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
