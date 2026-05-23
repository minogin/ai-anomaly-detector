package com.minogin.anomaly.api

import com.minogin.anomaly.internal.analyzer.*
import com.minogin.anomaly.internal.analyzer.model.*
import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.profiler.*
import com.minogin.anomaly.internal.store.*
import com.minogin.anomaly.internal.tracer.*
import kotlin.io.path.*

class AnomalyDetector(
    basePath: String,
    currentVersion: String,
    referenceVersion: String,
) {
    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                flush()
            }
        )
    }

    private val currentVersion = Version(currentVersion)
    private val referenceVersion = Version(referenceVersion)

    private val store = Store(Path(basePath))

    private val tracer = Tracer()

    private val profiler = Profiler()

    private val analyzer = Analyzer()

    fun checkpoint(
        step: String,
        input: String,
        output: String,
        nextStep: String? = null
    ) {
        tracer.checkpoint(
            step = Step(step),
            input = input,
            output = output,
            nextStep = nextStep?.let { Step(it) },
        )
    }

    fun flush() {
        store.save(
            version = currentVersion,
            checkpoints = tracer.checkpoints()
        )
    }

    fun report(): Report {
        val currentProfile = profiler.profile(
            version = currentVersion,
            checkpoints = tracer.checkpoints()
        )

        val referenceCheckpoints = store.load(referenceVersion)
        val referenceProfile = profiler.profile(
            version = referenceVersion,
            checkpoints = referenceCheckpoints
        )

        return analyzer.report(
            currentProfile = currentProfile,
            referenceProfile = referenceProfile
        )
    }
}