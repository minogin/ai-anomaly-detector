package com.minogin.anomaly.api

/**
 * Returned by [AnomalyDetector.checkpoint]. Use it to record where the workflow went after this
 * step, once your code has decided. It refers to exactly the checkpoint it came from, so it is safe
 * when many requests run through one detector at the same time.
 */
class CheckpointHandle internal constructor(
    private val onNextStep: (String) -> Unit
) {
    /**
     * Records the routing decision taken after this checkpoint. It means "the workflow decided to go
     * there", not "the next step completed". Calling it again replaces the earlier value.
     */
    fun nextStep(nextStep: String) {
        require(nextStep.isNotBlank()) { "nextStep must not be blank" }
        onNextStep(nextStep)
    }
}
