package com.example.dine2destiny;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.widget.SeekBar;
import android.widget.ToggleButton;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity"; // Tag for logging
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private SupportMapFragment mapFragment;
    private DatabaseReference databaseReference;
    private boolean cameraMoved = false;
    private LinearLayout locationDetailsContainer;
    private DrawerLayout drawerLayout;
    private LocationCallback locationCallback;

    private boolean locationDetailsLoaded = false; // Flag to control location details loading
    private LocationRequest locationRequest;

    // List to store markers on the map
    private List<Marker> locationMarkers = new ArrayList<>();
    private Set<String> visitedPlaceIds = new HashSet<>();
    private Map<String, List<String>> locationCreatorsMap = new HashMap<>();
    private Button filter;
    private int selectedDistance;
    private int selectedRating;
    private String selectedFoodCategory;
    private String selectedCategory;
    private ArrayList<String> selectedFoodItems;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: Initializing MainActivity");
        checkUserName();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav,
                R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

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
        // If no food items are selected, initialize the list to empty
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
            loadUserLocation();
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    // Move the camera to the user's current location
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

                                    // Retrieve the log file path from the intent
                                    String logFilePath = getIntent().getStringExtra("logFilePath");

                                    // Retrieve the list of creators that the user follows
                                    getFollowedCreators(new OnCompleteListener<List<String>>() {
                                        @Override
                                        public void onComplete(List<String> followedCreators) {
                                            if (followedCreators != null && !followedCreators.isEmpty()) {
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
                                                                }
                                                            }
                                                        }

                                                        if (destinationsList.isEmpty()) {
                                                            // No recommendations found for followed creators, show an alert dialog
                                                            showNoRecommendationsDialog();
                                                            Log.i(TAG, "No recommendations found for followed creators");
                                                        } else {
                                                            // Calculate distance using the Distance Matrix API for filtered destinations
                                                            calculateDistance(userLocation, destinationsList, names, creators, imgUrls, phoneNos, verifications);
                                                            locationDetailsLoaded = true;
                                                            Log.i(TAG, "Location details loaded");

                                                            // Append the location names to the log file
                                                            String locationNames = "\nRecommendations which are been loaded are:\n";
                                                            for (String locationName : names) {
                                                                locationNames += locationName + "\n";
                                                            }
                                                            if (appendLocationNamesToLogFile(logFilePath, locationNames)) {
                                                                Log.i(TAG, "Location names appended to the log file");
                                                            } else {
                                                                Log.e(TAG, "Failed to append location names to the log file");
                                                            }
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
        builder.setTitle("No Favorite Creators");
        builder.setMessage("You have not followed any creators. Follow some creators to get recommendations.");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle the OK button click
            }
        });

        builder.setCancelable(false);
        builder.show();
        Log.i(TAG, "showNoFollowedCreatorsDialog: Displaying 'No Favorite Creators' dialog");
    }

    private void showNoRecommendationsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Recommendations");
        builder.setMessage("Choose the filters options to get the perfect recommendation for you");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle the OK button click
            }
        });

        builder.setCancelable(false);
        builder.show();
        Log.i(TAG, "showNoRecommendationsDialog: Displaying 'No Recommendations' dialog");
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
                        userGreetingTextView.setText("Hello,  " + userName);
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
        builder.setTitle("Enter Your Name");

        final EditText inputName = new EditText(this);
        inputName.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(inputName);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userName = inputName.getText().toString().trim();
                if (!TextUtils.isEmpty(userName)) {
                    // Save the user's name in the "Users" table
                    saveUserName(userName);
                } else {
                    // Handle empty name input
                    Toast.makeText(MainActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void saveUserName(String userName) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference usersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);
        usersReference.child("name").setValue(userName);

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

                                            // Pass the correct distanceInKm to addLocationDetails
                                            addLocationDetails(name, creator, verification, distanceInKm, destinationLatLng, img, phoneno);
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

            locationDetailsContainer.addView(locationDetailsView);
        } else {
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
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_home) {
            Log.i("Navigation", "Home item selected");
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (item.getItemId() == R.id.nav_settings) {
            Log.i("Navigation", "Fav creator item selected");
            startActivity(new Intent(MainActivity.this, FavCreator.class));
            finish();
        } else if (item.getItemId() == R.id.nav_share) {
            Log.i("Navigation", "Report Bug item selected");
            startActivity(new Intent(MainActivity.this, ReportBug.class));
            finish();
        } else if (item.getItemId() == R.id.nav_logout) {
            Log.i("Navigation", "Logout item selected");
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, SendOTPActivity.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        // Close the app when the back button is pressed
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

}