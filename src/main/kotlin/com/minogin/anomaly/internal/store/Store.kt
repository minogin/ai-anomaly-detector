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
    }

    fun save(
        version: Version,
        checkpoints: List<Checkpoint>,
    ) {
        val path = samplesPath(version)
        Files.createDirectories(path.parent)

        val lines = checkpoints
            .joinToString(separator = "\n", postfix = "\n") { objectMapper.writeValueAsString(it) }

        Files.writeString(
            path,
            lines,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        )
    }

    fun load(version: Version): List<Checkpoint> {
        val path = samplesPath(version)

        if (!Files.exists(path)) {
            return emptyList()
        }

        return Files.readAllLines(path)
            .asSequence()
            .filter { it.isNotBlank() }
            .map { line -> objectMapper.readValue<Checkpoint>(line) }
            .toList()
    }

    private fun samplesPath(version: Version): Path =
        Path("$basePath/$version.jsonl")
}