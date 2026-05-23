package com.minogin.anomaly.cli

import com.minogin.anomaly.internal.analyzer.*
import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.printer.*
import com.minogin.anomaly.internal.profiler.*
import com.minogin.anomaly.internal.store.*
import kotlin.io.path.*
import kotlin.system.*

fun main(args: Array<String>) {
    if (args.size != 3) {
        System.err.println("Usage: anomaly-detector <basePath> <currentVersion> <referenceVersion>")
        System.err.println("Example: anomaly-detector .ai-anomaly-detector 1.1 1.0")
        exitProcess(1)
    }

    val (basePath, currentVersion, referenceVersion) = args
    val base = Path(basePath)

    try {
        val store = Store(base)
        val profiler = Profiler()
        val analyzer = Analyzer()
        val printer = Printer()

        val currentProfile = profiler.profile(Version(currentVersion), store.load(Version(currentVersion)))
        val referenceProfile = profiler.profile(Version(referenceVersion), store.load(Version(referenceVersion)))

        val report = analyzer.report(currentProfile, referenceProfile)
        printer.printReport(report)
        printer.printProfile(currentVersion, currentProfile)
    } catch (e: IllegalStateException) {
        System.err.println("Error: ${e.message}")
        exitProcess(1)
    }
}
