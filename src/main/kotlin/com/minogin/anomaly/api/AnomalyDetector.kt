package com.minogin.anomaly.api

import com.minogin.anomaly.internal.analyzer.*
import com.minogin.anomaly.internal.analyzer.model.*
import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.profiler.*
import com.minogin.anomaly.internal.store.*
import com.minogin.anomaly.internal.tracer.*
import kotlin.io.path.*

class AnomalyDetector @JvmOverloads constructor(
    basePath: String,
    currentVersion: String,
    config: AnalyzerConfig = AnalyzerConfig(),
) {
    private val currentVersion = Version(currentVersion.also {
        require(it.isNotBlank()) { "currentVersion must not be blank" }
    })

    private val store = Store(Path(basePath))
    private val tracer = Tracer()
    private val profiler = Profiler()
    private val analyzer = Analyzer(config)

    /**
     * Records one model call: call it right after the model returned, with what went in and what
     * came out. If the workflow then routes somewhere, report that on the returned handle:
     *
     * ```
     * val cp = detector.checkpoint(step = "classify-query", input = message, output = answer)
     * cp.nextStep(handler)
     * ```
     */
    fun checkpoint(
        step: String,
        input: String,
        output: String,
    ): CheckpointHandle {
        val cp = tracer.checkpoint(
            step = Step(step),
            input = input,
            output = output,
        )
        store.append(currentVersion, cp)
        // The store keeps the last record per checkpoint id, so the routing is an appended update.
        return CheckpointHandle { nextStep -> store.append(currentVersion, cp.copy(nextStep = Step(nextStep))) }
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
