package com.minogin.anomaly.demo

/**
 * Stand-in for an LLM: a lookup table keyed by version, node and conversation.
 * Same input, same output, every run. No network, no keys.
 *
 * Versions and what drifts in each:
 * - 1.0  baseline
 * - 1.1  the router wraps some answers in markdown bold; the workflow's parser does not recognise
 *        them and falls back to support
 * - 1.2  the sales handler nests its discount field inside a new "offer" object
 * - 1.3  the router quietly classifies every feedback message as support; no formatting change
 */
class ScriptedModel(private val version: String) {

    companion object {
        val VERSIONS = listOf("1.0", "1.1", "1.2", "1.3")

        val DRIFT_DESCRIPTIONS = mapOf(
            "1.1" to "router answers in bold for some messages, unrecognised answers fall back to support",
            "1.2" to "sales handler nests the discount inside a new offer object",
            "1.3" to "router silently classifies every feedback message as support",
        )

        /**
         * Conversations whose router answer comes back bold in 1.1: two feedback, one support, one sales.
         * The first one is a feedback message so the report's example reads `**feedback**`, the talk's opening incident.
         */
        private val BOLD_IN_1_1 = setOf(3, 4, 6, 7)
    }

    init {
        require(version in VERSIONS) { "Unknown demo version '$version'. Known: ${VERSIONS.joinToString()}" }
    }

    /** Node classify-query: returns the category as a bare word. */
    fun classify(conversation: Conversation): String {
        val answer = when (version) {
            "1.3" -> if (conversation.category == Category.FEEDBACK) Category.SUPPORT.label else conversation.category.label
            else -> conversation.category.label
        }
        return if (version == "1.1" && conversation.id in BOLD_IN_1_1) "**$answer**" else answer
    }

    /** Nodes handle-support, handle-sales, handle-feedback. */
    fun handle(handler: String, conversation: Conversation): String = when (handler) {
        "handle-support" -> "Sorry about the trouble. Our team will look into it and get back to you shortly."
        "handle-feedback" -> "Thank you for the feedback, it has been passed on to the product team."
        "handle-sales" -> when (version) {
            "1.2" -> """{"reply": "Happy to help with pricing.", "offer": {"discount": 10}}"""
            else -> """{"reply": "Happy to help with pricing.", "discount": 10}"""
        }
        else -> error("Unknown handler '$handler'")
    }

    /** Node summarize: one line for the ticket log. */
    fun summarize(handler: String, conversation: Conversation): String =
        "${handler.removePrefix("handle-")} request handled: ${conversation.message}"
}
