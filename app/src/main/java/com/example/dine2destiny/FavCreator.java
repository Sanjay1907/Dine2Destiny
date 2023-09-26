package com.example.dine2destiny;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.ProgressDialog;

public class FavCreator extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DatabaseReference databaseReference;
    private ListView creatorListView;
    private SearchView searchView;
    private List<String> creatorNames;
    private ArrayAdapter<String> adapter;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private ProgressDialog progressDialog;

    // Define a data structure to keep track of followed creators and their states
    private Map<String, Boolean> followedCreatorsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_creator);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching followers...");
        progressDialog.setCancelable(false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav,
                R.string.close_nav);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        mAuth = FirebaseAuth.getInstance();

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Creators");
        creatorNames = new ArrayList<>();
        creatorListView = findViewById(R.id.creatorListView);
        searchView = findViewById(R.id.searchView);
        adapter = new ArrayAdapter<>(this, R.layout.creator_item, R.id.creatorNameTextView, creatorNames);

        creatorListView.setAdapter(adapter);
        progressDialog.show();

        // Listen for changes in the Firebase Realtime Database
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String creatorName = dataSnapshot.child("name").getValue(String.class);
                creatorNames.add(creatorName);

                // Update the data structure with the initial state (not following)
                followedCreatorsMap.put(creatorName, false);

                adapter.notifyDataSetChanged();
                progressDialog.dismiss();
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
                progressDialog.dismiss();
            }
        });

        // Load the initial follow state when the data is loaded
        loadInitialFollowState();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);

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
                        } else {
                            followButton.setText("Follow");
                        }
                    }
                }

                return false;
            }
        });
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

    public void followCreator(View view) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            final String currentUserId = currentUser.getUid();
            View parentView = (View) view.getParent();
            TextView creatorNameTextView = parentView.findViewById(R.id.creatorNameTextView);
            final String creatorName = creatorNameTextView.getText().toString();

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
                Toast.makeText(FavCreator.this, "You unfollowed " + creatorName, Toast.LENGTH_SHORT).show();
            } else {
                userFavoritesRef.child(creatorName).setValue(true);
                followedCreatorsMap.put(creatorName, true);
                ((Button) view).setText("Following");
                Toast.makeText(FavCreator.this, "You are now following " + creatorName, Toast.LENGTH_SHORT).show();
            }
        } else {
            // Handle the case where the user is not signed in
            // You can redirect the user to the login screen or perform other actions.
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_home) {
            startActivity(new Intent(FavCreator.this, MainActivity.class));
            finish();
        } else if (item.getItemId() == R.id.nav_settings) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (item.getItemId() == R.id.nav_share) {
            startActivity(new Intent(FavCreator.this, ReportBug.class));
            finish();
        } else if (item.getItemId() == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, SendOTPActivity.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}