package com.minogin.anomaly.internal.printer

import com.minogin.anomaly.internal.analyzer.model.*
import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.profiler.model.*
import com.minogin.anomaly.internal.profiler.model.JsonSchema.Primitive.*
import org.junit.jupiter.api.Test
import java.io.*
import kotlin.test.*

class PrinterTest {
    private fun obj(vararg fields: Pair<String, JsonSchema>) = JsonSchema.ObjectSchema(mapOf(*fields))

    private fun capture(block: () -> Unit): String {
        val original = System.out
        val buffer = ByteArrayOutputStream()
        System.setOut(PrintStream(buffer, true, Charsets.UTF_8))
        try {
            block()
        } finally {
            System.setOut(original)
        }
        return buffer.toString(Charsets.UTF_8).replace("\r\n", "\n")
    }

    private fun report(vararg findings: Finding) =
        Report(currentVersion = "1.1", referenceVersion = "1.0", findings = findings.toList())

    @Test
    fun `prints no findings`() {
        val out = capture { Printer().printReport(report()) }
        assertEquals(
            """
            Anomaly Detector: 1.0 -> 1.1
            ==================================================
            No findings.


            """.trimIndent(),
            out
        )
    }

    @Test
    fun `labels a type change as output form changed`() {
        val out = capture {
            Printer().printReport(
                report(
                    Finding.OutputFormChanged(
                        step = "classify-query",
                        referenceOutputForms = setOf(OutputForm(OutputForm.Type.STRING, quoted = false)),
                        currentOutputForms = setOf(OutputForm(OutputForm.Type.MARKDOWN, quoted = false)),
                    )
                )
            )
        }
        assertContains(
            out,
            """
            [HIGH] Output form changed at 'classify-query'
              Reference: STRING
              Current:   MARKDOWN
            """.trimIndent()
        )
        assertFalse(out.contains("Added:"))
        assertFalse(out.contains("Removed:"))
    }

    @Test
    fun `labels a schema-only change as json schema changed with field details`() {
        val out = capture {
            Printer().printReport(
                report(
                    Finding.OutputFormChanged(
                        step = "handle-sales",
                        referenceOutputForms = setOf(
                            OutputForm(OutputForm.Type.JSON_OBJECT, quoted = false, schema = obj("reply" to STRING, "discount" to INTEGER, "valid" to BOOLEAN))
                        ),
                        currentOutputForms = setOf(
                            OutputForm(OutputForm.Type.JSON_OBJECT, quoted = false, schema = obj("reply" to STRING, "offer" to obj("discount" to INTEGER), "valid" to STRING))
                        ),
                    )
                )
            )
        }
        assertContains(
            out,
            """
            [HIGH] JSON schema changed at 'handle-sales'
              Reference: JSON_OBJECT {reply=STRING, discount=INTEGER, valid=BOOLEAN}
              Current:   JSON_OBJECT {reply=STRING, offer={discount=INTEGER}, valid=STRING}
              Added:     offer: {discount=INTEGER}
              Removed:   discount: INTEGER
              Changed:   valid: BOOLEAN -> STRING
            """.trimIndent()
        )
    }

    @Test
    fun `a json object becoming a quoted json object is an output form change even with equal schema`() {
        val schema = obj("reply" to STRING)
        val out = capture {
            Printer().printReport(
                report(
                    Finding.OutputFormChanged(
                        step = "handle-sales",
                        referenceOutputForms = setOf(OutputForm(OutputForm.Type.JSON_OBJECT, quoted = false, schema = schema)),
                        currentOutputForms = setOf(OutputForm(OutputForm.Type.JSON_OBJECT, quoted = true, schema = schema)),
                    )
                )
            )
        }
        assertContains(out, "[HIGH] Output form changed at 'handle-sales'")
        assertContains(out, "Current:   quoted(JSON_OBJECT {reply=STRING})")
    }

    @Test
    fun `multiple forms on one side never get the schema label`() {
        val out = capture {
            Printer().printReport(
                report(
                    Finding.OutputFormChanged(
                        step = "handle-sales",
                        referenceOutputForms = setOf(OutputForm(OutputForm.Type.JSON_OBJECT, quoted = false, schema = obj("a" to STRING))),
                        currentOutputForms = setOf(
                            OutputForm(OutputForm.Type.JSON_OBJECT, quoted = false, schema = obj("b" to STRING)),
                            OutputForm(OutputForm.Type.STRING, quoted = false),
                        ),
                    )
                )
            )
        }
        assertContains(out, "[HIGH] Output form changed at 'handle-sales'")
        assertContains(out, "Current:   JSON_OBJECT {b=STRING}, STRING")
    }

