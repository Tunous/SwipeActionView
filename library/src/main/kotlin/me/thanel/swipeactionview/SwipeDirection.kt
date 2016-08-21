package me.thanel.swipeactionview

enum class SwipeDirection {
    Left,
    Right;

    internal val internalId: Int
        get() = when (this) {
            SwipeDirection.Left -> 1
            SwipeDirection.Right -> 2
        }
}
