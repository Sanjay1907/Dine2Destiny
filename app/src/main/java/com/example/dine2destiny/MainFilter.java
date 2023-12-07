package com.example.dine2destiny;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

public class MainFilter extends AppCompatActivity implements View.OnTouchListener {
    private static final String TAG = "Mainfilter";
    private int initialDistance = 1; // Changed initial distance to 1
    private int maxDistance = 10;
    private TextView textViewSelectedDistance, textViewPreviousDistance, textViewNextDistance;
    private RadioGroup foodTypeRadioGroup;
    private RadioButton vegRadioButton, nonVegRadioButton, both;
    private String selectedFoodType = "All";
    private String selectedCategory = "All";
    private Button clrbtn, nxtbtn;
    private Switch purevegSwitch;
    private float startX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_filter);
        ArrayList<String> followedCreators = getIntent().getStringArrayListExtra("followedCreators");

        foodTypeRadioGroup = findViewById(R.id.foodTypeRadioGroup);
        vegRadioButton = findViewById(R.id.vegRadioButton);
        nonVegRadioButton = findViewById(R.id.nonVegRadioButton);
        both = findViewById(R.id.both);
        clrbtn = findViewById(R.id.clrbtn);
        purevegSwitch = findViewById(R.id.purevegButton);
        nxtbtn = findViewById(R.id.nxtbtn);
        textViewSelectedDistance = findViewById(R.id.textViewSelectedDistance);
        textViewPreviousDistance = findViewById(R.id.textViewPreviousDistance);
        textViewNextDistance = findViewById(R.id.textViewNextDistance);
        setInteractionsEnabled(false);
        showOverlay();

        textViewSelectedDistance.setOnTouchListener(this);
        RelativeLayout distanceRelativeLayout = findViewById(R.id.disfilter);
        distanceRelativeLayout.setOnTouchListener(new View.OnTouchListener() {
            private float initialX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = event.getX();
                        break;
                    case MotionEvent.ACTION_UP:
                        float finalX = event.getX();
                        float deltaX = finalX - initialX;

                        if (deltaX > 100) { // Swiping from left to right
                            decreaseDistance();
                        } else if (deltaX < -100) { // Swiping from right to left
                            increaseDistance();
                        }
                        break;
                }
                return true;
            }
        });

        foodTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.vegRadioButton) {
                    selectedFoodType = "Veg";
                } else if (checkedId == R.id.nonVegRadioButton) {
                    selectedFoodType = "Non-Veg";
                } else if (checkedId == R.id.both) {
                    selectedFoodType = "Both";
                } else {
                    selectedFoodType = "All";
                }
                Log.i(TAG, "onCheckedChanged: Selected Food Type: " + selectedFoodType);
            }
        });

        purevegSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    selectedCategory = "Veg";
                } else {
                    selectedCategory = "All";
                }
                Log.i(TAG, "onCheckedChanged: Selected Category: " + selectedCategory);
            }
        });

        textViewSelectedDistance.setText(initialDistance + " km");
        textViewPreviousDistance.setText("");
        textViewNextDistance.setText(String.valueOf(initialDistance + 1));

        nxtbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an intent to start the Distance Range Activity
                Intent intent = new Intent(MainFilter.this, DistanceRangeActivity.class);

                // Put the selected data as extras in the intent
                intent.putExtra("selectedDistance", initialDistance);
                intent.putExtra("selectedSpecialType", selectedFoodType);
                intent.putExtra("selectedFoodCategory", selectedCategory);
                intent.putStringArrayListExtra("followedCreators", followedCreators);

                // Start the Distance Range Activity with the intent
                startActivity(intent);
            }
        });

        clrbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                foodTypeRadioGroup.clearCheck();
                initialDistance = 1;
                textViewSelectedDistance.setText("1 km");
                textViewPreviousDistance.setText("");
                textViewNextDistance.setText(String.valueOf(initialDistance + 1));
                purevegSwitch.setChecked(false);
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX(); // Store the initial touch position (X-axis)
                break;
            case MotionEvent.ACTION_UP:
                float endX = event.getX(); // Get the ending position of touch (X-axis)
                handleScrolling(startX, endX);
                break;
        }
        return true;
    }

    private void handleScrolling(float startX, float endX) {
        if (startX > endX) {
            increaseDistanceWithAnimation(); // Swipe right (increase number) with animation
        } else {
            decreaseDistanceWithAnimation(); // Swipe left (decrease number) with animation
        }
    }

    private void increaseDistanceWithAnimation() {
        if (initialDistance < maxDistance) {
            initialDistance++;
            animateDistanceChange(true);
            Log.i(TAG, "increaseDistance: Selected Distance: " + initialDistance + " km");
        } else {
            Log.i(TAG, "increaseDistance: Maximum distance reached");
        }
    }

    private void decreaseDistanceWithAnimation() {
        if (initialDistance > 1) {
            initialDistance--;
            animateDistanceChange(false);
            Log.i(TAG, "decreaseDistance: Selected Distance: " + initialDistance + " km");
        } else {
            Log.i(TAG, "decreaseDistance: Minimum distance reached");
        }
    }

    private void animateDistanceChange(final boolean isIncrease) {
        final float distanceToMove = isIncrease ? -textViewSelectedDistance.getWidth() : textViewSelectedDistance.getWidth();

        textViewSelectedDistance.animate()
                .translationXBy(distanceToMove)
                .setDuration(300) // Set the duration for animation in milliseconds
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        textViewSelectedDistance.setTranslationX(0); // Reset the text view position
                        updateTextViews();
                    }
                })
                .start();
    }

    private void updateTextViews() {
        textViewSelectedDistance.setText(String.valueOf(initialDistance) + " km");

        if (initialDistance > 1) {
            textViewPreviousDistance.setText(String.valueOf(initialDistance - 1));
        } else {
            textViewPreviousDistance.setText(""); // Clear text or set to a default value
        }

        if (initialDistance < maxDistance) {
            textViewNextDistance.setText(String.valueOf(initialDistance + 1));
        } else {
            textViewNextDistance.setText(""); // Clear text or set to a default value
        }
    }


    private void increaseDistance() {
        if (initialDistance < maxDistance) {
            initialDistance++;
            updateTextViews();
            Log.i(TAG, "increaseDistance: Selected Distance: " + initialDistance + " km");
        } else {
            Log.i(TAG, "increaseDistance: Maximum distance reached");
        }
    }

    private void decreaseDistance() {
        if (initialDistance > 1) {
            initialDistance--;
            updateTextViews();
            Log.i(TAG, "decreaseDistance: Selected Distance: " + initialDistance + " km");
        } else {
            Log.i(TAG, "decreaseDistance: Minimum distance reached");
        }
    }
    private void setInteractionsEnabled(boolean enabled) {
        foodTypeRadioGroup.setEnabled(enabled);
        vegRadioButton.setEnabled(enabled);
        nonVegRadioButton.setEnabled(enabled);
        both.setEnabled(enabled);
        clrbtn.setEnabled(enabled);
        purevegSwitch.setEnabled(enabled);
        nxtbtn.setEnabled(enabled);
        textViewSelectedDistance.setEnabled(enabled);
        textViewPreviousDistance.setEnabled(enabled);
        textViewNextDistance.setEnabled(enabled);
    }
    private void showOverlay() {
        final RelativeLayout overlayLayout = findViewById(R.id.overlayLayout);
        final ImageView arrowImageView = findViewById(R.id.arrowImageView);
        final Button skipbtn = findViewById(R.id.skipbtn);
        overlayLayout.setVisibility(View.VISIBLE);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_out);
        arrowImageView.setVisibility(View.VISIBLE);
        arrowImageView.startAnimation(animation);
        skipbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlayLayout.setVisibility(View.GONE);
                arrowImageView.clearAnimation();
                setInteractionsEnabled(true);
            }
        });


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (overlayLayout.getVisibility() == View.VISIBLE) {
                    overlayLayout.setVisibility(View.GONE);
                    arrowImageView.clearAnimation();
                    setInteractionsEnabled(true);
                }
            }
        }, 6000);
    }

}
