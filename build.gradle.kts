plugins {
    kotlin("jvm") version "2.3.20"
    application
    `maven-publish`
}

application {
    mainClass = "com.minogin.anomaly.cli.CliKt"
}

group = "com.minogin"
version = "0.2.2"

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

java {
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.register<Jar>("cliJar") {
    archiveClassifier = "cli"
    manifest {
        attributes["Main-Class"] = "com.minogin.anomaly.cli.CliKt"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}