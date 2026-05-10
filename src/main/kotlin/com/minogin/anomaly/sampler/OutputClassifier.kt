package com.minogin.anomaly.sampler

import com.minogin.anomaly.sampler.model.OutputKind
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

class OutputClassifier(
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {

    fun classify(output: String): OutputKind {
        val text = output.trim()

        if (text.isEmpty()) {
            return OutputKind.EMPTY
        }

        if (text.length >= 2 && text.startsWith('"') && text.endsWith('"')) {
            return classifyQuoted(text.substring(1, text.length - 1))
        }

        parseJson(text)?.let { json ->
            when {
                json.isObject -> return OutputKind.JSON_OBJECT
                json.isArray -> return OutputKind.JSON_ARRAY
            }
        }

        return classifyPlainText(text)
    }

    private fun classifyQuoted(inner: String): OutputKind {
        val innerJson = parseJson(inner.trim())
        return when {
            innerJson?.isObject == true -> OutputKind.QUOTED_JSON_OBJECT
            innerJson?.isArray == true -> OutputKind.QUOTED_JSON_ARRAY
            else -> OutputKind.QUOTED_SCALAR
        }
    }

    private fun classifyPlainText(text: String): OutputKind {
        return if (text.lines().size > 1) {
            OutputKind.MULTILINE_TEXT
        } else {
            OutputKind.SCALAR
        }
    }

    private fun parseJson(text: String): JsonNode? {
        return try {
            objectMapper.readTree(text)
        } catch (_: Exception) {
            null
        }
    }
}