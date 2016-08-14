package me.thanel.swipeactionview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import me.thanel.swipeactionview.animation.SwipeActionViewAnimator
import me.thanel.swipeactionview.utils.clamp
import me.thanel.swipeactionview.utils.dpToPx

/**
 * View that allows users to perform various actions by swiping it to the sides.
 */
@Suppress("unused")
class SwipeActionView : FrameLayout {
    private var disabledEdges: Int = 0
    private var swipeFirstViewLeft = true

    internal lateinit var container: View
        private set

    var leftSwipeAnimator: SwipeActionViewAnimator? = null
    var rightSwipeAnimator: SwipeActionViewAnimator? = null

    var leftSwipeView: View? = null
        private set

    var rightSwipeView: View? = null
        private set

    internal var onClickListener: OnClickListener? = null

    internal var onLongClickListener: OnLongClickListener? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeActionView)

        swipeFirstViewLeft = typedArray.getBoolean(R.styleable.SwipeActionView_swipeFirstViewLeft, swipeFirstViewLeft)
        disabledEdges = typedArray.getInt(R.styleable.SwipeActionView_disabledEdges, 0)

        typedArray.recycle()
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        isClickable = listener != null
        onClickListener = listener
    }

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        isLongClickable = listener != null
        onLongClickListener = listener
    }

    override fun performClick(): Boolean {
        super.setOnClickListener(onClickListener)
        val result = super.performClick()
        super.setOnClickListener(null)
        return result
    }

    override fun performLongClick(): Boolean {
        super.setOnLongClickListener(onLongClickListener)
        val result = super.performLongClick()
        super.setOnLongClickListener(null)
        return result
    }

    /**
     * Enable edge for the specified [edge].
     */
    fun enableEdge(edge: Int) {
        disabledEdges = disabledEdges or edge
    }

    /**
     * Disable edge for the specified [edge].
     */
    fun disableEdge(edge: Int) {
        disabledEdges = disabledEdges and edge.inv()
    }

    /**
     * Tells whether swiping in the specified [direction] is enabled.
     */
    fun hasEnabledDirection(direction: Int): Boolean {
        return when (direction) {
            DIRECTION_LEFT -> leftSwipeView?.visibility == View.VISIBLE
            DIRECTION_RIGHT -> rightSwipeView?.visibility == View.VISIBLE

            else -> throw IllegalArgumentException("Unknown direction: $direction")
        }
    }

    /**
     * Tells whether edge for the specified [edge] is enabled.
     */
    fun hasEnabledEdge(edge: Int): Boolean =
            (disabledEdges and edge) != edge

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (childCount < 1) {
            throw IllegalStateException("Specify at least 1 child view to use as foreground content.")
        }
        if (childCount > 3) {
            throw IllegalStateException("Specify only up to 3 views.")
        }

        if (childCount >= 2) {
            if (swipeFirstViewLeft) {
                leftSwipeView = getChildAt(0)
            } else {
                rightSwipeView = getChildAt(0)
            }

            if (childCount == 3) {
                if (swipeFirstViewLeft) {
                    rightSwipeView = getChildAt(1)
                } else {
                    leftSwipeView = getChildAt(1)
                }
            }
        }

        // Last view becomes foreground container
        container = getChildAt(childCount - 1)
    }

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
    private val edgeSlop = (20 * context.resources.displayMetrics.density + 0.5f).toInt()

    /**
     * Distance of motion in pixels required before the view can be dragged.
     */
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

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
    private val maxSwipeDistance = 52f.dpToPx(context)

    /**
     * The minimal distance required to execute swipe callback.
     */
    private val minActivationDistance = 0.8f * maxSwipeDistance

    /**
     * The minimal speed required to execute swipe callback if user didn't
     * swipe far enough.
     */
    private val minActivationSpeed = 200f

    /**
     * The velocity tracker.
     */
    private val velocityTracker = VelocityTracker.obtain()

    /**
     * The long press gesture handler.
     */
    private val handler = PressTimeoutHandler(this)

    /**
     * The currently running view animator.
     */
    private var animator: ObjectAnimator? = null

    /**
     * Tells whether new swipe action can be executed.
     */
    private var canPerformSwipeAction = true

    /**
     * The x coordinate of the initial motion event.
     */
    private var initialX = 0f

    /**
     * The y coordinate of the initial motion event.
     */
    private var initialY = 0f

    /**
     * The x coordinate of the last received move motion event.
     */
    private var lastX = 0f

    /**
     * Tells whether the drag is currently being performed.
     */
    private var dragging = false

    /**
     * Tells whether current action is a long press.
     */
    private var inLongPress = false

    /**
     * Tells whether currently performed touch action is valid.
     */
    private var isTouchValid = false

    /**
     * Limits the value between the maximal and minimal swipe distance values.
     */
    private fun limitInDistance(value: Float) = clamp(value,
            if (hasEnabledDirection(SwipeActionView.DIRECTION_LEFT)) -maxSwipeDistance else 0f,
            if (hasEnabledDirection(SwipeActionView.DIRECTION_RIGHT)) maxSwipeDistance else 0f)

    /**
     * Tells whether the drag can start.
     */
    private fun canDrag(e: MotionEvent) =
            isEnabledDirection(e.rawX - lastX) && Math.abs(e.rawX - initialX) > touchSlop && isTouchValid

    /**
     * Tell whether swiping in the direction for the specified [delta] is enabled.
     *
     * @param delta The swiping motion delta. Negative means left and positive right direction.
     *
     * @return Whether the direction for the specified [delta] is enabled.
     */
    private fun isEnabledDirection(delta: Float) = when {
        delta < 0 -> hasEnabledDirection(SwipeActionView.DIRECTION_LEFT)
        delta > 0 -> hasEnabledDirection(SwipeActionView.DIRECTION_RIGHT)
        else -> false
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
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

    override fun onTouchEvent(e: MotionEvent): Boolean {
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
                if (isClickable && !dragging && !inLongPress && Math.abs(e.rawY - initialY) < touchSlop && isTouchValid) {
                    setPressed(true, e.x, e.y)
                    performClick()
                }

                if (isPressed) {
                    // Unhighlight view after delay
                    if (!postDelayed({ setPressed(false, e.x, e.y) }, pressedStateDuration)) {
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
            parent.requestDisallowInterceptTouchEvent(true)
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
        val isLeftEdgeValid = hasEnabledEdge(SwipeActionView.EDGE_LEFT) || e.x > edgeSlop
        val isRightEdgeValid = hasEnabledEdge(SwipeActionView.EDGE_RIGHT) || e.x < width - edgeSlop

        isTouchValid = isLeftEdgeValid && isRightEdgeValid
    }

    private fun setPressed(pressed: Boolean, x: Float = 0f, y: Float = 0f) {
        isPressed = pressed

        if (pressed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawableHotspotChanged(x, y)
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
        if (isClickable || isLongClickable) {
            handler.sendEmptyMessageAtTime(TAP, e.downTime + tapTimeout)

            if (isLongClickable) {
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
        if (delta > 0 == container.translationX > 0 || container.translationX == 0f) {
            delta /= 3
        }

        container.translationX += delta
        container.translationX = limitInDistance(container.translationX)

        performAnimations()
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

        val swipedFarEnough = Math.abs(container.translationX) > minActivationDistance

        if (swipedFarEnough || swipedFastEnough) {
            activate(container.translationX > 0)
        } else {
            snap()
        }
    }

    private fun cancel() {
        if (dragging) {
            parent.requestDisallowInterceptTouchEvent(false)
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

        animateContainer(if (swipedRight) maxSwipeDistance else -maxSwipeDistance, 250) {
            val shouldFinish = if (swipedRight) {
                swipeGestureListener?.onSwipedRight(this)
            } else {
                swipeGestureListener?.onSwipedLeft(this)
            }

            if (shouldFinish ?: true) {
                snap(200)
            }
        }
    }

    /**
     * Move the view to its original position.
     */
    internal fun snap(startDelay: Long = 0) {
        animateContainer(0f, 350, startDelay) {
            canPerformSwipeAction = true
        }
    }

    private fun animateContainer(targetTranslationX: Float, duration: Long, startDelay: Long = 0, onEnd: () -> Unit) {
        animator = ObjectAnimator.ofFloat(container, View.TRANSLATION_X, targetTranslationX)
        animator?.startDelay = startDelay
        animator?.duration = duration
        animator?.interpolator = DecelerateInterpolator()
        animator?.addUpdateListener { performAnimations() }
        animator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                onEnd()
            }
        })
        animator?.start()
    }

    private fun performAnimations() {
        val absTranslationX = Math.abs(container.translationX)

        val progress = absTranslationX / maxSwipeDistance
        val minActivationProgress = minActivationDistance / maxSwipeDistance

        val swipeView: View?
        val animator: SwipeActionViewAnimator?

        if (container.translationX < 0) {
            swipeView = leftSwipeView
            animator = leftSwipeAnimator
        } else if (container.translationX > 0) {
            swipeView = rightSwipeView
            animator = rightSwipeAnimator
        } else {
            return
        }

        if (swipeView != null && animator != null) {
            animator.onUpdateSwipeProgress(swipeView, progress, minActivationProgress)
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

        private class PressTimeoutHandler(private val swipeActionView: SwipeActionView) : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    LONG_PRESS -> {
                        swipeActionView.inLongPress = true
                        swipeActionView.performLongClick()
                    }
                    TAP -> {
                        swipeActionView.setPressed(true, swipeActionView.initialX, swipeActionView.initialY)
                    }
                }
            }

            fun removeAllMessages() {
                removeMessages(LONG_PRESS)
                removeMessages(TAP)
            }
        }

        /**
         * Describes direction of swiping of the view to the left side.
         */
        const val DIRECTION_LEFT = 1

        /**
         * Describes direction for swiping of the view to the right side.
         */
        const val DIRECTION_RIGHT = 2

        /**
         * The left edge.
         */
        const val EDGE_LEFT = 1

        /**
         * The right edge.
         */
        const val EDGE_RIGHT = 2
    }
}
