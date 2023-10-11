package com.example.dine2destiny;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class SendOTPActivity extends AppCompatActivity {
    private static final String TAG = "SendOTPActivity"; // Added for debugging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if the user is already signed in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, open the dashboard activity
            Log.d(TAG, "User is already signed in, opening MainActivity.");
            startActivity(new Intent(SendOTPActivity.this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_send_otpactivity);

        final EditText inputmobile = findViewById(R.id.inputmobile);
        Button getotp = findViewById(R.id.buttonGetOTP);
        final ProgressBar progressBar = findViewById(R.id.progressbar);

        getotp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mobileNumber = inputmobile.getText().toString();

                if (mobileNumber.isEmpty()) {
                    Toast.makeText(SendOTPActivity.this, "Enter Mobile Number", Toast.LENGTH_SHORT).show();
                } else if (mobileNumber.length() != 10) {
                    Toast.makeText(SendOTPActivity.this, "Enter a valid 10-digit Mobile Number", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Starting phone number verification for: " + mobileNumber);
                    progressBar.setVisibility(View.VISIBLE);
                    getotp.setVisibility(View.INVISIBLE);

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            "+91" + mobileNumber,
                            60,
                            TimeUnit.SECONDS,
                            SendOTPActivity.this,
                            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                @Override
                                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                    Log.d(TAG, "Phone number verification completed.");
                                    progressBar.setVisibility(View.GONE);
                                    getotp.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onVerificationFailed(@NonNull FirebaseException e) {
                                    Log.e(TAG, "Phone number verification failed: " + e.getMessage());
                                    progressBar.setVisibility(View.GONE);
                                    getotp.setVisibility(View.VISIBLE);
                                    Toast.makeText(SendOTPActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                    Log.d(TAG, "Verification code sent.");
                                    progressBar.setVisibility(View.GONE);
                                    getotp.setVisibility(View.VISIBLE);
                                    Intent intent = new Intent(getApplicationContext(), VerifyOTPActivity.class);
                                    intent.putExtra("mobile", mobileNumber);
                                    intent.putExtra("verificationId", verificationId);
                                    startActivity(intent);
                                }
                            }
                    );
                }
            }
        });
    }
}
