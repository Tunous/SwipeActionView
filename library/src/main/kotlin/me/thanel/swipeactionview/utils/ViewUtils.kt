package me.thanel.swipeactionview.utils

import android.annotation.SuppressLint
import android.support.v4.view.GravityCompat
import android.support.v4.view.MarginLayoutParamsCompat
import android.support.v4.view.ViewCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

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
@SuppressLint("RtlHardcoded")
internal fun View.isRightAligned(): Boolean {
    val gravity = (layoutParams as FrameLayout.LayoutParams).gravity
    val layoutDirection = ViewCompat.getLayoutDirection(this)

    val absGravity = GravityCompat.getAbsoluteGravity(gravity, layoutDirection)
    if (absGravity <= 0) return false

    return absGravity and Gravity.END == Gravity.END ||
            absGravity and Gravity.RIGHT == Gravity.RIGHT
}

/**
 * The total width of the view including both start and end margins.
 */
internal val View.totalWidth: Int
    get() {
        val layoutParams = layoutParams as ViewGroup.MarginLayoutParams
        val marginEnd = MarginLayoutParamsCompat.getMarginEnd(layoutParams)
        val marginStart = MarginLayoutParamsCompat.getMarginStart(layoutParams)

        return marginStart + width + marginEnd
    }

/**
 * The start margin of the view.
 */
internal val View.marginStart: Int
    get() {
        val layoutParams = layoutParams as ViewGroup.MarginLayoutParams
        return MarginLayoutParamsCompat.getMarginStart(layoutParams)
    }

/**
 * The end margin of the view.
 */
internal val View.marginEnd: Int
    get() {
        val layoutParams = layoutParams as ViewGroup.MarginLayoutParams
        return MarginLayoutParamsCompat.getMarginEnd(layoutParams)
    }
