package me.thanel.swipeactionview.utils

import android.content.Context
import android.util.TypedValue
import android.view.View

/**
 * Convert dp unit to equivalent pixels, depending on device density.
 *
 * @param context Context to get resources and device specific display metrics.
 * @return A float value to represent px equivalent to dp depending on device density.
 */
internal fun Float.dpToPx(context: Context) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)

/**
 * Clamp the value between a minimum float and maximum float value.
 *
 * @param value The value to clamp.
 * @param min The minimum float value.
 * @param max The maximum float value.
 */
internal fun clamp(value: Float, min: Float, max: Float): Float = Math.max(min, Math.min(value, max))

/**
 * Set both vertical and horizontal scale of the view the the specified [scale].
 *
 * @param scale The target scale.
 */
internal fun View.setScale(scale: Float) {
    this.scaleX = scale
    this.scaleY = scale
}
