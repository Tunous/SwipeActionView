package me.thanel.swipeactionview.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Region
import android.support.v4.view.GravityCompat
import android.support.v4.view.MarginLayoutParamsCompat
import android.support.v4.view.ViewCompat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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

    return isRightAlignedGravity(absGravity)
}

/**
 * Returns whether the specified gravity has `end` or `rights` flag set.
 *
 * @param gravity The gravity to check.
 *
 * @return Whether the gravity is right aligned.
 */
@SuppressLint("RtlHardcoded")
internal fun isRightAlignedGravity(gravity: Int): Boolean {
    return gravity and Gravity.END == Gravity.END ||
            gravity and Gravity.RIGHT == Gravity.RIGHT
}

/**
 * The total width of the view including both start and end margins.
 */
internal val View.totalWidth: Int
    get() {
        val layoutParams = (layoutParams as ViewGroup.MarginLayoutParams)
        val marginEnd = MarginLayoutParamsCompat.getMarginEnd(layoutParams)
        val marginStart = MarginLayoutParamsCompat.getMarginStart(layoutParams)

        return marginStart + width + marginEnd
    }

internal fun Rect.setBoundsFrom(view: View) {
    set(view.left, view.top, view.right, view.bottom)
}

internal fun radius(x: Double, y: Double) = Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0))

/**
 * Perform draw actions in bounds of the specified [View] with selected [Region.Op] modification
 * applied.
 *
 * @param view View from which to get bounds.
 * @param op How the bounds are modified.
 * @param drawAction The draw actions to perform.
 */
internal fun Canvas.drawInBoundsOf(view: View, op: Region.Op, includePadding: Boolean = false, drawAction: () -> Unit) {
    val saveCount = save()

    val translationX = view.translationX.toInt()
    val bounds = clipBounds

    var left = view.left + translationX
    var top = view.top
    var right = view.right + translationX
    var bottom = view.bottom

    if (includePadding) {
        left += view.paddingLeft
        top += view.paddingTop
        right -= view.paddingRight
        bottom -= view.paddingBottom
    }

    bounds.set(left, top, right, bottom)
    clipRect(bounds, op)

    drawAction()

    restoreToCount(saveCount)
}
