package com.minogin.anomaly.internal.tracer

import com.minogin.anomaly.internal.common.model.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

class TracerTest {
    @Test
    fun `checkpoints are recorded correctly`() {
        val now = Clock.System.now()
        val clock = object : Clock {
            private var currentTime = now

            override fun now(): Instant {
                return currentTime
            }

            fun advanceBy(duration: Duration) {
                currentTime = currentTime.plus(duration)
            }
        }
        val tracer = Tracer(
            clock = clock
        )

        tracer.checkpoint(
            step = Step("step1"),
            input = "input1",
            output = "output1",
            nextStep = Step("step2")
        )

        clock.advanceBy(1.seconds)

        tracer.checkpoint(
            step = Step("step2"),
            output = "output2"
        )

        val checkpoints = tracer.checkpoints()

        assertEquals(2, checkpoints.size)

        with(checkpoints[0]) {
            assertEquals(Step("step1"), step)
            assertEquals("input1", input)
            assertEquals("output1", output)
            assertEquals(Step("step2"), nextStep)
            assertEquals(now, timestamp)
        }

        with(checkpoints[1]) {
            assertEquals(Step("step2"), step)
            assertNull(input)
            assertEquals("output2", output)
            assertNull(nextStep)
            assertEquals(now.plus(1.seconds), timestamp)
        }

        assertEquals(checkpoints[0].runId, checkpoints[1].runId)
    }
}