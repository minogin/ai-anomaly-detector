package com.minogin.anomaly.internal.analyzer

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
}