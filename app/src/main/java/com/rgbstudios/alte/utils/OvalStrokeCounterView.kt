package com.rgbstudios.alte.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import com.rgbstudios.alte.R

class OvalStrokeCounterView(context: Context, totalPeeps: Int) : Drawable() {

    private val strokePaint: Paint = Paint()
    private val dashWidth: Float
    private val dashGap: Float

    init {
        val strokeWidth = context.resources.getDimension(R.dimen.dimen_1_5)
        dashGap = context.resources.getDimension(R.dimen.dimen_4)

        strokePaint.isAntiAlias = true
        strokePaint.color = context.getColor(R.color.md_theme_light_primary)
        strokePaint.strokeWidth = strokeWidth
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeCap = Paint.Cap.ROUND

        // Calculate dash width based on totalPeeps
        dashWidth = when (totalPeeps) {
            1 -> context.resources.getDimension(R.dimen.dimen_140)
            2 -> context.resources.getDimension(R.dimen.dimen_70)
            3 -> context.resources.getDimension(R.dimen.dimen_35)
            4 -> context.resources.getDimension(R.dimen.dimen_17_5)
            5 -> context.resources.getDimension(R.dimen.dimen_8_75)
            6 -> context.resources.getDimension(R.dimen.dimen_4)
            else -> context.resources.getDimension(R.dimen.dimen_2) // Default value
        }
    }

    override fun draw(canvas: Canvas) {
        val bounds: Rect = bounds
        val centerX: Float = (bounds.left + bounds.right) / 2f
        val centerY: Float = (bounds.top + bounds.bottom) / 2f
        val radius: Float =
            ((bounds.right - bounds.left) / 2f).coerceAtMost((bounds.bottom - bounds.top) / 2f)

        // Create a Path for oval shape
        val ovalPath = Path()
        ovalPath.addOval(RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius), Path.Direction.CW)

        // Create a PathEffect for dashed line
        val dashPathEffect = DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)

        // Apply the dash effect to the paint
        strokePaint.pathEffect = dashPathEffect

        // Draw the oval shape with dashed stroke
        canvas.drawPath(ovalPath, strokePaint)
    }

    override fun setAlpha(alpha: Int) {
        strokePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        strokePaint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        // Since it's a solid color with transparency, we return TRANSLUCENT
        return PixelFormat.TRANSLUCENT
    }
}

