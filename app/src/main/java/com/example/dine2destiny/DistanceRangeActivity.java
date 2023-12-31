package com.example.dine2destiny;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.ads.AdRequest;

public class DistanceRangeActivity extends AppCompatActivity {
    private static final String TAG = "DistanceRangeActivity";
    private Button btnApplyDistance, backbtn;
    private RatingBar ratingBar;
    private int selectedRating = 0;
    private Button clrbtn;
    private AutoCompleteTextView autoCompleteTextView;
    private ArrayAdapter<String> autoCompleteAdapter;
    private List<String> recommendationNames = new ArrayList<>();
    private DatabaseReference databaseReference;
    private TextView selectedFoodItemsTextView;
    private int foodItemCounter = 1;
    private Set<String> selectedFoodSet = new HashSet<>();
    private List<String> selectedFoodItemsList = new ArrayList<>();
    private String logFilePath;
    private int selectedDistance;
    private String  selectedSpecialType, selectedFoodType;
    private InterstitialAd mInterstitialAd;
    private ArrayList<String> followedCreators;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance_range);
        databaseReference = FirebaseDatabase.getInstance().getReference("Creators");
        selectedDistance = getIntent().getIntExtra("selectedDistance", 1);
        selectedSpecialType = getIntent().getStringExtra("selectedSpecialType");
        if (selectedSpecialType == null) {
            selectedSpecialType = "All";
        }
        selectedFoodType = getIntent().getStringExtra("selectedFoodCategory");
        if (selectedFoodType == null){
            selectedFoodType = "All";
        }
        followedCreators = getIntent().getStringArrayListExtra("followedCreators");
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        autoCompleteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, recommendationNames);
        autoCompleteTextView.setAdapter(autoCompleteAdapter);
        autoCompleteTextView.setThreshold(1); // Minimum characters to trigger suggestions

        populateRecommendationNames();
        btnApplyDistance = findViewById(R.id.btnApplyDistance);
        backbtn = findViewById(R.id.btnback);
        clrbtn = findViewById(R.id.clrbtn);
        ratingBar = findViewById(R.id.ratingBar);
        selectedFoodItemsTextView = findViewById(R.id.selectedFoodItemsTextView);
        selectedFoodItemsTextView.setText("Selected Food Items:");
        dialog = new Dialog(DistanceRangeActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_wait1);
        dialog.setCanceledOnTouchOutside(false);
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedRecommendation = autoCompleteAdapter.getItem(position);
            Log.i(TAG, "Selected Recommendation: " + selectedRecommendation);

            // Add the selected food item to the TextView
            addFoodItemToTextView(selectedRecommendation);
        });
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                // Convert the float rating to an integer
                selectedRating = (int) rating;

                // Log the selected rating
                Log.i(TAG, "onRatingChanged: Selected Rating: " + selectedRating);
            }
        });

        btnApplyDistance.setOnClickListener(v -> {
            dialog.show();
            AdRequest adRequest = new AdRequest.Builder().build();

            InterstitialAd.load(this,"ca-app-pub-6500906048555901/1675315806", adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            // The mInterstitialAd reference will be null until an ad is loaded.
                            dialog.dismiss();
                            mInterstitialAd = interstitialAd;
                            Log.i(TAG, "onAdLoaded");

                            if (mInterstitialAd != null) {
                                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        navigateToNextPage();
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        navigateToNextPage();
                                    }
                                });
                                mInterstitialAd.show(DistanceRangeActivity.this);
                            } else {
                                navigateToNextPage();
                            }
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Handle the error
                            dialog.dismiss();
                            Log.d(TAG, loadAdError.toString());
                            mInterstitialAd = null;
                            navigateToNextPage();
                        }
                    });
        });
        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DistanceRangeActivity.this, MainFilter.class);
                startActivity(intent);
            }
        });
        clrbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ratingBar.setRating(0);
                selectedFoodItemsTextView.setText("Selected Food Items:");
                selectedFoodItemsList.clear();
                foodItemCounter = 1;
            }
        });
        Button addFoodItemButton = findViewById(R.id.addbtn);
        addFoodItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String selectedRecommendation = autoCompleteTextView.getText().toString().trim();

                if (!selectedRecommendation.isEmpty()) {
                    // Add the selected food item to the TextView
                    addFoodItemToTextView(selectedRecommendation);
                }
            }
        });
    }
    private void navigateToNextPage() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());

        // Initialize the filter message
        StringBuilder filterMessage = new StringBuilder("Filters selected by the user:");

        // Calculate and append the total number of filters selected
        int totalFiltersSelected = 0;
        // Log the list of followed creators if it's not empty
        if (followedCreators != null && !followedCreators.isEmpty()) {
            filterMessage.append("\nFollowed Creators:");
            for (String creator : followedCreators) {
                filterMessage.append("\n- ").append(creator);
            }
            totalFiltersSelected++;
        }

        // Log the selected Distance if it's greater than 0
        if (selectedDistance > 0) {
            filterMessage.append("\nSelected Distance: ").append(selectedDistance).append(" km");
            totalFiltersSelected++;
        }

        // Log the selected FoodType if it's not "All"
        if (!"All".equals(selectedSpecialType)) {
            filterMessage.append("\nSelected FoodType: ").append(selectedSpecialType);
            totalFiltersSelected++;
        } else {
            filterMessage.append("\nSelected FoodType: NOT SELECTED");
        }


        // Log the selected Rating if it's greater than 0
        if (selectedRating > 0) {
            filterMessage.append("\nSelected Rating: ").append(selectedRating);
            totalFiltersSelected++;
        } else {
            filterMessage.append("\nSelected Rating: NOT SELECTED");
        }

        // Log the selected Category if it's not "All"
        if (!"All".equals(selectedFoodType)) {
            filterMessage.append("\nSelected Category: ").append(selectedFoodType);
            totalFiltersSelected++;
        } else {
            filterMessage.append("\nSelected Category: NOT SELECTED");
        }

        // Log the selected food items if any
        if (!selectedFoodItemsList.isEmpty()) {
            filterMessage.append("\nSelected Food Items:");
            for (String foodItem : selectedFoodItemsList) {
                filterMessage.append("\n- ").append(foodItem);
            }
            totalFiltersSelected++;
        } else {
            filterMessage.append("\nSelected Interested foods: NOT SELECTED");
        }

        filterMessage.append("\n\nTotal Filters Selected: ").append(totalFiltersSelected);

        Log.i(TAG, "Timestamp when Apply button was clicked: " + timestamp);
        // Log the filter message
        Log.i(TAG, filterMessage.toString());

        if (createLogTextFile("Timestamp when Search operation started: " + timestamp + "\n" +
                filterMessage.toString())) {
            // Log file created successfully, add it to the intent
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("selectedDistance", selectedDistance);
            intent.putExtra("selectedFoodCategory", selectedSpecialType);
            intent.putExtra("selectedRating", selectedRating);
            intent.putExtra("selectedCategory", selectedFoodType);
            intent.putExtra("timestamp", timestamp);
            intent.putExtra("logFilePath", logFilePath); // Add the log file path

            // Pass the selected food items list to the MainActivity
            intent.putStringArrayListExtra("selectedFoodItems", new ArrayList<>(selectedFoodItemsList));

            startActivity(intent);
            finish();
        }
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
                            String[] hashtags = hashtagString.split("\\s+|,|(?<=\\S)(?=[#])");

                            for (String hashtag : hashtags) {
                                String cleanHashtag = hashtag.trim().replaceFirst("^#", "").toLowerCase();

                                if (!cleanHashtag.isEmpty()) {
                                    uniqueHashtags.add(cleanHashtag);
                                }
                            }
                        }
                    }
                }
                autoCompleteAdapter = new ArrayAdapter<String>(DistanceRangeActivity.this, android.R.layout.simple_dropdown_item_1line) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        TextView textView = (TextView) super.getView(position, convertView, parent);

                        String searchQuery = autoCompleteTextView.getText().toString().toLowerCase();
                        String item = getItem(position).toLowerCase();
                        SpannableStringBuilder builder = new SpannableStringBuilder(item);
                        if (item.contains(searchQuery)) {
                            int startPos = item.indexOf(searchQuery);
                            int endPos = startPos + searchQuery.length();
                            builder.setSpan(new ForegroundColorSpan(Color.RED), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        textView.setText(builder);
                        return textView;
                    }
                };

                List<String> sortedHashtags = new ArrayList<>(uniqueHashtags);
                Collections.sort(sortedHashtags);

                autoCompleteAdapter.addAll(sortedHashtags);
                autoCompleteTextView.setAdapter(autoCompleteAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database Error: " + databaseError.getMessage());
            }
        });
    }


    private boolean createLogTextFile(String logMessage) {
        try {
            // Create a unique log file with a timestamp in the filename
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault());
            String formattedDate = dateFormat.format(new Date());
            String logFileName = "app_log_" + formattedDate + ".txt";

            // Get the directory for saving log files
            File appDir = new File(Environment.getExternalStorageDirectory(), "Android/media/com.example.dine2destiny");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }

            File logFile = new File(appDir, logFileName);
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
            writer.write(logMessage + "\n");
            writer.close();

            // Store the log file path
            logFilePath = logFile.getAbsolutePath();

            // Display a toast message indicating the log file creation
            //Toast.makeText(this, "Log file created: " + logFileName, Toast.LENGTH_LONG).show();
            return true; // Return true to indicate success
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating log file", Toast.LENGTH_LONG).show();
            logFilePath = null; // Set logFilePath to null in case of an error
            return false; // Return false to indicate failure
        }
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