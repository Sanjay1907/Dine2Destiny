package com.example.dine2destiny;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.telephony.SmsManager;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.widget.SeekBar;
import android.widget.ToggleButton;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback{
    private static final String TAG = "MainActivity"; // Tag for logging
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private SupportMapFragment mapFragment;
    private DatabaseReference databaseReference;
    private boolean cameraMoved = false;
    private LinearLayout locationDetailsContainer;
    private LocationCallback locationCallback;
    private boolean locationDetailsLoaded = false; // Flag to control location details loading
    private LocationRequest locationRequest;
    private List<Marker> locationMarkers = new ArrayList<>();
    private Set<String> visitedPlaceIds = new HashSet<>();
    private Map<String, List<String>> locationCreatorsMap = new HashMap<>();
    private Button filter;
    private int selectedDistance;
    private int selectedRating;
    private String selectedFoodCategory;
    private String selectedCategory;
    private ArrayList<String> selectedFoodItems;
    private boolean filterDialogShown = false;
    private boolean recommendationMessageAdded = false;
    private boolean recommendationMessageloaded = false;
    private Button loadnxtbtn;
    private int currentRecommendationIndex = 0;
    private LatLng origin;
    private List<LatLng> destinations;
    private List<String> names;
    private List<String> creators;
    private List<String> imgUrls;
    private List<String> phoneNos;
    private List<String> verifications;
    private HashSet<String> addedNamesToLogFile = new HashSet<>();
    private AlertDialog phoneNumberDialog;
    private AlertDialog otpdialog;
    private AlertDialog nameDialog;
    private FloatingActionButton add, reportbug, logout;
    private boolean aBoolean = true;
    private RelativeLayout container;
    private boolean doubleBackToExitPressedOnce = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: Initializing MainActivity");
        checkUserName();
        checkUserPhoneNumber();
        showFilterDialog();
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        filter = findViewById(R.id.filterbtn);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Creators");

        locationDetailsContainer = findViewById(R.id.locationDetailsContainer);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        loadnxtbtn = findViewById(R.id.loadnextbtn);

        add = findViewById(R.id.add);
        reportbug = findViewById(R.id.reportbug);
        logout = findViewById(R.id.logout);
        container = findViewById(R.id.maincontainer);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.i(TAG, "onLocationResult: Location updated");
            }
        };
        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, FavCreatorfilter.class);
                startActivity(intent);
            }
        });
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (aBoolean) {
                    showButtonsWithAnimation(reportbug);
                    showButtonsWithAnimation(logout);
                    add.setImageResource(R.drawable.baseline_close_24);
                    aBoolean = false;
                } else {
                    hideButtonsWithAnimation(reportbug);
                    hideButtonsWithAnimation(logout);
                    add.setImageResource(R.drawable.ic_menu);
                    aBoolean = true;
                }
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogoutConfirmationDialog();
            }
        });
        reportbug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ReportBug.class);
                startActivity(intent);
            }
        });
        loadnxtbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateAirDistance(origin, destinations, names, creators, imgUrls, phoneNos, verifications);
            }
        });
        selectedDistance = getIntent().getIntExtra("selectedDistance", 0);
        selectedFoodCategory = getIntent().getStringExtra("selectedFoodCategory");

        if (selectedFoodCategory == null) {
            selectedFoodCategory = "All";
        }

        selectedRating = getIntent().getIntExtra("selectedRating", 0);
        selectedCategory = getIntent().getStringExtra("selectedCategory");

        if (selectedCategory == null) {
            selectedCategory = "All";
        }
        selectedFoodItems = getIntent().getStringArrayListExtra("selectedFoodItems");
        if (selectedFoodItems == null) {
            selectedFoodItems = new ArrayList<>();
        }

        Log.i(TAG, "Selected Distance: " + selectedDistance);
        Log.i(TAG, "Selected Food Category: " + selectedFoodCategory);
        Log.i(TAG, "Selected Category: " + selectedCategory);
        Log.i(TAG, "Selected Rating: " + selectedRating);
        for (String foodItem : selectedFoodItems) {
            Log.i(TAG, "Selected Food Item: " + foodItem);
        }

    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            showFilterDialog();
            loadUserLocation();
            moveToUserLocation();
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    moveToUserLocation();
                    Log.i(TAG, "onMyLocationButtonClick: User's location button clicked");
                    return true; // Return true to consume the event
                }
            });

            View locationButton = ((View) mapFragment.getView().findViewById(Integer.parseInt("1")).
                    getParent()).findViewById(Integer.parseInt("2"));
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 30);
        } else {
            requestLocationPermission();
            loadUserLocation();
            Log.i(TAG, "onMapReady: Location permission not granted");
        }

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                cameraMoved = true;
                Log.i(TAG, "onCameraMove: Camera moved");
            }
        });
    }
    private void showButtonsWithAnimation(View view) {
        view.setVisibility(View.VISIBLE);
        ScaleAnimation scaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(300);
        view.startAnimation(scaleAnimation);
    }

    private void hideButtonsWithAnimation(View view) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(300);
        view.startAnimation(scaleAnimation);
        view.setVisibility(View.INVISIBLE);
    }
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        Log.i(TAG, "requestLocationPermission: Requesting location permission");
    }
    private boolean appendLocationNamesToLogFile(String logFilePath, String locationNames) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true)); // Append mode
            writer.write(locationNames);
            writer.close();
            return true; // Return true to indicate success
        } catch (IOException e) {
            e.printStackTrace();
            return false; // Return false to indicate failure
        }
    }
    private void loadUserLocation() {
        Log.i(TAG, "Executing loadUserLocation function");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                Log.i(TAG, "User location loaded - Lat: " +
                                        userLocation.latitude + ", Lng: " + userLocation.longitude);

                                if (!cameraMoved) {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
                                    cameraMoved = true;
                                    Log.i(TAG, "Camera moved to user location");
                                }

                                if (!locationDetailsLoaded) {
                                    locationDetailsContainer.removeAllViews();
                                    Log.i(TAG, "Loading location details");
                                    Log.i(TAG, "Selected Distance: " + selectedDistance);
                                    Log.i(TAG, "Selected Food Category: " + selectedFoodCategory);
                                    Log.i(TAG, "Selected Food Items: " + selectedFoodItems);
                                    Log.i(TAG, "Selected Category: " + selectedCategory);
                                    Log.i(TAG, "Selected Rating: " + selectedRating);
                                    getFollowedCreators(new OnCompleteListener<List<String>>() {
                                        @Override
                                        public void onComplete(List<String> followedCreators) {
                                            if (followedCreators != null && !followedCreators.isEmpty() && followedCreators.size() >= 5) {
                                                Log.i(TAG, "Followed Creators:");
                                                for (String followedCreator : followedCreators) {
                                                    Log.i(TAG, followedCreator);
                                                }
                                                ValueEventListener valueEventListener = new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        List<LatLng> destinationsList = new ArrayList<>(); // List to store destination locations
                                                        List<String> names = new ArrayList<>();
                                                        List<String> creators = new ArrayList<>();
                                                        List<String> imgUrls = new ArrayList<>();
                                                        List<String> phoneNos = new ArrayList<>();
                                                        List<String> verifications = new ArrayList<>();

                                                        for (DataSnapshot creatorSnapshot : dataSnapshot.getChildren()) {
                                                            String creatorName = creatorSnapshot.child("name").getValue(String.class);
                                                            String verification = creatorSnapshot.child("request_verification")
                                                                    .child(creatorSnapshot.getKey()) // Assuming creator's UID is the key
                                                                    .child("verification")
                                                                    .getValue(String.class);
                                                            // Check if the creator is in the list of followed creators
                                                            if (followedCreators.contains(creatorName)) {
                                                                DataSnapshot recommendationSnapshot = creatorSnapshot.child("recommendation");
                                                                for (DataSnapshot recSnapshot : recommendationSnapshot.getChildren()) {
                                                                    String locationStr = recSnapshot.child("location").getValue(String.class);

                                                                    // Check if locationStr is in the expected format (e.g., "Lat: 12.345, Lng: 67.890")
                                                                    if (locationStr != null && locationStr.matches("Latitude: [0-9]+\\.[0-9]+, Longitude: [0-9]+\\.[0-9]+")) {
                                                                        // Parse latitude and longitude
                                                                        double latitude = Double.parseDouble(locationStr.split(", ")[0].split(": ")[1]);
                                                                        double longitude = Double.parseDouble(locationStr.split(", ")[1].split(": ")[1]);
                                                                        LatLng locationLatLng = new LatLng(latitude, longitude);

                                                                        float[] distanceResult = new float[1];
                                                                        Location.distanceBetween(
                                                                                userLocation.latitude, userLocation.longitude,
                                                                                locationLatLng.latitude, locationLatLng.longitude,
                                                                                distanceResult);
                                                                        // Check if the location is open and within the selected distance
                                                                        String timings = recSnapshot.child("timings").getValue(String.class);
                                                                        String foodType = recSnapshot.child("specialType").getValue(String.class);
                                                                        String Category = recSnapshot.child("foodType").getValue(String.class);
                                                                        int rating = recSnapshot.child("rating").getValue(Integer.class);
                                                                        String hashtags = recSnapshot.child("hashtag").getValue(String.class);
                                                                        String img = recSnapshot.child("imageUrl").getValue(String.class);
                                                                        String phoneNo = recSnapshot.child("contactNumber").getValue(String.class);

                                                                        boolean containsSelectedFoodItem = false;

                                                                        // Only check for food item filter if there are selected food items
                                                                        if (!selectedFoodItems.isEmpty()) {
                                                                            for (String selectedFoodItem : selectedFoodItems) {
                                                                                if (hashtags != null && hashtags.contains("#" + selectedFoodItem)) {
                                                                                    containsSelectedFoodItem = true;
                                                                                    break; // No need to continue checking if one is found
                                                                                }
                                                                            }
                                                                        } else {
                                                                            // If no food items are selected, set containsSelectedFoodItem to true
                                                                            containsSelectedFoodItem = true;
                                                                        }

                                                                        if (distanceResult[0] <= selectedDistance * 1000
                                                                                && isLocationOpen(timings)
                                                                                && (selectedFoodCategory.equals("All") || selectedFoodCategory.equals(foodType))
                                                                                && (selectedCategory.equals("All") || selectedCategory.equals(Category))
                                                                                && (selectedRating == 0 || rating >= selectedRating)
                                                                                && containsSelectedFoodItem) {
                                                                            String locationName = recSnapshot.child("name").getValue(String.class);
                                                                            Log.i(TAG, "Location Name: " + locationName);
                                                                            destinationsList.add(locationLatLng);
                                                                            names.add(locationName);
                                                                            creators.add(creatorName);
                                                                            imgUrls.add(img);
                                                                            phoneNos.add(phoneNo);
                                                                            verifications.add(verification);
                                                                            if (timings != null) {
                                                                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                                                                String[] timingParts = timings.split("-");
                                                                                if (timingParts.length == 2) {
                                                                                    String endTimeStr = timingParts[1];
                                                                                    try {
                                                                                        Calendar calendar = Calendar.getInstance();
                                                                                        String currentTimeStr = sdf.format(calendar.getTime());
                                                                                        Date endTime = sdf.parse(endTimeStr);
                                                                                        Date currentTime = sdf.parse(currentTimeStr);

                                                                                        long remainingMinutes = (endTime.getTime() - currentTime.getTime()) / (60 * 1000);

                                                                                        if (remainingMinutes <= 60 && remainingMinutes > 0) {
                                                                                            // Calculate the difference between the end time and current time in minutes
                                                                                            String remainingTime = "Closing in " + remainingMinutes + " mins";
                                                                                            int index = destinationsList.size() - 1;
                                                                                            names.set(index, names.get(index) + " - " + remainingTime);
                                                                                        }
                                                                                    } catch (
                                                                                            ParseException e) {
                                                                                        e.printStackTrace();
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }else {
                                                                        Log.e(TAG, "Invalid location format: " + locationStr);
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if (destinationsList.isEmpty()) {
                                                            showNoRecommendationsDialog();
                                                            Log.i(TAG, "No recommendations found for followed creators");
                                                        } else {
                                                            // Calculate distance using the Distance Matrix API for filtered destinations
                                                            calculateAirDistance(userLocation, destinationsList, names, creators, imgUrls, phoneNos, verifications);
                                                            locationDetailsLoaded = true;
                                                            Log.i(TAG, "Location details loaded");
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                                        // Handle database error
                                                        Log.i(TAG, "Database error: " + databaseError.getMessage());
                                                    }
                                                };

                                                databaseReference.addListenerForSingleValueEvent(valueEventListener);
                                            } else {
                                                // No followed creators found, show an alert dialog
                                                showNoFollowedCreatorsDialog();
                                                Log.i(TAG, "No followed creators found");
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    });
        } else {
            requestLocationPermission();
            Log.i(TAG, "Location permission not granted");
        }
    }
    private void showNoFollowedCreatorsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate the custom layout
        View customLayout = getLayoutInflater().inflate(R.layout.nocreator, null);
        builder.setView(customLayout);

        TextView dialogTitle = customLayout.findViewById(R.id.dialogTitle);
        TextView dialogMessage = customLayout.findViewById(R.id.dialogMessage);
        Button dialogButton = customLayout.findViewById(R.id.dialogButton);

        dialogTitle.setText("Celebrate Flavor and Variety");
        dialogMessage.setText("Enhance your experience by following at least 5 creators, and unlock a world of diverse and exciting recommendations tailored just for you.");

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle the OK button click by navigating to the FavCreator class
                Intent intent = new Intent(MainActivity.this, FavCreator.class);
                startActivity(intent);
            }
        });

        builder.setCancelable(false);
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Set a transparent background
        alertDialog.show();
        Log.i(TAG, "showNoFollowedCreatorsDialog: Displaying 'No Favorite Creators' dialog");
    }
    private void showNoRecommendationsDialog() {
        if (!filterDialogShown) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View customLayout = getLayoutInflater().inflate(R.layout.norecommendation, null);
            builder.setView(customLayout);

            TextView dialogTitle = customLayout.findViewById(R.id.dialogTitle);
            TextView dialogMessage = customLayout.findViewById(R.id.dialogMessage);
            Button dialogButton = customLayout.findViewById(R.id.dialogButton);
            dialogTitle.setText("No Recommendations Found");
            dialogMessage.setText("Sorry, no recommendations were found for the selected filter. Please adjust your filter criteria to discover new recommendations.");

            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Handle the OK button click by navigating to the FavCreator class
                    Intent intent = new Intent(MainActivity.this, FavCreatorfilter.class);
                    startActivity(intent);
                }
            });

            builder.setCancelable(false);
            AlertDialog alertDialog = builder.create();
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Set a transparent background
            alertDialog.show();
            Log.i(TAG, "showNoFollowedCreatorsDialog: Displaying 'No Favorite Creators' dialog");
        }
    }
    private void showFilterDialog() {
        // Check if the conditions are met to show the filter dialog
        if (selectedDistance == 0 && "All".equals(selectedFoodCategory)
                && "All".equals(selectedCategory) && selectedRating == 0
                && selectedFoodItems.isEmpty()) {
            filterDialogShown = true;
            // Create and show the filter dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            View customLayout = getLayoutInflater().inflate(R.layout.filterdialog, null);
            builder.setView(customLayout);

            TextView dialogTitle = customLayout.findViewById(R.id.dialogTitle);
            TextView dialogMessage = customLayout.findViewById(R.id.dialogMessage);
            Button dialogButton = customLayout.findViewById(R.id.dialogButton);
            dialogTitle.setText("Customize Your Recommendations");
            dialogMessage.setText("Discover the perfect recommendations tailored to your taste by customizing your filters. Select your preferences to unlock a world of tailored experiences.");

            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, FavCreatorfilter.class);
                    startActivity(intent);
                }
            });

            builder.setCancelable(false);
            container.setVisibility(View.GONE);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    filterDialogShown = false;
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Set a transparent background
            alertDialog.show();
            Log.i(TAG, "showFilterDialog: Displaying the filter dialog");
        }
    }
    private void getFollowedCreators(final OnCompleteListener<List<String>> listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            final String currentUserId = currentUser.getUid();

            DatabaseReference userFavoritesRef = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Users")
                    .child(currentUserId)
                    .child("fav-creators");

            userFavoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<String> followedCreators = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        // Add the name of the creator to the list
                        followedCreators.add(snapshot.getKey());
                    }
                    listener.onComplete(followedCreators);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle database errors if needed
                    Log.e("Firebase", "Database error: " + databaseError.getMessage());
                }
            });
        }
    }

    private interface OnCompleteListener<T> {
        void onComplete(T result);
    }

    private boolean isLocationOpen(String timings) {
        // Extract the start and end times from the timings string
        String[] times = timings.split(" - ");
        if (times.length == 2) {
            String startTimeStr = times[0];
            String endTimeStr = times[1];

            // Get the current time
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String currentTimeStr = sdf.format(calendar.getTime());

            // Compare the current time with the location's timings
            try {
                Date startTime = sdf.parse(startTimeStr);
                Date endTime = sdf.parse(endTimeStr);
                Date currentTime = sdf.parse(currentTimeStr);

                return currentTime.after(startTime) && currentTime.before(endTime);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void checkUserName() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference usersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);
        usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("name")) {
                    String userName = dataSnapshot.child("name").getValue(String.class);
                    if (userName != null) {
                        // Update the user greeting in the navigation header
                        TextView userGreetingTextView = findViewById(R.id.userGreeting);
                        //userGreetingTextView.setText("Hello,  " + userName);
                    }
                } else {
                    // The user's name is not present, show a dialog to get the name
                    showNameInputDialog();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle database error
            }
        });
    }

    private void showNameInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_name, null);
        builder.setView(dialogView);

        final EditText inputName = dialogView.findViewById(R.id.edit_text_name);
        Button submitbtn = dialogView.findViewById(R.id.btn_submit);
        nameDialog = builder.create();
        nameDialog.setCanceledOnTouchOutside(false);
        nameDialog.setCancelable(false);
        nameDialog.show();

        submitbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = inputName.getText().toString().trim();
                if (!TextUtils.isEmpty(userName)) {
                    saveUserName(userName);
                    nameDialog.dismiss();
                } else {
                    // Handle empty name input
                    Toast.makeText(MainActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveUserName(String userName) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference usersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);
        usersReference.child("name").setValue(userName);

    }
    private void checkUserPhoneNumber() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference usersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);
        usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild("phoneNumber")) {
                    showPhoneNumberInputDialog();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
    private void showPhoneNumberInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_phone_number, null);
        builder.setView(dialogView);

        final EditText phoneNumberEditText = dialogView.findViewById(R.id.edit_text_phone_number);
        Button verifyButton = dialogView.findViewById(R.id.btn_verify);

        phoneNumberDialog = builder.create();
        phoneNumberDialog.setCanceledOnTouchOutside(false); // Set dialog to not cancelable on outside touch
        phoneNumberDialog.setCancelable(false); // Set dialog to not cancelable on back press
        phoneNumberDialog.show();
        phoneNumberDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = phoneNumberEditText.getText().toString().trim();
                if (!phoneNumber.isEmpty() && phoneNumber.length() == 10) {
                    try {
                        long number = Long.parseLong(phoneNumber);
                        String otp = generateOTP(); // Generating a 6-digit OTP

                        // Check if SMS permission is granted
                        if (isSmsPermissionGranted()) {
                            sendOTPViaSMS(phoneNumber, otp);
                            phoneNumberDialog.dismiss();
                            showOTPInputDialog(otp, phoneNumber);
                        } else {
                            requestSmsPermission();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(getApplicationContext(), "Invalid phone number format", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isSmsPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    // Method to request SMS permission
    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
    }

    private String generateOTP() {
        Random random = new Random();
        int otpValue = 100000 + random.nextInt(900000); // Generating a random 6-digit number
        return String.valueOf(otpValue);
    }
    private void sendOTPViaSMS(String phoneNumber, String otp) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, "Code for Dine2Destiny : " + otp, null, null);
            Toast.makeText(getApplicationContext(), "Verification Code sent successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Failed to send Verification Code", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showOTPInputDialog(String generatedOTP, final String phoneNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_otp, null);
        builder.setView(dialogView);

        final EditText otpEditText = dialogView.findViewById(R.id.edit_text_otp);
        Button submitButton = dialogView.findViewById(R.id.btn_submit);

        otpdialog = builder.create();
        otpdialog.setCanceledOnTouchOutside(false);
        otpdialog.setCancelable(false);
        otpdialog.show();
        otpdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        final String otp = generatedOTP;

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredOTP = otpEditText.getText().toString().trim();
                if (!enteredOTP.isEmpty()) {
                    if (enteredOTP.equals(otp)) {
                        saveUserPhoneNumber(phoneNumber);
                        otpdialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Phone number verified successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Entered Verification Code is incorrect. Please re-enter the Code", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter the Verification Code", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void saveUserPhoneNumber (String userPhoneNumber) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference usersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);
        usersReference.child("phoneNumber").setValue(userPhoneNumber);

    }


    private void moveToUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
                            }
                        }
                    });
        }
    }
    private void calculateAirDistance(LatLng origin, List<LatLng> destinations, List<String> names, List<String> creators, List<String> imgUrls, List<String> phoneNos, List<String> verifications) {
        TreeMap<Float, Integer> distanceMap = new TreeMap<>(); // TreeMap to maintain sorted distances
        this.origin = origin;
        this.destinations = destinations;
        this.names = names;
        this.creators = creators;
        this.imgUrls = imgUrls;
        this.phoneNos = phoneNos;
        this.verifications = verifications;

        for (int i = 0; i < destinations.size(); i++) {
            LatLng destination = destinations.get(i);
            float distanceInKm = calculateAirDistanceInKm(origin, destination);
            boolean isLocationOpen = true; // Replace with your logic

            if (isLocationOpen && distanceInKm <= selectedDistance) {
                distanceMap.put(distanceInKm, i); // Store distance and index
            }
        }

        List<Map.Entry<Float, Integer>> sortedDistances = new ArrayList<>(distanceMap.entrySet());

        sortedDistances.sort(Map.Entry.comparingByKey()); // Sort distances in ascending order

        int totalRecommendations = sortedDistances.size();
        int remainingRecommendations = totalRecommendations - currentRecommendationIndex;
        loadnxtbtn.setVisibility(View.VISIBLE);

        if (remainingRecommendations > 0) {
            int recommendationsToLoad = Math.min(5, remainingRecommendations);

            List<Map.Entry<Float, Integer>> nextRecommendations = new ArrayList<>();

            for (int i = currentRecommendationIndex; i < currentRecommendationIndex + recommendationsToLoad; i++) {
                nextRecommendations.add(sortedDistances.get(i));
            }

            currentRecommendationIndex += recommendationsToLoad;

            List<LatLng> filteredDestinations = new ArrayList<>();
            List<String> filteredNames = new ArrayList<>();
            List<String> filteredCreators = new ArrayList<>();
            List<String> filteredImgUrls = new ArrayList<>();
            List<String> filteredPhoneNos = new ArrayList<>();
            List<String> filteredVerifications = new ArrayList<>();

            for (Map.Entry<Float, Integer> entry : nextRecommendations) {
                int index = entry.getValue();
                LatLng destinationLatLng = destinations.get(index);

                filteredDestinations.add(destinationLatLng);
                filteredNames.add(names.get(index));
                filteredCreators.add(creators.get(index));
                filteredImgUrls.add(imgUrls.get(index));
                filteredPhoneNos.add(phoneNos.get(index));
                filteredVerifications.add(verifications.get(index));
            }
            if (!recommendationMessageloaded) {
                String logFilePath = getIntent().getStringExtra("logFilePath"); // Assuming logFilePath is accessible here

                String recommendationMessage = "\nRecommendations loaded to user are:\n";

                if (appendLocationNamesToLogFile(logFilePath, recommendationMessage)) {
                    Log.i(TAG, "Recommendation message appended to the log file");
                    recommendationMessageloaded = true; // Set flag to true once the recommendation message is added
                } else {
                    Log.e(TAG, "Failed to append recommendation message to the log file");
                }
            }

            String logFilePath = getIntent().getStringExtra("logFilePath"); // Assuming logFilePath is accessible here

            StringBuilder namesBuilder = new StringBuilder();

            for (String name : names) {
                if (!addedNamesToLogFile.contains(name)) {
                    namesBuilder.append(name).append(",\n"); // Append each name followed by a comma and a newline character
                    addedNamesToLogFile.add(name); // Add the name to the set to mark it as already added
                }
            }

            if (namesBuilder.length() > 0) {
                namesBuilder.deleteCharAt(namesBuilder.length() - 2); // Remove the last comma and newline character
                String namesWithNewlines = namesBuilder.toString();

                if (appendLocationNamesToLogFile(logFilePath, namesWithNewlines)) {
                    Log.i(TAG, "Location names appended to the log file");
                } else {
                    Log.e(TAG, "Failed to append location names to the log file");
                }
            }

            calculateDistance(origin, filteredDestinations, filteredNames, filteredCreators, filteredImgUrls, filteredPhoneNos, filteredVerifications);
        } else {
            Toast.makeText(MainActivity.this, "No More Recommendations Available", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "No more recommendations available");
            loadnxtbtn.setVisibility(View.GONE);
        }
    }
    /*private void calculateAirDistance(LatLng origin, List<LatLng> destinations, List<String> names, List<String> creators, List<String> imgUrls, List<String> phoneNos, List<String> verifications) {
        TreeMap<Float, Integer> distanceMap = new TreeMap<>(); // TreeMap to maintain sorted distances

        for (int i = 0; i < destinations.size(); i++) {
            LatLng destination = destinations.get(i);
            float distanceInKm = calculateAirDistanceInKm(origin, destination);
            boolean isLocationOpen = true; // Replace with your logic

            if (isLocationOpen && distanceInKm <= selectedDistance) {
                distanceMap.put(distanceInKm, i); // Store distance and index
            }
        }

        int count = 0;
        List<LatLng> filteredDestinations = new ArrayList<>();
        List<String> filteredNames = new ArrayList<>();
        List<String> filteredCreators = new ArrayList<>();
        List<String> filteredImgUrls = new ArrayList<>();
        List<String> filteredPhoneNos = new ArrayList<>();
        List<String> filteredVerifications = new ArrayList<>();

        for (Map.Entry<Float, Integer> entry : distanceMap.entrySet()) {
            if (count >= 5) {
                break; // Break the loop after storing the nearest 5 locations
            }

            int index = entry.getValue();
            LatLng destinationLatLng = destinations.get(index);
            float distance = entry.getKey();

            filteredDestinations.add(destinationLatLng);
            filteredNames.add(names.get(index));
            filteredCreators.add(creators.get(index));
            filteredImgUrls.add(imgUrls.get(index));
            filteredPhoneNos.add(phoneNos.get(index));
            filteredVerifications.add(verifications.get(index));

            count++;
        }
        if (!recommendationMessageloaded) {
            String logFilePath = getIntent().getStringExtra("logFilePath"); // Assuming logFilePath is accessible here

            String recommendationMessage = "\nRecommendations loaded to user are:\n";

            if (appendLocationNamesToLogFile(logFilePath, recommendationMessage)) {
                Log.i(TAG, "Recommendation message appended to the log file");
                recommendationMessageloaded = true; // Set flag to true once the recommendation message is added
            } else {
                Log.e(TAG, "Failed to append recommendation message to the log file");
            }
        }

        String logFilePath = getIntent().getStringExtra("logFilePath"); // Assuming logFilePath is accessible here

        StringBuilder namesBuilder = new StringBuilder();

        for (String name : names) {
            namesBuilder.append(name).append(",\n"); // Append each name followed by a comma and a newline character
        }

        namesBuilder.deleteCharAt(namesBuilder.length() - 2);

        String namesWithNewlines = namesBuilder.toString();

        if (appendLocationNamesToLogFile(logFilePath, namesWithNewlines)) {
            Log.i(TAG, "Location names appended to the log file");
        } else {
            Log.e(TAG, "Failed to append location names to the log file");
        }

        calculateDistance(origin, filteredDestinations, filteredNames, filteredCreators, filteredImgUrls, filteredPhoneNos, filteredVerifications);
    }*/
    private float calculateAirDistanceInKm(LatLng origin, LatLng destination) {
        int R = 6371; // Radius of the Earth in kilometers

        double lat1 = Math.toRadians(origin.latitude);
        double lon1 = Math.toRadians(origin.longitude);
        double lat2 = Math.toRadians(destination.latitude);
        double lon2 = Math.toRadians(destination.longitude);

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;

        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (float) (R * c); // Distance in kilometers
    }
    private void calculateDistance(LatLng origin, List<LatLng> destinations, List<String> names, List<String> creators, List<String> imgUrls, List<String> phoneNos, List<String> verifications) {
        String apiKey = "AIzaSyDHoXOg6fB7_Aj9u9hCCkM76W0CzN5pZHE"; // Replace with your Google Maps API Key
        StringBuilder destinationsStr = new StringBuilder();

        for (int i = 0; i < destinations.size(); i++) {
            LatLng destination = destinations.get(i);

            destinationsStr.append(destination.latitude).append(",").append(destination.longitude).append("|");
        }

        String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" +
                origin.latitude + "," + origin.longitude +
                "&destinations=" + destinationsStr.toString() +
                "&key=" + apiKey;
        Log.i("Google Maps", "Distance Matrix API URL: " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray rows = response.getJSONArray("rows");
                            if (rows.length() > 0) {
                                JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");
                                for (int i = 0; i < elements.length(); i++) {
                                    JSONObject element = elements.getJSONObject(i);
                                    if (element.has("distance")) {
                                        float distanceValue = element.getJSONObject("distance").getInt("value");
                                        String placeId = element.optString("placeId", null);

                                        // Convert meters to kilometers
                                        float distanceInKm = distanceValue / 1000.0f;

                                        // Check if the location is open and within the selected distance
                                        // Replace this with your own logic for determining if a location is open
                                        boolean isLocationOpen = true; // Replace with your logic

                                        if ((placeId == null || !visitedPlaceIds.contains(placeId)) && distanceInKm <= selectedDistance && isLocationOpen) {
                                            String name = names.get(i);
                                            String creator = creators.get(i);
                                            String verification = verifications.get(i);
                                            String snippet = "Creator: " + creator;
                                            String img = imgUrls.get(i);
                                            String phoneno = phoneNos.get(i);

                                            LatLng destinationLatLng = destinations.get(i);

                                            MarkerOptions markerOptions = new MarkerOptions()
                                                    .position(destinationLatLng)
                                                    .title(name)
                                                    .snippet(snippet);

                                            // Add a marker to the map and store it in the locationMarkers list
                                            Marker marker = mMap.addMarker(markerOptions);
                                            locationMarkers.add(marker);

                                            if (verification != null && !verification.isEmpty()) {
                                                addLocationDetails(name, creator, verification, distanceInKm, destinationLatLng, img, phoneno);
                                            } else {
                                                showNoRecommendationsDialog();
                                            }
                                            if (placeId != null) {
                                                visitedPlaceIds.add(placeId);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                        error.printStackTrace();
                    }
                });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private void addLocationDetails(final String name, String creator, String verification, final float distance, final LatLng destinationLatLng, String img, String phoneno) {
        // Round the distance to 2 decimal places
        float roundedDistance = roundDistance(distance, 2);

        // Check if the location details view is not already added
        View existingLocationDetailsView = findLocationDetailsViewByName(name);

        if (existingLocationDetailsView == null) {
            View locationDetailsView = getLayoutInflater().inflate(R.layout.location_details, null);

            TextView locationName = locationDetailsView.findViewById(R.id.locationName);
            TextView creatorName = locationDetailsView.findViewById(R.id.locationRating); // Change the ID if needed
            TextView locationDistance = locationDetailsView.findViewById(R.id.locationDistance);
            ImageView recommendationimg = locationDetailsView.findViewById(R.id.recommendationImage);
            TextView locationnumber = locationDetailsView.findViewById(R.id.locationNumber);

            if (img != null) {
                Glide.with(this).load(img).placeholder(R.drawable.default_hotel_img).into(recommendationimg);
            }

            locationDistance.setText(String.format("%.2f km", roundedDistance)); // Display the rounded distance
            String number = "Contact No: " + phoneno;
            locationnumber.setText(number);

            // Store the creator for this location
            List<String> creatorsList = new ArrayList<>();
            creatorsList.add(creator);
            locationCreatorsMap.put(name, creatorsList);

            // Check if the location name contains "Closing in x mins" and set the text color accordingly
            if (name.contains("Closing in")) {
                // Name contains "Closing in x mins", set text color to red
                locationName.setTextColor(Color.RED);
            } else {
                // Name doesn't contain "Closing in x mins", use the default text color
            }

            // Set the location name and creator's name
            locationName.setText(name);

            String creatorsText = "Creator: " + creator;
            creatorName.setText(creatorsText);

            // Show the tick mark if verification is 1
            if (verification != null && verification.equals("1")) {
                ImageView verify = locationDetailsView.findViewById(R.id.verifiedIcon);
                verify.setVisibility(View.VISIBLE);
            }

            // Add an OnClickListener to the location details view
            locationDetailsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Show a confirmation dialog
                    showDirectionsConfirmationDialog(name, destinationLatLng);
                }
            });

            // Add the location details view to the container
            locationDetailsContainer.addView(locationDetailsView);

            // Sort the location details views in the container by distance
            sortLocationDetailsViewsByDistance();
        } else {
            // Update the existing location details view if necessary
            List<String> creatorsList = locationCreatorsMap.get(name);
            if (!creatorsList.contains(creator)) {
                creatorsList.add(creator);

                TextView creatorName = existingLocationDetailsView.findViewById(R.id.locationRating);
                String existingCreatorsText = creatorName.getText().toString();
                String newCreatorsText = existingCreatorsText + ", " + creator;
                creatorName.setText(newCreatorsText);

                // Show the tick mark if verification is 1
                if (verification != null && verification.equals("1")) {
                    ImageView verify = existingLocationDetailsView.findViewById(R.id.verifiedIcon);
                    verify.setVisibility(View.VISIBLE);
                }
            }
        }
        if (!recommendationMessageAdded) {
            String logFilePath = getIntent().getStringExtra("logFilePath"); // Assuming logFilePath is accessible here

            String recommendationMessage = "\nRecommendations showing to user are:\n";

            if (appendLocationNamesToLogFile(logFilePath, recommendationMessage)) {
                Log.i(TAG, "Recommendation message appended to the log file");
                recommendationMessageAdded = true; // Set flag to true once the recommendation message is added
            } else {
                Log.e(TAG, "Failed to append recommendation message to the log file");
            }
        }

        String logFilePath = getIntent().getStringExtra("logFilePath"); // Assuming logFilePath is accessible here

        String nameWithNewline = name + "\n";

        if (appendLocationNamesToLogFile(logFilePath, nameWithNewline)) {
            Log.i(TAG, "Location name '" + name + "' appended to the log file");
        } else {
            Log.e(TAG, "Failed to append location name to the log file");
        }

    }


    private void sortLocationDetailsViewsByDistance() {
        List<View> locationDetailsViews = new ArrayList<>();
        for (int i = 0; i < locationDetailsContainer.getChildCount(); i++) {
            locationDetailsViews.add(locationDetailsContainer.getChildAt(i));
        }

        Collections.sort(locationDetailsViews, new Comparator<View>() {
            @Override
            public int compare(View view1, View view2) {
                TextView distanceView1 = view1.findViewById(R.id.locationDistance);
                TextView distanceView2 = view2.findViewById(R.id.locationDistance);
                float distance1 = Float.parseFloat(distanceView1.getText().toString().replace(" km", ""));
                float distance2 = Float.parseFloat(distanceView2.getText().toString().replace(" km", ""));
                return Float.compare(distance1, distance2);
            }
        });

        locationDetailsContainer.removeAllViews();
        for (View locationDetailsView : locationDetailsViews) {
            locationDetailsContainer.addView(locationDetailsView);
        }
    }
    private void showDirectionsConfirmationDialog(final String locationName, final LatLng destinationLatLng) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Get Directions");
        builder.setMessage("Do you want to get directions to " + locationName + "?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User selected "Yes," so navigate to Google Maps
                navigateToDestination(destinationLatLng);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // User selected "No," so close the dialog and stay on the page
                dialog.dismiss();
            }
        });

        builder.setCancelable(true); // Allow the user to dismiss the dialog by tapping outside

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void navigateToDestination(LatLng destination) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destination.latitude + "," + destination.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Handle the case where the Google Maps app is not installed on the device
            Toast.makeText(this, "Google Maps app not installed.", Toast.LENGTH_SHORT).show();
        }
    }


    private View findLocationDetailsViewByName(String name) {
        // Check if a location with the same name has already been added
        for (int i = 0; i < locationDetailsContainer.getChildCount(); i++) {
            View childView = locationDetailsContainer.getChildAt(i);
            TextView locationNameTextView = childView.findViewById(R.id.locationName);

            String existingName = locationNameTextView.getText().toString();
            if (name.equals(existingName)) {
                return childView;
            }
        }
        return null; // Location details not found
    }

    private float roundDistance(float distance, int decimalPlaces) {
        float multiplier = (float) Math.pow(10, decimalPlaces);
        return Math.round(distance * multiplier) / multiplier;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the location callback when the activity is destroyed
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestLocationPermission();
                } else {
                    // Initialize the fusedLocationClient here before using it
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

                    // Request location updates using the initialized fusedLocationClient
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    Log.i("Location", "Location updates requested.");
                }
            } else {
                Toast.makeText(this, "Location permission denied. App cannot function properly.", Toast.LENGTH_SHORT).show();
                Log.i("Location", "Location permission denied.");
            }
        } else if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed to send OTP or perform SMS-related actions
                // Call the method for sending OTP or perform relevant actions here
            } else {
                Toast.makeText(this, "SMS permission denied. Cannot send OTP.", Toast.LENGTH_SHORT).show();
                Log.i("SMS", "SMS permission denied.");
            }
        }
    }
    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            // User clicked Yes, log out
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, SendOTPActivity.class));
            finish();
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            // User clicked No, close the dialog
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if (phoneNumberDialog != null && phoneNumberDialog.isShowing()) {
            Toast.makeText(this, "Please complete the phone number verification", Toast.LENGTH_SHORT).show();
        } else if (otpdialog != null && otpdialog.isShowing()) {
            Toast.makeText(this, "Please enter the OTP for phone number verification", Toast.LENGTH_SHORT).show();
        } else {
            if (doubleBackToExitPressedOnce) {
                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            } else {
                this.doubleBackToExitPressedOnce = true;
                Toast.makeText(this, "Press BACK twice to exit", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000); // Reset after 2 seconds
            }
        }
    }

}