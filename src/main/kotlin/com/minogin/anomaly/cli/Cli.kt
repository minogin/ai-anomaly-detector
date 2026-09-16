package com.minogin.anomaly.cli

import com.minogin.anomaly.api.*
import com.minogin.anomaly.internal.analyzer.*
import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.printer.*
import com.minogin.anomaly.internal.profiler.*
import com.minogin.anomaly.internal.store.*
import kotlin.io.path.*
import kotlin.system.*

internal const val USAGE = """
Usage: anomaly-detector <basePath> <currentVersion> <referenceVersion> [options]
Example: anomaly-detector .ai-anomaly-detector 1.1 1.0

Options (routing distribution check, see AnalyzerConfig for how the defaults were chosen):
  --routing-threshold=<0..1>   share of routings that must change target to report (default 0.25)
  --routing-min-samples=<n>    routed samples each version needs for the check to run (default 5)
  --routing-max-targets=<n>    skip the check for steps with more distinct targets (default 10)
"""

internal data class CliArgs(
    val basePath: String,
    val currentVersion: String,
    val referenceVersion: String,
    val config: AnalyzerConfig,
)

internal fun parseArgs(args: Array<String>): CliArgs {
    val positional = args.filterNot { it.startsWith("--") }
    val options = args.filter { it.startsWith("--") }.associate { option ->
        val (name, value) = option.removePrefix("--").split("=", limit = 2).also {
            require(it.size == 2) { "Option '$option' needs a value: $option=<value>" }
        }
        name to value
    }
    require(positional.size == 3) { "Expected 3 arguments, got ${positional.size}" }

    val defaults = AnalyzerConfig()
    val config = AnalyzerConfig(
        routingShiftThreshold = options["routing-threshold"]?.let { it.toDoubleOrNull() ?: throw IllegalArgumentException("--routing-threshold must be a number, was '$it'") } ?: defaults.routingShiftThreshold,
        routingMinSamples = options["routing-min-samples"]?.let { it.toIntOrNull() ?: throw IllegalArgumentException("--routing-min-samples must be an integer, was '$it'") } ?: defaults.routingMinSamples,
        routingMaxTargets = options["routing-max-targets"]?.let { it.toIntOrNull() ?: throw IllegalArgumentException("--routing-max-targets must be an integer, was '$it'") } ?: defaults.routingMaxTargets,
    )
    val unknown = options.keys - setOf("routing-threshold", "routing-min-samples", "routing-max-targets")
    require(unknown.isEmpty()) { "Unknown option(s): ${unknown.joinToString { "--$it" }}" }

    return CliArgs(positional[0], positional[1], positional[2], config)
}

fun main(args: Array<String>) {
    val parsed = try {
        parseArgs(args)
    } catch (e: IllegalArgumentException) {
        System.err.println("Error: ${e.message}")
        System.err.println(USAGE.trimIndent())
        exitProcess(1)
    }

    try {
        val store = Store(Path(parsed.basePath))
        val profiler = Profiler()
        val analyzer = Analyzer(parsed.config)
        val printer = Printer()

        val currentProfile = profiler.profile(Version(parsed.currentVersion), store.load(Version(parsed.currentVersion)))
        val referenceProfile = profiler.profile(Version(parsed.referenceVersion), store.load(Version(parsed.referenceVersion)))

        val report = analyzer.report(currentProfile, referenceProfile)
        printer.printReport(report)
        printer.printProfile(parsed.currentVersion, currentProfile)
    } catch (e: IllegalStateException) {
        System.err.println("Error: ${e.message}")
        exitProcess(1)
    }
}
