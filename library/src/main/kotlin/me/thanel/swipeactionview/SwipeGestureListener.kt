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
     * @see SwipeActionView
     */
    fun onSwipedLeft(swipeActionView: SwipeActionView)

    /**
     * Callback method to be invoked when user swipes the [SwipeActionView] to the
     * right.
     *
     * @param swipeActionView The [SwipeActionView] from which this method was invoked.
     * @see SwipeActionView
     */
    fun onSwipedRight(swipeActionView: SwipeActionView)
}