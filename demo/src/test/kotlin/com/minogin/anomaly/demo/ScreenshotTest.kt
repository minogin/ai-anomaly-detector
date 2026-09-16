package com.minogin.anomaly.demo

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.*
import java.nio.file.*
import javax.imageio.*
import kotlin.test.*

class ScreenshotTest {
    @Test
    fun `renders text to a dark png sized to the content`(@TempDir dir: Path) {
        val short = dir.resolve("short.png")
        val long = dir.resolve("long.png")
        Screenshot.render("Anomaly Detector: 1.0 → 1.1\n[HIGH] one line", short)
        Screenshot.render("Anomaly Detector: 1.0 → 1.1\n[HIGH] one line\n[MID] a much longer second line of text\n\nPROFILE", long)

        val shortImage = ImageIO.read(short.toFile())
        val longImage = ImageIO.read(long.toFile())
        assertTrue(longImage.width > shortImage.width, "wider content gives a wider image")
        assertTrue(longImage.height > shortImage.height, "more lines give a taller image")
        assertEquals(0x1E1E1E, shortImage.getRGB(0, 0) and 0xFFFFFF, "background is dark")
    }

    @Test
    fun `screenshots renders one png per drifted version`(@TempDir dir: Path) {
        val out = dir.resolve("out")
        screenshots(dir.resolve("data").toString(), out)

        val files = Files.list(out).use { it.map { p -> p.fileName.toString() }.sorted().toList() }
        assertEquals(ScriptedModel.DRIFT_DESCRIPTIONS.keys.map { "demo-report-$it.png" }.sorted(), files)
        files.forEach { assertNotNull(ImageIO.read(out.resolve(it).toFile()), "$it is a readable image") }
    }

    @Test
    fun `picks a real monospace family or the platform fallback`() {
        val family = Screenshot.pickFontFamily()
        assertTrue(family.isNotBlank())
    }
}
