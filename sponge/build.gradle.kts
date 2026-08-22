plugins {
    java
    id("org.spongepowered.gradle.vanilla")
    id("org.spongepowered.gradle.plugin")
}

// The server jar, under Mojang's own names, so the native layer can call into it. SpongeVanilla
// puts plugins on the game module layer, so what is compiled against here is what is there at run
// time; nothing is shaded and nothing is downloaded by the plugin itself.
minecraft {
    version("26.2")
    platform(org.spongepowered.gradle.vanilla.repository.MinecraftPlatform.SERVER)
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

// SpongeGradle declares repositories on this project, and a project declaring any of its own
// ignores the ones settled in settings.gradle.kts. So this module carries the whole list rather
// than half of it; the other two still take theirs from the settings.
repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    compileOnly(rootProject.libs.sponge.api)
    annotationProcessor(rootProject.libs.sponge.api)

    // Mixin is here so that one can be written the day something needs the game's own behaviour
    // changed rather than merely asked. Nothing declares a config yet, and until something does the
    // jar carries no MixinConfigs attribute — see the comment on the jar task. Its annotation
    // processor, which writes the refmap, is deliberately not on yet: it drags Guava onto the
    // processor path and has nothing to process until there is a mixin to process.
    compileOnly(rootProject.libs.mixin)

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

// The plugin's own metadata, which is how SpongeVanilla finds the entrypoint at all. Declared here
// rather than written out by hand so that the version and the API it asks for are the ones actually
// built against; SpongeGradle generates META-INF/sponge_plugins.json from it. This is what the
// annotation processor did on SpongeAPI 7 — the API's only processor now is the one checking that
// an @Listener method is one the server could call.
sponge {
    apiVersion(rootProject.libs.versions.sponge.get())
    license("GPL-3.0-or-later")
    loader {
        name("java_plain")
        version("1.0")
    }
    plugin("craftbookultimate") {
        displayName("CraftBook Ultimate")
        entrypoint("com.xeonproductions.craftbookultimate.sponge.CraftBookSponge")
        description("Redstone integrated circuits built by writing a model number on a sign.")
        contributor("Brandon Scott") {}
        // Said outright rather than left implied, so a server refuses to load this against an API
        // it was not built for instead of failing somewhere further in.
        dependency("spongeapi") {
            version(rootProject.libs.versions.sponge.get())
            optional(false)
        }
    }
}

// SpongeAPI 20 is a snapshot line, so a build that resolved yesterday is not evidence that today's
// will. Refusing to cache it means a broken upstream shows up as a failed resolve rather than as a
// jar built against something no server is running.
configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

// SpongeVanilla reads MixinConfigs off the jar manifest and warns the operator, every start-up,
// that the plugin modifies the Minecraft server. That warning is worth paying only once something
// actually mixes in, so the attribute appears when a config does and not before. Writing it for an
// empty config would frighten an operator on behalf of nothing.
val mixinConfigs = fileTree("src/main/resources") { include("mixins.*.json") }

tasks.jar {
    archiveBaseName.set("CraftBookUltimate-Sponge")
    inputs.files(mixinConfigs).withPropertyName("mixinConfigs")
    doFirst {
        val declared = mixinConfigs.files.map { it.name }.sorted()
        if (declared.isNotEmpty()) {
            manifest.attributes("MixinConfigs" to declared.joinToString(","))
        }
    }
}
