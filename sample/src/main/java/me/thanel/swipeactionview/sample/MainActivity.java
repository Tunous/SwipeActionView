/*
 * Copyright © 2016-2018 Łukasz Rutkowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.thanel.swipeactionview.sample;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import me.thanel.swipeactionview.SwipeActionView;
import me.thanel.swipeactionview.SwipeDirection;
import me.thanel.swipeactionview.SwipeGestureListener;

public class MainActivity extends AppCompatActivity {

    private SwipeActionView swipeCustomLayout;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwipeGestureListener swipeGestureListener = new SwipeGestureListener() {
            @Override
            public boolean onSwipedHalfwayRight(@NonNull SwipeActionView swipeActionView) {
                showToast(true, true);
                return true;
            }

            @Override
            public boolean onSwipedHalfwayLeft(@NonNull SwipeActionView swipeActionView) {
                showToast(false, true);
                return false;
            }

            @Override
            public void onSwipeRightComplete(SwipeActionView swipeActionView) {
                // do nothing
            }

            @Override
            public void onSwipeLeftComplete(SwipeActionView swipeActionView) {
                // do nothing
            }

            @Override
            public boolean onSwipedLeft(@NonNull SwipeActionView swipeActionView) {
                showToast(false, false);
                return true;
            }

            @Override
            public boolean onSwipedRight(@NonNull SwipeActionView swipeActionView) {
                showToast(true, false);
                return true;
            }
        };

        SwipeActionView swipeRight = findViewById(R.id.swipe_right);
        swipeRight.setSwipeGestureListener(swipeGestureListener);

        SwipeActionView swipeLeft = findViewById(R.id.swipe_left);
        swipeLeft.setSwipeGestureListener(swipeGestureListener);

        SwipeActionView swipeBoth = findViewById(R.id.swipe_both);
        swipeBoth.setActivationDistanceRatio(0.5f);
        swipeBoth.setSwipeGestureListener(swipeGestureListener);

        SwipeActionView swipeWithRipples = findViewById(R.id.swipe_with_ripples);
        swipeWithRipples.setSwipeGestureListener(swipeGestureListener);

        SwipeActionView swipeCardView = findViewById(R.id.swipe_card_view);
        swipeCardView.setSwipeGestureListener(swipeGestureListener);

        SwipeGestureListener delayedSwipeGestureListener = new SwipeGestureListener() {
            @Override
            public boolean onSwipedHalfwayRight(@NonNull SwipeActionView swipeActionView) {
                return true;
            }

            @Override
            public boolean onSwipedHalfwayLeft(@NonNull SwipeActionView swipeActionView) {
                return true;
            }

            @Override
            public void onSwipeRightComplete(SwipeActionView swipeActionView) {
                //this won't be called since onSwipedRight returns false
            }

            @Override
            public void onSwipeLeftComplete(SwipeActionView swipeActionView) {
                //this won't be called since onSwipedLeft returns false
            }

            @Override
            public boolean onSwipedLeft(@NonNull SwipeActionView swipeActionView) {
                swipeActionView.animateToOriginalPosition(1000);
                return false;
            }

            @Override
            public boolean onSwipedRight(@NonNull SwipeActionView swipeActionView) {
                swipeActionView.animateToOriginalPosition(500);
                return false;
            }
        };

        SwipeActionView swipeDelayed = findViewById(R.id.swipe_delayed);
        swipeDelayed.setSwipeGestureListener(delayedSwipeGestureListener);

        swipeCustomLayout = findViewById(R.id.swipe_layout);
        swipeCustomLayout.setSwipeGestureListener(swipeGestureListener);



        SwipeGestureListener completeGestureListener = new SwipeGestureListener() {
            @Override
            public boolean onSwipedHalfwayLeft(@NonNull SwipeActionView swipeActionView) {
                return true;
            }

            @Override
            public boolean onSwipedHalfwayRight(@NonNull SwipeActionView swipeActionView) {
                return true;
            }

            @Override
            public void onSwipeRightComplete(SwipeActionView swipeActionView) {
                Toast.makeText(MainActivity.this, R.string.swipe_right_complete, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSwipeLeftComplete(SwipeActionView swipeActionView) {
                Toast.makeText(MainActivity.this, R.string.swipe_left_complete, Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean onSwipedLeft(@NonNull SwipeActionView swipeActionView) {
                //do nothing
                return true;
            }

            @Override
            public boolean onSwipedRight(@NonNull SwipeActionView swipeActionView) {
                //do nothing
                return true;
            }
        };

        SwipeActionView swipeComplete = findViewById(R.id.swipe_complete);
        swipeComplete.setSwipeGestureListener(completeGestureListener);
    }

    private void showToast(Boolean swipedRight, Boolean wasHalfway) {
        int resId = swipedRight ? R.string.swiped_right : R.string.swiped_left;
        String text = getString(resId);
        if(wasHalfway) {
            text+= getString(R.string.swiped_halfway);
        }

        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void swipeLeft(View view) {
        swipeCustomLayout.animateInDirection(SwipeDirection.Left, true);
        swipeCustomLayout.setActivationDistanceRatio(0.2f);
    }

    public void swipeRight(View view) {
        swipeCustomLayout.animateInDirection(SwipeDirection.Right, true);
        swipeCustomLayout.setActivationDistanceRatio(0.8f);
    }
}
