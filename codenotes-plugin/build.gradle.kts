plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.codenotes"
version = "1.0.0"

repositories {
    mavenCentral()
}

// No extra runtime dependencies beyond the IntelliJ Platform + Kotlin stdlib,
// so the plugin has zero external attack surface / dependency conflicts.
dependencies {
}

intellij {
    // Baseline SDK to compile against. The produced plugin itself is NOT
    // pinned to this version at runtime: patchPluginXml below sets an
    // open-ended since/until range so it keeps working on newer IDE builds.
    version.set("2023.1.5")
    type.set("IC") // IntelliJ IDEA Community — also works when installed into Ultimate
    plugins.set(listOf())
    updateSinceUntilBuild.set(false)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("")
    }

    // Skip the "Searchable Options" indexing build step — not needed for our
    // single lightweight Configurable and it requires a headless IDE launch
    // that isn't always possible in restricted build environments.
    buildSearchableOptions {
        enabled = false
    }

    runIde {
        // Convenience task: ./gradlew runIde launches a sandbox IDE with the
        // plugin installed, for manual testing.
    }
}
