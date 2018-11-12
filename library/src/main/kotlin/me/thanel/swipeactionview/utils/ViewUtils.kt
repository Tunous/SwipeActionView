/*
 * Copyright © 2016-2018 Łukasz Rutkowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.thanel.swipeactionview.utils

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.GravityCompat
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.ViewCompat

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
