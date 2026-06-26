package com.jo.agrisenseai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

import java.util.Random;

/**
 * EmailLoginActivity — Futuristic, Glassmorphic Passwordless Email OTP Authentication.
 */
public class EmailLoginActivity extends AppCompatActivity {

    private EditText etEmail, etOtp;
    private TextView tvError;
    private MaterialButton btnLogin;
    private ProgressBar progressLogin;
    private LinearLayout layoutOtp;

    private FirebaseAuth mAuth;
    private String mGeneratedOtp = "";
    private boolean mOtpSent = false;
    private String mEmailString = "";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);

        mAuth = FirebaseAuth.getInstance();

        // Bind views
        etEmail = findViewById(R.id.etEmail);
        etOtp = findViewById(R.id.etOtp);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        progressLogin = findViewById(R.id.progressLogin);
        layoutOtp = findViewById(R.id.layoutOtp);

        btnLogin.setOnClickListener(v -> handleActionButtonClick());
    }

    private void handleActionButtonClick() {
        tvError.setVisibility(View.GONE);

        if (!mOtpSent) {
            sendEmailOtp();
        } else {
            verifyOtpAndSignIn();
        }
    }

    private void sendEmailOtp() {
        mEmailString = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(mEmailString) || !Patterns.EMAIL_ADDRESS.matcher(mEmailString).matches()) {
            tvError.setText(R.string.error_invalid_email);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        setLoadingState(true);

        // Simulate sending network delay (600ms)
        new android.os.Handler().postDelayed(() -> {
            setLoadingState(false);

            // Generate 6-digit OTP code
            int code = 100000 + new Random().nextInt(900000);
            mGeneratedOtp = String.valueOf(code);
            mOtpSent = true;

            // Display simulated OTP inside a premium glassmorphic style dialog
            new MaterialAlertDialogBuilder(this)
                    .setTitle("AgriSense Security Code")
                    .setMessage("A verification code was sent to:\n" + mEmailString + "\n\nOTP Code: " + mGeneratedOtp + "\n\n(Simulated for test environments)")
                    .setPositiveButton("Close", null)
                    .show();

            // Toggle OTP input fields visible
            etEmail.setEnabled(false);
            layoutOtp.setVisibility(View.VISIBLE);
            btnLogin.setText("Verify & Sign In");
        }, 600);
    }

    private void verifyOtpAndSignIn() {
        String enteredOtp = etOtp.getText() != null ? etOtp.getText().toString().trim() : "";

        if (TextUtils.isEmpty(enteredOtp) || enteredOtp.length() < 6) {
            tvError.setText("Please enter the 6-digit verification code");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        if (!enteredOtp.equals(mGeneratedOtp)) {
            tvError.setText("Invalid OTP code. Please check and try again.");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        setLoadingState(true);

        // Derive secure unique password for the email
        String derivedPassword = mEmailString.toLowerCase() + "_agrisense_secure";

        // Attempt login
        mAuth.signInWithEmailAndPassword(mEmailString, derivedPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserProfile(user.getUid());
                        } else {
                            setLoadingState(false);
                            tvError.setText(R.string.error_auth_failed);
                            tvError.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // If user doesn't exist, automatically sign them up
                        if (task.getException() instanceof FirebaseAuthInvalidUserException || 
                           (task.getException() != null && task.getException().getMessage() != null && 
                            task.getException().getMessage().contains("no user record"))) {
                            
                            signUpNewUser(mEmailString, derivedPassword);
                        } else {
                            setLoadingState(false);
                            String errMsg = task.getException() != null ? task.getException().getMessage() : getString(R.string.error_auth_failed);
                            tvError.setText(errMsg);
                            tvError.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void signUpNewUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoadingState(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Redirect new user directly to Farmer Registration screen
                            Intent intent = new Intent(EmailLoginActivity.this, FarmerRegistrationActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        }
                    } else {
                        String errMsg = task.getException() != null ? task.getException().getMessage() : "Failed to register new account";
                        tvError.setText(errMsg);
                        tvError.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void checkUserProfile(String uid) {
        FirebaseHelper.getInstance().getUserProfile(uid, profile -> {
            setLoadingState(false);
            Intent intent;
            if (profile != null && profile.getName() != null && !profile.getName().isEmpty()) {
                intent = new Intent(EmailLoginActivity.this, MainActivity.class);
            } else {
                intent = new Intent(EmailLoginActivity.this, FarmerRegistrationActivity.class);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setLoadingState(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        etOtp.setEnabled(!isLoading);
        progressLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
