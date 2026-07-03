package com.silas.omaster.util

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

enum class OutputRatio(val label: String, val widthToHeight: Float, val platform: String) {
    SQUARE("1:1", 1f, "Instagram"),
    PORTRAIT_4_5("4:5", 0.8f, "Instagram"),
    PORTRAIT_3_4("3:4", 0.75f, "小红书"),
    FULL("9:16", 9f / 16f, "朋友圈/Stories"),
    LANDSCAPE_16_9("16:9", 16f / 9f, "封面/B站")
}

object FrameRenderer {

    private const val BASE = 1080
    private const val TOP_RATIO_WITH_TITLE = 0.30f
    private const val TOP_RATIO_WITHOUT_TITLE = 0.08f
    private const val PADDING_RATIO = 0.035f
    private const val BOTTOM_RATIO = 0.04f
    private const val TITLE_SIZE_RATIO = 0.09f
    private const val TITLE_MAX_SIZE = 64f
    private const val TITLE_MIN_SIZE = 32f
    private const val ROUNDED_RADIUS_RATIO = 0.04f

    data class Params(
        val source: Bitmap,
        val dominantColor: Int,
        val textColor: Int,
        val title: String = "",
        val useRoundedCorners: Boolean = true,
        val ratio: OutputRatio = OutputRatio.FULL,
        val showWatermark: Boolean = true
    )

    fun render(params: Params): Bitmap = with(params) {
        val isLandscape = ratio.widthToHeight > 1f
        val ratioValue = if (isLandscape) 1f / ratio.widthToHeight else ratio.widthToHeight

        val outputWidth: Int
        val outputHeight: Int
        if (isLandscape) {
            outputHeight = BASE
            outputWidth = (BASE / ratioValue).toInt()
        } else {
            outputWidth = BASE
            outputHeight = (BASE / ratioValue).toInt()
        }

        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        drawBackground(canvas, dominantColor, outputWidth, outputHeight)

        val hasTitle = title.isNotBlank()
        val topRatio = if (hasTitle) TOP_RATIO_WITH_TITLE else TOP_RATIO_WITHOUT_TITLE
        val topAreaBottom = outputHeight * topRatio

        val imagePadding = maxOf(16f, outputWidth * PADDING_RATIO)
        val bottomReserved = maxOf(36f, outputHeight * BOTTOM_RATIO)

        if (hasTitle) {
            val titleSize = minOf(TITLE_MAX_SIZE, maxOf(TITLE_MIN_SIZE, topAreaBottom * TITLE_SIZE_RATIO))
            drawTitleText(canvas, title, textColor, outputWidth, topAreaBottom, titleSize)
        }

        val imageTop = topAreaBottom + imagePadding
        val imageMaxWidth = outputWidth - imagePadding * 2
        val imageMaxHeight = outputHeight - imageTop - bottomReserved

        val scale = minOf(
            imageMaxWidth / source.width.toFloat(),
            imageMaxHeight / source.height.toFloat()
        )
        val drawWidth = source.width * scale
        val drawHeight = source.height * scale
        val drawLeft = (outputWidth - drawWidth) / 2f
        val drawTop = imageTop + (imageMaxHeight - drawHeight) / 2f

        val imageRect = RectF(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight)
        val roundedRadius = maxOf(16f, outputWidth * ROUNDED_RADIUS_RATIO)
        val cr = if (useRoundedCorners) roundedRadius else 0f

        if (cr > 0f) {
            drawImageShadow(canvas, imageRect, roundedRadius)
        }

        val clipPath = Path().apply {
            addRoundRect(imageRect, cr, cr, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(source, null, imageRect, Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        })
        canvas.restore()

        if (showWatermark) {
            drawWatermark(canvas, textColor, outputWidth, outputHeight, bottomReserved)
        }

        output
    }

    private fun drawBackground(canvas: Canvas, color: Int, w: Int, h: Int) {
        val lighter = adjustBrightness(color, 1.06f)
        val paint = Paint().apply {
            isAntiAlias = true
            shader = android.graphics.LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(lighter, color),
                floatArrayOf(0f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    private fun adjustBrightness(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return AndroidColor.HSVToColor(hsv)
    }

    private fun drawTitleText(canvas: Canvas, title: String, textColor: Int, w: Int, topBottom: Float, titleSize: Float) {
        val paint = Paint().apply {
            color = textColor
            textSize = titleSize
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            setShadowLayer(2f, 0f, 1f, 0x33000000)
        }
        canvas.drawText(title, w / 2f, topBottom / 2 + titleSize / 3, paint)
    }

    private fun drawImageShadow(canvas: Canvas, rect: RectF, radius: Float) {
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            color = 0x22000000
            maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        }
        val shadowRect = RectF(rect.left + 4f, rect.top + 8f, rect.right - 2f, rect.bottom + 8f)
        val shadowPath = Path().apply {
            addRoundRect(shadowRect, radius, radius, Path.Direction.CW)
        }
        canvas.drawPath(shadowPath, shadowPaint)
    }

    private fun drawWatermark(canvas: Canvas, textColor: Int, w: Int, h: Int, bottomReserved: Float) {
        val watermarkSize = maxOf(14f, bottomReserved * 0.45f)
        val paint = Paint().apply {
            color = textColor and 0x00FFFFFF or 0x35000000
            textSize = watermarkSize
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("OMaster", w / 2f, h - bottomReserved / 2 + watermarkSize / 3, paint)
    }
}
