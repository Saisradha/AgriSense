package com.jo.agrisenseai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * DeviceCheckActivity — Guides user to link their IoT hardware or use Simulated Demo Mode.
 */
public class DeviceCheckActivity extends AppCompatActivity {

    private MaterialButton btnLinkDevice, btnStartDemo;
    private ProgressBar progress;

    private FirebaseAuth mAuth;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_check);

        mAuth = FirebaseAuth.getInstance();

        btnLinkDevice = findViewById(R.id.btnLinkDevice);
        btnStartDemo = findViewById(R.id.btnStartDemo);
        progress = findViewById(R.id.progressDeviceCheck);

        btnLinkDevice.setOnClickListener(v -> updateDeviceState(true, false));
        btnStartDemo.setOnClickListener(v -> updateDeviceState(false, true));
    }

    private void updateDeviceState(boolean hasDevice, boolean isDemoMode) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLinkDevice.setEnabled(false);
        btnStartDemo.setEnabled(false);
        progress.setVisibility(View.VISIBLE);

        String uid = user.getUid();
        FirebaseHelper.getInstance().getUserProfile(uid, profile -> {
            if (profile == null) {
                // If profile is missing for some reason, create a dummy one
                profile = new UserProfile(
                        uid,
                        "Farmer",
                        user.getPhoneNumber(),
                        "Unknown",
                        "Unknown",
                        "Unknown",
                        hasDevice,
                        isDemoMode,
                        System.currentTimeMillis()
                );
            } else {
                profile.setHasDevice(hasDevice);
                profile.setDemoMode(isDemoMode);
            }

            // Save updated profile
            FirebaseHelper.getInstance().saveUserProfile(profile, (error, ref) -> {
                progress.setVisibility(View.GONE);
                btnLinkDevice.setEnabled(true);
                btnStartDemo.setEnabled(true);

                if (error == null) {
                    Intent intent = new Intent(DeviceCheckActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } else {
                    Toast.makeText(DeviceCheckActivity.this, "Failed to update preference: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
