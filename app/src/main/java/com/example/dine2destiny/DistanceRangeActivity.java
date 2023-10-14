package com.example.dine2destiny;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DistanceRangeActivity extends AppCompatActivity {
    private static final String TAG = "DistanceRangeActivity";
    private SeekBar seekBarDistance;
    private TextView textViewSelectedDistance;
    private Button btnApplyDistance, backbtn;
    private int initialDistance = 1;
    private RadioGroup foodTypeRadioGroup;
    private RadioButton vegRadioButton;
    private RadioButton nonVegRadioButton;
    private String selectedFoodType = "Any";
    private RadioGroup ratingRadioGroup;
    private RadioButton rating4PlusRadioButton;
    private RadioButton rating3PlusRadioButton;
    private RadioButton rating2PlusRadioButton;
    private RadioButton rating1PlusRadioButton;
    private int selectedRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance_range);

        seekBarDistance = findViewById(R.id.seekBarDistance);
        textViewSelectedDistance = findViewById(R.id.textViewSelectedDistance);
        btnApplyDistance = findViewById(R.id.btnApplyDistance);
        backbtn = findViewById(R.id.btnback);
        ratingRadioGroup = findViewById(R.id.ratingRadioGroup);
        rating4PlusRadioButton = findViewById(R.id.rating4PlusRadioButton);
        rating3PlusRadioButton = findViewById(R.id.rating3PlusRadioButton);
        rating2PlusRadioButton = findViewById(R.id.rating2PlusRadioButton);
        rating1PlusRadioButton = findViewById(R.id.rating1PlusRadioButton);
        foodTypeRadioGroup = findViewById(R.id.foodTypeRadioGroup);
        vegRadioButton = findViewById(R.id.vegRadioButton);
        nonVegRadioButton = findViewById(R.id.nonVegRadioButton);

        // Initialize initialDistance based on the user's previous selection or a default value
        initialDistance = getIntent().getIntExtra("selectedDistance", 1);

        // Set the SeekBar's progress to match the initialDistance
        seekBarDistance.setProgress(initialDistance - 1); // Subtract 1 to match the SeekBar's 0-based progress
        textViewSelectedDistance.setText(initialDistance + " km");

        seekBarDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                initialDistance = progress+1;
                int roundedDistance = (int) Math.floor(initialDistance);
                textViewSelectedDistance.setText(roundedDistance + " km");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        foodTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.vegRadioButton) {
                    selectedFoodType = "Veg";
                } else if (checkedId == R.id.nonVegRadioButton) {
                    selectedFoodType = "Non-Veg";
                } else {
                    selectedFoodType = "Any"; // Default if neither is selected
                }
                Log.d(TAG, "onCheckedChanged: Selected Food Type: " + selectedFoodType);
            }
        });
        ratingRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rating4PlusRadioButton){
                    selectedRating = 4;
                } else if (checkedId == R.id.rating3PlusRadioButton){
                    selectedRating = 3;
                } else if (checkedId == R.id.rating2PlusRadioButton){
                    selectedRating = 2;
                } else if (checkedId == R.id.rating1PlusRadioButton){
                    selectedRating = 1;
                } else{
                    selectedRating = 0;
                }
                Log.d(TAG, "onCheckedChanged: Selected Rating: " + selectedRating);
            }
        });
        btnApplyDistance.setOnClickListener(v -> {
            int selectedDistance = seekBarDistance.getProgress() + 1; // Add 1 to match the range 1-10 km

            // Log the selected distance
            Log.d(TAG, "Selected Distance: " + selectedDistance + " km");
            Log.d(TAG, "Selected FoodType: " + selectedFoodType);
            Log.d(TAG, "Selected Rating: " + selectedRating);

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("selectedDistance", selectedDistance);
            intent.putExtra("selectedFoodCategory", selectedFoodType);
            intent.putExtra("selectedRating", selectedRating);
            startActivity(intent);
            finish();
        });
        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DistanceRangeActivity.this, FavCreatorfilter.class);
                startActivity(intent);
            }
        });

    }
}
