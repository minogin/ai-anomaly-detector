package com.minogin.anomaly.internal.analyzer

import com.minogin.anomaly.api.*
import com.minogin.anomaly.internal.analyzer.model.*
import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.profiler.model.*
import kotlin.math.*

internal class Analyzer(
    private val config: AnalyzerConfig = AnalyzerConfig()
) {
    fun report(
        currentProfile: Profile,
        referenceProfile: Profile
    ): Report {
        val findings = mutableListOf<Finding>()

        val missingSteps = referenceProfile.steps - currentProfile.steps
        findings += missingSteps.map { step ->
            Finding.MissingStep(
                step = step.name,
                referenceOutputForms = referenceProfile.stepOutputForms.getValue(step),
                referenceNextSteps = referenceProfile.stepTransitions.getOrDefault(step, emptySet()).map { it.name }.toSet()
            )
        }

        val newSteps = currentProfile.steps - referenceProfile.steps
        findings += newSteps.map { step ->
            Finding.NewStep(
                step = step.name,
                currentOutputForms = currentProfile.stepOutputForms.getValue(step),
                currentNextSteps = currentProfile.stepTransitions.getOrDefault(step, emptySet()).map { it.name }.toSet()
            )
        }

        currentProfile.steps.forEach { step ->
            val outputForms = currentProfile.stepOutputForms[step] ?: throw IllegalStateException("Malformed profile [${currentProfile.version}]: step [${step.name}] has no output forms")
            if (outputForms.size > 1) {
                findings += Finding.MultipleOutputFormsPerStep(
                    step = step.name,
                    outputForms = outputForms
                )
            }
        }

        val commonSteps = currentProfile.steps intersect referenceProfile.steps
        findings += commonSteps.mapNotNull { step ->
            val currentOutputForms = currentProfile.stepOutputForms.getValue(step)
            val referenceOutputForms = referenceProfile.stepOutputForms.getValue(step)

            if (currentOutputForms != referenceOutputForms) {
                Finding.OutputFormChanged(
                    step = step.name,
                    currentOutputForms = currentOutputForms,
                    referenceOutputForms = referenceOutputForms
                )
            } else
                null
        }

        findings += commonSteps.mapNotNull { step ->
            compareRouting(
                step = step,
                referenceCounts = referenceProfile.stepTransitionCounts.getOrDefault(step, emptyMap()),
                currentCounts = currentProfile.stepTransitionCounts.getOrDefault(step, emptyMap()),
            )
        }

        return Report(
            currentVersion = currentProfile.version.value,
            referenceVersion = referenceProfile.version.value,
            findings = findings
        )
    }

    /**
     * Two separate questions, answered by two separate findings so that one change is never reported twice:
     * 1. Did the set of targets change? No threshold; a target that appears or disappears is always reported.
     * 2. Same targets, but did the shares move? Reported only above the threshold, and only when there is
     *    enough data for shares to mean anything.
     */
    private fun compareRouting(
        step: Step,
        referenceCounts: Map<Step, Int>,
        currentCounts: Map<Step, Int>,
    ): Finding? {
        val added = currentCounts.keys - referenceCounts.keys
        val removed = referenceCounts.keys - currentCounts.keys
        if (added.isNotEmpty() || removed.isNotEmpty()) {
            return Finding.TransitionChanged(
                step = step.name,
                addedNextSteps = added.map { it.name }.toSet(),
                removedNextSteps = removed.map { it.name }.toSet(),
                referenceCounts = referenceCounts.byName(),
                currentCounts = currentCounts.byName(),
            )
        }

        if (referenceCounts.isEmpty()) return null
        if (referenceCounts.size > config.routingMaxTargets) return null
        val referenceTotal = referenceCounts.values.sum()
        val currentTotal = currentCounts.values.sum()
        if (referenceTotal < config.routingMinSamples || currentTotal < config.routingMinSamples) return null

        val shift = routingShift(referenceCounts, currentCounts)
        // Inclusive: a shift exactly at the threshold is reported. The tolerance absorbs
        // floating-point error, e.g. 0.7-0.5 + 0.5-0.3 halved evaluates to 0.19999999999999998.
        if (shift < config.routingShiftThreshold - 1e-9) return null

        return Finding.RoutingDistributionChanged(
            step = step.name,
            referenceCounts = referenceCounts.byName(),
            currentCounts = currentCounts.byName(),
            shift = shift,
            threshold = config.routingShiftThreshold,
        )
    }

    private fun Map<Step, Int>.byName(): Map<String, Int> =
        entries.sortedBy { it.key.name }.associate { it.key.name to it.value }

    companion object {
        /**
         * Share of routings that ended up at a different target than before:
         * half the sum over targets of |current share - reference share|. Ranges from 0 to 1.
         */
        fun routingShift(referenceCounts: Map<Step, Int>, currentCounts: Map<Step, Int>): Double {
            val referenceTotal = referenceCounts.values.sum().toDouble()
            val currentTotal = currentCounts.values.sum().toDouble()
            val targets = referenceCounts.keys + currentCounts.keys
            return targets.sumOf { target ->
                val referenceShare = (referenceCounts[target] ?: 0) / referenceTotal
                val currentShare = (currentCounts[target] ?: 0) / currentTotal
                abs(currentShare - referenceShare)
            } / 2
        }
    }
}
