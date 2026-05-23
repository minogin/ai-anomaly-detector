package com.minogin.anomaly.internal.printer

import com.minogin.anomaly.internal.analyzer.model.*
import com.minogin.anomaly.internal.profiler.model.*

internal class Printer {

    fun printReport(report: Report) {
        println("Anomaly Detector: ${report.referenceVersion} → ${report.currentVersion}")
        println("=".repeat(50))
        if (report.findings.isEmpty()) {
            println("No findings.")
        } else {
            println("FINDINGS")
            println()
            report.findings.sortedByDescending { it.severity }.forEach { finding ->
                when (finding) {
                    is Finding.OutputFormChanged ->
                        println(
                            "[${finding.severity}] Output form changed at '${finding.step}'\n" +
                                    "  Reference: ${finding.referenceOutputForms.joinToString { formatForm(it) }}\n" +
                                    "  Current:   ${finding.currentOutputForms.joinToString { formatForm(it) }}"
                        )
                    is Finding.MultipleOutputFormsPerStep ->
                        println(
                            "[${finding.severity}] Inconsistent output forms at '${finding.step}'\n" +
                                    "  Forms: ${finding.outputForms.joinToString { formatForm(it) }}"
                        )
                    is Finding.TransitionChanged -> {
                        println("[${finding.severity}] Transitions changed at '${finding.step}'")
                        if (finding.addedNextSteps.isNotEmpty())
                            println("  Added:   ${finding.addedNextSteps.joinToString()}")
                        if (finding.removedNextSteps.isNotEmpty())
                            println("  Removed: ${finding.removedNextSteps.joinToString()}")
                    }
                    is Finding.MissingStep -> {
                        println("[${finding.severity}] Step missing: '${finding.step}'")
                        println("  Reference forms: ${finding.referenceOutputForms.joinToString { formatForm(it) }}")
                        if (finding.referenceNextSteps.isNotEmpty())
                            println("  Reference transitions: ${finding.referenceNextSteps.joinToString()}")
                    }
                    is Finding.NewStep -> {
                        println("[${finding.severity}] New step: '${finding.step}'")
                        println("  Current forms: ${finding.currentOutputForms.joinToString { formatForm(it) }}")
                        if (finding.currentNextSteps.isNotEmpty())
                            println("  Current transitions: ${finding.currentNextSteps.joinToString()}")
                    }
                }
            }
        }
        println()
    }

    fun printProfile(label: String, profile: Profile) {
        println("PROFILE ($label)")
        println()
        profile.steps.sortedBy { it.name }.forEach { step ->
            val forms = profile.stepOutputForms[step]?.joinToString { formatForm(it) } ?: "-"
            val transitions = profile.stepTransitions[step]?.joinToString { it.name }
            val transitionStr = if (transitions != null) " → $transitions" else ""
            println("  ${step.name}: $forms$transitionStr")
        }
    }

    private fun formatForm(form: OutputForm): String {
        val schema = form.schema?.let { " $it" } ?: ""
        return if (form.quoted) "quoted(${form.type}$schema)" else "${form.type}$schema"
    }
}
