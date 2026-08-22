plugins {
    `java-library`
}

description = "Platform-independent CraftBook Ultimate domain model. Contains no server API references."

dependencies {
    api(platform(rootProject.libs.adventure.bom))
    api(rootProject.libs.adventure.api)
    api(rootProject.libs.jspecify)
    implementation(rootProject.libs.adventure.serializer.plain)
    implementation(rootProject.libs.adventure.serializer.legacy)

    testImplementation(platform(rootProject.libs.junit.bom))
    testImplementation(rootProject.libs.junit.jupiter)
    testImplementation(rootProject.libs.assertj)
    testRuntimeOnly(rootProject.libs.junit.platform.launcher)
}

// The catalogue is the only honest source for what chips exist, and it touches no server API, so
// the page is written by running it rather than by anybody keeping a list in step by hand.
tasks.register<JavaExec>("generateIcDocs") {
    group = "documentation"
    description = "Writes docs/ics.md from the IC catalogue."
    mainClass.set("com.xeonproductions.craftbookultimate.core.ic.GenerateIcDocs")
    classpath = sourceSets["main"].runtimeClasspath
    args(rootProject.file("docs/ics.md").absolutePath)
}

// A test reads the generated catalogue page, so a change to it has to make that test run again.
// Without this Gradle sees only unchanged sources and skips the one check that would notice the
// page has drifted.
tasks.test {
    inputs.file(rootProject.file("docs/ics.md")).withPropertyName("icDocs")
}
