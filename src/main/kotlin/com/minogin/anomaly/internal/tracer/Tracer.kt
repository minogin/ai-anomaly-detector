package com.minogin.anomaly.internal.tracer

import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.tracer.model.*
import com.minogin.anomaly.internal.util.*
import java.util.*
import kotlin.time.*

internal class Tracer(
    private val clock: Clock = Clock.System
) {
    private val checkpoints = ConcurrentList<Checkpoint>()
    private val runId = UUID.randomUUID()

    fun checkpoint(
        step: Step,
        input: String? = null,
        output: String,
        nextStep: Step? = null
    ): Checkpoint {
        val cp = Checkpoint(
            id = UUID.randomUUID(),
            runId = runId,
            timestamp = clock.now(),
            step = step,
            input = input,
            output = output,
            nextStep = nextStep
        )
        checkpoints.add(cp)
        return cp
    }

    fun setNextStep(step: Step, nextStep: Step): Checkpoint? =
        checkpoints.updateLast({ it.step == step }) { it.copy(nextStep = nextStep) }

    fun checkpoints(): List<Checkpoint> = checkpoints.snapshot()
}
