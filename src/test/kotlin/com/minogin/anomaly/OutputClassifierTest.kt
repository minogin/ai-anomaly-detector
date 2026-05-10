package com.minogin.anomaly

import com.minogin.anomaly.sampler.model.OutputClassifier
import com.minogin.anomaly.sampler.model.OutputKind
import org.junit.jupiter.api.Test
import kotlin.test.*

class OutputClassifierTest {

    private val classifier = OutputClassifier()

    @Test
    fun `classifies empty output`() {
        assertEquals(
            OutputKind.EMPTY,
            classifier.classify("")
        )

        assertEquals(
            OutputKind.EMPTY,
            classifier.classify("   ")
        )

        assertEquals(
            OutputKind.EMPTY,
            classifier.classify("  \n   \t \n  \r  ")
        )
    }

    @Test
    fun `classifies json object`() {
        assertEquals(
            OutputKind.JSON_OBJECT,
            classifier.classify("""{"riskLevel":"HIGH"}""")
        )
    }

    @Test
    fun `classifies json array`() {
        assertEquals(
            OutputKind.JSON_ARRAY,
            classifier.classify("""["HIGH", "LOW"]""")
        )
    }

    @Test
    fun `classifies quoted json object`() {
        assertEquals(
            OutputKind.QUOTED_JSON_OBJECT,
            classifier.classify(""""{"riskLevel":"HIGH"}"""")
        )
    }

    @Test
    fun `classifies quoted json array`() {
        assertEquals(
            OutputKind.QUOTED_JSON_ARRAY,
            classifier.classify(""""["HIGH","LOW"]"""")
        )
    }

    @Test
    fun `classifies quoted scalar`() {
        assertEquals(
            OutputKind.QUOTED_SCALAR,
            classifier.classify(""""HIGH"""")
        )
    }

    @Test
    fun `classifies plain scalar`() {
        assertEquals(
            OutputKind.SCALAR,
            classifier.classify("HIGH")
        )
    }

    @Test
    fun `classifies multiline text`() {
        assertEquals(
            OutputKind.MULTILINE_TEXT,
            classifier.classify(
                """
                Supplier should be reviewed.
                Ownership is unclear.
                """.trimIndent()
            )
        )
    }

    @Test
    fun `classifies malformed json-like text as scalar`() {
        assertEquals(
            OutputKind.SCALAR,
            classifier.classify("""{"riskLevel":"HIGH"""")
        )
    }
}