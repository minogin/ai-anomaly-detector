package com.minogin.anomaly.internal.profiler.model

data class OutputForm(
    val type: Type,
    val quoted: Boolean,
    val schema: JsonSchema? = null
) {
    enum class Type {
        EMPTY,
        JSON_OBJECT,
        JSON_ARRAY,
        INTEGER,
        DECIMAL,
        STRING,
        MARKDOWN,
        HTML
    }
}