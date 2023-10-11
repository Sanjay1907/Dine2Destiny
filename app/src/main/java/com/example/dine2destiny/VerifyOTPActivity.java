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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

        verificationId = getIntent().getStringExtra("verificationId");

        verifyOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = otpInput.getText().toString().trim();

                if (code.isEmpty()) {
                    Toast.makeText(VerifyOTPActivity.this, "Please enter a valid OTP", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (verificationId != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    verifyOTP.setVisibility(View.INVISIBLE);
                    PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(
                            verificationId,
                            code
                    );
                    FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    progressBar.setVisibility(View.GONE);
                                    verifyOTP.setVisibility(View.VISIBLE);
                                    if (task.isSuccessful()) {
                                        savePhoneNumberInDatabase(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                        Log.d(TAG, "Phone number verification successful.");
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    } else {
                                        Log.e(TAG, "Phone number verification failed.");
                                        Toast.makeText(VerifyOTPActivity.this, "The OTP entered was invalid", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
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
