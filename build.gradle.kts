plugins {
    kotlin("jvm") version "2.3.20"
    application
}

application {
    mainClass = "com.minogin.checkpoint.cli.CliKt"
}

group = "com.minogin"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("tools.jackson.module:jackson-module-kotlin:3.1.3")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Jar>("cliJar") {
    archiveClassifier = "cli"
    manifest {
        attributes["Main-Class"] = "com.minogin.checkpoint.cli.CliKt"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}