package com.minogin.anomaly.comparator

import com.minogin.anomaly.common.*
import com.minogin.anomaly.comparator.model.*
import com.minogin.anomaly.sampler.model.OutputKind
import com.minogin.anomaly.tracer.model.*

internal class DiffReporter(
    private val serializer: Serializer
) {
    fun report(
        currentVersion: String,
        referenceVersion: String,
    ): DiffReport {
        val reference = serializer.load(referenceVersion)
        val current = serializer.load(currentVersion)

        val findings = mutableListOf<Finding>()

        findings += compareNodeProfiles(reference, current)
        findings += compareMissingNodes(reference, current)
        findings += compareExactInputs(reference, current)
        findings += compareNextSteps(reference, current)

        return DiffReport(findings)
    }

    private fun compareNodeProfiles(
        reference: List<Checkpoint>,
        current: List<Checkpoint>
    ): List<Finding> {
        val referenceByNode = reference.groupBy { it.step }
        val currentByNode = current.groupBy { it.step }

        val commonNodes = referenceByNode.keys intersect currentByNode.keys

        return commonNodes.mapNotNull { node ->
            val referenceKinds = referenceByNode.getValue(node).countsByKind()
            val currentKinds = currentByNode.getValue(node).countsByKind()

            val newKinds = currentKinds.keys - referenceKinds.keys

            if (newKinds.isEmpty()) {
                null
            } else {
                Finding(
                    type = Finding.Type.NODE_OUTPUT_PROFILE_CHANGED,
                    node = node,
                    inputHash = null,
                    message = "Node produced output kind(s) not seen in reference: $newKinds",
                    referenceKinds = referenceKinds,
                    currentKinds = currentKinds,
                    referenceExampleOutput = referenceByNode.getValue(node).firstOrNull()?.exampleOutput,
                    currentExampleOutput = currentByNode.getValue(node)
                        .firstOrNull { it.outputKind in newKinds }
                        ?.exampleOutput
                )
            }
        }
    }

    private fun compareMissingNodes(
        reference: List<Checkpoint>,
        current: List<Checkpoint>
    ): List<Finding> {
        val referenceNodes = reference.map { it.step }.toSet()
        val currentNodes = current.map { it.step }.toSet()

        val missingNodes = referenceNodes - currentNodes

        return missingNodes.map { node ->
            val samples = reference.filter { it.step == node }

            Finding(
                type = Finding.Type.NODE_DISAPPEARED,
                node = node,
                inputHash = null,
                message = "Node existed in reference version but is missing in current version.",
                referenceKinds = samples.countsByKind(),
                currentKinds = emptyMap(),
                referenceExampleOutput = samples.firstOrNull()?.exampleOutput,
                currentExampleOutput = null
            )
        }
    }

    private fun compareExactInputs(
        reference: List<Checkpoint>,
        current: List<Checkpoint>
    ): List<Finding> {
        val referenceGroups = reference.groupBy { ExactInputKey(it.step, it.inputHash) }
        val currentGroups = current.groupBy { ExactInputKey(it.step, it.inputHash) }

        val commonKeys = referenceGroups.keys intersect currentGroups.keys

        return commonKeys.mapNotNull { key ->
            val referenceKinds = referenceGroups.getValue(key).countsByKind()
            val currentKinds = currentGroups.getValue(key).countsByKind()

            val newKinds = currentKinds.keys - referenceKinds.keys

            if (newKinds.isEmpty()) {
                null
            } else {
                Finding(
                    type = Finding.Type.EXACT_INPUT_OUTPUT_KIND_CHANGED,
                    node = key.node,
                    inputHash = key.inputHash,
                    message = "Same node/input produced output kind(s) not seen in reference: $newKinds",
                    referenceKinds = referenceKinds,
                    currentKinds = currentKinds,
                    referenceExampleOutput = referenceGroups.getValue(key).firstOrNull()?.exampleOutput,
                    currentExampleOutput = currentGroups.getValue(key)
                        .firstOrNull { it.outputKind in newKinds }
                        ?.exampleOutput
                )
            }
        }
    }

    private fun compareNextSteps(
        reference: List<Checkpoint>,
        current: List<Checkpoint>
    ): List<Finding> {
        val referenceGroups = reference
            .filter { it.nextStep != null }
            .groupBy { ExactInputKey(it.step, it.inputHash) }

        val currentGroups = current
            .filter { it.nextStep != null }
            .groupBy { ExactInputKey(it.step, it.inputHash) }

        val commonKeys = referenceGroups.keys intersect currentGroups.keys

        return commonKeys.mapNotNull { key ->
            val referenceNextSteps = referenceGroups.getValue(key)
                .mapNotNull { it.nextStep }
                .toSet()

            val currentNextSteps = currentGroups.getValue(key)
                .mapNotNull { it.nextStep }
                .toSet()

            val newNextSteps = currentNextSteps - referenceNextSteps

            if (newNextSteps.isEmpty()) {
                null
            } else {
                Finding(
                    type = Finding.Type.NEXT_STEP_DRIFT,
                    node = key.node,
                    inputHash = key.inputHash,
                    message = "Same node/input produced nextStep(s) not seen in reference: $newNextSteps",
                    referenceKinds = emptyMap(),
                    currentKinds = emptyMap(),
                    referenceExampleOutput = "nextStep: ${referenceNextSteps.joinToString()}",
                    currentExampleOutput = "nextStep: ${currentNextSteps.joinToString()}"
                )
            }
        }
    }

    private fun List<Checkpoint>.countsByKind(): Map<OutputKind, Int> =
        groupBy { it.outputKind }
            .mapValues { (_, samples) -> samples.sumOf { it.count } }
}

private data class ExactInputKey(
    val node: String,
    val inputHash: String
)

