package com.minogin.anomaly.internal.analyzer

import com.minogin.anomaly.api.*
import com.minogin.anomaly.internal.analyzer.model.*
import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.profiler.model.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class AnalyzerTest {
    @Test
    fun `can analyze profiles and detect anomalies`() {
        val analyzer = Analyzer()
        val referenceProfile = Profile(
            version = Version("1.0"),
            steps = setOf(
                Step("Step 1"),
                Step("Step 2"),
                Step("Step 3")
            ),
            stepOutputForms = mapOf(
                Step("Step 1") to setOf(OutputForm(type = OutputForm.Type.INTEGER, quoted = false)),
                Step("Step 2") to setOf(OutputForm(type = OutputForm.Type.STRING, quoted = true)),
                Step("Step 3") to setOf(OutputForm(type = OutputForm.Type.MARKDOWN, quoted = false))
            )
        )
        val currentProfile = Profile(
            version = Version("1.1"),
            steps = setOf(
                Step("Step 2"),
                Step("Step 3"),
                Step("Step 4")
            ),
            stepOutputForms = mapOf(
                Step("Step 2") to setOf(OutputForm(type = OutputForm.Type.STRING, quoted = true)),
                Step("Step 3") to setOf(OutputForm(type = OutputForm.Type.MARKDOWN, quoted = false)),
                Step("Step 4") to setOf(OutputForm(type = OutputForm.Type.JSON_OBJECT, quoted = false))
            )
        )

        val report = analyzer.report(currentProfile, referenceProfile)
        assertEquals("1.1", report.currentVersion)
        assertEquals("1.0", report.referenceVersion)
        assertEquals(2, report.findings.size)

        val missingStepFinding = report.findings.filterIsInstance<Finding.MissingStep>().singleOrNull()
        assertNotNull(missingStepFinding)
        assertEquals("Step 1", missingStepFinding.step)
    }

    @Test
    fun `detects when a step gains a new transition`() {
        val analyzer = Analyzer()
        val step = Step("classify")
        val profile = Profile(
            version = Version("1.0"),
            steps = setOf(step),
            stepOutputForms = mapOf(step to setOf(OutputForm(OutputForm.Type.STRING, false))),
            stepTransitionCounts = mapOf(step to mapOf(Step("approve") to 1, Step("reject") to 1))
        )
        val updatedProfile = Profile(
            version = Version("1.1"),
            steps = setOf(step),
            stepOutputForms = mapOf(step to setOf(OutputForm(OutputForm.Type.STRING, false))),
            stepTransitionCounts = mapOf(step to mapOf(Step("approve") to 1, Step("reject") to 1, Step("escalate") to 1))
        )

        val report = analyzer.report(updatedProfile, profile)
        val finding = report.findings.filterIsInstance<Finding.TransitionChanged>().singleOrNull()
        assertNotNull(finding)
        assertEquals("classify", finding.step)
        assertEquals(setOf("escalate"), finding.addedNextSteps)
        assertTrue(finding.removedNextSteps.isEmpty())
    }

    @Test
    fun `detects when a step loses a transition`() {
        val analyzer = Analyzer()
        val step = Step("classify")
        val profile = Profile(
            version = Version("1.0"),
            steps = setOf(step),
            stepOutputForms = mapOf(step to setOf(OutputForm(OutputForm.Type.STRING, false))),
            stepTransitionCounts = mapOf(step to mapOf(Step("approve") to 1, Step("reject") to 1))
        )
        val updatedProfile = Profile(
            version = Version("1.1"),
            steps = setOf(step),
            stepOutputForms = mapOf(step to setOf(OutputForm(OutputForm.Type.STRING, false))),
            stepTransitionCounts = mapOf(step to mapOf(Step("approve") to 1))
        )

        val report = analyzer.report(updatedProfile, profile)
        val finding = report.findings.filterIsInstance<Finding.TransitionChanged>().singleOrNull()
        assertNotNull(finding)
        assertEquals("classify", finding.step)
        assertTrue(finding.addedNextSteps.isEmpty())
        assertEquals(setOf("reject"), finding.removedNextSteps)
    }

    @Test
    fun `does not flag unchanged transitions`() {
        val analyzer = Analyzer()
        val step = Step("classify")
        val transitions = mapOf(step to mapOf(Step("approve") to 1, Step("reject") to 1))
        val profile = Profile(
            version = Version("1.0"),
            steps = setOf(step),
            stepOutputForms = mapOf(step to setOf(OutputForm(OutputForm.Type.STRING, false))),
            stepTransitionCounts = transitions
        )
        val updatedProfile = profile.copy(version = Version("1.1"))

        val report = analyzer.report(updatedProfile, profile)
        assertTrue(report.findings.filterIsInstance<Finding.TransitionChanged>().isEmpty())
    }

    @Test
    fun `detects output form changed between versions`() {
        val analyzer = Analyzer()
        val step = Step("classify")
        val reference = Profile(
            version = Version("1.0"),
            steps = setOf(step),
            stepOutputForms = mapOf(step to setOf(OutputForm(OutputForm.Type.JSON_OBJECT, false)))
        )
        val current = Profile(
            version = Version("1.1"),
            steps = setOf(step),
            stepOutputForms = mapOf(step to setOf(OutputForm(OutputForm.Type.STRING, true)))
        )

        val report = analyzer.report(current, reference)
        val finding = report.findings.filterIsInstance<Finding.OutputFormChanged>().singleOrNull()
        assertNotNull(finding)
        assertEquals("classify", finding.step)
        assertEquals(setOf(OutputForm(OutputForm.Type.JSON_OBJECT, false)), finding.referenceOutputForms)
        assertEquals(setOf(OutputForm(OutputForm.Type.STRING, true)), finding.currentOutputForms)
    }

    @Test
    fun `detects multiple output forms within same version`() {
        val analyzer = Analyzer()
        val step = Step("classify")
        val profile = Profile(
            version = Version("1.0"),
            steps = setOf(step),
            stepOutputForms = mapOf(
                step to setOf(
                    OutputForm(OutputForm.Type.JSON_OBJECT, false),
                    OutputForm(OutputForm.Type.STRING, false)
                )
            )
        )

        val report = analyzer.report(profile, profile)
        val finding = report.findings.filterIsInstance<Finding.MultipleOutputFormsPerStep>().singleOrNull()
        assertNotNull(finding)
        assertEquals("classify", finding.step)
        assertEquals(2, finding.outputForms.size)
    }

    // --- routing distribution ---

    private val step = Step("classify-query")
    private val support = Step("handle-support")
    private val sales = Step("handle-sales")
    private val feedback = Step("handle-feedback")

    private fun routingProfile(version: String, counts: Map<Step, Int>) = Profile(
        version = Version(version),
        steps = setOf(step),
        stepOutputForms = mapOf(step to setOf(OutputForm(OutputForm.Type.STRING, false))),
        stepTransitionCounts = mapOf(step to counts)
    )

    @Test
    fun `routing shift is half the summed absolute share differences`() {
        assertEquals(0.0, Analyzer.routingShift(mapOf(support to 3, sales to 7), mapOf(support to 6, sales to 14)), 1e-9)
        assertEquals(0.4, Analyzer.routingShift(mapOf(support to 3, sales to 3, feedback to 4), mapOf(support to 7, sales to 1, feedback to 2)), 1e-9)
        assertEquals(1.0, Analyzer.routingShift(mapOf(support to 10), mapOf(sales to 10)), 1e-9)
    }

    @Test
    fun `reports a routing distribution change when targets are the same and the shift reaches the threshold`() {
        val reference = routingProfile("1.0", mapOf(support to 3, sales to 3, feedback to 4))
        val current = routingProfile("1.1", mapOf(support to 7, sales to 1, feedback to 2))

        val report = Analyzer().report(current, reference)

        val finding = report.findings.filterIsInstance<Finding.RoutingDistributionChanged>().single()
        assertEquals(1, report.findings.size, report.findings.toString())
        assertEquals("classify-query", finding.step)
        assertEquals(mapOf("handle-feedback" to 4, "handle-sales" to 3, "handle-support" to 3), finding.referenceCounts)
        assertEquals(mapOf("handle-feedback" to 2, "handle-sales" to 1, "handle-support" to 7), finding.currentCounts)
        assertEquals(0.4, finding.shift, 1e-9)
        assertEquals(0.25, finding.threshold)
    }

    @Test
    fun `does not report a shift below the threshold`() {
        val reference = routingProfile("1.0", mapOf(support to 5, sales to 5))
        val current = routingProfile("1.1", mapOf(support to 7, sales to 3))

        assertTrue(Analyzer().report(current, reference).findings.isEmpty())
        assertEquals(1, Analyzer(AnalyzerConfig(routingShiftThreshold = 0.2)).report(current, reference).findings.size)
    }

    @Test
    fun `when the target set changed only transitions changed is reported and it carries the counts`() {
        val reference = routingProfile("1.0", mapOf(support to 3, sales to 3, feedback to 4))
        val current = routingProfile("1.1", mapOf(support to 7, sales to 3))

        val report = Analyzer().report(current, reference)

        val finding = report.findings.filterIsInstance<Finding.TransitionChanged>().single()
        assertEquals(1, report.findings.size, report.findings.toString())
        assertEquals(setOf("handle-feedback"), finding.removedNextSteps)
        assertEquals(mapOf("handle-feedback" to 4, "handle-sales" to 3, "handle-support" to 3), finding.referenceCounts)
        assertEquals(mapOf("handle-sales" to 3, "handle-support" to 7), finding.currentCounts)
    }

    @Test
    fun `a vanished target is reported regardless of how small its share was`() {
        val reference = routingProfile("1.0", (1..100).associate { Step("target-$it") to 1 })
        val current = routingProfile("1.1", (1..99).associate { Step("target-$it") to 1 } + (Step("target-1") to 2))

        val finding = Analyzer().report(current, reference).findings.filterIsInstance<Finding.TransitionChanged>().single()
        assertEquals(setOf("target-100"), finding.removedNextSteps)
    }

    @Test
    fun `skips the distribution check when a version has too few samples`() {
        val reference = routingProfile("1.0", mapOf(support to 2, sales to 2))
        val current = routingProfile("1.1", mapOf(support to 4, sales to 1))

        assertTrue(Analyzer().report(current, reference).findings.isEmpty())
        assertEquals(1, Analyzer(AnalyzerConfig(routingMinSamples = 4)).report(current, reference).findings.size)
    }

    @Test
    fun `skips the distribution check when there are too many targets`() {
        val referenceCounts = (1..11).associate { Step("target-$it") to 10 }
        val currentCounts = referenceCounts + (Step("target-1") to 100)
        val reference = routingProfile("1.0", referenceCounts)
        val current = routingProfile("1.1", currentCounts)

        assertTrue(Analyzer().report(current, reference).findings.isEmpty())
        assertEquals(1, Analyzer(AnalyzerConfig(routingMaxTargets = 11)).report(current, reference).findings.size)
    }

    @Test
    fun `steps that never route produce no routing findings`() {
        val profile = Profile(
            version = Version("1.0"),
            steps = setOf(step),
            stepOutputForms = mapOf(step to setOf(OutputForm(OutputForm.Type.STRING, false))),
        )
        assertTrue(Analyzer().report(profile.copy(version = Version("1.1")), profile).findings.isEmpty())
    }

    @Test
    fun `config rejects nonsensical values`() {
        assertFailsWith<IllegalArgumentException> { AnalyzerConfig(routingShiftThreshold = 1.5) }
        assertFailsWith<IllegalArgumentException> { AnalyzerConfig(routingShiftThreshold = -0.1) }
        assertFailsWith<IllegalArgumentException> { AnalyzerConfig(routingMinSamples = 0) }
        assertFailsWith<IllegalArgumentException> { AnalyzerConfig(routingMaxTargets = 1) }
    }

    @Test
    fun `does not flag unchanged output forms`() {
        val analyzer = Analyzer()
        val step = Step("classify")
        val forms = mapOf(step to setOf(OutputForm(OutputForm.Type.JSON_OBJECT, false)))
        val profile = Profile(version = Version("1.0"), steps = setOf(step), stepOutputForms = forms)
        val updated = profile.copy(version = Version("1.1"))

        val report = analyzer.report(updated, profile)
        assertTrue(report.findings.filterIsInstance<Finding.OutputFormChanged>().isEmpty())
    }
}