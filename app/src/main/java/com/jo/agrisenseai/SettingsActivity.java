package com.jo.agrisenseai;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * SettingsActivity
 *
 * Opened when the user taps the "Settings" card on the Profile screen.
 *
 * Displays three setting cards (same design as Profile screen):
 *   1. Dark Mode         — non-interactive placeholder (always checked)
 *   2. Voice Assistant   — persisted via VoicePreferenceManager
 *   3. Auto Speak        — persisted via VoicePreferenceManager
 *
 * The toolbar shows a ← back arrow that returns to ProfileFragment.
 */
public class SettingsActivity extends AppCompatActivity {

    private MaterialSwitch switchDarkMode;
    private MaterialSwitch switchVoice;
    private MaterialSwitch switchAutoSpeak;
    private MaterialSwitch switchAlertsMaster;

    private boolean isWaitingForPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ── Toolbar: back arrow + title ──────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        // ── Find switches ────────────────────────────────────────────────────
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchVoice = findViewById(R.id.switchVoiceEnabled);
        switchAutoSpeak = findViewById(R.id.switchAutoSpeak);
        switchAlertsMaster = findViewById(R.id.switchAlertsMaster);

        // Set initial checked state without firing listener
        switchDarkMode.setChecked(ThemePreferenceManager.isDarkMode(this));

        // ── Bind switch listeners ────────────────────────────────────────────
        switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            ThemePreferenceManager.setDarkMode(this, checked);
            if (checked) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        switchVoice.setOnCheckedChangeListener((btn, checked) ->
                VoicePreferenceManager.setVoiceEnabled(this, checked));

        switchAutoSpeak.setOnCheckedChangeListener((btn, checked) ->
                VoicePreferenceManager.setAutoSpeak(this, checked));

        switchAlertsMaster.setOnCheckedChangeListener(this::handleAlertsMasterSwitchChange);

        // ── Notification Settings card ───────────────────────────────────────
        MaterialCardView cardNotifSettings = findViewById(R.id.cardNotificationSettings);
        cardNotifSettings.setOnClickListener(v ->
                startActivity(new Intent(SettingsActivity.this, NotificationSettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load & synchronize preference values in onResume in case they were modified
        // in sub-pages (e.g. toggled inside NotificationSettingsActivity).
        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener(null);
            switchDarkMode.setChecked(ThemePreferenceManager.isDarkMode(this));
            switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
                ThemePreferenceManager.setDarkMode(this, checked);
                if (checked) {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                }
            });
        }
        if (switchVoice != null) {
            switchVoice.setChecked(VoicePreferenceManager.isVoiceEnabled(this));
        }
        if (switchAutoSpeak != null) {
            switchAutoSpeak.setChecked(VoicePreferenceManager.isAutoSpeak(this));
        }
        if (switchAlertsMaster != null) {
            boolean systemEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled();
            Log.d("NotificationDebug", "Notification status detected: enabled=" + systemEnabled);
            
            if (isWaitingForPermission && systemEnabled) {
                NotificationHelper.setEnabled(this, true);
                isWaitingForPermission = false;
            }

            boolean isChecked = NotificationHelper.isEnabled(this) && systemEnabled;
            Log.d("NotificationDebug", "Toggle state restored: switch checked=" + isChecked);

            switchAlertsMaster.setOnCheckedChangeListener(null);
            switchAlertsMaster.setChecked(isChecked);
            switchAlertsMaster.setEnabled(systemEnabled);
            switchAlertsMaster.setOnCheckedChangeListener(this::handleAlertsMasterSwitchChange);
        }
    }

    private void handleAlertsMasterSwitchChange(android.widget.CompoundButton buttonView, boolean checked) {
        if (checked) {
            if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                NotificationHelper.setEnabled(this, true);
            } else {
                syncSwitchState(false);
                isWaitingForPermission = true;
                openNotificationSettings();
            }
        } else {
            NotificationHelper.setEnabled(this, false);
        }
    }

    private void syncSwitchState(boolean checked) {
        if (switchAlertsMaster != null) {
            switchAlertsMaster.setOnCheckedChangeListener(null);
            switchAlertsMaster.setChecked(checked);
            switchAlertsMaster.setOnCheckedChangeListener(this::handleAlertsMasterSwitchChange);
        }
    }

    // Permission dialog is handled by startup checks now.

    private void openNotificationSettings() {
        Log.d("NotificationDebug", "Settings page opened");
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallback.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(fallback);
        }
    }

    /** Handles the ← back arrow tap in the toolbar. */
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
