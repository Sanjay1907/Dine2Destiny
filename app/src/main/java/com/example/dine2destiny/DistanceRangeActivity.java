package com.example.dine2destiny;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
public class DistanceRangeActivity extends AppCompatActivity {
    private static final String TAG = "DistanceRangeActivity";
    private SeekBar seekBarDistance;
    private TextView textViewSelectedDistance;
    private Button btnApplyDistance, backbtn;
    private int initialDistance = 1;
    private RadioGroup foodTypeRadioGroup;
    private RadioButton vegRadioButton;
    private RadioButton nonVegRadioButton;
    private RadioButton both;
    private String selectedFoodType="Any";
    private String selectedCategory="Any";
    private RadioGroup ratingRadioGroup;
    private RadioButton rating4PlusRadioButton;
    private RadioButton rating3PlusRadioButton;
    private RadioButton rating2PlusRadioButton;
    private RadioButton rating1PlusRadioButton;
    private int selectedRating = 0;
    private Button clrbtn;
    private RadioButton purevegButton;
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> autoCompleteAdapter;
    private List<String> recommendationNames = new ArrayList<>();
    private DatabaseReference databaseReference;
    private TextView selectedFoodItemsTextView;
    private int foodItemCounter = 1;
    private Set<String> selectedFoodSet = new HashSet<>();
    private List<String> selectedFoodItemsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance_range);
        databaseReference = FirebaseDatabase.getInstance().getReference("Creators");

        // Initialize AutoCompleteTextView
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        autoCompleteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, recommendationNames);
        autoCompleteTextView.setAdapter(autoCompleteAdapter);
        autoCompleteTextView.setThreshold(1); // Minimum characters to trigger suggestions

        // Call this to populate recommendation names
        populateRecommendationNames();

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
        both = findViewById(R.id.both);
        clrbtn = findViewById(R.id.clrbtn);
        purevegButton = findViewById(R.id.purevegButton);
        selectedFoodItemsTextView = findViewById(R.id.selectedFoodItemsTextView);
        selectedFoodItemsTextView.setText("Selected Food Items:");

        // Initialize initialDistance based on the user's previous selection or a default value
        initialDistance = getIntent().getIntExtra("selectedDistance", 1);

        // Set the SeekBar's progress to match the initialDistance
        seekBarDistance.setProgress(initialDistance - 1); // Subtract 1 to match the SeekBar's 0-based progress
        textViewSelectedDistance.setText(initialDistance + " km");
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedRecommendation = autoCompleteAdapter.getItem(position);

            // Handle the selected recommendation as needed (e.g., display it in your app)
            // You can perform a Firebase query based on the selected recommendation if needed.
            Log.i(TAG, "Selected Recommendation: " + selectedRecommendation);

            // Add the selected food item to the TextView
            addFoodItemToTextView(selectedRecommendation);
        });

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
                } else if (checkedId == R.id.both){
                    selectedFoodType = "Both";
                } else{
                    selectedFoodType = "Any";
                }
                Log.i(TAG, "onCheckedChanged: Selected Food Type: " + selectedFoodType);
            }
        });
        purevegButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    selectedCategory = "Veg";
                } else{
                    selectedCategory = "Any";
                }
                Log.i(TAG, "onCheckedChanged: Selected Category: " + selectedCategory);
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
                Log.i(TAG, "onCheckedChanged: Selected Rating: " + selectedRating);
            }
        });
        btnApplyDistance.setOnClickListener(v -> {
            int selectedDistance = seekBarDistance.getProgress() + 1; // Add 1 to match the range 1-10 km

            // Log the selected distance
            Log.i(TAG, "Selected Distance: " + selectedDistance + " km");
            Log.i(TAG, "Selected FoodType: " + selectedFoodType);
            Log.i(TAG, "Selected Rating: " + selectedRating);
            Log.i(TAG, "Selected Category: " + selectedCategory);

            // Log the selected food items
            for (String foodItem : selectedFoodItemsList) {
                Log.i(TAG, "Selected Food Item: " + foodItem);
            }

            // Create an intent to start the MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("selectedDistance", selectedDistance);
            intent.putExtra("selectedFoodCategory", selectedFoodType);
            intent.putExtra("selectedRating", selectedRating);
            intent.putExtra("selectedCategory", selectedCategory);

            // Pass the selected food items list to the MainActivity
            intent.putStringArrayListExtra("selectedFoodItems", new ArrayList<>(selectedFoodItemsList));

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
        clrbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                foodTypeRadioGroup.clearCheck();
                ratingRadioGroup.clearCheck();
                initialDistance = 1;
                seekBarDistance.setProgress(0);
                textViewSelectedDistance.setText("1 km");
                purevegButton.setChecked(false);
                selectedFoodItemsTextView.setText("Selected Food Items:");
                selectedFoodItemsList.clear();
                foodItemCounter = 1;
            }
        });
    }
    private void populateRecommendationNames() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Set<String> uniqueHashtags = new HashSet<>();

                for (DataSnapshot creatorSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot recommendationsSnapshot = creatorSnapshot.child("recommendation");
                    for (DataSnapshot recommendationSnapshot : recommendationsSnapshot.getChildren()) {
                        String hashtagString = recommendationSnapshot.child("hashtag").getValue(String.class);

                        if (hashtagString != null) {
                            // Split the hashtags using various delimiters: space, comma, or no space
                            String[] hashtags = hashtagString.split("\\s+|,|(?<=\\S)(?=[#])");

                            for (String hashtag : hashtags) {
                                // Remove leading '#' and any leading/trailing whitespaces
                                String cleanHashtag = hashtag.trim().replaceFirst("^#", "").toLowerCase();

                                if (!cleanHashtag.isEmpty()) {
                                    uniqueHashtags.add(cleanHashtag);
                                }
                            }
                        }
                    }
                }

                // Update the adapter with the unique hashtags
                recommendationNames.clear();
                recommendationNames.addAll(uniqueHashtags);

                // Sort the hashtags for a better user experience
                List<String> sortedHashtags = new ArrayList<>(recommendationNames);
                Collections.sort(sortedHashtags);

                // Update the AutoCompleteTextView adapter
                autoCompleteAdapter.clear();
                autoCompleteAdapter.addAll(sortedHashtags);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database Error: " + databaseError.getMessage());
            }
        });
    }
    private void addFoodItemToTextView(String foodItem) {
        // Check for duplicates with case sensitivity
        if (selectedFoodSet.contains(foodItem)) {
            // Display a toast message indicating that the food item is already added
            Toast.makeText(this, "Food item '" + foodItem + "' is already added.", Toast.LENGTH_SHORT).show();
        } else {
            String currentText = selectedFoodItemsTextView.getText().toString();
            String newText = currentText + "\n" + foodItemCounter + ". " + foodItem;
            selectedFoodItemsTextView.setText(newText);

            // Clear the AutoCompleteTextView
            autoCompleteTextView.setText("");

            // Add the food item to the list to pass it later
            selectedFoodItemsList.add(foodItem);

            // Add the food item to the set to prevent duplicates
            selectedFoodSet.add(foodItem);

            // Increment the counter for the next food item
            foodItemCounter++;
        }
    }
}