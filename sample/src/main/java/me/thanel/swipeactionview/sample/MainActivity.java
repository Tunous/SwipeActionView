package me.thanel.swipeactionview.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import me.thanel.swipeactionview.SwipeActionView;
import me.thanel.swipeactionview.SwipeDirection;
import me.thanel.swipeactionview.SwipeGestureListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwipeActionView swipeLeft = (SwipeActionView) findViewById(R.id.swipe_left);
        SwipeActionView swipeRight = (SwipeActionView) findViewById(R.id.swipe_right);
        SwipeActionView swipeBoth = (SwipeActionView) findViewById(R.id.swipe_both);
        SwipeActionView swipeToggle = (SwipeActionView) findViewById(R.id.swipe_to_toggle);

        SwipeGestureListener swipeGestureListener = new SwipeGestureListener() {
            @Override
            public boolean onSwipedLeft(@NotNull SwipeActionView swipeActionView) {
                showToast(false);
                return true;
            }

            @Override
            public boolean onSwipedRight(@NotNull SwipeActionView swipeActionView) {
                showToast(true);
                return true;
            }
        };

        swipeLeft.setSwipeGestureListener(swipeGestureListener);
        swipeRight.setSwipeGestureListener(swipeGestureListener);
        swipeBoth.setSwipeGestureListener(swipeGestureListener);

        swipeToggle.setSwipeGestureListener(new SwipeGestureListener() {
            @Override
            public boolean onSwipedLeft(@NotNull SwipeActionView swipeActionView) {
                showToast(false);
                return true;
            }

            @Override
            public boolean onSwipedRight(@NotNull SwipeActionView swipeActionView) {
                boolean enabled = !swipeActionView.hasEnabledDirection(SwipeDirection.Left);
                swipeActionView.setDirectionEnabled(SwipeDirection.Left, enabled);
                return true;
            }
        });
    }

    private void showToast(Boolean swipedRight) {
        int resId = swipedRight ? R.string.swiped_right : R.string.swiped_left;
        String text = getString(resId);

        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
