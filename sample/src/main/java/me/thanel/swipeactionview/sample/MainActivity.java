package me.thanel.swipeactionview.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

import org.jetbrains.annotations.NotNull;

import me.thanel.swipeactionview.SwipeActionView;
import me.thanel.swipeactionview.SwipeGestureListener;
import me.thanel.swipeactionview.animation.ScalableIconAnimator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        final View icon = findViewById(R.id.icon);

        SwipeActionView swipeActionView = (SwipeActionView) findViewById(R.id.swipe_action_view);
        swipeActionView.setRightSwipeAnimator(new ScalableIconAnimator());
        swipeActionView.setSwipeGestureListener(new SwipeGestureListener() {
            @Override
            public boolean onSwipedLeft(@NotNull SwipeActionView swipeActionView) {
                return false;
            }

            @Override
            public boolean onSwipedRight(@NotNull final SwipeActionView swipeActionView) {
                icon.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);

                swipeActionView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        icon.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        swipeActionView.reset();
                    }
                }, 2000);
                return false;
            }
        });
    }
}
