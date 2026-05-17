package com.minogin.anomaly.internal.common.model

import com.fasterxml.jackson.annotation.*

@JvmInline
internal value class Version(
    @get:JsonValue
    val value: String
) {
    override fun toString(): String = value
}