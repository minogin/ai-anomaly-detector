package com.minogin.anomaly.internal.store

import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.tracer.model.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.*
import java.nio.file.*
import java.util.*
import kotlin.test.assertFailsWith
import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

class StoreTest {
    @Test
    fun `can append and load checkpoints`(@TempDir tempDir: Path) {
        val store = Store(tempDir)
        val version = Version("1.0.0")
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val timestamp1 = Clock.System.now()
        val timestamp2 = timestamp1 + 1.seconds
        val checkpoints = listOf(
            Checkpoint(id = UUID.randomUUID(), runId = uuid1, timestamp = timestamp1, step = Step("step-1"), input = "input-1", output = "output-1", nextStep = Step("step-2")),
            Checkpoint(id = UUID.randomUUID(), runId = uuid2, timestamp = timestamp2, step = Step("step-2"), input = null, output = "output-2", nextStep = null)
        )

        checkpoints.forEach { store.append(version, it) }

        assertEquals(checkpoints.toSet(), store.load(version).toSet())
    }

    @Test
    fun `load throws when version file does not exist`(@TempDir tempDir: Path) {
        val store = Store(tempDir)
        val ex = assertFailsWith<IllegalStateException> {
            store.load(Version("missing"))
        }
        assertTrue(ex.message!!.contains("missing"))
    }

    @Test
    fun `load deduplicates by id keeping last write`(@TempDir tempDir: Path) {
        val store = Store(tempDir)
        val version = Version("1.0.0")
        val id = UUID.randomUUID()
        val runId = UUID.randomUUID()
        val timestamp = Clock.System.now()

        val original = Checkpoint(id = id, runId = runId, timestamp = timestamp, step = Step("classify"), input = "", output = "HIGH", nextStep = null)
        val updated = original.copy(nextStep = Step("approve"))

        store.append(version, original)
        store.append(version, updated)

        val loaded = store.load(version)
        assertEquals(1, loaded.size)
        assertEquals(Step("approve"), loaded[0].nextStep)
    }
}
