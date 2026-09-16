package com.minogin.anomaly.api

/**
 * Tunable limits for the comparison. All defaults are judgment calls, explained per field.
 *
 * The routing-distribution check looks at one step at a time and asks: of all the times this step
 * routed somewhere, what share went to each target, and how much did those shares move between the
 * two versions? It only runs when the set of targets is identical in both versions. When a target
 * appears or disappears, the "transitions changed" finding reports that instead, so a change is
 * never reported twice.
 */
data class AnalyzerConfig(
    /**
     * Minimum share of routings that must have changed target for the check to report a finding.
     *
     * The measure is: sum over all targets of |current share - reference share|, divided by two.
     * It ranges from 0 (identical) to 1 (nothing goes where it used to) and reads as
     * "this share of routings now ends up at a different target than before".
     * Example: 30/30/40 percent becoming 70/10/20 percent is (40 + 20 + 20) / 2 = 40 percent.
     *
     * Default 0.25: a quarter of the traffic changed destination. Chosen so that with ten samples
     * per version one or two flipped routings stay quiet and three fire. Lower it for high-traffic
     * steps where a few percent matter; raise it for noisy steps.
     */
    val routingShiftThreshold: Double = 0.25,

    /**
     * Minimum number of routed samples each version must have for the check to run at all.
     * With fewer samples one conversation is a large share by itself and proportions are noise.
     * Default 5: one sample is then at most 20 percent, below the default threshold.
     */
    val routingMinSamples: Int = 5,

    /**
     * Maximum number of distinct targets for the check to run. With many targets each carries a
     * small share and the measure stays small even when routing changed a lot; the check is skipped
     * rather than reporting something meaningless. Default 10.
     */
    val routingMaxTargets: Int = 10,

    /**
     * Attach one example output per output form to the findings, so the report shows what the
     * STRING or MARKDOWN actually looked like. Examples are the recorded outputs themselves, so turn
     * this off when the report must not carry potentially sensitive text. Note that the checkpoint
     * files on disk always contain the full inputs and outputs regardless of this setting.
     */
    val includeExamples: Boolean = true,
) {
    init {
        require(routingShiftThreshold in 0.0..1.0) { "routingShiftThreshold must be between 0 and 1, was $routingShiftThreshold" }
        require(routingMinSamples >= 1) { "routingMinSamples must be at least 1, was $routingMinSamples" }
        require(routingMaxTargets >= 2) { "routingMaxTargets must be at least 2, was $routingMaxTargets" }
    }
}
