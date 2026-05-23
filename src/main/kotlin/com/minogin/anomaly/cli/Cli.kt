package com.minogin.anomaly.cli

import com.minogin.anomaly.internal.analyzer.*
import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.profiler.*
import com.minogin.anomaly.internal.renderer.*
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

    val currentFile = base.resolve("$currentVersion.jsonl")
    val referenceFile = base.resolve("$referenceVersion.jsonl")

    if (!currentFile.exists()) {
        System.err.println("No data for version '$currentVersion' — expected $currentFile")
        exitProcess(1)
    }
    if (!referenceFile.exists()) {
        System.err.println("No data for version '$referenceVersion' — expected $referenceFile")
        exitProcess(1)
    }

    val store = Store(base)
    val profiler = Profiler()
    val analyzer = Analyzer()
    val renderer = ReportRenderer()

    val currentProfile = profiler.profile(Version(currentVersion), store.load(Version(currentVersion)))
    val referenceProfile = profiler.profile(Version(referenceVersion), store.load(Version(referenceVersion)))

    val report = analyzer.report(currentProfile, referenceProfile)
    renderer.printReport(report)
}
