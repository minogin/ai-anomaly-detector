package com.minogin.anomaly.cli

import com.minogin.anomaly.api.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class CliTest {
    @Test
    fun `parses the three positional arguments with default config`() {
        val parsed = parseArgs(arrayOf(".ai-anomaly-detector", "1.1", "1.0"))
        assertEquals(".ai-anomaly-detector", parsed.basePath)
        assertEquals("1.1", parsed.currentVersion)
        assertEquals("1.0", parsed.referenceVersion)
        assertEquals(AnalyzerConfig(), parsed.config)
    }

    @Test
    fun `options may appear anywhere and override the defaults`() {
        val parsed = parseArgs(arrayOf("--routing-threshold=0.1", "data", "1.1", "--routing-min-samples=20", "1.0", "--routing-max-targets=4"))
        assertEquals("data", parsed.basePath)
        assertEquals(AnalyzerConfig(routingShiftThreshold = 0.1, routingMinSamples = 20, routingMaxTargets = 4), parsed.config)
    }

    @Test
    fun `no-examples flag turns examples off and takes no value`() {
        assertTrue(parseArgs(arrayOf("data", "1.1", "1.0")).config.includeExamples)
        assertFalse(parseArgs(arrayOf("data", "1.1", "1.0", "--no-examples")).config.includeExamples)
        assertFailsWith<IllegalArgumentException> { parseArgs(arrayOf("data", "1.1", "1.0", "--no-examples=true")) }
    }

    @Test
    fun `rejects wrong argument count`() {
        assertFailsWith<IllegalArgumentException> { parseArgs(arrayOf("data", "1.1")) }
        assertFailsWith<IllegalArgumentException> { parseArgs(arrayOf("data", "1.1", "1.0", "extra")) }
    }

    @Test
    fun `rejects unknown options and malformed values`() {
        assertFailsWith<IllegalArgumentException> { parseArgs(arrayOf("data", "1.1", "1.0", "--threshold=0.1")) }.also {
            assertContains(it.message!!, "--threshold")
        }
        assertFailsWith<IllegalArgumentException> { parseArgs(arrayOf("data", "1.1", "1.0", "--routing-threshold")) }
        assertFailsWith<IllegalArgumentException> { parseArgs(arrayOf("data", "1.1", "1.0", "--routing-threshold=high")) }
        assertFailsWith<IllegalArgumentException> { parseArgs(arrayOf("data", "1.1", "1.0", "--routing-threshold=1.5")) }
        assertFailsWith<IllegalArgumentException> { parseArgs(arrayOf("data", "1.1", "1.0", "--routing-min-samples=2.5")) }
    }
}
