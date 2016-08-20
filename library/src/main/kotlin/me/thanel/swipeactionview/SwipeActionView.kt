package me.thanel.swipeactionview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
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
import me.thanel.swipeactionview.utils.*

/**
 * View that allows users to perform various actions by swiping it to the left or right sides.
 */
class SwipeActionView : FrameLayout {
    //region Private constants

    /**
     * Distance from edges in pixels that prevents starting of drag movement.
     *
     * NOTE: Using custom calculations to fix issues with drawer. Scaled edge slop from view
     * configuration is different by the one used by drawer layout which was giving issues.
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
     * The minimum speed required to execute swipe callback if user didn't swipe far enough.
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
     * The percentage of the [maxLeftSwipeDistance] or [maxRightSwipeDistance] after which swipe
     * callbacks can can be executed.
     */
    private val minActivationDistanceRatio = 0.8f

    /**
     * The interpolator used for swipe animations.
     */
    private val animationInterpolator = DecelerateInterpolator()

    /**
     * Ripple displayed after performing swipe left gesture.
     */
    private val leftSwipeRipple = SwipeRippleDrawable()

    /**
     * Ripple displayed after peforming swipe right gesture.
     */
    private val rightSwipeRipple = SwipeRippleDrawable()

    /**
     * The duration of ripple animation.
     */
    private val rippleAnimationDuration = 400L

    /**
     * Bounds for the ripple animations.
     */
    private val swipeBounds = Rect()

    //endregion

    //region Private properties

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
     * The view which appears when swiping content to the left side.
     */
    private var leftSwipeView: View? = null

    /**
     * The view which appears when swiping content to the right side.
     */
    private var rightSwipeView: View? = null

    /**
     * The container view which is displayed above [leftSwipeView] and [rightSwipeView].
     */
    private lateinit var container: View

    /**
     * The maximum distance allowed for dragging of the view to the left side.
     */
    private var maxLeftSwipeDistance = 0f

    /**
     * The maximum distance allowed for dragging of the view to the right side.
     */
    private var maxRightSwipeDistance = 0f

    /**
     * The minimum distance required to execute swipe callbacks when swiping to the left side.
     */
    private var minLeftActivationDistance = 0f

    /**
     * The minimum distance required to execute swipe callbacks when swiping to the right side.
     */
    private var minRightActivationDistance = 0f

    //endregion

    //region Initialization

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
        val typedArray = context.obtainStyledAttributes(intArrayOf(R.attr.colorPrimary))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()

