package com.minogin.anomaly.internal.analyzer

import com.minogin.anomaly.internal.analyzer.model.*
import com.minogin.anomaly.internal.profiler.model.*

internal class Analyzer {
    fun report(
        currentProfile: Profile,
        referenceProfile: Profile
    ): Report {
        val findings = mutableListOf<Finding>()

        val missingSteps = referenceProfile.steps - currentProfile.steps
        findings += missingSteps.map { step ->
            Finding.MissingStep(step.name)
        }

        val newSteps = currentProfile.steps - referenceProfile.steps
        findings += newSteps.map { step ->
            Finding.NewStep(step.name)
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

        return Report(
            currentVersion = currentProfile.version.value,
            referenceVersion = referenceProfile.version.value,
            findings = findings
        )
    }
}