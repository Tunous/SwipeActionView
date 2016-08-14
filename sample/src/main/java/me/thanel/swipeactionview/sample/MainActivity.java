package me.thanel.swipeactionview.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import me.thanel.swipeactionview.SwipeActionView;
import me.thanel.swipeactionview.animation.ScalableIconAnimator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwipeActionView bothDirections = (SwipeActionView) findViewById(R.id.both_directions);
        bothDirections.setLeftSwipeAnimator(new ScalableIconAnimator());
    }
}
