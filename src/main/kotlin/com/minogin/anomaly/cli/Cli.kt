package com.minogin.anomaly.cli

import kotlin.system.*

fun main(args: Array<String>) {
    if (args.size != 3) {
        System.err.println("Usage: checkpoint-cli <basePath> <currentVersion> <referenceVersion>")
        System.err.println("Example: checkpoint-cli .anomaly-detector 2.0 1.0")
        exitProcess(1)
    }

    val (basePath, currentVersion, referenceVersion) = args

//     TODO
//    AnomalyDetector(
//        basePath = basePath,
//        currentVersion = currentVersion,
//        referenceVersion = referenceVersion
//    ).printReport()
}
