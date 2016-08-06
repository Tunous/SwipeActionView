package me.thanel.swipeactionview.animation

import android.view.View
import me.thanel.swipeactionview.utils.setScale

class ScalableIconAnimator : SwipeActionViewBackgroundAnimator {
    override fun onUpdateSwipeProgress(view: View, progress: Float, minActivationProgress: Float) {
        var scale = 0.65f

        if (progress > minActivationProgress) {
            val xOverActivation = progress - minActivationProgress
            scale += Math.min(xOverActivation / 0.4f, 1f - scale)
        }

        view.setScale(scale)
    }

    override fun onActivate() {
    }
}
