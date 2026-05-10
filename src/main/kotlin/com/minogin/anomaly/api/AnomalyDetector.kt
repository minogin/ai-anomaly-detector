package com.minogin.anomaly.api

import com.minogin.anomaly.common.*
import com.minogin.anomaly.comparator.*
import com.minogin.anomaly.comparator.model.*
import com.minogin.anomaly.tracer.*

class AnomalyDetector(
    private val basePath: String,
    private val currentVersion: String,
    private val referenceVersion: String,
) {
    private val serializer = Serializer(basePath)

    private val sampler = Sampler(
        version = currentVersion,
        serializer = serializer
    )

    private val diffReporter = DiffReporter(
        serializer = serializer
    )

    fun checkpoint(
        step: String,
        input: String,
        output: String,
        nextStep: String? = null
    ) {
        sampler.checkpoint(
            step = step,
            input = input,
            output = output,
            nextStep = nextStep,
        )
    }

    fun flush() {
        sampler.flush()
    }

    fun report(): DiffReport =
        diffReporter.report(
            currentVersion = currentVersion,
            referenceVersion = referenceVersion
        )

    fun printReport() {
        val report = report()
        if (report.hasProblems()) {
            println("Anomalies detected:")
            report.findings.forEach { finding ->
                println("- ${finding.type} at node '${finding.node}': ${finding.message}")
            }
        } else {
            println("No anomalies detected.")
        }
    }
}