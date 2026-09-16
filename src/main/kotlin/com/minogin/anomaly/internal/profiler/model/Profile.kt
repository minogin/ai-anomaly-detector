package com.minogin.anomaly.internal.profiler.model

import com.minogin.anomaly.internal.common.model.*

internal data class Profile(
    val version: Version,
    val steps: Set<Step>,
    val stepOutputForms: Map<Step, Set<OutputForm>>,
    /** For each step, how many times it routed to each next step. Steps that never routed are absent. */
    val stepTransitionCounts: Map<Step, Map<Step, Int>> = emptyMap(),
    /** For each step, the first output seen for each of its output forms. */
    val stepExamples: Map<Step, Map<OutputForm, String>> = emptyMap(),
) {
    /** The set of next steps each step routed to, derived from the counts. */
    val stepTransitions: Map<Step, Set<Step>>
        get() = stepTransitionCounts.mapValues { (_, counts) -> counts.keys }
}
