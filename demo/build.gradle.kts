plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

// Checkpoints are written under demo/.ai-anomaly-detector/<version>.jsonl (git-ignored).
fun JavaExec.demoRun(vararg arguments: String) {
    group = "demo"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.minogin.anomaly.demo.DemoKt"
    workingDir = projectDir
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    args(*arguments)
}

// One task per version rather than a -Pv=1.1 property: PowerShell splits "-Pv=1.1" at the dot
// and Gradle then looks for a task named ".1". Must match ScriptedModel.VERSIONS.
val baseline = "1.0"
val versions = listOf("1.0", "1.1", "1.2", "1.3")

// ./gradlew -q :demo:record-1.1
versions.forEach { version ->
    tasks.register<JavaExec>("record-$version") {
        description = "Run the synthetic workflow as version $version and record its checkpoints"
        demoRun("record", version)
    }
}

// ./gradlew -q :demo:diff-1.1
versions.filter { it != baseline }.forEach { version ->
    tasks.register<JavaExec>("diff-$version") {
        description = "Compare recorded version $version against the $baseline baseline"
        demoRun("diff", version, baseline)
    }
}

// ./gradlew -q demo
tasks.register<JavaExec>("demo") {
    description = "Record all demo versions and print every drift report against the baseline"
    demoRun("all")
}

// ./gradlew -q :demo:screenshots   -> docs/demo-report-<version>.png
tasks.register<JavaExec>("screenshots") {
    description = "Render every drift report as a PNG for slides into docs/"
    demoRun("screenshots", rootProject.layout.projectDirectory.dir("docs").asFile.absolutePath)
}
