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

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            NotificationHelper.setEnabled(NotificationSettingsActivity.this, true);
                            syncSwitchState(true);
                            updateSoundCardState(true);
                        } else {
                            NotificationHelper.setEnabled(NotificationSettingsActivity.this, false);
                            syncSwitchState(false);
                            updateSoundCardState(false);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (!shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                                    showNotificationReminderDialog();
                                }
                            }
                        }
                    }
            );

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

        // ── Load & set current preferences states ────────────────────────────
        boolean systemEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled();
        Log.d("NotificationDebug", "Notification status detected: enabled=" + systemEnabled);
        boolean alertsEnabled = NotificationHelper.isEnabled(this) && systemEnabled;
        Log.d("NotificationDebug", "Toggle state restored: switch checked=" + alertsEnabled);
        boolean soundEnabled = NotificationHelper.isSoundEnabled(this);

        switchAlertsEnabled.setChecked(alertsEnabled);
        switchNotificationSound.setChecked(soundEnabled);
        updateSoundCardState(alertsEnabled);

        // ── Toggle Listeners ─────────────────────────────────────────────────
        switchAlertsEnabled.setOnCheckedChangeListener(this::handleAlertsEnabledSwitchChange);

        switchNotificationSound.setOnCheckedChangeListener((btn, checked) -> {
            NotificationHelper.setSoundEnabled(NotificationSettingsActivity.this, checked);
        });
    }

    /** Helper to update the visual state of the sound options card based on the master toggle. */
    private void updateSoundCardState(boolean alertsEnabled) {
        switchNotificationSound.setEnabled(alertsEnabled);
        cardNotificationSound.setAlpha(alertsEnabled ? 1.0f : 0.5f);
    }

    private void handleAlertsEnabledSwitchChange(android.widget.CompoundButton buttonView, boolean checked) {
        if (checked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
                    NotificationHelper.setEnabled(this, true);
                    updateSoundCardState(true);
                } else {
                    syncSwitchState(false);
                    updateSoundCardState(false);
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                    NotificationHelper.setEnabled(this, true);
                    updateSoundCardState(true);
                } else {
                    syncSwitchState(false);
                    updateSoundCardState(false);
                    showNotificationReminderDialog();
                }
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

    private void showNotificationReminderDialog() {
        Log.d("NotificationDebug", "Dialog displayed");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("ALLOW NOTIFICATIONS")
                .setMessage("Enable notifications to receive important farm alerts such as:\n" +
                        "• Low Soil Moisture\n" +
                        "• High Temperature\n" +
                        "• Low Water Tank Level")
                .setPositiveButton("ALLOW", (dialogInterface, which) -> {
                    Log.d("NotificationDebug", "ALLOW button clicked");
                    openNotificationSettings();
                })
                .setNegativeButton("DON'T ALLOW", (dialogInterface, which) -> {
                    Log.d("NotificationDebug", "DON'T ALLOW button clicked");
                    dialogInterface.dismiss();
                })
                .setCancelable(false)
                .create();

        dialog.show();

        int greenColor = ContextCompat.getColor(this, R.color.primary_green);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(greenColor);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTypeface(null, android.graphics.Typeface.BOLD);

        int grayColor = ContextCompat.getColor(this, R.color.text_secondary);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(grayColor);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTypeface(null, android.graphics.Typeface.NORMAL);
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
