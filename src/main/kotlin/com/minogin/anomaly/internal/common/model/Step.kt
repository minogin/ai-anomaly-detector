package com.minogin.anomaly.internal.common.model

import com.fasterxml.jackson.annotation.*

@JvmInline
internal value class Step(
    @get:JsonValue
    val name: String
)