package com.minogin.anomaly.internal.renderer

import com.minogin.anomaly.internal.analyzer.model.*
import com.minogin.anomaly.internal.profiler.model.*

internal class ReportRenderer {
    fun printReport(report: Report) {
        println("Anomaly Detector: ${report.referenceVersion} → ${report.currentVersion}")
        println("=".repeat(50))
        if (report.findings.isEmpty()) {
            println("No anomalies detected.")
            return
        }
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

                is Finding.MissingStep ->
                    println("[${finding.severity}] Step missing: '${finding.step}'")

                is Finding.NewStep ->
                    println("[${finding.severity}] New step: '${finding.step}'")
            }
        }
    }

    private fun formatForm(form: OutputForm): String {
        val schema = form.schema?.let { " $it" } ?: ""
        return if (form.quoted) "quoted(${form.type}$schema)" else "${form.type}$schema"
    }
}