/*
 * Copyright © 2016 Łukasz Rutkowski
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

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Region
import android.view.View

/**
 * Clamp the value between a minimum float and maximum float value.
 *
 * @param value The value to clamp.
 * @param min The minimum float value.
 * @param max The maximum float value.
 *
 * @return Value clamped between a minimum float and maximum float value.
 */
internal fun clamp(value: Float, min: Float, max: Float) = Math.max(min, Math.min(value, max))

/**
 * Set the rectangle's coordinates from coordinates of the specified view.
 *
 * @param view View from which to take coordinates.
 */
internal fun Rect.setBoundsFrom(view: View) = set(view.left, view.top, view.right, view.bottom)

/**
 * Calculate circle radius from x and y coordinates.
 *
 * @param x The x coordinate.
 * @param y The y coordinate.
 *
 * @return The circle radius.
 */
internal fun radius(x: Double, y: Double) = Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0))

/**
 * Perform draw actions in bounds of the specified [View] with selected [Region.Op] modification
 * applied.
 *
 * @param view View from which to get bounds.
 * @param op How the bounds are modified.
 * @param includePadding Whether padding should reduce size of drawing bounds.
 * @param drawAction The draw actions to perform.
 */
internal fun Canvas.drawInBoundsOf(view: View, op: Region.Op, includePadding: Boolean = false,
                                   drawAction: (Int) -> Unit) {
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

    drawAction(saveCount)

    restoreToCount(saveCount)
}
