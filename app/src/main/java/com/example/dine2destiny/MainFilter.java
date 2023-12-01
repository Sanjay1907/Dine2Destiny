package com.example.dine2destiny;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

public class MainFilter extends AppCompatActivity {
    private static final String TAG = "Mainfilter";
    private int initialDistance = 1;
    private int maxDistance = 10;
    private TextView textViewSelectedDistance;
    private RadioGroup foodTypeRadioGroup;
    private RadioButton vegRadioButton;
    private RadioButton nonVegRadioButton;
    private RadioButton both;
    private String selectedFoodType = "All";
    private String selectedCategory = "All";
    private Button clrbtn, nxtbtn, backbtn, incrementButton, decrementButton;
    private Switch purevegSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_filter);
        ArrayList<String> followedCreators = getIntent().getStringArrayListExtra("followedCreators");
        textViewSelectedDistance = findViewById(R.id.textViewSelectedDistance);
        foodTypeRadioGroup = findViewById(R.id.foodTypeRadioGroup);
        vegRadioButton = findViewById(R.id.vegRadioButton);
        nonVegRadioButton = findViewById(R.id.nonVegRadioButton);
        both = findViewById(R.id.both);
        clrbtn = findViewById(R.id.clrbtn);
        purevegSwitch = findViewById(R.id.purevegButton);
        nxtbtn = findViewById(R.id.nxtbtn);
        backbtn = findViewById(R.id.btnback);
        incrementButton = findViewById(R.id.incrementbtn);
        decrementButton = findViewById(R.id.decrementbtn);
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
        incrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseDistance();
            }
        });
        decrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decreaseDistance();
            }
        });
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
        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainFilter.this, FavCreatorfilter.class);
                startActivity(intent);
            }
        });
        clrbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                foodTypeRadioGroup.clearCheck();
                initialDistance = 1;
                textViewSelectedDistance.setText("1 km");
                purevegSwitch.setChecked(false);
            }
        });
    }
    private void increaseDistance() {
        if (initialDistance < maxDistance) {
            initialDistance++;
            textViewSelectedDistance.setText(initialDistance + " km");
            Log.i(TAG, "increaseDistance: Selected Distance: " + initialDistance + " km");
        } else {
            Log.i(TAG, "increaseDistance: Maximum distance reached");
        }
    }

    private void decreaseDistance() {
        if (initialDistance > 1) {
            initialDistance--;
            textViewSelectedDistance.setText(initialDistance + " km");
            Log.i(TAG, "decreaseDistance: Selected Distance: " + initialDistance + " km");
        } else {
            Log.i(TAG, "decreaseDistance: Minimum distance reached");
        }
    }
}