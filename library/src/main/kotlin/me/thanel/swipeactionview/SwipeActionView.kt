@file:Suppress("unused")

package me.thanel.swipeactionview

import android.content.Context
import android.support.v4.view.GravityCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import me.thanel.swipeactionview.animation.SwipeActionViewAnimator

/**
 * View that allows users to perform various actions by swiping it to the sides.
 */
class SwipeActionView : FrameLayout {
    private val swipeGestureDetector: SwipeGestureDetector = SwipeGestureDetector(this)
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

    fun reset() {
        swipeGestureDetector.snap()
    }

    /**
     * The swipe gesture listener.
     */
    var swipeGestureListener: SwipeGestureListener?
        get() = swipeGestureDetector.swipeGestureListener
        set(listener) {
            swipeGestureDetector.swipeGestureListener = listener
        }

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

    override fun onInterceptTouchEvent(ev: MotionEvent) =
            swipeGestureDetector.onInterceptTouchEvent(ev)

    override fun onTouchEvent(event: MotionEvent) =
            swipeGestureDetector.onTouchEvent(event)

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
                leftSwipeView?.setGravity(GravityCompat.END)
            } else {
                rightSwipeView = getChildAt(0)
            }

            if (childCount == 3) {
                if (swipeFirstViewLeft) {
                    rightSwipeView = getChildAt(1)
                } else {
                    leftSwipeView = getChildAt(1)
                    leftSwipeView?.setGravity(GravityCompat.END)
                }
            }
        }

        // Last view becomes foreground container
        container = getChildAt(childCount - 1)
    }

    private fun View.setGravity(gravity: Int) {
        val params = layoutParams as FrameLayout.LayoutParams
        params.gravity = gravity
        layoutParams = params
    }

    companion object {
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
