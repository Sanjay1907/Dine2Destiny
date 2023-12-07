package com.example.dine2destiny;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class VerifyOTPActivity extends AppCompatActivity {
    private static final String TAG = "VerifyOTPActivity"; // Added for debugging

    private EditText otpInput;
    private String verificationId;
    private Button resendOTP;
    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otpactivity);

        TextView textmobile = findViewById(R.id.textmobile);
        textmobile.setText(String.format("+91-%s", getIntent().getStringExtra("mobile")));

        otpInput = findViewById(R.id.otpInput);

        sendOTPInput();

        final ProgressBar progressBar = findViewById(R.id.progressbar);
        final Button verifyOTP = findViewById(R.id.verifyOTP);

        final String otpFromIntent = getIntent().getStringExtra("otp");

        verifyOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = otpInput.getText().toString().trim();

                if (code.isEmpty()) {
                    Toast.makeText(VerifyOTPActivity.this, "Please enter a valid OTP", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (otpFromIntent != null && code.equals(otpFromIntent)) {
                    // OTP verification successful

                    progressBar.setVisibility(View.VISIBLE);
                    verifyOTP.setVisibility(View.INVISIBLE);

                    final String mobileNumber = getIntent().getStringExtra("mobile");

                    // Check if the phone number exists in the database
                    DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
                    usersRef.orderByChild("phoneNumber").equalTo(mobileNumber)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        // Phone number exists, log in with the existing user ID
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            String userId = snapshot.getKey();
                                            // Log in with existing user ID
                                            signInWithUserId(userId);
                                            return;
                                        }
                                    } else {
                                        // Phone number doesn't exist, create a new user ID
                                        createNewUser(mobileNumber);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Log.e(TAG, "Database error: " + databaseError.getMessage());
                                    Toast.makeText(VerifyOTPActivity.this, "Database error occurred", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    // Incorrect OTP entered
                    Toast.makeText(VerifyOTPActivity.this, "The OTP entered was invalid", Toast.LENGTH_SHORT).show();
                }
            }
        });

        resendOTP = findViewById(R.id.resendOTP);
        resendOTP.setEnabled(true);

        resendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resendOTP.setEnabled(false);

                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        "+91" + getIntent().getStringExtra("mobile"),
                        60,
                        TimeUnit.SECONDS,
                        VerifyOTPActivity.this,
                        new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                Log.d(TAG, "Phone number verification completed.");
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Log.e(TAG, "Phone number verification failed: " + e.getMessage());
                                Toast.makeText(VerifyOTPActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String newVerificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                verificationId = newVerificationId;
                                Log.d(TAG, "Verification code sent.");
                                Toast.makeText(VerifyOTPActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                                resendTimer.start();
                            }
                        }
                );
            }
        });

        resendTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                resendOTP.setText(getString(R.string.resend_otp_in, secondsRemaining));

                if (secondsRemaining <= 0) {
                    resendOTP.setEnabled(true);
                    resendOTP.setText(R.string.resend_otp);
                }
            }

            @Override
            public void onFinish() {
                resendOTP.setEnabled(true);
                resendOTP.setText(R.string.resend_otp);
            }
        };

        resendTimer.start();
    }

    private void sendOTPInput() {
        otpInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // You can implement any specific logic here if needed.
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }
    // Helper method to sign in with existing user ID
    private void signInWithUserId(String userId) {
        // Sign in with the existing user ID
        Log.d(TAG, "Signing in with existing user ID: " + userId);

        // Here, you will sign in the user with the provided userId.
        // Use Firebase Authentication to sign in the user based on their UID.
        // You may use signInWithCustomToken or other appropriate methods based on your authentication flow.

        // For example, signing in with Firebase Auth using the UID:
        FirebaseAuth.getInstance().signInWithCustomToken(userId)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User signed in successfully with UID: " + userId);
                            // Start the MainActivity after successful sign-in
                            startMainActivity();
                        } else {
                            // If sign-in fails, log the error message
                            Exception exception = task.getException();
                            if (exception != null) {
                                Log.e(TAG, "Failed to sign in with UID: " + userId + ", Reason: " + exception.getMessage());
                            } else {
                                Log.e(TAG, "Failed to sign in with UID: " + userId + ", Reason unknown");
                            }
                            Toast.makeText(VerifyOTPActivity.this, "Failed to sign in", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Helper method to start MainActivity after successful sign-in
    private void startMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Optional: Finish the current activity if not needed anymore
    }


    // Helper method to create a new user in Firebase database
    private void createNewUser(String mobileNumber) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        String userId = usersRef.push().getKey(); // Generate a new user ID

        if (userId != null) {
            usersRef.child(userId).child("phoneNumber").setValue(mobileNumber);
            Log.e(TAG, "New user created with ID: " + userId);
            // Handle the new user creation as needed in your app
            // For example: You might want to log in this new user here
            signInWithUserId(userId);
        } else {
            Log.e(TAG, "Failed to create a new user.");
            Toast.makeText(VerifyOTPActivity.this, "Failed to create a new user", Toast.LENGTH_SHORT).show();
        }
    }
    private void savePhoneNumberInDatabase(String userId) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        DatabaseReference userReference = databaseReference.child(userId);

        String phoneNumber = getIntent().getStringExtra("mobile");

        userReference.child("phoneNumber").setValue(phoneNumber);
        Log.d(TAG, "Phone number saved in the database.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}
