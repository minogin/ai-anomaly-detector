package com.minogin.anomaly.internal.profiler

import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.profiler.model.*
import com.minogin.anomaly.internal.tracer.model.*

internal class Profiler {
    private val classifier = OutputClassifier()

    fun profile(
        version: Version,
        checkpoints: List<Checkpoint>
    ): Profile =
        Profile(
            version = version,
            steps = checkpoints.map { it.step }.toSet(),
            stepOutputForms = checkpoints
                .groupBy { it.step }
                .mapValues { (_, checkpoints) ->
                    checkpoints.map { classifier.classify(it.output) }.toSet()
                }
        )
}
