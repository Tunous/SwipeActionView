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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Message
import androidx.annotation.ColorInt
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
    private val tapTimeout = ViewConfiguration.getTapTimeout().toLong()

    /**
     * The duration in milliseconds we will wait to see if a touch event is a long tap.
     */
    private val longPressTimeout = tapTimeout + ViewConfiguration.getLongPressTimeout().toLong()

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
    @SuppressLint("Recycle")
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
     * Ripple displayed after performing swipe left gesture.
     */
    private val leftSwipeRipple = SwipeRippleDrawable()

    /**
     * Ripple displayed after performing swipe right gesture.
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

    /**
     * The container translation animator.
     */
    private val animator by lazy {
        ObjectAnimator.ofFloat(container, View.TRANSLATION_X, 0f).apply {
            interpolator = DecelerateInterpolator()
            addUpdateListener { performViewAnimations() }
        }
    }

    /**
     * Tells whether new swipe action can be executed.
     */
    private var canPerformSwipeAction = true

    /**
     * The raw x coordinate of the initial motion event.
     */
    private var initialRawX = 0f

    /**
     * The raw y coordinate of the initial motion event.
     */
    private var initialRawY = 0f

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

    /**
     * Determines whether ripple drawables should have padding.
     */
    private var rippleTakesPadding = false

    /**
     * Tells whether the background views and main background should be always drawn - no matter if
     * they are behind the container or not.
     *
     * By default they are only drawn when not located under container view. This setting is useful
     * when dealing with transparent or special views such as `CardView`.
     */
    private var alwaysDrawBackground = false

    /**
     * Id of background to preview. Controlled by `sav_tools_previewBackground` attribute.
     */
    private var previewBackground = 0

    /**
     * Id of ripple to preview. Controlled by `sav_tools_previewRipple` attribute.
     */
    private var previewRipple = 0

    /**
     * The callback to be invoked when this view is clicked.
     */
    private var onClickListener: OnClickListener? = null

    /**
     * The callback to be invoked when this view is clicked and held.
     */
    private var onLongClickListener: OnLongClickListener? = null

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
        val swipeLeftRippleColor = typedArray.getColorStateList(R.styleable.SwipeActionView_sav_swipeLeftRippleColor)
        val swipeRightRippleColor = typedArray.getColorStateList(R.styleable.SwipeActionView_sav_swipeRightRippleColor)
        rippleTakesPadding = typedArray.getBoolean(R.styleable.SwipeActionView_sav_rippleTakesPadding, false)
        alwaysDrawBackground = typedArray.getBoolean(R.styleable.SwipeActionView_sav_alwaysDrawBackground, false)

        if (isInEditMode) {
            previewBackground = typedArray.getInt(R.styleable.SwipeActionView_sav_tools_previewBackground, 0)
            previewRipple = typedArray.getInt(R.styleable.SwipeActionView_sav_tools_previewRipple, 0)
        }

        typedArray.recycle()

        leftSwipeRipple.color = swipeLeftRippleColor?.defaultColor ?: -1
        rightSwipeRipple.color = swipeRightRippleColor?.defaultColor ?: -1
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

        leftSwipeRipple.bounds = swipeBounds
        rightSwipeRipple.bounds = swipeBounds

        val verticalCenter = ((bottom - top) / 2).toFloat()
        val width = swipeBounds.right - swipeBounds.left
        leftSwipeRipple.setCenter((edgeSlop + width).toFloat(), verticalCenter)
        rightSwipeRipple.setCenter(-edgeSlop.toFloat(), verticalCenter)

        val maxRadius = radius(width.toDouble(), verticalCenter.toDouble()).toFloat()
        leftSwipeRipple.maxRadius = maxRadius
        rightSwipeRipple.maxRadius = maxRadius

        leftSwipeView?.let {
            maxLeftSwipeDistance = it.totalWidth.toFloat() - container.marginEnd
            minLeftActivationDistance = minActivationDistanceRatio * maxLeftSwipeDistance
        }

        rightSwipeView?.let {
            maxRightSwipeDistance = it.totalWidth.toFloat() - container.marginStart
            minRightActivationDistance = minActivationDistanceRatio * maxRightSwipeDistance
        }

        if (isInEditMode) {
            when (previewBackground) {
                SwipeDirection.LEFT -> leftSwipeView?.let {
                    container.translationX = -maxLeftSwipeDistance
                }
                SwipeDirection.RIGHT -> rightSwipeView?.let {
                    container.translationX = maxRightSwipeDistance
                }
            }

            leftSwipeRipple.progress = 0.75f
            rightSwipeRipple.progress = 0.75f
        }
    }

    private fun requireOppositeGravity(view: View?) {
        if (view != null) {
            throw IllegalStateException(
                    "Background views must have opposite horizontal gravity." +
                            " One aligned to start and one to end.")
        }
    }

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
     * @throws IllegalAccessException When view for the specified direction doesn't exist.
     */
    @Suppress("unused")
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

    /**
     * Set ripple color for the specified swipe direction. Use -1 to disable ripple.
     *
     * @param direction The direction of the swipe gesture.
     * @param color The ripple color.
     */
    @Suppress("unused")
    fun setRippleColor(direction: SwipeDirection, @ColorInt color: Int) = when (direction) {
        SwipeDirection.Left -> leftSwipeRipple.color = color
        SwipeDirection.Right -> rightSwipeRipple.color = color
    }

    private fun getViewForDirection(direction: SwipeDirection) = when (direction) {
        SwipeDirection.Left -> leftSwipeView
        else -> rightSwipeView
    }

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

    override fun draw(canvas: Canvas) {
        if (alwaysDrawBackground) {
            super.draw(canvas)
        } else {
            canvas.drawInBoundsOf(container, Region.Op.DIFFERENCE) {
                super.draw(canvas)
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        canvas.drawInBoundsOf(container, Region.Op.REPLACE, rippleTakesPadding) {
            if (isInEditMode) {
                when (previewRipple) {
                    SwipeDirection.LEFT -> {
                        if (leftSwipeRipple.hasColor) {
                            leftSwipeRipple.draw(canvas)
                        }
                    }
                    SwipeDirection.RIGHT -> {
                        if (rightSwipeRipple.hasColor) {
                            rightSwipeRipple.draw(canvas)
                        }
                    }
                }
                return@drawInBoundsOf
            }

            if (leftSwipeRipple.hasColor && leftSwipeRipple.isRunning) {
                leftSwipeRipple.draw(canvas)
            }
            if (rightSwipeRipple.hasColor && rightSwipeRipple.isRunning) {
                rightSwipeRipple.draw(canvas)
            }
        }
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        return if (alwaysDrawBackground || child != container) {
            super.drawChild(canvas, child, drawingTime)
        } else {
            canvas.drawInBoundsOf(container, Region.Op.REPLACE) {
                super.drawChild(canvas, child, drawingTime)
            }
        }
    }

    private fun prepareDrag(e: MotionEvent) {
        checkTouchIsValid(e)

        velocityTracker.clear()
        velocityTracker.addMovement(e)

        lastX = e.rawX
        initialRawX = e.rawX
        initialRawY = e.rawY

        // Stop the animator to allow "catching" of view.
        // By "catching" I mean the possibility for user to click on the view and continue swiping
        // from the position at which it was when starting new swipe.
        animator.cancel()

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

        handler.x = e.x
        handler.y = e.y
        handler.sendEmptyMessageDelayed(TAP, tapTimeout)

        if (isLongClickable) {
            handler.sendEmptyMessageDelayed(LONG_PRESS, longPressTimeout)
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
    private fun hasMovedVertically(e: MotionEvent) = Math.abs(e.rawY - initialRawY) >= touchSlop

    /**
     * Tells whether the drag can be started by the user based on provided motion event.
     */
    private fun canStartDrag(e: MotionEvent): Boolean {
        val movedFarEnough = Math.abs(e.rawX - initialRawX) > touchSlop
        return movedFarEnough && isTouchValid
    }

    private fun resetClickAndLongClick() {
        if (isPressed) {
            stopPress()
        }
        if (handler.hasOurMessages()) {
            handler.removeOurMessages()
        }
    }

    private fun stopPress() {
        isPressed = false
    }

    private fun startPress(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawableHotspotChanged(x, y)
        }

        isPressed = true
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
     * Limits the value between the maximal and minimal swipe distance values to make sure that
     * view won't be swiped too far away.
     *
     * @param value The value to limit.
     *
     * @return The value clamped between maximum swipe distances.
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
     * Tell whether swiping in the direction for the specified [delta] is valid.
     *
     * This makes sure that fling gesture in incorrect direction won't execute gesture listener and
     * start animation of the container.
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
     * Move the view to fully swiped position and execute correct swipe callback.
     *
     * @param swipedRight Tells whether the view was swiped to the right instead of left side.
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
            rightSwipeRipple.restart()
        } else {
            leftSwipeRipple.restart()
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
        with(animator) {
            setStartDelay(startDelay)
            setDuration(duration)
            setFloatValues(targetTranslationX)
            removeAllListeners()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    onEnd()
                }
            })
            start()
        }
    }

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
     * Get max possible swipe distance for the specified delta. Users won't be able to swipe further
     * than this value.
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
     * Get min activation distance for the specified delta. Once users swipes view above this
     * distance swipe callback will be called upon view release.
     *
     * @param delta The swipe delta.
     *
     * @return Min distance to swipe to be able to execute swipe callbacks.
     */
    private fun getMinActivationDistance(delta: Float) = when {
        delta < 0 -> minLeftActivationDistance
        else -> minRightActivationDistance
    }

    companion object {
        private const val LONG_PRESS = 1
        private const val TAP = 2

        private class PressTimeoutHandler(private val swipeActionView: SwipeActionView) : Handler() {
            var x = 0f
            var y = 0f

            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    LONG_PRESS -> {
                        swipeActionView.inLongPress = true
                        swipeActionView.performLongClick()
                    }
                    TAP -> {
                        swipeActionView.startPress(x, y)
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
