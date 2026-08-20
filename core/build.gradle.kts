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
