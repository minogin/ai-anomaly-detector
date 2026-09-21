package com.minogin.anomaly.demo

import com.minogin.anomaly.api.*

/**
 * classify-query -> handle-support | handle-sales | handle-feedback -> summarize
 *
 * Instrumented exactly as a real workflow would be: one checkpoint per model call,
 * plus the routing decision recorded with nextStep.
 */
class Workflow(
    private val detector: AnomalyDetector,
    private val model: ScriptedModel,
) {
    fun run(conversation: Conversation) {
        val category = model.classify(conversation)
        val routerCheckpoint = detector.checkpoint(step = "classify-query", input = conversation.message, output = category)

        val handler = when (category) {
            "support" -> "handle-support"
            "sales" -> "handle-sales"
            "feedback" -> "handle-feedback"
            else -> "handle-support" // fallback: anything unrecognised is treated as a support request
        }
        routerCheckpoint.nextStep(handler)

        val reply = model.handle(handler, conversation)
        detector.checkpoint(step = handler, input = conversation.message, output = reply).nextStep("summarize")

        val summary = model.summarize(handler, conversation)
        detector.checkpoint(step = "summarize", input = reply, output = summary)
    }
}
