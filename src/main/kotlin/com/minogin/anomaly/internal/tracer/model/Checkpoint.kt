package com.minogin.anomaly.internal.tracer.model

import com.minogin.anomaly.internal.common.model.*
import java.util.*
import kotlin.time.*

internal data class Checkpoint(
    val runId: UUID,
    val timestamp: Instant,
    val step: Step,
    val input: String?,
    val output: String,
    val nextStep: Step?,
)