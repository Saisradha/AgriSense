package com.jo.agrisenseai;

import android.content.Context;
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
 * NotificationSettingsActivity
 *
 * Dedicated screen for configuring notification alerts preferences.
 * Provides two toggles:
 *   1. Enable Alerts      - Master toggle to filter/block system tray alerts
 *   2. Notification Sound - Switch to play sound or keep alerts silent
 */
public class NotificationSettingsActivity extends AppCompatActivity {

    private MaterialCardView cardNotificationSound;
    private MaterialSwitch switchAlertsEnabled;
    private MaterialSwitch switchNotificationSound;

    private boolean isWaitingForPermission = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        // ── Toolbar setup ────────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbarNotifSettings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }

        // ── Find views ───────────────────────────────────────────────────────
        cardNotificationSound = findViewById(R.id.cardNotificationSound);
        switchAlertsEnabled = findViewById(R.id.switchAlertsEnabled);
        switchNotificationSound = findViewById(R.id.switchNotificationSound);

        // Set sound toggle preference states & listener
        switchNotificationSound.setChecked(NotificationHelper.isSoundEnabled(this));
        switchNotificationSound.setOnCheckedChangeListener((btn, checked) -> {
            NotificationHelper.setSoundEnabled(NotificationSettingsActivity.this, checked);
        });
    }

    /** Helper to update the visual state of the sound options card based on the master toggle. */
    private void updateSoundCardState(boolean alertsEnabled) {
        switchNotificationSound.setEnabled(alertsEnabled);
        cardNotificationSound.setAlpha(alertsEnabled ? 1.0f : 0.5f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean systemEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled();

        if (isWaitingForPermission && systemEnabled) {
            NotificationHelper.setEnabled(this, true);
            isWaitingForPermission = false;
        }

        boolean alertsEnabled = NotificationHelper.isEnabled(this) && systemEnabled;

        switchAlertsEnabled.setOnCheckedChangeListener(null);
        switchAlertsEnabled.setChecked(alertsEnabled);
        switchAlertsEnabled.setEnabled(systemEnabled);
        switchAlertsEnabled.setOnCheckedChangeListener(this::handleAlertsEnabledSwitchChange);

        updateSoundCardState(alertsEnabled);
    }

    private void handleAlertsEnabledSwitchChange(android.widget.CompoundButton buttonView, boolean checked) {
        if (checked) {
            if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                NotificationHelper.setEnabled(this, true);
                updateSoundCardState(true);
            } else {
                syncSwitchState(false);
                updateSoundCardState(false);
                isWaitingForPermission = true;
                openNotificationSettings();
            }
        } else {
            NotificationHelper.setEnabled(this, false);
            updateSoundCardState(false);
        }
    }

    private void syncSwitchState(boolean checked) {
        if (switchAlertsEnabled != null) {
            switchAlertsEnabled.setOnCheckedChangeListener(null);
            switchAlertsEnabled.setChecked(checked);
            switchAlertsEnabled.setOnCheckedChangeListener(this::handleAlertsEnabledSwitchChange);
        }
    }

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

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
