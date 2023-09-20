package com.example.dine2destiny;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
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

    private EditText in1, in2, in3, in4, in5, in6;
    private String verificationId;
    private Button resendOTP;
    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otpactivity);

        TextView textmobile = findViewById(R.id.textmobile);
        textmobile.setText(String.format(
                "+91-%s", getIntent().getStringExtra("mobile")
        ));
        in1 = findViewById(R.id.inputcode1);
        in2 = findViewById(R.id.inputcode2);
        in3 = findViewById(R.id.inputcode3);
        in4 = findViewById(R.id.inputcode4);
        in5 = findViewById(R.id.inputcode5);
        in6 = findViewById(R.id.inputcode6);

        sendOTPInputs();

        final ProgressBar progressBar = findViewById(R.id.progressbar);
        final Button verifyOTp = findViewById(R.id.verifyOTP);

        verificationId = getIntent().getStringExtra("verificationId");

        verifyOTp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (in1.getText().toString().trim().isEmpty()
                        || in2.getText().toString().trim().isEmpty()
                        || in3.getText().toString().trim().isEmpty()
                        || in4.getText().toString().trim().isEmpty()
                        || in5.getText().toString().trim().isEmpty()
                        || in6.getText().toString().trim().isEmpty()) {
                    Toast.makeText(VerifyOTPActivity.this, "Please enter the valid OTP", Toast.LENGTH_SHORT).show();
                    return;
                }

                String code =
                        in1.getText().toString() +
                                in2.getText().toString() +
                                in3.getText().toString() +
                                in4.getText().toString() +
                                in5.getText().toString() +
                                in6.getText().toString();

                if (verificationId != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    verifyOTp.setVisibility(View.INVISIBLE);
                    PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(
                            verificationId,
                            code
                    );
                    FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    progressBar.setVisibility(View.GONE);
                                    verifyOTp.setVisibility(View.VISIBLE);
                                    if (task.isSuccessful()) {
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(VerifyOTPActivity.this, "The OTP entered was invalid", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        resendOTP = findViewById(R.id.resendOTP);

        // Enable the Resend OTP button initially
        resendOTP.setEnabled(true);

        resendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Disable the button while sending the OTP
                resendOTP.setEnabled(false);

                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        "+91" + getIntent().getStringExtra("mobile"),
                        60,
                        TimeUnit.SECONDS,
                        VerifyOTPActivity.this,
                        new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                Toast.makeText(VerifyOTPActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String newverificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                verificationId = newverificationId;
                                Toast.makeText(VerifyOTPActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();

                                // Start the countdown timer
                                resendTimer.start();
                            }
                        }
                );
            }
        });

        // Initialize the countdown timer
        resendTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                resendOTP.setText(getString(R.string.resend_otp_in, secondsRemaining));

                // Enable the Resend OTP button when the timer finishes
                if (secondsRemaining <= 0) {
                    resendOTP.setEnabled(true);
                    resendOTP.setText(R.string.resend_otp);
                }
            }

            @Override
            public void onFinish() {
                // Enable the Resend OTP button when the timer finishes
                resendOTP.setEnabled(true);
                resendOTP.setText(R.string.resend_otp);
            }
        };

        // Start the countdown timer initially
        resendTimer.start();
    }

    private void sendOTPInputs() {
        in1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().trim().isEmpty()) {
                    in2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        in2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().trim().isEmpty()) {
                    in3.requestFocus();
                } else if (charSequence.length() == 0) {
                    in1.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        in3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().trim().isEmpty()) {
                    in4.requestFocus();
                } else if (charSequence.length() == 0) {
                    in2.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        in4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().trim().isEmpty()) {
                    in5.requestFocus();
                } else if (charSequence.length() == 0) {
                    in3.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        in5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().trim().isEmpty()) {
                    in6.requestFocus();
                } else if (charSequence.length() == 0) {
                    in4.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        in6.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!charSequence.toString().trim().isEmpty()) {
                    // Last EditText, do nothing when a character is entered
                } else if (charSequence.length() == 0) {
                    in5.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel the timer when the activity is destroyed to prevent memory leaks
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}