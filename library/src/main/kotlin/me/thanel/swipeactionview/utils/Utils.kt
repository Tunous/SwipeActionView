package me.thanel.swipeactionview.utils

import android.content.Context
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewCompat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout

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
internal fun clamp(value: Float, min: Float, max: Float) = Math.max(min, Math.min(value, max))

/**
 * Set both vertical and horizontal scale of the view to the specified value.
 *
 * @param scale The target scale.
 */
internal fun View.setScale(scale: Float) {
    this.scaleX = scale
    this.scaleY = scale
}

/**
 * Get whether the view is right aligned.
 *
 * @return Whether the view is right aligned.
 */
internal fun View.isRightAligned(): Boolean {
    val gravity = (layoutParams as FrameLayout.LayoutParams).gravity
    val layoutDirection = ViewCompat.getLayoutDirection(this)

    val absGravity = GravityCompat.getAbsoluteGravity(gravity, layoutDirection)
    if (absGravity <= 0) return false

    return isRightAlignedGravity(gravity)
}

/**
 * Returns whether the specified gravity has `end` or `rights` flag set.
 *
 * @param gravity The gravity to check.
 *
 * @return Whether the gravity is right aligned.
 */
internal fun isRightAlignedGravity(gravity: Int): Boolean {
    return gravity and Gravity.END == Gravity.END ||
            gravity and Gravity.RIGHT == Gravity.RIGHT
}
