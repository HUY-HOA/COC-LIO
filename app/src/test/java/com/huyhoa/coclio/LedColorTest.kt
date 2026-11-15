package com.huyhoa.coclio

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LedColorTest {
    @Test
    fun remotePaletteBlendsAcrossTheLastToFirstColor() {
        val palette = listOf(
            Color(0xFF00E5FF),
            Color(0xFF168BFF),
            Color(0xFF6548FF),
            Color(0xFFB33CFF),
            Color(0xFFFF35B8),
            Color(0xFFFF4A72),
            Color(0xFFFFA43A),
            Color(0xFFB7F34A),
            Color(0xFF35E6A2)
        )

        val justBeforeLoop = movingLedColors(palette, 0.999f).first()
        val justAfterLoop = movingLedColors(palette, 0.001f).first()

        assertTrue(colorDistance(justBeforeLoop, justAfterLoop) < 24)
    }

    @Test
    fun transitionContainsBlendedColorsBetweenStops() {
        val palette = listOf(Color(0xFFFF0000), Color(0xFF0000FF))
        val colors = movingLedColors(palette, 0f, sampleCount = 4)

        assertTrue(colors.size == 4)
        assertTrue(colors[1].toArgb() != palette[0].toArgb())
        assertTrue(colors[1].toArgb() != palette[1].toArgb())
    }

    @Test
    fun generatedColorsStayWithinValidChannelRange() {
        val palette = listOf(
            Color(0xFF00E5FF),
            Color(0xFF168BFF),
            Color(0xFF6548FF),
            Color(0xFFFF35B8),
            Color(0xFFFFA43A)
        )

        movingLedColors(palette, 0.37f).forEach { color ->
            assertTrue(color.red in 0f..1f)
            assertTrue(color.green in 0f..1f)
            assertTrue(color.blue in 0f..1f)
            assertTrue(color.alpha in 0f..1f)
        }
    }

    private fun colorDistance(first: Color, second: Color): Int {
        val a = first.toArgb()
        val b = second.toArgb()
        return abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) +
            abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) +
            abs((a and 0xFF) - (b and 0xFF))
    }
}
