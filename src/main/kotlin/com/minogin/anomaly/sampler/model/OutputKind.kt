package com.minogin.anomaly.sampler.model

enum class OutputKind {
    EMPTY,
    JSON_OBJECT,
    JSON_ARRAY,
    QUOTED_JSON_OBJECT,
    QUOTED_JSON_ARRAY,
    QUOTED_SCALAR,
    SCALAR,
    MULTILINE_TEXT
}