package com.silas.omaster.util

import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DominantColorResult(
    val dominant: Int,
    val vibrant: Int?,
    val muted: Int?,
    val textColor: Int,
    val palette: List<Int> = emptyList()
)

object ColorExtractor {

    suspend fun extract(bitmap: Bitmap): DominantColorResult = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap)
            .maximumColorCount(16)
            .generate()

        val dominant = palette.getVibrantColor(
            palette.getDominantColor(android.graphics.Color.GRAY)
        )

        val vibrant = palette.getVibrantColor(
            palette.getLightVibrantColor(dominant)
        ).let { if (it != dominant) it else null }

        val muted = palette.getMutedColor(
            palette.getDarkMutedColor(dominant)
        ).let { if (it != dominant) it else null }

        val paletteColors = buildList {
            add(dominant)
            palette.vibrantSwatch?.let { s -> if (s.rgb != dominant) add(s.rgb) }
            palette.lightVibrantSwatch?.let { s -> if (s.rgb != dominant && !contains(s.rgb)) add(s.rgb) }
            palette.darkVibrantSwatch?.let { s -> if (s.rgb != dominant && !contains(s.rgb)) add(s.rgb) }
            palette.mutedSwatch?.let { s -> if (s.rgb != dominant && !contains(s.rgb)) add(s.rgb) }
            palette.lightMutedSwatch?.let { s -> if (s.rgb != dominant && !contains(s.rgb)) add(s.rgb) }
            palette.darkMutedSwatch?.let { s -> if (s.rgb != dominant && !contains(s.rgb)) add(s.rgb) }
        }

        val r = android.graphics.Color.red(dominant)
        val g = android.graphics.Color.green(dominant)
        val b = android.graphics.Color.blue(dominant)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        val textColor = if (luminance > 0.55) android.graphics.Color.BLACK
        else android.graphics.Color.WHITE

        DominantColorResult(
            dominant = dominant,
            vibrant = vibrant,
            muted = muted,
            textColor = textColor,
            palette = paletteColors
        )
    }

    private fun MutableList<Int>.contains(color: Int): Boolean {
        return any { colorSimilar(it, color) }
    }

    private fun colorSimilar(a: Int, b: Int, threshold: Int = 30): Boolean {
        return kotlin.math.abs(android.graphics.Color.red(a) - android.graphics.Color.red(b)) < threshold &&
                kotlin.math.abs(android.graphics.Color.green(a) - android.graphics.Color.green(b)) < threshold &&
                kotlin.math.abs(android.graphics.Color.blue(a) - android.graphics.Color.blue(b)) < threshold
    }
}
