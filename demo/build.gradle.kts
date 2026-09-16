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

// ./gradlew -q :demo:record -Pv=1.1
tasks.register<JavaExec>("record") {
    description = "Run the synthetic workflow as version -Pv (default 1.0) and record its checkpoints"
    demoRun("record", (project.findProperty("v") ?: "1.0").toString())
}

// ./gradlew -q :demo:diff -Pv=1.1
tasks.register<JavaExec>("diff") {
    description = "Compare recorded version -Pv (default 1.1) against the 1.0 baseline"
    demoRun("diff", (project.findProperty("v") ?: "1.1").toString(), "1.0")
}

// ./gradlew -q demo
tasks.register<JavaExec>("demo") {
    description = "Record all demo versions and print every drift report against the baseline"
    demoRun("all")
}
