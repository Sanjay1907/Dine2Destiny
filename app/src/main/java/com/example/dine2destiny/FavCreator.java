package com.example.dine2destiny;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
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
import java.util.List;
import androidx.appcompat.app.AppCompatActivity;

public class FavCreator extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private DatabaseReference databaseReference;
    private ListView creatorListView;
    private SearchView searchView;
    private List<String> creatorNames;
    private ArrayAdapter<String> adapter;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fav_creator);

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

        // Listen for changes in the Firebase Realtime Database
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String creatorName = dataSnapshot.child("name").getValue(String.class);
                creatorNames.add(creatorName);
                adapter.notifyDataSetChanged();
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
                // Handle database errors if needed
            }
        });

        // Load the initial state of the follow button
        loadInitialFollowState();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    private void filterCreators(String searchText) {
        List<String> filteredList = new ArrayList<>();
        for (String creatorName : creatorNames) {
            if (creatorName.toLowerCase().contains(searchText.toLowerCase())) {
                filteredList.add(creatorName);
            }
        }
        adapter.clear();
        adapter.addAll(filteredList);
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
                    for (String creatorName : creatorNames) {
                        if (dataSnapshot.hasChild(creatorName)) {
                            // Creator is in favorites, so set the button text to "Following"
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

    // Set a click listener for the "Follow" button in your creator_item.xml
    public void followCreator(View view) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            final String currentUserId = currentUser.getUid();
            View parentView = (View) view.getParent();
            TextView creatorNameTextView = parentView.findViewById(R.id.creatorNameTextView);
            final String creatorName = creatorNameTextView.getText().toString();

            // Get a reference to the current user's favorites in the database
            DatabaseReference userFavoritesRef = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Users")
                    .child(currentUserId)
                    .child("fav-creators");

            // Add or remove the creator's name from the user's favorites based on the current state
            if ("Following".equals(((Button) view).getText())) {
                // If the button text is "Following," remove the creator from favorites
                userFavoritesRef.child(creatorName).removeValue();
                ((Button) view).setText("Follow");
                Toast.makeText(FavCreator.this, "You unfollowed  " + creatorName, Toast.LENGTH_SHORT).show();
            } else {
                // If the button text is not "Following," add the creator to favorites
                userFavoritesRef.child(creatorName).setValue(true);
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
        }  else if (item.getItemId() == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, SendOTPActivity.class));
            finish();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    @Override
    public void onBackPressed() {
        // Create an intent to navigate back to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Finish the current activity to remove it from the back stack
    }
}
