package com.minogin.anomaly.internal.analyzer.model

import com.minogin.anomaly.internal.profiler.model.*

data class Report(
    val currentVersion: String,
    val referenceVersion: String,
    val findings: List<Finding>
) {
    fun hasProblems(): Boolean = findings.isNotEmpty()
}

sealed interface Finding {
    val severity: Severity

    enum class Severity {
        LOW, MID, HIGH
    }

    data class MultipleOutputFormsPerStep(
        val step: String,
        val outputForms: Set<OutputForm>
    ) : Finding {
        override val severity: Severity
            get() = Severity.HIGH
    }

    data class OutputFormChanged(
        val step: String,
        val currentOutputForms: Set<OutputForm>,
        val referenceOutputForms: Set<OutputForm>,
    ) : Finding {
        override val severity: Severity
            get() = Severity.HIGH
    }

    data class NewStep(
        val step: String,
        val currentOutputForms: Set<OutputForm>,
        val currentNextSteps: Set<String>,
    ) : Finding {
        override val severity: Severity
            get() = Severity.LOW
    }

    data class MissingStep(
        val step: String,
        val referenceOutputForms: Set<OutputForm>,
        val referenceNextSteps: Set<String>,
    ) : Finding {
        override val severity: Severity
            get() = Severity.MID
    }

    /** The set of targets a step routes to differs between versions. Counts per target are included for context. */
    data class TransitionChanged(
        val step: String,
        val addedNextSteps: Set<String>,
        val removedNextSteps: Set<String>,
        val referenceCounts: Map<String, Int> = emptyMap(),
        val currentCounts: Map<String, Int> = emptyMap(),
    ) : Finding {
        override val severity: Severity
            get() = Severity.MID
    }

    /**
     * Same set of targets in both versions, but the share of routings going to each target moved
     * by at least the configured threshold. [shift] is the share of routings that changed target.
     */
    data class RoutingDistributionChanged(
        val step: String,
        val referenceCounts: Map<String, Int>,
        val currentCounts: Map<String, Int>,
        val shift: Double,
        val threshold: Double,
    ) : Finding {
        override val severity: Severity
            get() = Severity.MID
    }
}
