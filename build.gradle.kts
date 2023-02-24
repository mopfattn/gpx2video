import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
    application
}

group = "de.pfattner.gpx2video"
version = "0.1"

sourceSets {
    main {
        java {
            srcDirs(
                "src/main/kotlin",
                "src/main/android", // Android stubs used by MapsForge
            )
        }
    }
}

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("commons-cli:commons-cli:1.5.0")

    val mapsforgeVersion = "0.18.0"
    implementation("org.mapsforge:mapsforge-core:$mapsforgeVersion")
    implementation("org.mapsforge:mapsforge-map:$mapsforgeVersion")
    implementation("org.mapsforge:mapsforge-map-android:$mapsforgeVersion")

    // XML parser
    implementation("com.github.kobjects:kxml2:2.4.0")

    // YAML serializer
    implementation("com.charleskorn.kaml:kaml:0.40.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("MainKt")
}

task("fatJar", type = Jar::class) {
    dependsOn += "build"
    archiveBaseName.set("gpx2video")
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks["jar"] as CopySpec)
    duplicatesStrategy = DuplicatesStrategy.WARN
}