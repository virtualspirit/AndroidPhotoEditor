package com.virtualspirit.photoeditor.shape

import android.graphics.DashPathEffect
import android.graphics.PathEffect
import com.virtualspirit.photoediting.StrokeStyle

/**
 * Central factory for [DashPathEffect] calculations.
 *
 * All multipliers live here, so changing dash/dot proportions only requires
 * editing this one file regardless of how many views consume the effect.
 *
 * Formula (Paint.Cap.ROUND):
 *   visual dash length = on + strokeWidth
 *   visual gap         = off - strokeWidth   → must always be > 0
 *
 * DASHED : on = 1.0 × w,  off = 2.0 × w  → visual dash 2w, gap w
 * DOTTED : on = dotOn(w), off = 2.0 × w  → circular dot diameter ≈ w, gap w
 *   dotOn is kept proportional to w (not fixed) so the dot stays circular at
 *   any parentScale. coerceAtLeast(0.5f) prevents sub-pixel values that
 *   Android may silently round to zero.
 */
object StrokePathEffect {

    private const val DASH_ON_RATIO  = 1.0f
    private const val DASH_OFF_RATIO = 2.0f

    private const val DOT_ON_RATIO   = 0.05f
    private const val DOT_ON_MIN     = 0.5f
    private const val DOT_OFF_RATIO  = 2.0f

    /**
     * Returns the appropriate [PathEffect] for the given [style] and [strokeWidth],
     * or `null` for [StrokeStyle.SOLID].
     *
     * @param strokeWidth The paint's current stroke width in pixels (already
     *                    compensated for any parent view scale).
     */
    fun create(style: StrokeStyle, strokeWidth: Float): PathEffect? {
        val w = strokeWidth.coerceAtLeast(1f)
        return when (style) {
            StrokeStyle.DASHED -> DashPathEffect(
                floatArrayOf(w * DASH_ON_RATIO, w * DASH_OFF_RATIO), 0f
            )
            StrokeStyle.DOTTED -> {
                val dotOn = (w * DOT_ON_RATIO).coerceAtLeast(DOT_ON_MIN)
                DashPathEffect(floatArrayOf(dotOn, w * DOT_OFF_RATIO), 0f)
            }
            StrokeStyle.SOLID -> null
        }
    }
}
