package com.minogin.anomaly.internal.store

import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.tracer.model.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.*
import java.nio.file.*
import java.util.*
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

class StoreTest {
    @Test
    fun `can save and load checkpoints`(@TempDir tempDir: Path) {
        val store = Store(tempDir)

        val version = Version("1.0.0")
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val timestamp1 = Clock.System.now()
        val timestamp2 = timestamp1 + 1.seconds
        val checkpoints = listOf(
            Checkpoint(
                runId = uuid1,
                timestamp = timestamp1,
                step = Step("step-1"),
                input = "input-1",
                output = "output-1",
                nextStep = Step("step-2")
            ),
            Checkpoint(
                runId = uuid2,
                timestamp = timestamp2,
                step = Step("step-2"),
                input = null,
                output = "output-2",
                nextStep = null
            )
        )
        store.save(version, checkpoints)

        val loadedCheckpoints = store.load(version)
        assertEquals(checkpoints, loadedCheckpoints)
    }
}