    @Test
    fun `prints findings by severity then in fixed kind order regardless of input order`() {
        val string = OutputForm(OutputForm.Type.STRING, false)
        val markdown = OutputForm(OutputForm.Type.MARKDOWN, false)
        // Deliberately scrambled: analyzer insertion order must not matter.
        val out = capture {
            Printer().printReport(
                report(
                    Finding.NewStep("extra", setOf(string), emptySet()),
                    Finding.MissingStep("handle-feedback", setOf(string), emptySet()),
                    Finding.MultipleOutputFormsPerStep("summarize", setOf(string, markdown)),
                    Finding.TransitionChanged("classify-query", addedNextSteps = emptySet(), removedNextSteps = setOf("handle-feedback")),
                    Finding.OutputFormChanged("classify-query", currentOutputForms = setOf(markdown), referenceOutputForms = setOf(string)),
                )
            )
        }
        val headers = out.lines().filter { it.startsWith("[") }
        assertEquals(
            listOf(
                "[HIGH] Output form changed at 'classify-query'",
                "[HIGH] Inconsistent output forms at 'summarize'",
                "[MID] Transitions changed at 'classify-query'",
                "[MID] Step missing: 'handle-feedback'",
                "[LOW] New step: 'extra'",
            ),
            headers
        )
    }

    @Test
    fun `profile lists steps and their targets sorted by name`() {
        val string = OutputForm(OutputForm.Type.STRING, false)
        val profile = Profile(
            version = Version("1.1"),
            steps = setOf(Step("summarize"), Step("classify-query")),
            stepOutputForms = mapOf(Step("summarize") to setOf(string), Step("classify-query") to setOf(string)),
            stepTransitionCounts = mapOf(
                Step("classify-query") to linkedMapOf(Step("handle-support") to 6, Step("handle-feedback") to 2, Step("handle-sales") to 2)
            ),
        )
        val out = capture { Printer().printProfile("1.1", profile) }
        assertEquals(
            """
            PROFILE (1.1)

              classify-query: STRING -> handle-feedback, handle-sales, handle-support
              summarize: STRING

            """.trimIndent(),
            out
        )
    }

    @Test
    fun `routing distribution finding prints both distributions and the shift against the threshold`() {
        val out = capture {
            Printer().printReport(
                report(
                    Finding.RoutingDistributionChanged(
                        step = "classify-query",
                        referenceCounts = mapOf("handle-feedback" to 4, "handle-sales" to 3, "handle-support" to 3),
                        currentCounts = mapOf("handle-feedback" to 2, "handle-sales" to 1, "handle-support" to 7),
                        shift = 0.4,
                        threshold = 0.25,
                    )
                )
            )
        }
        assertContains(
            out,
            """
            [MID] Routing distribution changed at 'classify-query'
              Reference: handle-feedback 40%, handle-sales 30%, handle-support 30% (10 samples)
              Current:   handle-feedback 20%, handle-sales 10%, handle-support 70% (10 samples)
              Shift:     40% of routings changed target (threshold 25%)
            """.trimIndent()
        )
    }

    @Test
    fun `routing distribution finding prints after step missing and before new step`() {
        val string = OutputForm(OutputForm.Type.STRING, false)
        val out = capture {
            Printer().printReport(
                report(
                    Finding.NewStep("extra", setOf(string), emptySet()),
                    Finding.RoutingDistributionChanged("classify-query", mapOf("a" to 5, "b" to 5), mapOf("a" to 9, "b" to 1), 0.4, 0.25),
                    Finding.MissingStep("handle-feedback", setOf(string), emptySet()),
                )
            )
        }
        assertEquals(
            listOf(
                "[MID] Step missing: 'handle-feedback'",
                "[MID] Routing distribution changed at 'classify-query'",
                "[LOW] New step: 'extra'",
            ),
            out.lines().filter { it.startsWith("[") }
        )
    }

    @Test
    fun `transitions changed prints the distributions when counts are known`() {
        val out = capture {
            Printer().printReport(
                report(
                    Finding.TransitionChanged(
                        step = "classify-query",
                        addedNextSteps = emptySet(),
                        removedNextSteps = setOf("handle-feedback"),
                        referenceCounts = mapOf("handle-feedback" to 4, "handle-sales" to 3, "handle-support" to 3),
                        currentCounts = mapOf("handle-sales" to 3, "handle-support" to 7),
                    )
                )
            )
        }
        assertContains(
            out,
            """
            [MID] Transitions changed at 'classify-query'
              Removed:   handle-feedback
              Reference: handle-feedback 40%, handle-sales 30%, handle-support 30% (10 samples)
              Current:   handle-sales 30%, handle-support 70% (10 samples)
            """.trimIndent()
        )
    }

    @Test
    fun `transitions changed without counts prints only added and removed`() {
        val out = capture {
            Printer().printReport(
                report(Finding.TransitionChanged("classify-query", addedNextSteps = setOf("escalate"), removedNextSteps = emptySet()))
            )
        }
        assertContains(out, "[MID] Transitions changed at 'classify-query'\n  Added:     escalate\n")
        assertFalse(out.contains("Reference:"))
        assertFalse(out.contains("Current:"))
    }
}
