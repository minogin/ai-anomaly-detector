package com.minogin.anomaly.comparator.model

import com.minogin.anomaly.sampler.model.OutputKind

data class Finding(
    val type: Type,
    val node: String,
    val inputHash: String?,
    val message: String,
    val referenceKinds: Map<OutputKind, Int>,
    val currentKinds: Map<OutputKind, Int>,
    val referenceExampleOutput: String?,
    val currentExampleOutput: String?
) {
    enum class Type {
        NODE_OUTPUT_PROFILE_CHANGED,
        EXACT_INPUT_OUTPUT_KIND_CHANGED,
        NODE_DISAPPEARED,
        NEXT_STEP_DRIFT
    }
}