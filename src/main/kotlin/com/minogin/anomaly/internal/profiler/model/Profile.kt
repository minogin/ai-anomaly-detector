package com.minogin.anomaly.internal.profiler.model

import com.minogin.anomaly.internal.common.model.*

internal data class Profile(
    val version: Version,
    val steps: Set<Step>,
    val stepOutputForms: Map<Step, Set<OutputForm>>,
    val stepTransitions: Map<Step, Set<Step>> = emptyMap(),
)