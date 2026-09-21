package com.minogin.anomaly.api

import com.minogin.anomaly.internal.analyzer.model.*
import com.minogin.anomaly.internal.profiler.model.*
import com.minogin.anomaly.internal.profiler.model.JsonSchema.Primitive.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.*
import java.nio.file.*
import java.util.concurrent.*
import kotlin.test.*

/** End-to-end: record two versions through the public API, then compare them. */
class AnomalyDetectorTest {

    private fun runWorkflow(detector: AnomalyDetector, routerOutput: String, salesOutput: String) {
        detector.checkpoint(step = "classify-query", input = "I want a discount", output = routerOutput)
            .nextStep("handle-sales")
        detector.checkpoint(step = "handle-sales", input = "I want a discount", output = salesOutput)
    }

    @Test
    fun `identical versions produce no findings`(@TempDir dir: Path) {
        runWorkflow(AnomalyDetector(dir.toString(), "1.0"), "sales", """{"reply":"Sure","discount":10}""")
        runWorkflow(AnomalyDetector(dir.toString(), "1.1"), "sales", """{"reply":"Sure","discount":10}""")

        val report = AnomalyDetector(dir.toString(), "1.1").report(referenceVersion = "1.0")

        assertEquals("1.0", report.referenceVersion)
        assertEquals("1.1", report.currentVersion)
        assertFalse(report.hasProblems())
    }

    @Test
    fun `detects form change and schema change between recorded versions`(@TempDir dir: Path) {
        runWorkflow(AnomalyDetector(dir.toString(), "1.0"), "sales", """{"reply":"Sure","discount":10}""")
        runWorkflow(AnomalyDetector(dir.toString(), "1.1"), "**sales**", """{"reply":"Sure","offer":{"discount":10}}""")

        val report = AnomalyDetector(dir.toString(), "1.1").report(referenceVersion = "1.0")

        assertEquals(2, report.findings.size, report.findings.toString())

        val router = report.findings.filterIsInstance<Finding.OutputFormChanged>().single { it.step == "classify-query" }
        assertEquals(setOf(OutputForm(OutputForm.Type.STRING, false)), router.referenceOutputForms)
        assertEquals(setOf(OutputForm(OutputForm.Type.MARKDOWN, false)), router.currentOutputForms)

        val sales = report.findings.filterIsInstance<Finding.OutputFormChanged>().single { it.step == "handle-sales" }
        val referenceSchema = sales.referenceOutputForms.single().schema
        val currentSchema = sales.currentOutputForms.single().schema
        assertEquals(JsonSchema.ObjectSchema(mapOf("reply" to STRING, "discount" to INTEGER)), referenceSchema)
        assertEquals(
            JsonSchema.ObjectSchema(mapOf("reply" to STRING, "offer" to JsonSchema.ObjectSchema(mapOf("discount" to INTEGER)))),
            currentSchema
        )
    }

    @Test
    fun `routing recorded on the handle after the checkpoint is part of the profile`(@TempDir dir: Path) {
        val v1 = AnomalyDetector(dir.toString(), "1.0")
        v1.checkpoint("classify-query", "hi", "support").nextStep("handle-support")
        v1.checkpoint("classify-query", "buy", "sales").nextStep("handle-sales")

        val v2 = AnomalyDetector(dir.toString(), "1.1")
        v2.checkpoint("classify-query", "hi", "support").nextStep("handle-support")
        v2.checkpoint("classify-query", "buy", "support").nextStep("handle-support")

        val report = v2.report(referenceVersion = "1.0")

        val transition = report.findings.filterIsInstance<Finding.TransitionChanged>().single()
        assertEquals("classify-query", transition.step)
        assertEquals(setOf("handle-sales"), transition.removedNextSteps)
        assertTrue(transition.addedNextSteps.isEmpty())
    }

    @Test
    fun `a handle routes its own checkpoint even when a later checkpoint of the same step exists`(@TempDir dir: Path) {
        val reference = AnomalyDetector(dir.toString(), "1.0")
        reference.checkpoint("classify-query", "a", "support").nextStep("handle-support")
        reference.checkpoint("classify-query", "b", "sales").nextStep("handle-sales")

        // Two requests overlap: both checkpoints are written before either routing decision.
        val current = AnomalyDetector(dir.toString(), "1.1")
        val first = current.checkpoint("classify-query", "a", "support")
        val second = current.checkpoint("classify-query", "b", "sales")
        first.nextStep("handle-support")
        second.nextStep("handle-sales")

        assertFalse(current.report("1.0").hasProblems(), "each routing must land on its own checkpoint")
    }

