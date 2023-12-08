package com.example.dine2destiny;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.ProgressDialog;
import android.util.Log;

public class FavCreator extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "FavCreator"; // Tag for logging
    private DatabaseReference databaseReference;
    private ListView creatorListView;
    private SearchView searchView;
    private List<Pair<Pair<String, String>, Pair<Integer, String>>> creatorNames = new ArrayList<>();
    private CreatorPairAdapter adapter;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private ProgressDialog progressDialog;
    private Map<String, Boolean> followedCreatorsMap = new HashMap<>();
    private Dialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_creator);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching followers...");
        progressDialog.setCancelable(false);
        dialog = new Dialog(FavCreator.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_wait1);
        dialog.setCanceledOnTouchOutside(false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav,
                R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.black));
        toggle.getDrawerArrowDrawable().setBarThickness(10);
        toggle.getDrawerArrowDrawable().setBarLength(50);
        toggle.syncState();

        mAuth = FirebaseAuth.getInstance();
        updateUserNameInNavigationHeader();

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Creators");
        creatorNames = new ArrayList<>();
        creatorListView = findViewById(R.id.creatorListView);
        searchView = findViewById(R.id.searchView);

        Log.d(TAG, "onCreate: Initializing...");
        adapter = new CreatorPairAdapter(this, creatorNames);
        creatorListView.setAdapter(adapter);
        dialog.show();

        // Listen for changes in the Firebase Realtime Database
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String creatorName = dataSnapshot.child("name").getValue(String.class);
                String creatorName2 = dataSnapshot.child("name2").getValue(String.class);
                String verificationStatusString = dataSnapshot.child("request_verification")
                        .child(dataSnapshot.getKey())
                        .child("verification")
                        .getValue(String.class);
                int verificationStatus = 0;

                if (verificationStatusString != null && !verificationStatusString.isEmpty()) {
                    try {
                        verificationStatus = Integer.parseInt(verificationStatusString);
                    } catch (NumberFormatException e) {
                    }
                }
                String profileImageUrl = dataSnapshot.child("profileImage").getValue(String.class);
                creatorNames.add(new Pair<>(new Pair<>(creatorName, creatorName2),
                        new Pair<>(verificationStatus, profileImageUrl)));
                // Update the data structure with the initial state (not following)
                followedCreatorsMap.put(creatorName, false);

                adapter.notifyDataSetChanged();
                updateButtonStatus();
                dialog.dismiss();

                Log.d(TAG, "onChildAdded: Creator added - " + creatorName);
                loadInitialFollowState();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                // Empty implementation if you don't need to handle changes
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Handle removal of data if needed
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                // Handle data movement if needed
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                dialog.dismiss();
                Log.e(TAG, "onCancelled: Database error - " + databaseError.getMessage());
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                updateButtonStatus();
                return false;
            }
        });
    }
    private void updateButtonStatus(){
        // Update button states for filtered items
        for (int i = 0; i < creatorListView.getCount(); i++) {
            View view = creatorListView.getChildAt(i);
            if (view != null) {
                TextView creatorNameTextView = view.findViewById(R.id.creatorNameTextView);
                Button followButton = view.findViewById(R.id.followButton);
                String creatorName = creatorNameTextView.getText().toString();

                boolean isFollowing = followedCreatorsMap.get(creatorName);
                if (isFollowing) {
                    followButton.setText("Following");
                    followButton.setBackgroundTintList(getResources().getColorStateList(R.color.following));
                    followButton.setTextColor(getResources().getColorStateList(R.color.black));
                } else {
                    followButton.setText("Follow");
                    followButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimaryDark));
                    followButton.setTextColor(getResources().getColorStateList(R.color.white));
                }
            }
        }
    }
    private void loadInitialFollowState() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
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
                    for (int i = 0; i < creatorListView.getChildCount(); i++) {
                        View view = creatorListView.getChildAt(i);
                        TextView creatorNameTextView = view.findViewById(R.id.creatorNameTextView);
                        Button followButton = view.findViewById(R.id.followButton);
                        String creatorName = creatorNameTextView.getText().toString();

                        if (dataSnapshot.hasChild(creatorName)) {
                            // Update the data structure with the "Following" state
                            followedCreatorsMap.put(creatorName, true);
                            setButtonToFollowing(creatorName);
                            followButton.setBackgroundTintList(getResources().getColorStateList(R.color.following));
                            followButton.setTextColor(getResources().getColorStateList(R.color.black));// Black tint
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle database errors if needed
                }
            });
        }
    }

    private void setButtonToFollowing(String creatorName) {
        for (int i = 0; i < creatorListView.getChildCount(); i++) {
            View view = creatorListView.getChildAt(i);
            TextView creatorNameTextView = view.findViewById(R.id.creatorNameTextView);
            Button followButton = view.findViewById(R.id.followButton);

            if (creatorNameTextView.getText().toString().equals(creatorName)) {
                followButton.setText("Following");
                break;
            }
        }
    }
    private void updateUserNameInNavigationHeader() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();

            DatabaseReference usersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId);
            usersReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild("name")) {
                        String userName = dataSnapshot.child("name").getValue(String.class);
                        if (userName != null) {
                            TextView userNameTextView = findViewById(R.id.userGreeting);
                            userNameTextView.setText("Hello, " + userName);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle database error
                }
            });
        }
    }
    public void followCreator(View view) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            final String currentUserId = currentUser.getUid();
            View parentView = (View) view.getParent();
            TextView creatorNameTextView = parentView.findViewById(R.id.creatorNameTextView);
            final String creatorName = creatorNameTextView.getText().toString();
            Button followButton = (Button) view;
            DatabaseReference userFavoritesRef = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Users")
                    .child(currentUserId)
                    .child("fav-creators");

            boolean isFollowing = followedCreatorsMap.get(creatorName);

            if (isFollowing) {
                userFavoritesRef.child(creatorName).removeValue();
                followedCreatorsMap.put(creatorName, false);
                ((Button) view).setText("Follow");
                followButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimaryDark));
                followButton.setTextColor(getResources().getColorStateList(R.color.white));
                Toast.makeText(FavCreator.this, "You unfollowed " + creatorName, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "followCreator: Unfollowed - " + creatorName);
            } else {
                userFavoritesRef.child(creatorName).setValue(true);
                followedCreatorsMap.put(creatorName, true);
                ((Button) view).setText("Following");
                followButton.setBackgroundTintList(getResources().getColorStateList(R.color.following));
                followButton.setTextColor(getResources().getColorStateList(R.color.black));
                Toast.makeText(FavCreator.this, "You are now following " + creatorName, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "followCreator: Followed - " + creatorName);
            }
        } else {
            // Handle the case where the user is not signed in
            // You can redirect the user to the login screen or perform other actions.
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_home) {
            Log.d(TAG, "Navigation: Home selected");
            startActivity(new Intent(FavCreator.this, MainActivity.class));
            finish();
        } else if (item.getItemId() == R.id.nav_settings) {
            Log.d(TAG, "Navigation: Settings selected");
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (item.getItemId() == R.id.nav_share) {
            Log.d(TAG, "Navigation: Share selected");
            startActivity(new Intent(FavCreator.this, ReportBug.class));
            finish();
        } else if (item.getItemId() == R.id.nav_logout) {
            Log.d(TAG, "Navigation: Logout selected");
            showLogoutConfirmationDialog();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            // User clicked Yes, log out
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(FavCreator.this, SendOTPActivity.class));
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
        Log.d(TAG, "Back button pressed");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}