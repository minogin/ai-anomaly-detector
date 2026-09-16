package com.minogin.anomaly.demo

import java.awt.*
import java.awt.image.*
import java.nio.file.*
import javax.imageio.*

/**
 * Renders report text to a PNG that looks like a dark terminal: monospace, large type, severity tags
 * in colour. Used for the slide so the image never contains a real terminal's prompt, path or user name.
 */
object Screenshot {
    private val PREFERRED_FONTS = listOf("JetBrains Mono", "Cascadia Mono", "Consolas", "Menlo", "DejaVu Sans Mono", "Monospaced")

    private val BACKGROUND = Color(0x1E1E1E)
    private val TEXT = Color(0xD4D4D4)
    private val DIM = Color(0x808080)
    private val HIGH = Color(0xF14C4C)
    private val MID = Color(0xE5C07B)
    private val LOW = Color(0x61AFEF)
    private val HEADING = Color(0xFFFFFF)

    fun render(text: String, output: Path, fontSize: Int = 28) {
        System.setProperty("java.awt.headless", "true")
        val font = Font(pickFontFamily(), Font.PLAIN, fontSize)
        val lines = text.trimEnd().lines()

        val metrics = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics().also { it.font = font }.fontMetrics
        val lineHeight = (metrics.height * 1.3).toInt()
        val padding = fontSize * 2
        val width = (lines.maxOfOrNull { metrics.stringWidth(it) } ?: 0) + 2 * padding
        val height = lines.size * lineHeight + 2 * padding

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g.color = BACKGROUND
        g.fillRect(0, 0, width, height)
        g.font = font

        lines.forEachIndexed { index, line ->
            drawLine(g, line, padding, padding + index * lineHeight + metrics.ascent)
        }
        g.dispose()

        Files.createDirectories(output.toAbsolutePath().parent)
        ImageIO.write(image, "png", output.toFile())
    }

    private fun drawLine(g: Graphics2D, line: String, x: Int, y: Int) {
        val tag = listOf("[HIGH]" to HIGH, "[MID]" to MID, "[LOW]" to LOW).firstOrNull { line.startsWith(it.first) }
        when {
            tag != null -> {
                g.color = tag.second
                g.drawString(tag.first, x, y)
                g.color = TEXT
                g.drawString(line.removePrefix(tag.first), x + g.fontMetrics.stringWidth(tag.first), y)
            }
            line.startsWith("Anomaly Detector") || line == "FINDINGS" || line.startsWith("PROFILE") -> {
                g.color = HEADING
                g.drawString(line, x, y)
            }
            line.all { it == '=' } -> {
                g.color = DIM
                g.drawString(line, x, y)
            }
            else -> {
                g.color = TEXT
                g.drawString(line, x, y)
            }
        }
    }

    internal fun pickFontFamily(): String {
        val available = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toSet()
        return PREFERRED_FONTS.firstOrNull { it in available } ?: Font.MONOSPACED
    }
}
