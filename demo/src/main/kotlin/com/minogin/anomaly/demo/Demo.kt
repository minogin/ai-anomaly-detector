package com.minogin.anomaly.demo

import com.minogin.anomaly.api.*
import java.io.*
import java.nio.file.*
import kotlin.io.path.*
import kotlin.system.*

const val BASE_PATH = ".ai-anomaly-detector"
const val BASELINE = "1.0"

/** Runs all conversations through the workflow as [version]. Re-recording a version replaces its data. */
fun record(basePath: String, version: String) {
    val model = ScriptedModel(version)
    Path(basePath, "$version.jsonl").deleteIfExists()
    val workflow = Workflow(AnomalyDetector(basePath, version), model)
    CONVERSATIONS.forEach { workflow.run(it) }
    println("Recorded version $version: ${CONVERSATIONS.size} conversations -> $basePath/$version.jsonl")
}

/** Prints the drift report of [currentVersion] against [referenceVersion] using the library's CLI. */
fun diff(basePath: String, currentVersion: String, referenceVersion: String) {
    com.minogin.anomaly.cli.main(arrayOf(basePath, currentVersion, referenceVersion))
}

/** Same as [diff] but returns the report text instead of printing it. */
fun diffText(basePath: String, currentVersion: String, referenceVersion: String): String {
    val original = System.out
    val buffer = ByteArrayOutputStream()
    System.setOut(PrintStream(buffer, true, Charsets.UTF_8))
    try {
        diff(basePath, currentVersion, referenceVersion)
    } finally {
        System.setOut(original)
    }
    return buffer.toString(Charsets.UTF_8)
}

/** Records every version and renders one PNG per drifted version into [outputDir]. */
fun screenshots(basePath: String, outputDir: Path) {
    ScriptedModel.VERSIONS.forEach { record(basePath, it) }
    ScriptedModel.DRIFT_DESCRIPTIONS.keys.forEach { version ->
        val output = outputDir.resolve("demo-report-$version.png")
        Screenshot.render(diffText(basePath, version, BASELINE), output)
        println("Rendered $output")
    }
}

fun main(args: Array<String>) {
    try {
        run(args)
    } catch (e: IllegalArgumentException) {
        System.err.println("Error: ${e.message}")
        exitProcess(1)
    }
}

private fun run(args: Array<String>) {
    when (args.firstOrNull()) {
        "record" -> record(BASE_PATH, args.getOrElse(1) { BASELINE })

        "diff" -> diff(BASE_PATH, args.getOrElse(1) { "1.1" }, args.getOrElse(2) { BASELINE })

        "all" -> {
            ScriptedModel.VERSIONS.forEach { record(BASE_PATH, it) }
            ScriptedModel.DRIFT_DESCRIPTIONS.forEach { (version, description) ->
                println()
                println("### $BASELINE -> $version: $description")
                println()
                diff(BASE_PATH, version, BASELINE)
            }
        }

        "screenshots" -> screenshots(BASE_PATH, Path(args.getOrElse(1) { "screenshots" }))

        else -> {
            System.err.println(
                """
                Usage:
                  demo record <version>            record one version (${ScriptedModel.VERSIONS.joinToString()})
                  demo diff <current> [reference]  print the drift report (reference defaults to $BASELINE)
                  demo all                         record every version and print all reports
                  demo screenshots <dir>           record every version and render each report as a PNG
                """.trimIndent()
            )
            exitProcess(1)
        }
    }
}
