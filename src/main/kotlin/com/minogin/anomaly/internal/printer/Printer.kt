package com.minogin.anomaly.internal.printer

import com.minogin.anomaly.internal.analyzer.model.*
import com.minogin.anomaly.internal.profiler.model.*

internal class Printer {

    companion object {
        /** Reports are meant to be readable on a projector. */
        const val MAX_WIDTH = 100
        const val MAX_EXAMPLE_LENGTH = 60
    }

    /**
     * Findings print by severity, highest first. Within one severity the order is fixed per kind
     * so that a report reads from "what the step produces" to "where the flow goes":
     * form/schema change, inconsistent forms, then transitions, then missing steps, then new steps.
     */
    private val displayOrder: Comparator<Finding> =
        compareByDescending<Finding> { it.severity }.thenBy {
            when (it) {
                is Finding.OutputFormChanged -> 0
                is Finding.MultipleOutputFormsPerStep -> 1
                is Finding.TransitionChanged -> 2
                is Finding.MissingStep -> 3
                is Finding.RoutingDistributionChanged -> 4
                is Finding.NewStep -> 5
            }
        }

    fun printReport(report: Report) {
        println("Anomaly Detector: ${report.referenceVersion} -> ${report.currentVersion}")
        println("=".repeat(50))
        if (report.findings.isEmpty()) {
            println("No findings.")
        } else {
            println("FINDINGS")
            println()
            report.findings.sortedWith(displayOrder).forEach { finding ->
                when (finding) {
                    is Finding.OutputFormChanged -> printOutputFormChanged(finding)
                    is Finding.MultipleOutputFormsPerStep -> {
                        println("[${finding.severity}] Inconsistent output forms at '${finding.step}'")
                        printForms("Forms:", finding.outputForms, finding.examples)
                    }
                    is Finding.TransitionChanged -> {
                        println("[${finding.severity}] Transitions changed at '${finding.step}'")
                        if (finding.addedNextSteps.isNotEmpty())
                            println("  Added:     ${finding.addedNextSteps.joinToString()}")
                        if (finding.removedNextSteps.isNotEmpty())
                            println("  Removed:   ${finding.removedNextSteps.joinToString()}")
                        if (finding.referenceCounts.isNotEmpty())
                            println("  Reference: ${formatDistribution(finding.referenceCounts)}")
                        if (finding.currentCounts.isNotEmpty())
                            println("  Current:   ${formatDistribution(finding.currentCounts)}")
                    }
                    is Finding.RoutingDistributionChanged -> {
                        println("[${finding.severity}] Routing distribution changed at '${finding.step}'")
                        println("  Reference: ${formatDistribution(finding.referenceCounts)}")
                        println("  Current:   ${formatDistribution(finding.currentCounts)}")
                        println("  Shift:     ${percent(finding.shift)} of routings changed target (threshold ${percent(finding.threshold)})")
                    }
                    is Finding.MissingStep -> {
                        println("[${finding.severity}] Step missing: '${finding.step}'")
                        printForms("Reference forms:", finding.referenceOutputForms, finding.referenceExamples)
                        if (finding.referenceNextSteps.isNotEmpty())
                            println("  Reference transitions: ${finding.referenceNextSteps.joinToString()}")
                    }
                    is Finding.NewStep -> {
                        println("[${finding.severity}] New step: '${finding.step}'")
                        printForms("Current forms:", finding.currentOutputForms, finding.currentExamples)
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
            val transitions = profile.stepTransitions[step]?.map { it.name }?.sorted()?.joinToString()
            val transitionStr = if (transitions != null) " -> $transitions" else ""
            println("  ${step.name}: $forms$transitionStr")
        }
    }

    private fun printOutputFormChanged(finding: Finding.OutputFormChanged) {
        val reference = finding.referenceOutputForms.singleOrNull()
        val current = finding.currentOutputForms.singleOrNull()
        val schemaDiff = schemaOnlyDiff(reference, current)

        val title = if (schemaDiff != null) "JSON schema changed" else "Output form changed"
        println("[${finding.severity}] $title at '${finding.step}'")
        printForms("Reference:", finding.referenceOutputForms, finding.referenceExamples)
        printForms("Current:  ", finding.currentOutputForms, finding.currentExamples)

        if (schemaDiff != null) {
            if (schemaDiff.added.isNotEmpty())
                println("  Added:     ${schemaDiff.added.entries.joinToString { (path, schema) -> "$path: ${schema.format()}" }}")
            if (schemaDiff.removed.isNotEmpty())
                println("  Removed:   ${schemaDiff.removed.entries.joinToString { (path, schema) -> "$path: ${schema.format()}" }}")
            if (schemaDiff.changed.isNotEmpty())
                println("  Changed:   ${schemaDiff.changed.entries.joinToString { (path, change) -> "$path: ${change.first.format()} -> ${change.second.format()}" }}")
        }
    }

    /**
     * Without examples: `  Label: FORM_A, FORM_B` on one line, as before.
     * With examples: one form per line, its example inline after `e.g.` when that fits in
     * [MAX_WIDTH] columns, otherwise on the next line under the form.
     */
    private fun printForms(label: String, forms: Set<OutputForm>, examples: Map<OutputForm, String>) {
        if (examples.isEmpty()) {
            println("  $label ${forms.joinToString { formatForm(it) }}")
            return
        }
        val indent = " ".repeat(2 + label.length + 1)
        forms.forEachIndexed { index, form ->
            val prefix = if (index == 0) "  $label " else indent
            val formText = formatForm(form)
            val example = examples[form]?.let { "e.g. ${abbreviate(it)}" }
            when {
                example == null -> println(prefix + formText)
                (prefix + formText + "  " + example).length <= MAX_WIDTH -> println("$prefix$formText  $example")
                else -> {
                    println(prefix + formText)
                    println(indent + example)
                }
            }
        }
    }

    /** One line, whitespace collapsed, at most [MAX_EXAMPLE_LENGTH] characters. */
    private fun abbreviate(output: String): String {
        val oneLine = output.replace(Regex("\\s+"), " ").trim()
        return if (oneLine.length <= MAX_EXAMPLE_LENGTH) oneLine else oneLine.take(MAX_EXAMPLE_LENGTH - 3) + "..."
    }

    /** Non-null only when both sides are a single form of the same type that differ in schema alone. */
    private fun schemaOnlyDiff(reference: OutputForm?, current: OutputForm?): JsonSchemaDiff? {
        if (reference == null || current == null) return null
        if (reference.type != current.type || reference.quoted != current.quoted) return null
        val referenceSchema = reference.schema ?: return null
        val currentSchema = current.schema ?: return null
        if (referenceSchema == currentSchema) return null
        return JsonSchemaDiff.of(referenceSchema, currentSchema)
    }

    /** `handle-sales 30%, handle-support 70% (10 samples)` */
    private fun formatDistribution(counts: Map<String, Int>): String {
        val total = counts.values.sum()
        val shares = counts.entries.joinToString { (target, count) -> "$target ${percent(count.toDouble() / total)}" }
        return "$shares ($total samples)"
    }

    private fun percent(share: Double): String = "${Math.round(share * 100)}%"

    private fun formatForm(form: OutputForm): String {
        val schema = form.schema?.let { " ${it.format()}" } ?: ""
        return if (form.quoted) "quoted(${form.type}$schema)" else "${form.type}$schema"
    }
}