        leftSwipeRipple.color = color
        rightSwipeRipple.color = color
        leftSwipeRipple.duration = rippleAnimationDuration
        rightSwipeRipple.duration = rippleAnimationDuration
        leftSwipeRipple.callback = this
        rightSwipeRipple.callback = this
    }

    override fun verifyDrawable(drawable: Drawable?) =
            drawable == leftSwipeRipple || drawable == rightSwipeRipple

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (childCount < 1) {
            throw IllegalStateException("Specify at least 1 child view to use as foreground content.")
        }
        if (childCount > 3) {
            throw IllegalStateException("Specify only up to 3 views.")
        }

        // The swipe direction of child views is determined by their `layout_gravity` attribute.
        // When they have set the `end` or `right` flags then they will be swipe-able to the left
        // side and otherwise to the right side.

        if (childCount >= 2) {
            val firstChild = getChildAt(0)

            if (firstChild.isRightAligned()) {
                leftSwipeView = firstChild
            } else {
                rightSwipeView = firstChild
            }

            if (childCount == 3) {
                val secondChild = getChildAt(1)

                if (secondChild.isRightAligned()) {
                    requireOppositeGravity(leftSwipeView)
                    leftSwipeView = secondChild
                } else {
                    requireOppositeGravity(rightSwipeView)
                    rightSwipeView = secondChild
                }
            }
        }

        // Last view always becomes foreground container
        container = getChildAt(childCount - 1)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        swipeBounds.setBoundsFrom(container)
        // TODO: Option to include padding in bounds
        //swipeBounds.setPaddingBoundsFrom(container)

        leftSwipeRipple.bounds = swipeBounds
        rightSwipeRipple.bounds = swipeBounds

        val verticalCenter = ((bottom - top) / 2).toFloat()
        val width = swipeBounds.right - swipeBounds.left
        leftSwipeRipple.setCenter(-edgeSlop.toFloat(), verticalCenter)
        rightSwipeRipple.setCenter((edgeSlop + width).toFloat(), verticalCenter)

        val maxRadius = radius(width.toDouble(), verticalCenter.toDouble()).toFloat()
        leftSwipeRipple.maxRadius = maxRadius
        rightSwipeRipple.maxRadius = maxRadius

        leftSwipeView?.let {
            maxLeftSwipeDistance = it.totalWidth.toFloat()
            minLeftActivationDistance = minActivationDistanceRatio * maxLeftSwipeDistance
        }

        rightSwipeView?.let {
            maxRightSwipeDistance = it.totalWidth.toFloat()
            minRightActivationDistance = minActivationDistanceRatio * maxRightSwipeDistance
        }
    }

    private fun requireOppositeGravity(view: View?) {
        if (view != null) {
            throw IllegalStateException(
                    "Background views must have opposite horizontal gravity." +
                            " One aligned to start and one to end.")
        }
    }

    //endregion

    //region Click and long click

    /**
     * The callback to be invoked when this view is clicked.
     */
    private var onClickListener: OnClickListener? = null

    /**
     * The callback to be invoked when this view is clicked and held.
     */
    private var onLongClickListener: OnLongClickListener? = null

    // The [setOnClickListener] and [setOnLongClickListener] together with their corresponding
    // performClick methods are overridden to make sure that the default view behavior won't execute
    // click callbacks by itself.
    // This works by hiding the existence of these listeners from the view and only switching them
    // to the real ones when performing click actions.

    override fun setOnClickListener(listener: OnClickListener?) {
        isClickable = listener != null
        onClickListener = listener
    }

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        isLongClickable = listener != null
        onLongClickListener = listener
    }

    override fun performClick(): Boolean {
        if (onClickListener == null) return super.performClick()

        super.setOnClickListener(onClickListener)
        val result = super.performClick()
        super.setOnClickListener(null)
        return result
    }

    override fun performLongClick(): Boolean {
        if (onLongClickListener == null) return super.performLongClick()

        super.setOnLongClickListener(onLongClickListener)
        val result = super.performLongClick()
        super.setOnLongClickListener(null)
        return result
    }

    //endregion

    //region Public API

    /**
     * Listener for the swipe left and right gestures.
     */
    var swipeGestureListener: SwipeGestureListener? = null

    /**
     * Animator for the view visible when swiping to the left.
     */
    var leftSwipeAnimator: SwipeActionViewAnimator? = null

    /**
     * Animator for the view visible when swiping to the right.
     */
    var rightSwipeAnimator: SwipeActionViewAnimator? = null

    /**
     * Tells whether swiping in the specified direction is enabled.
     *
     * @param direction The direction to check.
     *
     * @return Whether swiping in the specified direction is enabled.
     */
    fun hasEnabledDirection(direction: SwipeDirection): Boolean {
        val swipeView = getViewForDirection(direction) ?: return false
        return swipeView.visibility != View.GONE
    }

    /**
     * Set swiping in the specified direction as enabled or disabled.
     *
     * @param direction The swipe direction.
     * @param enabled Whether swiping in the specified direction should be enabled.
     */
    fun setDirectionEnabled(direction: SwipeDirection, enabled: Boolean) {
        val view = getViewForDirection(direction) ?:
                throw IllegalArgumentException("View for the specified direction doesn't exist.")
        view.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    /**
     * Move the view to its original position.
     */
    fun moveToOriginalPosition() {
        moveToOriginalPosition(0)
    }

    /**
     * Move the view to its original position.
     *
     * @param startDelay
     * The amount of delay, in milliseconds, to wait before starting the movement animation.
     */
    fun moveToOriginalPosition(startDelay: Long) {
        animateContainer(0f, 350, startDelay) {
            canPerformSwipeAction = true
        }
    }

    private fun getViewForDirection(direction: SwipeDirection) = when (direction) {
        SwipeDirection.Left -> leftSwipeView
        else -> rightSwipeView
    }

    //endregion

    //region Event handling

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                prepareDrag(e)
            }

            MotionEvent.ACTION_MOVE -> {
                return handleMoveEvent(e)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                cancelDrag()
                moveToOriginalPosition()
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
                if (isClickable && isTouchValid && !dragging && !inLongPress && !hasMovedVertically(e)) {
                    startPress(e.x, e.y)
                    performClick()
                }

                if (isPressed) {
                    // Unhighlight view after delay
                    if (!postDelayed({ stopPress() }, pressedStateDuration)) {
                        stopPress()
                    }
                }

                finishDrag()
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelDrag()
                moveToOriginalPosition()
            }
        }

        return dragging
    }

    //endregion

    //region Drawing

    private var originalBounds: Rect? = null

    override fun draw(canvas: Canvas) {
        originalBounds = canvas.clipBounds

        drawClipped(canvas) { super.draw(canvas) }
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.clipRect(originalBounds, Region.Op.REPLACE)

        val drawingTime = drawingTime

        drawClipped(canvas) {
            if (it < 0) {
                leftSwipeView?.let { drawChild(canvas, it, drawingTime) }
            } else if (it > 0) {
                rightSwipeView?.let { drawChild(canvas, it, drawingTime) }
            }
        }

        val saveCount = canvas.save()
        canvas.translate(container.translationX, 0f)

        if (leftSwipeRipple.isRunning) {
            leftSwipeRipple.draw(canvas)
        }
        if (rightSwipeRipple.isRunning) {
            rightSwipeRipple.draw(canvas)
        }

        canvas.restoreToCount(saveCount)

        drawChild(canvas, container, drawingTime)
    }

    /**
     * Draw only the part of view which becomes visible as a result of swiping gesture.
     *
     * @param canvas the [Canvas] to which the [View] is rendered.
     * @param drawAction the draw action to perform.
     */
    private fun drawClipped(canvas: Canvas, drawAction: (Int) -> Unit) {
        val translation = container.translationX.toInt()
        val originalBounds = canvas.clipBounds
        val bounds = canvas.clipBounds

        if (translation < 0) {
            bounds.set(bounds.right + translation, bounds.top, bounds.right, bounds.bottom)
        } else if (translation > 0) {
            bounds.set(bounds.left, bounds.top, bounds.left + translation, bounds.bottom)
        } else {
            bounds.set(0, 0, 0, 0)
        }

        canvas.clipRect(bounds, Region.Op.REPLACE)

        drawAction(translation)

        canvas.clipRect(originalBounds, Region.Op.REPLACE)
    }

    //endregion

    //region Actual SwipeActionView behavior code

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

        // Stop the animator to allow "catching" of view.
        // By "catching" I mean the possibility for user to click on the view and continue swiping
        // from the position at which it was when starting new swipe.
        animator?.cancel()
        animator = null

        handler.removeOurMessages()
    }

    /**
     * Check whether the touch is valid. Its validity is based on the distance from edges and
     * whether these edges are enabled.
     *
     * If we start drag close to edge then it means that we shouldn't handle this touch event. This
     * behavior is used because views like navigation drawer disturb with swiping when we start it
     * from the edge at which they appear and that's the only solution I found.
     *
     * @param e The motion even for which to check validity.
     */
    private fun checkTouchIsValid(e: MotionEvent) {
        val isLeftEdgeValid = e.x > edgeSlop
        val isRightEdgeValid = e.x < width - edgeSlop

        isTouchValid = isLeftEdgeValid && isRightEdgeValid
    }

    /**
     * Prepare the tap and long press actions by setting the times at which they should be executed.
     *
     * @param e The motion event for which to prepare the messages.
     */
    private fun prepareMessages(e: MotionEvent) {
        if (!isClickable && !isLongClickable) return

        handler.sendEmptyMessageAtTime(TAP, e.downTime + tapTimeout)

        if (isLongClickable) {
            handler.sendEmptyMessageAtTime(LONG_PRESS, e.downTime + longPressTimeout)
        }
    }

    /**
     * Handle the movement even and try to preform the view drag action.
     *
     * @param e The motion event to handle.
     *
     * @return Whether the view is being dragged.
     */
    private fun handleMoveEvent(e: MotionEvent): Boolean {
        if (inLongPress) return false

        if (!dragging) {
            if (hasMovedVertically(e)) {
                handler.removeOurMessages()
                return false
            }

            dragging = canStartDrag(e)
        }

        if (dragging) {
            parent.requestDisallowInterceptTouchEvent(true)
            velocityTracker.addMovement(e)
            resetClickAndLongClick()
            performDrag(e)
        }

        lastX = e.rawX
        return dragging
    }

    /**
     * Tells whether the user has moved their finger vertically.
     *
     * @param e The latest motion event.
     *
     * @return Whether the user has moved their finger vertically.
     */
    private fun hasMovedVertically(e: MotionEvent) = Math.abs(e.rawY - initialY) >= touchSlop

    /**
     * Tells whether the drag can be started.
     */
    private fun canStartDrag(e: MotionEvent): Boolean {
        val movedFarEnough = Math.abs(e.rawX - initialX) > touchSlop
        return isValidDelta(e.rawX - lastX) && movedFarEnough && isTouchValid
    }

    /**
     * Reset the callbacks and states used to execute click and long click actions.
     */
    private fun resetClickAndLongClick() {
        if (isPressed) {
            stopPress()
        }
        if (handler.hasOurMessages()) {
            handler.removeOurMessages()
        }
    }

    /**
     * Disable the pressed state of this view.
     */
    private fun stopPress() {
        isPressed = false
    }

    /**
     * Enable the pressed state of this view.
     *
     * @param x The x coordinate of click position.
     * @param y The y coordinate of click position.
     */
    private fun startPress(x: Float, y: Float) {
        isPressed = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawableHotspotChanged(x, y)
        }
    }

    /**
     * Tell whether swiping in the direction for the specified [delta] is valid.
     *
     * @param delta The swiping motion delta. Negative means left and positive right direction.
     *
     * @return Whether the direction for the specified [delta] is enabled.
     */
    private fun isValidDelta(delta: Float) = when {
        delta < 0 -> hasEnabledDirection(SwipeDirection.Left)
        delta > 0 -> hasEnabledDirection(SwipeDirection.Right)
        else -> false
    }

    /**
     * Tell whether the user has swiped view far enough to perform swipe callback
     *
     * @param swipeDistance The performed swipe distance.
     *
     * @return Whether the user has swiped far enough
     */
    private fun hasSwipedFarEnough(swipeDistance: Float) = when {
        swipeDistance < 0 -> swipeDistance < -minLeftActivationDistance
        swipeDistance > 0 -> swipeDistance > minRightActivationDistance
        else -> false
    }

    /**
     * Perform the drag by horizontally moving the view by movement delta.
     *
     * @param e The move motion even that triggered the current movement.
     */
    private fun performDrag(e: MotionEvent) {
        var delta = e.rawX - lastX

        // If we are swiping view away from view's default position make the swiping feel much
        // harder to drag.
        if (delta > 0 == container.translationX > 0 || container.translationX == 0f) {
            delta /= 3
        }

        container.translationX += delta
        container.translationX = limitInDistance(container.translationX)

        performViewAnimations()
    }

    /**
     * Limits the value between the maximal and minimal swipe distance values.
     */
    private fun limitInDistance(value: Float): Float {
        val min = if (hasEnabledDirection(SwipeDirection.Left)) -maxLeftSwipeDistance else 0f
        val max = if (hasEnabledDirection(SwipeDirection.Right)) maxRightSwipeDistance else 0f

        return clamp(value, min, max)
    }

    /**
     * Finish the drag by animating the view to its default position and executing an action if the
     * swipe was fast/far enough.
     */
    private fun finishDrag() {
        cancelDrag(false)
        velocityTracker.computeCurrentVelocity(100)

        val swipedFastEnough = Math.abs(velocityTracker.xVelocity) > minActivationSpeed

        if (swipedFastEnough && !isValidDelta(velocityTracker.xVelocity)) {
            moveToOriginalPosition()
            return
        }

        if (hasSwipedFarEnough(container.translationX) || swipedFastEnough) {
            activate(container.translationX > 0)
        } else {
            moveToOriginalPosition()
        }
    }

    /**
     * Move the view to fully swiped position and execute correct swipe callback.
     *
     * @param swipedRight Tells whether the view was swiped to the right side.
     */
    private fun activate(swipedRight: Boolean) {
        // If activation animation didn't finish, move the view to original position without
        // executing activate callback.
        if (!canPerformSwipeAction) {
            moveToOriginalPosition()
            return
        }
        canPerformSwipeAction = false

        if (swipedRight) {
            leftSwipeRipple.restart()
        } else {
            rightSwipeRipple.restart()
        }

        animateContainer(if (swipedRight) maxRightSwipeDistance else -maxLeftSwipeDistance, 250) {
            val shouldFinish = if (swipedRight) {
                swipeGestureListener?.onSwipedRight(this)
            } else {
                swipeGestureListener?.onSwipedLeft(this)
            }

            if (shouldFinish ?: true) {
                moveToOriginalPosition(200)
            }
        }
    }

    /**
     * Cancel the drag and click callbacks.
     *
     * @param stopPress Whether the pressed state should also be disabled.
     */
    private fun cancelDrag(stopPress: Boolean = true) {
        if (stopPress) {
            stopPress()
        }

        if (dragging) {
            parent.requestDisallowInterceptTouchEvent(false)
            dragging = false
        }
        handler.removeOurMessages()
        inLongPress = false
    }

    /**
     * Animate the swipe position of the container.
     *
     * @param targetTranslationX The target horizontal translation.
     * @param duration The duration of the animation.
     * @param startDelay The amount of delay, in milliseconds, to wait before starting animation,
     * @param onEnd The callback to be executed once animation finishes.
     */
    private fun animateContainer(targetTranslationX: Float,
                                 duration: Long,
                                 startDelay: Long = 0,
                                 onEnd: () -> Unit) {
        animator = ObjectAnimator.ofFloat(container, View.TRANSLATION_X, targetTranslationX).apply {
            setStartDelay(startDelay)
            setDuration(duration)
            interpolator = animationInterpolator
            addUpdateListener { performViewAnimations() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    onEnd()
                }
            })
        }
        animator!!.start()
    }

    /**
     * Perform animations on the views located in background.
     */
    private fun performViewAnimations() {
        invalidate()

        val swipeView: View?
        val animator: SwipeActionViewAnimator?
        val swipeDistance = container.translationX

        if (swipeDistance < 0) {
            swipeView = leftSwipeView
            animator = leftSwipeAnimator
        } else if (swipeDistance > 0) {
            swipeView = rightSwipeView
            animator = rightSwipeAnimator
        } else {
            return
        }

        if (swipeView == null || animator == null) return

        val absTranslationX = Math.abs(swipeDistance)
        val maxSwipeDistance = getMaxSwipeDistance(swipeDistance)

        val progress = absTranslationX / maxSwipeDistance
        val minActivationProgress = getMinActivationDistance(swipeDistance) / maxSwipeDistance

        animator.onUpdateSwipeProgress(swipeView, progress, minActivationProgress)
    }

    /**
     * Get max swipe distance for the specified delta.
     *
     * @param delta The swipe delta.
     *
     * @return Max swipe distance.
     */
    private fun getMaxSwipeDistance(delta: Float) = when {
        delta < 0 -> maxLeftSwipeDistance
        else -> maxRightSwipeDistance
    }

    /**
     * Get min activation distance for the specified delta.
     *
     * @param delta The swipe delta.
     *
     * @return Min activation distance.
     */
    private fun getMinActivationDistance(delta: Float) = when {
        delta < 0 -> minLeftActivationDistance
        else -> minRightActivationDistance
    }

    //endregion

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
                        swipeActionView.startPress(swipeActionView.initialX, swipeActionView.initialY)
                    }
                }
            }

            fun hasOurMessages() = hasMessages(LONG_PRESS) || hasMessages(TAP)

            fun removeOurMessages() {
                removeMessages(LONG_PRESS)
                removeMessages(TAP)
            }
        }
    }
}
