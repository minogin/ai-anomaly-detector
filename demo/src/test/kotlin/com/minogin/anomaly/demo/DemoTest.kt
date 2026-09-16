package com.minogin.anomaly.demo

import com.minogin.anomaly.api.*
import com.minogin.anomaly.internal.analyzer.model.*
import com.minogin.anomaly.internal.profiler.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.*
import java.io.*
import java.nio.file.*
import kotlin.test.*

/**
 * The demo exists to put specific findings on a slide. These tests pin exactly which findings
 * each drifted version produces against the baseline, so a change to the scripts cannot silently
 * change the talk.
 */
class DemoTest {

    private fun report(dir: Path, version: String): Report {
        record(dir.toString(), BASELINE)
        record(dir.toString(), version)
        return AnomalyDetector(dir.toString(), version).report(BASELINE)
    }

    @Test
    fun `baseline against itself is clean`(@TempDir dir: Path) {
        record(dir.toString(), BASELINE)
        val report = AnomalyDetector(dir.toString(), BASELINE).report(BASELINE)
        assertFalse(report.hasProblems(), report.findings.toString())
    }

    @Test
    fun `1_1 bold answers with fallback - form inconsistency and a routing shift, nothing vanishes`(@TempDir dir: Path) {
        val report = report(dir, "1.1")

        assertEquals(
            setOf(
                Finding.MultipleOutputFormsPerStep::class,
                Finding.OutputFormChanged::class,
                Finding.RoutingDistributionChanged::class,
            ),
            report.findings.map { it::class }.toSet(),
            report.findings.toString()
        )
        assertEquals(3, report.findings.size)

        val forms = report.findings.filterIsInstance<Finding.OutputFormChanged>().single()
        assertEquals("classify-query", forms.step)
        assertEquals(setOf(OutputForm.Type.STRING), forms.referenceOutputForms.map { it.type }.toSet())
        assertEquals(setOf(OutputForm.Type.STRING, OutputForm.Type.MARKDOWN), forms.currentOutputForms.map { it.type }.toSet())

        val routing = report.findings.filterIsInstance<Finding.RoutingDistributionChanged>().single()
        assertEquals("classify-query", routing.step)
        assertEquals(mapOf("handle-feedback" to 4, "handle-sales" to 3, "handle-support" to 3), routing.referenceCounts)
        assertEquals(mapOf("handle-feedback" to 2, "handle-sales" to 2, "handle-support" to 6), routing.currentCounts)
        assertEquals(0.3, routing.shift, 1e-9)
    }

    @Test
    fun `1_2 nested offer - a single schema change at handle-sales`(@TempDir dir: Path) {
        val report = report(dir, "1.2")

        val finding = report.findings.single() as Finding.OutputFormChanged
        assertEquals("handle-sales", finding.step)
        val reference = finding.referenceOutputForms.single()
        val current = finding.currentOutputForms.single()
        assertEquals(OutputForm.Type.JSON_OBJECT, reference.type)
        assertEquals(OutputForm.Type.JSON_OBJECT, current.type)
        assertEquals(setOf("reply", "discount"), (reference.schema as JsonSchema.ObjectSchema).fields.keys)
        assertEquals(setOf("reply", "offer"), (current.schema as JsonSchema.ObjectSchema).fields.keys)
    }

    @Test
    fun `1_3 silent reclassification - feedback target and step vanish`(@TempDir dir: Path) {
        val report = report(dir, "1.3")

        assertEquals(
            setOf(Finding.TransitionChanged::class, Finding.MissingStep::class),
            report.findings.map { it::class }.toSet(),
            report.findings.toString()
        )
        assertEquals(2, report.findings.size)

        val transition = report.findings.filterIsInstance<Finding.TransitionChanged>().single()
        assertEquals("classify-query", transition.step)
        assertEquals(setOf("handle-feedback"), transition.removedNextSteps)
        assertTrue(transition.addedNextSteps.isEmpty())
        assertEquals(mapOf("handle-sales" to 3, "handle-support" to 7), transition.currentCounts)

        assertEquals("handle-feedback", report.findings.filterIsInstance<Finding.MissingStep>().single().step)
    }

    @Test
    fun `re-recording a version replaces its data instead of appending`(@TempDir dir: Path) {
        record(dir.toString(), BASELINE)
        val once = Files.readAllLines(dir.resolve("$BASELINE.jsonl")).count { it.isNotBlank() }
        record(dir.toString(), BASELINE)
        val twice = Files.readAllLines(dir.resolve("$BASELINE.jsonl")).count { it.isNotBlank() }
        assertEquals(once, twice)
    }

    @Test
    fun `printed reports fit a projector and contain nothing identifying`(@TempDir dir: Path) {
        ScriptedModel.VERSIONS.forEach { record(dir.toString(), it) }
        val out = capture {
            ScriptedModel.DRIFT_DESCRIPTIONS.keys.forEach { diff(dir.toString(), it, BASELINE) }
        }

        val tooLong = out.lines().filter { it.length > 100 }
        assertTrue(tooLong.isEmpty(), "Lines over 100 columns:\n${tooLong.joinToString("\n")}")
        assertFalse(out.contains(dir.toString()), "report leaks the data directory")
        assertFalse(out.contains(System.getProperty("user.name")), "report leaks the user name")
        assertFalse(Regex("""\d{4}-\d{2}-\d{2}""").containsMatchIn(out), "report contains a date")
    }

    private fun capture(block: () -> Unit): String {
        val original = System.out
        val buffer = ByteArrayOutputStream()
        System.setOut(PrintStream(buffer, true, Charsets.UTF_8))
        try {
            block()
        } finally {
            System.setOut(original)
        }
        return buffer.toString(Charsets.UTF_8)
    }
}
