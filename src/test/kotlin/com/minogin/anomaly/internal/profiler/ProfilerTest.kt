package com.minogin.anomaly.internal.profiler

import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.profiler.model.*
import com.minogin.anomaly.internal.profiler.model.OutputForm.Type.*
import com.minogin.anomaly.internal.tracer.model.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.*
import kotlin.time.*

class ProfilerTest {
    @Test
    fun `can profile checkpoints`() {
        val profiler = Profiler()

        val version = Version("1.0.0")
        val checkpoints = listOf(
            checkpoint("step-1", "input-1", "output-1", "step-2"),
            checkpoint("step-1", "input-1", "123", "step-2"),
            checkpoint("step-2", "input-2", "output-2", null),
            checkpoint("step-3", null, "<html></html>", null),
        )

        val profile = profiler.profile(version, checkpoints)

        assertEquals(version, profile.version)
        assertEquals(
            setOf(
                Step("step-1"),
                Step("step-2"),
                Step("step-3"),
            ),
            profile.steps
        )
        assertEquals(
            mapOf(
                Step("step-1") to setOf(
                    OutputForm(type = STRING, quoted = false),
                    OutputForm(type = INTEGER, quoted = false)
                ),
                Step("step-2") to setOf(
                    OutputForm(type = STRING, quoted = false)
                ),
                Step("step-3") to setOf(
                    OutputForm(type = HTML, quoted = false)
                )
            ),
            profile.stepOutputForms
        )
    }

    @Test
    fun `profiles step transitions from nextStep fields`() {
        val profiler = Profiler()
        val checkpoints = listOf(
            checkpoint("classify", null, "HIGH", "escalate"),
            checkpoint("classify", null, "LOW", "approve"),
            checkpoint("classify", null, "LOW", "approve"),
        )

        val profile = profiler.profile(Version("1.0"), checkpoints)

        assertEquals(
            mapOf(Step("classify") to setOf(Step("escalate"), Step("approve"))),
            profile.stepTransitions
        )
    }

    @Test
    fun `steps with no nextStep have no transitions`() {
        val profiler = Profiler()
        val checkpoints = listOf(checkpoint("final-step", null, "done", null))

        val profile = profiler.profile(Version("1.0"), checkpoints)

        assertTrue(profile.stepTransitions.isEmpty())
    }

    private fun checkpoint(
        step: String,
        input: String?,
        output: String,
        nextStep: String?
    ) = Checkpoint(
        id = UUID.randomUUID(),
        runId = UUID.randomUUID(),
        timestamp = Clock.System.now(),
        step = Step(step),
        input = input,
        output = output,
        nextStep = nextStep?.let { Step(it) }
    )
}