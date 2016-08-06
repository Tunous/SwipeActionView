@file:Suppress("unused")

package me.thanel.swipeactionview

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

/**
 * View that allows users to perform various actions by swiping it to the sides.
 */
class SwipeActionView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private val swipeGestureDetector: SwipeGestureDetector = SwipeGestureDetector(this)
    private var enabledDirections: Int
    private var disabledEdges: Int

    internal lateinit var container: View
        private set

    var leftBackground: View? = null
        private set

    var rightBackground: View? = null
        private set

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

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeActionView)

        enabledDirections = typedArray.getInt(R.styleable.SwipeActionView_swipeDirections, DIRECTION_LEFT or DIRECTION_RIGHT)
        disabledEdges = typedArray.getInt(R.styleable.SwipeActionView_disabledEdges, 0)

        typedArray.recycle()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent) =
            swipeGestureDetector.onInterceptTouchEvent(ev)

    override fun onTouchEvent(event: MotionEvent) =
            swipeGestureDetector.onTouchEvent(event)

    /**
     * Enable swiping in the specified [direction].
     */
    fun enableSwipeDirection(direction: Int) {
        enabledDirections = enabledDirections or direction
    }

    /**
     * Disable swiping in the specified [direction].
     */
    fun disableSwipeDirection(direction: Int) {
        enabledDirections = enabledDirections and direction.inv()
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
        val background = when (direction) {
            DIRECTION_LEFT -> rightBackground
            DIRECTION_RIGHT -> leftBackground

            else -> throw IllegalArgumentException("Unknown direction: $direction")
        }

        return background != null && (enabledDirections and direction) == direction
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
            leftBackground = getChildAt(0)

            if (childCount == 3) {
                rightBackground = getChildAt(1)

                val layoutParams = rightBackground?.layoutParams as FrameLayout.LayoutParams
                layoutParams.gravity = Gravity.END
                rightBackground?.layoutParams = layoutParams
            }
        }

        container = getChildAt(childCount - 1)
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
