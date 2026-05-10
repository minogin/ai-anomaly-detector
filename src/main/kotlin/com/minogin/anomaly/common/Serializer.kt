package com.minogin.anomaly.common

import com.minogin.anomaly.tracer.model.*
import tools.jackson.module.kotlin.*
import java.nio.file.*
import kotlin.io.path.*

internal class Serializer(
    private val basePath: String
) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
    }

    fun save(
        checkpoints: Collection<Checkpoint>,
        version: String
    ) {
        val path = samplesPath(version)
        Files.createDirectories(path.parent)

        val lines = checkpoints
            .sortedWith(compareBy<Checkpoint> { it.step }
                .thenBy { it.inputHash }
                .thenBy { it.outputKind.name })
            .joinToString("\n") { objectMapper.writeValueAsString(it) }

        Files.writeString(
            path,
            lines + "\n",
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    fun load(version: String): List<Checkpoint> {
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

    private fun samplesPath(version: String): Path =
        Path("$basePath/$version.jsonl")
}