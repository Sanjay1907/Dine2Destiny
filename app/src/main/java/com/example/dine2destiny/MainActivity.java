package com.example.dine2destiny;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
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
import java.util.List;
import java.util.Locale;
import android.widget.SeekBar;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private RadioGroup foodTypeRadioGroup;
    private RadioButton vegRadioButton;
    private RadioButton nonVegRadioButton;
    private String selectedFoodType = "Any";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private SupportMapFragment mapFragment;
    private DatabaseReference databaseReference;
    private boolean cameraMoved = false;
    private LinearLayout locationDetailsContainer;
    private DrawerLayout drawerLayout;
    private SeekBar distanceSeekBar;
    private TextView distanceText;
    private LocationCallback locationCallback;

    private boolean locationDetailsLoaded = false; // Flag to control location details loading
    private LocationRequest locationRequest;

    private int selectedDistance = 1;
    private RadioGroup ratingRadioGroup;
    private RadioButton rating4PlusRadioButton;
    private RadioButton rating3PlusRadioButton;
    private RadioButton rating2PlusRadioButton;
    private RadioButton rating1PlusRadioButton;
    private int selectedRating = 0; // 0 means no rating filter

    // List to store markers on the map
    private List<Marker> locationMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Creators");

        locationDetailsContainer = findViewById(R.id.locationDetailsContainer);
        distanceSeekBar = findViewById(R.id.distanceSeekBar);
        distanceText = findViewById(R.id.distanceText);
        foodTypeRadioGroup = findViewById(R.id.foodTypeRadioGroup);
        vegRadioButton = findViewById(R.id.vegRadioButton);
        nonVegRadioButton = findViewById(R.id.nonVegRadioButton);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        ratingRadioGroup = findViewById(R.id.ratingRadioGroup);
        rating4PlusRadioButton = findViewById(R.id.rating4PlusRadioButton);
        rating3PlusRadioButton = findViewById(R.id.rating3PlusRadioButton);
        rating2PlusRadioButton = findViewById(R.id.rating2PlusRadioButton);
        rating1PlusRadioButton = findViewById(R.id.rating1PlusRadioButton);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // Handle location updates here
            }
        };
        distanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedDistance = progress + 1; // Add 1 to make it 1-10 km
                // Round down the distance to the nearest integer
                int roundedDistance = (int) Math.floor(selectedDistance);
                distanceText.setText(roundedDistance + " km");
                locationDetailsLoaded = false;
                mMap.clear(); // Clear existing markers
                locationMarkers.clear(); // Clear the list of markers
                loadUserLocation();
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
                locationDetailsLoaded = false;
                mMap.clear();
                locationMarkers.clear();
                loadUserLocation();
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
                locationDetailsLoaded = false;
                mMap.clear();
                locationMarkers.clear();
                loadUserLocation();
            }
        });
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
        }

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                cameraMoved = true;
            }
        });
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void loadUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                                if (!cameraMoved) {
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
                                    cameraMoved = true;
                                }

                                if (!locationDetailsLoaded) {
                                    locationDetailsContainer.removeAllViews();

                                    // Retrieve the list of creators that the user follows
                                    getFollowedCreators(new OnCompleteListener<List<String>>() {
                                        @Override
                                        public void onComplete(List<String> followedCreators) {
                                            if (followedCreators != null && !followedCreators.isEmpty()) {
                                                ValueEventListener valueEventListener = new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        List<LatLng> destinationsList = new ArrayList<>(); // List to store destination locations
                                                        List<String> names = new ArrayList<>();
                                                        List<String> creators = new ArrayList<>();

                                                        for (DataSnapshot creatorSnapshot : dataSnapshot.getChildren()) {
                                                            String creatorName = creatorSnapshot.child("name").getValue(String.class);

                                                            // Check if the creator is in the list of followed creators
                                                            if (followedCreators.contains(creatorName)) {
                                                                DataSnapshot recommendationSnapshot = creatorSnapshot.child("recommendation");
                                                                for (DataSnapshot recSnapshot : recommendationSnapshot.getChildren()) {
                                                                    String locationStr = recSnapshot.child("location").getValue(String.class);
                                                                    double latitude = Double.parseDouble(locationStr.split(", ")[0].split(": ")[1]);
                                                                    double longitude = Double.parseDouble(locationStr.split(", ")[1].split(": ")[1]);
                                                                    LatLng locationLatLng = new LatLng(latitude, longitude);

                                                                    // Calculate distance between user location and recommendation location
                                                                    float[] distanceResult = new float[1];
                                                                    Location.distanceBetween(
                                                                            userLocation.latitude, userLocation.longitude,
                                                                            locationLatLng.latitude, locationLatLng.longitude,
                                                                            distanceResult);

                                                                    // Check if the location is open, within the selected distance, and matches the selected food type
                                                                    String timings = recSnapshot.child("timings").getValue(String.class);
                                                                    String foodType = recSnapshot.child("foodType").getValue(String.class);

                                                                    int rating = recSnapshot.child("rating").getValue(Integer.class);

                                                                    if (distanceResult[0] <= selectedDistance * 1000
                                                                            && isLocationOpen(timings)
                                                                            && (selectedFoodType.equals("Any") || selectedFoodType.equals(foodType))
                                                                            && (selectedRating == 0 || rating >= selectedRating)) {
                                                                        destinationsList.add(locationLatLng);
                                                                        names.add(recSnapshot.child("name").getValue(String.class));
                                                                        creators.add(creatorName);
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        if (destinationsList.isEmpty()) {
                                                            // No recommendations found for followed creators, show an alert dialog
                                                            showNoRecommendationsDialog();
                                                        } else {
                                                            // Calculate distance using the Distance Matrix API for filtered destinations
                                                            calculateDistance(userLocation, destinationsList, names, creators);
                                                            locationDetailsLoaded = true;
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                                        // Handle database error
                                                    }
                                                };

                                                databaseReference.addListenerForSingleValueEvent(valueEventListener);
                                            } else {
                                                // No followed creators found, show an alert dialog
                                                showNoFollowedCreatorsDialog();
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    });
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
    }

    private void showNoRecommendationsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Recommendations");
        builder.setMessage("There are no recommendations available in the selected distance range from the creators you follow.");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle the OK button click
            }
        });

        builder.setCancelable(false);
        builder.show();
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
                }
            });
        }
    }

    // Define an interface for completion callbacks
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
                if (!dataSnapshot.hasChild("name")) {
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

    private void calculateDistance(LatLng origin, List<LatLng> destinations, List<String> names, List<String> creators) {
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

                                        // Convert meters to kilometers
                                        float distanceInKm = distanceValue / 1000.0f;

                                        // Check if the location is open and within the selected distance
                                        // Replace this with your own logic for determining if a location is open
                                        boolean isLocationOpen = true; // Replace with your logic

                                        if (distanceInKm <= selectedDistance && isLocationOpen) {
                                            String name = names.get(i);
                                            String creator = creators.get(i);
                                            String snippet = "Creator: " + creator;

                                            LatLng destinationLatLng = destinations.get(i);

                                            MarkerOptions markerOptions = new MarkerOptions()
                                                    .position(destinationLatLng)
                                                    .title(name)
                                                    .snippet(snippet);

                                            // Add a marker to the map and store it in the locationMarkers list
                                            Marker marker = mMap.addMarker(markerOptions);
                                            locationMarkers.add(marker);

                                            // Pass the correct distanceInKm to addLocationDetails
                                            addLocationDetails(name, creator, distanceInKm);
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

    private void addLocationDetails(String name, String creator, float distance) {
        // Round the distance to 2 decimal places
        float roundedDistance = roundDistance(distance, 2);

        // Check if the location details view is not already added
        if (!isLocationDetailsAlreadyAdded(name)) {
            View locationDetailsView = getLayoutInflater().inflate(R.layout.location_details, null);

            TextView locationName = locationDetailsView.findViewById(R.id.locationName);
            TextView creatorName = locationDetailsView.findViewById(R.id.locationRating); // Change the ID if needed
            TextView locationDistance = locationDetailsView.findViewById(R.id.locationDistance);

            locationName.setText(name);

            List<String> creatorsList = new ArrayList<>();
            creatorsList.add(creator);

            for (int i = 0; i < locationDetailsContainer.getChildCount(); i++) {
                View childView = locationDetailsContainer.getChildAt(i);
                TextView locationNameTextView = childView.findViewById(R.id.locationName);
                TextView creatorNameTextView = childView.findViewById(R.id.locationRating); // Change the ID if needed

                String existingName = locationNameTextView.getText().toString();
                if (name.equals(existingName)) {
                    String existingCreator = creatorNameTextView.getText().toString();
                    creatorsList.add(existingCreator.replace("Creator: ", ""));
                    locationDetailsContainer.removeView(childView);
                    break;
                }
            }

            String creatorsText = TextUtils.join(", ", creatorsList);
            creatorName.setText("Creator: " + creatorsText); // Set the creator's names with a comma
            locationDistance.setText(String.format("%.2f km", roundedDistance)); // Display the rounded distance

            locationDetailsContainer.addView(locationDetailsView);
        }
    }

    private boolean isLocationDetailsAlreadyAdded(String name) {
        // Check if a location with the same name has already been added
        for (int i = 0; i < locationDetailsContainer.getChildCount(); i++) {
            View childView = locationDetailsContainer.getChildAt(i);
            TextView locationNameTextView = childView.findViewById(R.id.locationName);

            String existingName = locationNameTextView.getText().toString();
            if (name.equals(existingName)) {
                return true; // Location details already added
            }
        }
        return false; // Location details not added yet
    }


    private float roundDistance(float distance, int decimalPlaces) {
        float multiplier = (float) Math.pow(10, decimalPlaces);
        return Math.round(distance * multiplier) / multiplier;
    }

    private boolean isLocationDetailsAlreadyAdded(String name, String creator) {
        // Check if a location with the same name and creator has already been added
        for (int i = 0; i < locationDetailsContainer.getChildCount(); i++) {
            View childView = locationDetailsContainer.getChildAt(i);
            TextView locationNameTextView = childView.findViewById(R.id.locationName);
            TextView creatorNameTextView = childView.findViewById(R.id.locationRating); // Change the ID if needed

            String existingName = locationNameTextView.getText().toString();
            String existingCreator = creatorNameTextView.getText().toString();

            if (name.equals(existingName) && ("Creator: " + creator).equals(existingCreator)) {
                return true; // Location details already added
            }
        }
        return false; // Location details not added yet
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
                }
            } else {
                Toast.makeText(this, "Location permission denied. App cannot function properly.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_home) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (item.getItemId() == R.id.nav_settings) {
            startActivity(new Intent(MainActivity.this, FavCreator.class));
            finish();
        } else if (item.getItemId() == R.id.nav_share) {
            startActivity(new Intent(MainActivity.this, ReportBug.class));
            finish();
        }  else if (item.getItemId() == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, SendOTPActivity.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}