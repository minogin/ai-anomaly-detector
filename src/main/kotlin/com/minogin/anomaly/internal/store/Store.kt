package com.minogin.anomaly.internal.store

import com.minogin.anomaly.internal.common.model.*
import com.minogin.anomaly.internal.tracer.model.*
import tools.jackson.module.kotlin.*
import java.nio.file.*
import kotlin.io.path.*

internal class Store(
    private val basePath: Path
) {
    companion object {
        private val objectMapper = jacksonObjectMapper()

        /** One lock for all stores: two detectors in one process may write the same file. */
        private val writeLock = Any()
    }

    fun append(version: Version, checkpoint: Checkpoint) {
        val path = samplesPath(version)
        val line = objectMapper.writeValueAsString(checkpoint) + "\n"
        // Requests running in parallel must not interleave their lines.
        synchronized(writeLock) {
            Files.createDirectories(path.parent)
            Files.writeString(path, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        }
    }

    fun load(version: Version): List<Checkpoint> {
        val path = samplesPath(version)

        if (!Files.exists(path)) {
            throw IllegalStateException("No data for version '$version', run the app with currentVersion=\"$version\" first")
        }

        return Files.readAllLines(path)
            .asSequence()
            .filter { it.isNotBlank() }
            .map { line -> objectMapper.readValue<Checkpoint>(line) }
            .associateBy { it.id }
            .values
            .toList()
    }

    private fun samplesPath(version: Version): Path =
        Path("$basePath/$version.jsonl")
}
