package me.thanel.swipeactionview

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class MultiSwipeActionView : LinearLayout {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
}