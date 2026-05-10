package com.minogin.anomaly.comparator.model

data class DiffReport(
    val findings: List<Finding>
) {
    fun hasProblems(): Boolean = findings.isNotEmpty()
}