    @Test
    fun `calling nextStep again replaces the earlier value and blank is rejected`(@TempDir dir: Path) {
        val reference = AnomalyDetector(dir.toString(), "1.0")
        reference.checkpoint("classify-query", "a", "sales").nextStep("handle-sales")

        val current = AnomalyDetector(dir.toString(), "1.1")
        val handle = current.checkpoint("classify-query", "a", "sales")
        handle.nextStep("handle-support")
        handle.nextStep("handle-sales")

        assertFalse(current.report("1.0").hasProblems())
        assertFailsWith<IllegalArgumentException> { handle.nextStep(" ") }
    }

    @Test
    fun `routing stays correct when many requests run in parallel through one detector`(@TempDir dir: Path) {
        val targets = listOf("handle-support", "handle-sales", "handle-feedback")
        val requests = 300

        val reference = AnomalyDetector(dir.toString(), "1.0")
        repeat(requests) { i -> reference.checkpoint("classify-query", "m$i", "word").nextStep(targets[i % 3]) }

        val current = AnomalyDetector(dir.toString(), "1.1", AnalyzerConfig(routingShiftThreshold = 0.001))
        val pool = Executors.newFixedThreadPool(16)
        try {
            (0 until requests).map { i ->
                pool.submit {
                    val handle = current.checkpoint("classify-query", "m$i", "word")
                    Thread.yield()
                    handle.nextStep(targets[i % 3])
                }
            }.forEach { it.get(30, TimeUnit.SECONDS) }
        } finally {
            pool.shutdown()
        }

        // One routing landing on the wrong checkpoint, or one lost line, shifts the shares by more than 0.1%.
        val report = current.report("1.0")
        assertFalse(report.hasProblems(), report.findings.toString())
    }

    private fun route(detector: AnomalyDetector, vararg targets: String) {
        targets.forEach { target ->
            detector.checkpoint("classify-query", "message", target.removePrefix("handle-")).nextStep(target)
        }
    }

    @Test
    fun `detects a routing distribution shift when the targets stay the same`(@TempDir dir: Path) {
        val support = "handle-support"
        val sales = "handle-sales"
        val feedback = "handle-feedback"
        route(AnomalyDetector(dir.toString(), "1.0"), support, support, support, sales, sales, sales, feedback, feedback, feedback, feedback)
        route(AnomalyDetector(dir.toString(), "1.1"), support, support, support, support, support, support, support, sales, feedback, feedback)

        val report = AnomalyDetector(dir.toString(), "1.1").report(referenceVersion = "1.0")

        val finding = report.findings.filterIsInstance<Finding.RoutingDistributionChanged>().single()
        assertEquals(1, report.findings.size, report.findings.toString())
        assertEquals(mapOf(feedback to 4, sales to 3, support to 3), finding.referenceCounts)
        assertEquals(mapOf(feedback to 2, sales to 1, support to 7), finding.currentCounts)
        assertEquals(0.4, finding.shift, 1e-9)
        assertEquals(0.25, finding.threshold)
    }

    @Test
    fun `a custom threshold is honoured`(@TempDir dir: Path) {
        val support = "handle-support"
        val sales = "handle-sales"
        route(AnomalyDetector(dir.toString(), "1.0"), support, support, support, support, support, sales, sales, sales, sales, sales)
        route(AnomalyDetector(dir.toString(), "1.1"), support, support, support, support, support, support, sales, sales, sales, sales)

        val default = AnomalyDetector(dir.toString(), "1.1").report("1.0")
        assertFalse(default.hasProblems(), "10% shift is below the default threshold")

        val strict = AnomalyDetector(dir.toString(), "1.1", AnalyzerConfig(routingShiftThreshold = 0.1)).report("1.0")
        assertEquals(1, strict.findings.filterIsInstance<Finding.RoutingDistributionChanged>().size)
    }

    @Test
    fun `report fails clearly when the reference version was never recorded`(@TempDir dir: Path) {
        val detector = AnomalyDetector(dir.toString(), "1.1")
        detector.checkpoint("classify-query", "hi", "support")

        val ex = assertFailsWith<IllegalStateException> { detector.report(referenceVersion = "1.0") }
        assertTrue(ex.message!!.contains("1.0"), ex.message)
    }
}
