package com.jo.agrisenseai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * EmailLoginActivity — Standard Email and Password Login screen.
 */
public class EmailLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private TextView tvError;
    private MaterialButton btnLogin;
    private ProgressBar progressLogin;

    private FirebaseAuth mAuth;

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
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        progressLogin = findViewById(R.id.progressLogin);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvSignUpLink = findViewById(R.id.tvSignUpLink);

        // Listeners
        btnLogin.setOnClickListener(v -> handleLogin());
        
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        
        tvSignUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(EmailLoginActivity.this, EmailSignUpActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void handleLogin() {
        tvError.setVisibility(View.GONE);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvError.setText(R.string.error_invalid_email);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            tvError.setText("Password must be at least 6 characters");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        setLoadingState(true);

        mAuth.signInWithEmailAndPassword(email, password)
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
                        setLoadingState(false);
                        String errMsg = task.getException() != null ? task.getException().getMessage() : getString(R.string.error_auth_failed);
                        tvError.setText(errMsg);
                        tvError.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void handleForgotPassword() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Forgot Password")
                    .setMessage("Please enter a valid email address in the Email field first, then click Forgot Password.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        setLoadingState(true);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoadingState(false);
                    if (task.isSuccessful()) {
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Password Reset Sent")
                                .setMessage("A password reset link has been sent to:\n" + email + "\n\nPlease follow the link in the email to choose a new password.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        String errMsg = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Error")
                                .setMessage(errMsg)
                                .setPositiveButton("OK", null)
                                .show();
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
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        progressLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
