package com.minogin.anomaly.tracer

import com.minogin.anomaly.common.*
import com.minogin.anomaly.tracer.model.*
import com.minogin.anomaly.util.*
import java.util.concurrent.*

internal class Tracer(
    private val version: String,
) {
    private val ref = AtomicReference(listOf<String>())

    private val checkpoints = ConcurrentHashMap<Checkpoint.Key, Checkpoint>()

    init {
        checkpoints.putAll(serializer.load(version).associateBy { it.key })

        Runtime.getRuntime().addShutdownHook(
            Thread {
                flush()
            }
        )
    }

    fun checkpoint(
        step: String,
        input: String,
        output: String,
        nextStep: String? = null
    ) {
        val inputHash = sha256(input)
        val outputKind = classifier.classify(output)

        val key = Checkpoint.Key(
            version = version,
            step = step,
            inputHash = inputHash,
            outputKind = outputKind,
            nextStep = nextStep,
        )

        checkpoints.compute(key) { _, existing ->
            if (existing == null) {
                Checkpoint(
                    key = key,
                    count = 1,
                    exampleInput = input,
                    exampleOutput = output
                )
            } else {
                existing.copy(count = existing.count + 1)
            }
        }
    }

    fun flush() {
        serializer.save(checkpoints.values, version)
    }
}