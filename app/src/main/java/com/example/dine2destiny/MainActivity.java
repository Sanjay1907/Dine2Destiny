package com.example.dine2destiny;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private SupportMapFragment mapFragment;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;
    private boolean cameraMoved = false;
    private LinearLayout locationDetailsContainer;
    private boolean locationDetailsLoaded = false; // Flag to control location details loading

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkUserName();

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Creators");

        locationDetailsContainer = findViewById(R.id.locationDetailsContainer);
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
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    Location lastLocation = locationResult.getLastLocation();
                    LatLng userLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

                    if (!cameraMoved) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
                    }

                    if (!locationDetailsLoaded) {
                        locationDetailsContainer.removeAllViews();

                        valueEventListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot creatorSnapshot : dataSnapshot.getChildren()) {
                                    DataSnapshot recommendationSnapshot = creatorSnapshot.child("recommendation");
                                    String creatorName = creatorSnapshot.child("name").getValue(String.class);
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

                                        // Check if the location is open
                                        String timings = recSnapshot.child("timings").getValue(String.class);
                                        if (distanceResult[0] <= 5000 && isLocationOpen(timings)) {
                                            String name = recSnapshot.child("name").getValue(String.class);
                                            double rating = recSnapshot.child("rating").getValue(Double.class);
                                            String snippet = "Rating: " + String.format("%.1f/5", rating);

                                            MarkerOptions markerOptions = new MarkerOptions()
                                                    .position(locationLatLng)
                                                    .title(name)
                                                    .snippet(snippet);
                                            mMap.addMarker(markerOptions);

                                            addLocationDetails(name, creatorName, distanceResult[0]);
                                        }
                                    }
                                }
                                locationDetailsLoaded = true;
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // Handle database error
                            }
                        };

                        databaseReference.addListenerForSingleValueEvent(valueEventListener);
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
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

        // You can also update the user's name in your app UI if needed
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

    private void addLocationDetails(String name, String creator, float distance) {
        View locationDetailsView = getLayoutInflater().inflate(R.layout.location_details, null);

        TextView locationName = locationDetailsView.findViewById(R.id.locationName);
        TextView creatorName = locationDetailsView.findViewById(R.id.locationRating); // Change the ID if needed
        TextView locationDistance = locationDetailsView.findViewById(R.id.locationDistance);

        locationName.setText(name);
        creatorName.setText("Creator: " + creator); // Set the creator's name correctly
        locationDistance.setText(String.format("%.2f km", distance / 1000));

        locationDetailsContainer.addView(locationDetailsView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
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
                    fusedLocationClient.requestLocationUpdates(new LocationRequest(), locationCallback, null);
                }
            } else {
                Toast.makeText(this, "Location permission denied. App cannot function properly.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
