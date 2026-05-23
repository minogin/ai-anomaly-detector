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

    data class TransitionChanged(
        val step: String,
        val addedNextSteps: Set<String>,
        val removedNextSteps: Set<String>,
    ) : Finding {
        override val severity: Severity
            get() = Severity.MID
    }
}

//    val step: String,
//    val inputHash: String?,
//    val message: String,
//    val referenceKinds: Map<OutputType, Int>,
//    val currentKinds: Map<OutputType, Int>,
//    val referenceExampleOutput: String?,
//    val currentExampleOutput: String?

//        TODO
//        STEP_OUTPUT_PROFILE_CHANGED,
//        EXACT_INPUT_OUTPUT_KIND_CHANGED,
//        NODE_DISAPPEARED,
//        NEXT_STEP_DRIFT