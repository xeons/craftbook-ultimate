plugins {
    java
}

description = "SpongeVanilla (SpongeAPI 20) platform bindings for CraftBook Ultimate."

val core = project(":craftbook-ultimate-core")

// SpongeAPI 20 is built against Adventure 4.26.1; Paper 26.2 ships Adventure 5.2.0. The core module
// is source-compatible with both, so rather than depend on the core project — whose classes are
// compiled against Adventure 5 and would carry method references no Sponge server can satisfy —
// this module compiles the same sources itself against the Adventure the server actually has.
sourceSets {
    main {
        java.srcDir(core.file("src/main/java"))
        resources.srcDir(core.file("src/main/resources"))
    }
}

dependencies {
    compileOnly(rootProject.libs.sponge.api)
    annotationProcessor(rootProject.libs.sponge.api)

    // Pinned below the version core declares, because the server supplies these and the jar has to
    // agree with what it will find there.
    implementation(platform(rootProject.libs.adventure4.bom))
    implementation(rootProject.libs.adventure.api)
    implementation(rootProject.libs.adventure.serializer.plain)
    implementation(rootProject.libs.adventure.serializer.legacy)
    implementation(rootProject.libs.jspecify)

    testImplementation(platform(rootProject.libs.junit.bom))
    testImplementation(rootProject.libs.junit.jupiter)
    testImplementation(rootProject.libs.assertj)
    testImplementation(rootProject.libs.sponge.api)
    testRuntimeOnly(rootProject.libs.junit.platform.launcher)
}

// SpongeAPI 20 is a snapshot line, so a build that resolved yesterday is not evidence that today's
// will. Refusing to cache it means a broken upstream shows up as a failed resolve rather than as a
// jar built against something no server is running.
configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

tasks.jar {
    archiveBaseName.set("CraftBookUltimate-Sponge")
}
