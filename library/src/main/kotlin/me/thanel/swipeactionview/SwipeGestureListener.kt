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
     * @return Whether the container should return to default position. When `false`, then you
     * should manually call `reset` method to return to default position.
     * @see SwipeActionView
     */
    fun onSwipedLeft(swipeActionView: SwipeActionView): Boolean

    /**
     * Callback method to be invoked when user swipes the [SwipeActionView] to the
     * right.
     *
     * @param swipeActionView The [SwipeActionView] from which this method was invoked.
     * @return Whether the container should return to default position. When `false`, then you
     * should manually call `reset` method to return to default position.
     * @see SwipeActionView
     */
    fun onSwipedRight(swipeActionView: SwipeActionView): Boolean
}
