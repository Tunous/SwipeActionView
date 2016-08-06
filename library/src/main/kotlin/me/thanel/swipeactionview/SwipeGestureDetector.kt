package me.thanel.swipeactionview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Build
import android.os.Handler
import android.os.Message
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import me.thanel.swipeactionview.animation.ScalableIconAnimator
import me.thanel.swipeactionview.animation.SwipeActionViewBackgroundAnimator
import me.thanel.swipeactionview.utils.clamp
import me.thanel.swipeactionview.utils.dpToPx

internal class SwipeGestureDetector(private val swipeActionView: SwipeActionView) {
    /**
     * The swipe gesture listener.
     */
    internal var swipeGestureListener: SwipeGestureListener? = null

    /**
     * Distance from edges in pixels that prevents starting of the drag.
     *
     * NOTE: Using custom calculations to fix issues with drawer. Scaled edge
     * slop from view configuration is different by the one used by drawer
     * layout which was giving issues.
     */
    private val edgeSlop = (20 * swipeActionView.context.resources.displayMetrics.density + 0.5f).toInt()

    /**
     * Distance of motion in pixels required before the view can be dragged.
     */
    private val touchSlop = ViewConfiguration.get(swipeActionView.context).scaledTouchSlop

    /**
     * The duration in milliseconds we will wait to see if a touch event is a tap or a scroll.
     */
    private val tapTimeout = ViewConfiguration.getTapTimeout()

    /**
     * The duration in milliseconds we will wait to see if a touch event is a long tap.
     */
    private val longPressTimeout = tapTimeout + ViewConfiguration.getLongPressTimeout()

    /**
     * The duration of the pressed state.
     */
    private val pressedStateDuration = ViewConfiguration.getPressedStateDuration().toLong()

    /**
     * The maximum distance allowed for dragging of the view.
     */
    private val maxSwipeDistance: Float = 52f.dpToPx(swipeActionView.context)

    /**
     * The minimal distance required to execute swipe callback.
     */
    private val minActivationDistance: Float = 0.8f * maxSwipeDistance

    /**
     * The minimal speed required to execute swipe callback if user didn't
     * swipe far enough.
     */
    private val minActivationSpeed: Float = 200f

    /**
     * The velocity tracker.
     */
    private val velocityTracker: VelocityTracker = VelocityTracker.obtain()

    /**
     * The long press gesture handler.
     */
    private val handler: PressTimeoutHandler = PressTimeoutHandler(this)

    /**
     * The currently running view animator.
     */
    private var animator: ObjectAnimator? = null

    /**
     * Tells whether new swipe action can be executed.
     */
    private var canPerformSwipeAction: Boolean = true

    /**
     * The x coordinate of the initial motion event.
     */
    private var initialX: Float = 0f

    /**
     * The y coordinate of the initial motion event.
     */
    private var initialY: Float = 0f

    /**
     * The x coordinate of the last received move motion event.
     */
    private var lastX: Float = 0f

    /**
     * Tells whether the drag is currently being performed.
     */
    private var dragging: Boolean = false

    /**
     * Tells whether current action is a long press.
     */
    private var inLongPress: Boolean = false

    /**
     * Tells whether currently performed touch action is valid.
     */
    private var isTouchValid: Boolean = false

    /**
     * Limits the value between the maximal and minimal swipe distance values.
     */
    private fun limitInDistance(value: Float): Float = clamp(value,
            if (swipeActionView.hasEnabledDirection(SwipeActionView.DIRECTION_LEFT)) -maxSwipeDistance else 0f,
            if (swipeActionView.hasEnabledDirection(SwipeActionView.DIRECTION_RIGHT)) maxSwipeDistance else 0f)

    /**
     * Tells whether the drag can start.
     */
    private fun canDrag(e: MotionEvent): Boolean =
            isEnabledDirection(e.rawX - lastX) && Math.abs(e.rawX - initialX) > touchSlop && isTouchValid

    /**
     * Tell whether swiping in the direction for the specified [delta] is enabled.
     *
     * @param delta The swiping motion delta. Negative means left and positive right direction.
     *
     * @return Whether the direction for the specified [delta] is enabled.
     */
    private fun isEnabledDirection(delta: Float): Boolean {
        return when {
            delta < 0 -> swipeActionView.hasEnabledDirection(SwipeActionView.DIRECTION_LEFT)
            delta > 0 -> swipeActionView.hasEnabledDirection(SwipeActionView.DIRECTION_RIGHT)
            else -> false
        }
    }

