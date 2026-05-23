package com.minogin.anomaly.internal.profiler.model

sealed interface JsonSchema {

    data class ObjectSchema(val fields: Map<String, JsonSchema>) : JsonSchema

    data class ArraySchema(val elementSchemas: Set<JsonSchema>) : JsonSchema

    enum class Primitive : JsonSchema {
        STRING, INTEGER, DECIMAL, BOOLEAN, NULL
    }
}