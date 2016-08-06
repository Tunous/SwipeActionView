@file:Suppress("unused")

package me.thanel.swipeactionview

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout

/**
 * View that allows users to perform various actions by swiping it to the sides.
 */
class SwipeActionView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    private val swipeGestureDetector: SwipeGestureDetector = SwipeGestureDetector(this)
    private val iconLeftResource: Int
    private val iconRightResource: Int
    private val containerBackground: Drawable?
    private var enabledDirections: Int
    private var disabledEdges: Int

    /**
     * The container for custom views added to this [SwipeActionView].
     */
    internal lateinit var container: LinearLayout
        private set

    /**
     * The icon that appears on the left side of the view.
     */
    lateinit var iconLeft: ImageView
        private set

    /**
     * The icon that appears on the right side of the view.
     */
    lateinit var iconRight: ImageView
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

        iconLeftResource = typedArray.getResourceId(R.styleable.SwipeActionView_iconLeft, 0)
        iconRightResource = typedArray.getResourceId(R.styleable.SwipeActionView_iconRight, 0)

        enabledDirections = typedArray.getInt(R.styleable.SwipeActionView_swipeDirections, DIRECTION_LEFT or DIRECTION_RIGHT)
        disabledEdges = typedArray.getInt(R.styleable.SwipeActionView_disabledEdges, 0)

        containerBackground = typedArray.getDrawable(R.styleable.SwipeActionView_containerBackground)

        typedArray.recycle()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return swipeGestureDetector.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return swipeGestureDetector.onTouchEvent(event)
    }

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
    fun hasEnabledDirection(direction: Int): Boolean =
            (enabledDirections and direction) == direction

    /**
     * Tells whether edge for the specified [edge] is enabled.
     */
    fun hasEnabledEdge(edge: Int): Boolean =
            (disabledEdges and edge) != edge

    override fun onFinishInflate() {
        val children = detachChildren()

        val view = LayoutInflater.from(context).inflate(R.layout.swipe_view, this, true)
        container = view.findViewById(R.id.container) as LinearLayout
        iconLeft = view.findViewById(R.id.swipe_icon_left) as ImageView
        iconRight = view.findViewById(R.id.swipe_icon_right) as ImageView

        iconLeft.setImageResource(iconLeftResource)
        iconRight.setImageResource(iconRightResource)

        if (containerBackground != null) {
            container.background = containerBackground
        }

        addChildrenToContainer(children)

        super.onFinishInflate()
    }

    private fun addChildrenToContainer(children: Collection<View>) {
        children.forEach {
            container.addView(it)
        }
    }

    private fun detachChildren(): Collection<View> {
        val children = (0..childCount - 1)
                .map { getChildAt(it) }
        detachAllViewsFromParent()
        return children
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