    internal fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                prepareDrag(e)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                setPressed(false, e.x, e.y)
                cancel()
                snap()
            }

            MotionEvent.ACTION_MOVE -> {
                return handleMoveEvent(e)
            }
        }

        // In most cases we don't want to handle touch events alone. We give child views a chance to
        // intercept them.
        return false
    }

    internal fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                prepareDrag(e)
                prepareMessages(e)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                return handleMoveEvent(e)
            }

            MotionEvent.ACTION_UP -> {
                if (swipeActionView.isClickable && !dragging && !inLongPress && Math.abs(e.rawY - initialY) < touchSlop && isTouchValid) {
                    setPressed(true, e.x, e.y)
                    swipeActionView.performClick()
                }

                if (swipeActionView.isPressed) {
                    // Unhighlight view after delay
                    if (!swipeActionView.postDelayed({ setPressed(false, e.x, e.y) }, pressedStateDuration)) {
                        setPressed(false, e.x, e.y)
                    }
                }

                finishDrag()
            }

            MotionEvent.ACTION_CANCEL -> {
                setPressed(false, e.x, e.y)
                cancel()
                snap()
            }
        }

        return dragging
    }

    private fun handleMoveEvent(e: MotionEvent): Boolean {
        if (inLongPress) return false

        if (!dragging) {
            // Disallow drag if moved vertically.
            if (Math.abs(e.rawY - initialY) >= touchSlop) {
                handler.removeAllMessages()
                return false
            }

            dragging = canDrag(e)
        }

        if (dragging) {
            setPressed(false, e.x, e.y)
            handler.removeAllMessages()
            swipeActionView.parent.requestDisallowInterceptTouchEvent(true)
            velocityTracker.addMovement(e)
            performDrag(e)
        }

        lastX = e.rawX
        return dragging
    }

    /**
     * Check whether the touch is valid. Its validity is based on the distance from edges and whether
     * these edges are enabled.
     *
     * If we start drag close to disabled edge then it means that we shouldn't handle this touch
     * event. This behavior is used mostly because views like navigation drawer disturb with swiping
     * when we start it from the edge at which they appear.
     *
     * @param e The motion even for which to check validity.
     */
    private fun checkTouchIsValid(e: MotionEvent) {
        val isLeftEdgeValid = swipeActionView.hasEnabledEdge(SwipeActionView.EDGE_LEFT) || e.x > edgeSlop
        val isRightEdgeValid = swipeActionView.hasEnabledEdge(SwipeActionView.EDGE_RIGHT) || e.x < swipeActionView.width - edgeSlop

        isTouchValid = isLeftEdgeValid && isRightEdgeValid
    }

    private fun setPressed(pressed: Boolean, x: Float = 0f, y: Float = 0f) {
        swipeActionView.apply {
            isPressed = pressed

            if (pressed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawableHotspotChanged(x, y)
            }
        }
    }

    /**
     * Prepare all settings to start a new drag.
     *
     * @param e The received motion event.
     */
    private fun prepareDrag(e: MotionEvent) {
        checkTouchIsValid(e)

        velocityTracker.clear()
        velocityTracker.addMovement(e)

        lastX = e.rawX
        initialX = e.rawX
        initialY = e.rawY

        // Stop the animator to allow "catching" of view
        animator?.cancel()
        animator = null

        handler.removeAllMessages()
    }

    private fun prepareMessages(e: MotionEvent) {
        if (swipeActionView.isClickable || swipeActionView.isLongClickable) {
            handler.sendEmptyMessageAtTime(TAP, e.downTime + tapTimeout)

            if (swipeActionView.isLongClickable) {
                handler.sendEmptyMessageAtTime(LONG_PRESS, e.downTime + longPressTimeout)
            }
        }
    }

    /**
     * Perform the drag by horizontally moving the view by movement delta.
     *
     * @param e The move motion even that triggered the current movement.
     */
    private fun performDrag(e: MotionEvent) {
        var delta = e.rawX - lastX

        // If we are swiping view away from view's default position make
        // the swiping feel much harder
        if (delta > 0 == swipeActionView.container.translationX > 0 || swipeActionView.container.translationX == 0f) {
            delta /= 3
        }

        swipeActionView.container.translationX += delta
        swipeActionView.container.translationX = limitInDistance(swipeActionView.container.translationX)

        scaleIcon()
    }

    /**
     * Finish the drag by animating the view to its default position and
     * executing an action if the swipe was fast/far enough.
     */
    private fun finishDrag() {
        cancel()

        velocityTracker.computeCurrentVelocity(100)
        val swipedFastEnough = Math.abs(velocityTracker.xVelocity) > minActivationSpeed

        if (swipedFastEnough && !isEnabledDirection(velocityTracker.xVelocity)) {
            snap()
            return
        }

        val swipedFarEnough = Math.abs(swipeActionView.container.translationX) > minActivationDistance

        if (swipedFarEnough || swipedFastEnough) {
            activate(swipeActionView.container.translationX > 0)
        } else {
            snap()
        }
    }

    private fun cancel() {
        if (dragging) {
            swipeActionView.parent.requestDisallowInterceptTouchEvent(false)
            dragging = false
        }
        handler.removeAllMessages()
        inLongPress = false
    }

    /**
     * Move the view to fully swiped view and execute correct swipe callback.
     */
    private fun activate(swipedRight: Boolean) {
        // If activation animation didn't finish move the view to original
        // position without executing of activate callback.
        if (!canPerformSwipeAction) {
            snap()
            return
        }
        canPerformSwipeAction = false

        animateContainer(if (swipedRight) maxSwipeDistance else -maxSwipeDistance, 200) {
            if (swipedRight) {
                swipeGestureListener?.onSwipedRight(swipeActionView)
            } else {
                swipeGestureListener?.onSwipedLeft(swipeActionView)
            }

            snap()
        }
    }

    /**
     * Move the view to its original position.
     */
    private fun snap() {
        animateContainer(0f, 350) {
            canPerformSwipeAction = true
            swipeActionView.leftBackground?.visibility = View.GONE
            swipeActionView.rightBackground?.visibility = View.GONE
        }
    }

    private fun animateContainer(targetTranslationX: Float, duration: Long, onEnd: () -> Unit) {
        animator = ObjectAnimator.ofFloat(swipeActionView.container, View.TRANSLATION_X, targetTranslationX)
        animator?.duration = duration
        animator?.interpolator = DecelerateInterpolator()
        animator?.addUpdateListener { scaleIcon() }
        animator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                onEnd()
            }
        })
        animator?.start()
    }

    private var leftBackgroundAnimator: SwipeActionViewBackgroundAnimator? = ScalableIconAnimator()

    private fun scaleIcon() {
        val absTranslationX = Math.abs(swipeActionView.container.translationX)

        val progress = absTranslationX / maxSwipeDistance
        val minActivationProgress = minActivationDistance / maxSwipeDistance

        val leftBackground = swipeActionView.leftBackground
        if (leftBackground != null && leftBackgroundAnimator != null) {
            leftBackgroundAnimator!!.onUpdateSwipeProgress(leftBackground, progress, minActivationProgress)
        }

        if (swipeActionView.container.translationX > 0) {
            swipeActionView.leftBackground?.visibility = View.VISIBLE
            swipeActionView.rightBackground?.visibility = View.GONE
        } else {
            swipeActionView.leftBackground?.visibility = View.GONE
            swipeActionView.rightBackground?.visibility = View.VISIBLE
        }
    }

    companion object {
        /**
         * Long press handler message id.
         */
        private const val LONG_PRESS = 1

        /**
         * Tap handler message id.
         */
        private const val TAP = 2

        private class PressTimeoutHandler(private val swipeGestureDetector: SwipeGestureDetector) : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    LONG_PRESS -> {
                        swipeGestureDetector.inLongPress = true
                        swipeGestureDetector.swipeActionView.performLongClick()
                    }
                    TAP -> {
                        swipeGestureDetector.setPressed(true, swipeGestureDetector.initialX, swipeGestureDetector.initialY)
                    }
                }
            }

            fun removeAllMessages() {
                removeMessages(LONG_PRESS)
                removeMessages(TAP)
            }
        }
    }
}
