package com.minogin.anomaly.tracer.model

import com.minogin.anomaly.sampler.model.OutputKind

data class Checkpoint(
    val version: String,
    val step: String,
    val input: String?,
    val output: String,

    // TODO move
    val outputKind: OutputKind,
    val nextStep: String?,
    val count: Int,
    val exampleInput: String,
    val exampleOutput: String,
)