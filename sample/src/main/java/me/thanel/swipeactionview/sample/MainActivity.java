/*
 * Copyright © 2016-2017 Łukasz Rutkowski
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
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import me.thanel.swipeactionview.SwipeActionView;
import me.thanel.swipeactionview.SwipeGestureListener;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        SwipeActionView swipeRight = (SwipeActionView) findViewById(R.id.swipe_right);
        swipeRight.setSwipeGestureListener(swipeGestureListener);

        SwipeActionView swipeLeft = (SwipeActionView) findViewById(R.id.swipe_left);
        swipeLeft.setSwipeGestureListener(swipeGestureListener);

        SwipeActionView swipeBoth = (SwipeActionView) findViewById(R.id.swipe_both);
        swipeBoth.setSwipeGestureListener(swipeGestureListener);

        SwipeActionView swipeWithRipples = (SwipeActionView) findViewById(R.id.swipe_with_ripples);
        swipeWithRipples.setSwipeGestureListener(swipeGestureListener);

        SwipeActionView swipeCardView = (SwipeActionView) findViewById(R.id.swipe_card_view);
        swipeCardView.setSwipeGestureListener(swipeGestureListener);

        SwipeGestureListener delayedSwipeGestureListener = new SwipeGestureListener() {
            @Override
            public boolean onSwipedLeft(@NotNull SwipeActionView swipeActionView) {
                swipeActionView.moveToOriginalPosition(1000);
                return false;
            }

            @Override
            public boolean onSwipedRight(@NotNull SwipeActionView swipeActionView) {
                swipeActionView.moveToOriginalPosition(500);
                return false;
            }
        };

        SwipeActionView swipeDelayed = (SwipeActionView) findViewById(R.id.swipe_delayed);
        swipeDelayed.setSwipeGestureListener(delayedSwipeGestureListener);

        SwipeActionView swipeCustomLayout = (SwipeActionView) findViewById(R.id.swipe_layout);
        swipeCustomLayout.setSwipeGestureListener(swipeGestureListener);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, R.string.clicked, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showToast(Boolean swipedRight) {
        int resId = swipedRight ? R.string.swiped_right : R.string.swiped_left;

        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }
}
