package com.example.dine2destiny;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ReportBug extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 1;
    private static final String TAG = "ReportBugActivity"; // Added for debugging
    private EditText bugDescriptionEditText;
    private Button attachMediaButton;
    private Button submitBugButton;
    private TextView attachedMediaTextView;
    private FirebaseAuth mAuth;
    private DatabaseReference bugReportsRef;
    private StorageReference storageRef;
    private Uri mediaUri;
    private ProgressDialog progressDialog;
    private Dialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_bug);

        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        bugReportsRef = database.getReference("Users")
                .child(mAuth.getCurrentUser().getUid())
                .child("report_bug");

        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        bugDescriptionEditText = findViewById(R.id.bugDescriptionEditText);
        attachMediaButton = findViewById(R.id.attachMediaButton);
        submitBugButton = findViewById(R.id.submitBugButton);
        attachedMediaTextView = findViewById(R.id.attachedMediaTextView);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Reporting Bug...");
        progressDialog.setCancelable(false);
        dialog = new Dialog(ReportBug.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_wait1);
        dialog.setCanceledOnTouchOutside(false);

        attachMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the gallery for image selection
                openGallery();
            }
        });

        submitBugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String bugDescription = bugDescriptionEditText.getText().toString();
                if (!bugDescription.isEmpty()) {
                    // Show progress dialog while reporting the bug
                    dialog.show();

                    // Generate a unique ID for the bug report
                    final String reportId = bugReportsRef.push().getKey();

                    // Check if mediaUri (the URI of the attached media) is not null
                    if (mediaUri != null) {
                        // Upload media to Firebase Storage
                        final StorageReference mediaStorageRef = storageRef.child("bug_report")
                                .child(reportId + "_" + mAuth.getCurrentUser().getUid());

                        mediaStorageRef.putFile(mediaUri)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        // Get the download URL for the uploaded media
                                        mediaStorageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri mediaDownloadUrl) {
                                                // Create a BugReport object with the download URL
                                                BugReport bugReport = new BugReport(reportId, bugDescription, mediaDownloadUrl.toString());

                                                // Save the bug report in the Realtime Database
                                                bugReportsRef.child(reportId).setValue(bugReport);

                                                // Dismiss the progress dialog
                                                dialog.dismiss();

                                                // Show a toast message for successful submission
                                                Toast.makeText(ReportBug.this, "Bug reported successfully", Toast.LENGTH_SHORT).show();

                                                // Navigate to the main activity
                                                Intent intent = new Intent(ReportBug.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Handle media upload failure
                                        dialog.dismiss();
                                        Toast.makeText(ReportBug.this, "Media upload failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // If no media is attached, save the bug report with an empty media URL
                        BugReport bugReport = new BugReport(reportId, bugDescription, "");

                        // Save the bug report in the Realtime Database
                        bugReportsRef.child(reportId).setValue(bugReport);

                        // Dismiss the progress dialog
                        dialog.dismiss();

                        // Show a toast message for successful submission
                        Toast.makeText(ReportBug.this, "Bug reported successfully", Toast.LENGTH_SHORT).show();

                        // Navigate to the main activity
                        Intent intent = new Intent(ReportBug.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    // Handle empty bug report description
                    Toast.makeText(ReportBug.this, "Please enter a bug description", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            mediaUri = data.getData();

            // Display the selected image file name near the button
            String fileName = getFileName(mediaUri);
            attachedMediaTextView.setText(fileName);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
    @Override
    public void onBackPressed() {
        // Create an intent to navigate back to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Finish the current activity to remove it from the back stack
    }
}
