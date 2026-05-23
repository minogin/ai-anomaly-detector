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
) {
    private val currentVersion = Version(currentVersion.also {
        require(it.isNotBlank()) { "currentVersion must not be blank" }
    })

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
        val cp = tracer.checkpoint(
            step = Step(step),
            input = input,
            output = output,
            nextStep = nextStep?.let { Step(it) },
        )
        store.append(currentVersion, cp)
    }

    fun nextStep(step: String, nextStep: String) {
        tracer.setNextStep(Step(step), Step(nextStep))?.let { store.append(currentVersion, it) }
    }

    fun report(referenceVersion: String): Report {
        require(referenceVersion.isNotBlank()) { "referenceVersion must not be blank" }
        val ref = Version(referenceVersion)
        val currentProfile = profiler.profile(
            version = currentVersion,
            checkpoints = store.load(currentVersion)
        )

        val referenceProfile = profiler.profile(
            version = ref,
            checkpoints = store.load(ref)
        )

        return analyzer.report(
            currentProfile = currentProfile,
            referenceProfile = referenceProfile
        )
    }
}
