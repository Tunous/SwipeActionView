package me.thanel.swipeactionview.animation

import android.view.View

interface SwipeActionViewBackgroundAnimator {
    fun onUpdateSwipeProgress(view: View, progress: Float, minActivationProgress: Float)

    fun onActivate()
}
