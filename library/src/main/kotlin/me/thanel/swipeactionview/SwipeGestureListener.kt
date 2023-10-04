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

package me.thanel.swipeactionview

/**
 * Interface definition for callbacks to be invoked when swipe actions of the
 * [SwipeActionView] are performed.
 *
 * @see SwipeActionView
 */
interface SwipeGestureListener {
    /**
     * Callback method to be invoked when user swipes the [SwipeActionView] to the
     * left.
     *
     * @param swipeActionView The [SwipeActionView] from which this method was invoked.
     *
     * @return Whether the container should return to default position. When `false`, then you
     * should manually call `reset` method to return to default position.
     * @see SwipeActionView
     */
    fun onSwipedLeft(swipeActionView: SwipeActionView): Boolean {return true}

    /**
     * Callback method to be invoked when user swipes the [SwipeActionView] to the
     * right.
     *
     * @param swipeActionView The [SwipeActionView] from which this method was invoked.
     *
     * @return Whether the container should return to default position. When `false`, then you
     * should manually call `reset` method to return to default position.
     * @see SwipeActionView
     */
    fun onSwipedRight(swipeActionView: SwipeActionView): Boolean {return true}

    /**
     * Callback method to be invoked when user swipes the [SwipeActionView] to the
     * left, but only the first element. Requires MultiSwipeActionView as a Container.
     *
     * @param swipeActionView The [SwipeActionView] from which this method was invoked.
     *
     * @return Whether the container should return to default position. When `false`, then you
     * should manually call `reset` method to return to default position.
     * @see SwipeActionView
     */
    fun onSwipedHalfwayLeft(swipeActionView: SwipeActionView): Boolean {return true}

    /**
     * Callback method to be invoked when user swipes the [SwipeActionView] to the
     * right, but only the first element. Requires MultiSwipeActionView as a Container.
     *
     * @param swipeActionView The [SwipeActionView] from which this method was invoked.
     *
     * @return Whether the container should return to default position. When `false`, then you
     * should manually call `reset` method to return to default position.
     * @see SwipeActionView
     */
    fun onSwipedHalfwayRight(swipeActionView: SwipeActionView): Boolean {return true}

    /**
     * Callback method to be invoked when the left swipe is complete.
     * A swipe is considered complete once the view returns back to its original position.
     *
     * @param swipeActionView The [SwipeActionView] from which this method was invoked.
     */
    fun onSwipeLeftComplete(swipeActionView: SwipeActionView) {}
    /**
     * Callback method to be invoked when the right swipe is complete.
     * A swipe is considered complete once the view returns back to its original position.
     *
     * @param swipeActionView The [SwipeActionView] from which this method was invoked.
     */
    fun onSwipeRightComplete(swipeActionView: SwipeActionView) {}
}
