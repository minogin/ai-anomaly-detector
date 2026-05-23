package com.minogin.anomaly.internal.profiler

import com.minogin.anomaly.internal.profiler.model.*
import tools.jackson.databind.*
import tools.jackson.module.kotlin.*
import java.math.*

internal class OutputClassifier(
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
    fun classify(output: String): OutputForm {
        val text = output.trim()

        if (text.isEmpty()) {
            return OutputForm(OutputForm.Type.EMPTY, quoted = false)
        }

        parseJson(text)?.let { json ->
            return classifyJson(json, quoted = false)
        }

        return classifyPlainText(text, quoted = false)
    }

    private fun classifyJson(json: JsonNode, quoted: Boolean): OutputForm {
        return when {
            json.isObject ->
                OutputForm(OutputForm.Type.JSON_OBJECT, quoted, schema = extractSchema(json))

            json.isArray ->
                OutputForm(OutputForm.Type.JSON_ARRAY, quoted, schema = extractSchema(json))

            json.isIntegralNumber ->
                OutputForm(OutputForm.Type.INTEGER, quoted)

            json.isFloatingPointNumber ->
                OutputForm(OutputForm.Type.DECIMAL, quoted)

            json.isTextual ->
                classifyQuotedString(json.asText())

            else ->
                OutputForm(OutputForm.Type.STRING, quoted)
        }
    }

    private fun extractSchema(json: JsonNode): JsonSchema {
        return when {
            json.isObject -> JsonSchema.ObjectSchema(
                json.properties().associate { it.key to extractSchema(it.value) }
            )
            json.isArray -> {
                val schemas = mutableSetOf<JsonSchema>()
                for (element in json) schemas.add(extractSchema(element))
                JsonSchema.ArraySchema(schemas)
            }
            json.isIntegralNumber -> JsonSchema.Primitive.INTEGER
            json.isFloatingPointNumber -> JsonSchema.Primitive.DECIMAL
            json.isBoolean -> JsonSchema.Primitive.BOOLEAN
            json.isNull -> JsonSchema.Primitive.NULL
            else -> JsonSchema.Primitive.STRING
        }
    }

    private fun classifyQuotedString(value: String): OutputForm {
        val text = value.trim()

        parseJson(text)?.let { innerJson ->
            return classifyJson(innerJson, quoted = true)
        }

        return classifyPlainText(text, quoted = true)
    }

    private fun classifyPlainText(text: String, quoted: Boolean): OutputForm {
        return when {
            looksLikeMarkdown(text) ->
                OutputForm(OutputForm.Type.MARKDOWN, quoted)

            looksLikeHtml(text) ->
                OutputForm(OutputForm.Type.HTML, quoted)

            looksLikeInteger(text) ->
                OutputForm(OutputForm.Type.INTEGER, quoted)

            looksLikeDecimal(text) ->
                OutputForm(OutputForm.Type.DECIMAL, quoted)

            else ->
                OutputForm(OutputForm.Type.STRING, quoted)
        }
    }

    private fun looksLikeInteger(text: String): Boolean {
        return try {
            BigInteger(text)
            true
        } catch (_: NumberFormatException) {
            false
        }
    }

    private fun looksLikeDecimal(text: String): Boolean {
        if (!text.contains('.') && !text.contains('e', ignoreCase = true)) {
            return false
        }

        return try {
            BigDecimal(text)
            true
        } catch (_: NumberFormatException) {
            false
        }
    }

    private fun looksLikeMarkdown(text: String): Boolean {
        val lines = text.lines()

        return text.startsWith("```") ||
                lines.any { it.startsWith("# ") || it.startsWith("## ") || it.startsWith("### ") } ||
                lines.any { it.startsWith("- ") || it.startsWith("* ") } ||
                lines.any { it.startsWith("> ") } ||
                lines.any { Regex("""^\d+\. """).containsMatchIn(it) } ||
                lines.any { it.trimStart().startsWith("| ") && it.contains(" |") } ||
                Regex("""\[[^]]+]\([^)]+\)""").containsMatchIn(text) ||
                Regex("""!\[[^]]*]\([^)]+\)""").containsMatchIn(text) ||
                Regex("""\*\*[^*]+\*\*""").containsMatchIn(text) ||
                Regex("""__[^_]+__""").containsMatchIn(text) ||
                Regex("""~~[^~]+~~""").containsMatchIn(text) ||
                Regex("""`[^`]+`""").containsMatchIn(text)
    }

    private fun looksLikeHtml(text: String): Boolean {
        return Regex(
            pattern = """<([a-zA-Z][a-zA-Z0-9]*)\b[^>]*>.*</\1>""",
            option = RegexOption.DOT_MATCHES_ALL
        ).containsMatchIn(text)
    }

    private fun parseJson(text: String): JsonNode? {
        return try {
            objectMapper.readTree(text)
        } catch (_: Exception) {
            null
        }
    }
}