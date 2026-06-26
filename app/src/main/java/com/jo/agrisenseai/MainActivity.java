package com.jo.agrisenseai;

// ── Standard Android imports ─────────────────────────────────────────────────
import android.content.Context;           // Needed for LocaleHelper and permissions
import android.content.Intent;            // Needed to open SettingsActivity
import android.content.pm.PackageManager; // Lets us check whether a permission is granted
import android.net.Uri;                   // Needed for the system-settings URI
import android.os.Build;                  // Lets us compare the device's Android version at runtime
import android.os.Bundle;                // Carries saved state into onCreate
import android.provider.Settings;         // ACTION_APPLICATION_DETAILS_SETTINGS constant
import android.util.Log;

// ── AndroidX / Jetpack imports ───────────────────────────────────────────────
import androidx.appcompat.app.AlertDialog;       // Rationale + permanent-deny dialogs
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;      // Version-safe permission checker
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

// ── Material UI ───────────────────────────────────────────────────────────────
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity — App entry point.
 *
 * Responsibilities in this file:
 *   1. Inflate activity_main layout + set up bottom navigation.
 *   2. Create the "AgriSense Alerts" notification channel (once).
 *   3. Request POST_NOTIFICATIONS runtime permission (Android 13+).
 *      Handles: first-time request, rationale dialog, permanent denial.
 *   4. Start FirebaseAlertManager — sensor-level alert watcher.
 *   5. Start AINotificationWatcher — AI-level alert watcher.
 *   6. Load the HomeFragment as the default screen.
 *
 * All notification logic (posting, preferences, channel) lives in
 * NotificationHelper. All sensor-alert logic lives in FirebaseAlertManager.
 * MainActivity only orchestrates startup.
 */
public class MainActivity extends AppCompatActivity {

    // =========================================================================
    //  NOTIFICATION PERMISSION — FIELD-LEVEL DECLARATION
    // =========================================================================

    private androidx.appcompat.app.AlertDialog reminderDialog;

    // =========================================================================
    //  LOCALE (existing — must stay first in the Activity lifecycle)
    // =========================================================================

    /**
     * Apply the saved locale before the layout inflates so all string
     * resources resolve to the correct language immediately.
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    // =========================================================================
    //  LIFECYCLE
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        // ── Step 1: Create notification channel ──────────────────────────────
        // Must be called before posting any notification.
        // Safe to call every launch — Android ignores duplicate channel creation.
        NotificationHelper.createChannel(this);



        // ── Step 3: Start sensor-level alert watcher ─────────────────────────
        // FirebaseAlertManager listens to sensorData/ in Firebase and fires
        // notifications for: low soil moisture, high temperature, water tank low.
        FirebaseAlertManager.start(this);

        // ── Step 4: Start AI-level alert watcher (existing) ──────────────────
        // Seeds demo data if missing, runs AI engine, starts dashboard watcher.
        FirebaseHelper.getInstance().seedDemoSensorDataIfMissing();
        FirebaseHelper.getInstance().runAIEngine();
        AINotificationWatcher.start(this);

        // ── Step 5: Load default fragment ────────────────────────────────────
        if (savedInstanceState == null && NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            loadFragment(new HomeFragment());
        }

        // ── Step 6: Bottom navigation listener ───────────────────────────────
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_farm) {
                selectedFragment = new MyFarmFragment();
            } else if (itemId == R.id.nav_assistant) {
                selectedFragment = new AIAssistantFragment();
            } else if (itemId == R.id.nav_insights) {
                selectedFragment = new InsightsFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("NotificationDebug", "App launched");
        checkAndShowNotificationReminder();
    }

    // =========================================================================
    //  FRAGMENT NAVIGATION
    // =========================================================================

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // =========================================================================
    //  NOTIFICATION PERMISSION — Full handling logic
    // =========================================================================

    /**
     * Checks if notifications are enabled for the application.
     * If they are disabled, shows a custom Material Design reminder dialog.
     */
    private void checkAndShowNotificationReminder() {
        Log.d("NotificationDebug", "Notification status checked");
        boolean enabled = NotificationManagerCompat.from(this).areNotificationsEnabled();
        Log.d("NotificationDebug", "Notifications enabled/disabled result: " + enabled);
        if (enabled) {
            if (reminderDialog != null && reminderDialog.isShowing()) {
                reminderDialog.dismiss();
            }
            // Load HomeFragment if it isn't loaded yet
            if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) == null) {
                loadFragment(new HomeFragment());
            }
        } else {
            showNotificationReminderDialog();
        }
    }

    /**
     * Displays a Material Design alert dialog prompting the user to allow notifications.
     */
    private void showNotificationReminderDialog() {
        runOnUiThread(() -> {
            if (reminderDialog != null && reminderDialog.isShowing()) {
                return;
            }
            Log.d("NotificationDebug", "Dialog displayed");
            reminderDialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("Notifications Required")
                    .setMessage("AgriSense AI requires notification permission to send irrigation alerts, AI recommendations and pump status updates.")
                    .setPositiveButton("Open Settings", (dialogInterface, which) -> {
                        Log.d("NotificationDebug", "Open Settings button clicked");
                        openNotificationSettings();
                    })
                    .setNegativeButton("Exit App", (dialogInterface, which) -> {
                        Log.d("NotificationDebug", "Exit App button clicked");
                        finish();
                    })
                    .setCancelable(false)
                    .create();

            reminderDialog.show();

            // Style Open Settings button: primary green color, bold text
            int greenColor = ContextCompat.getColor(MainActivity.this, R.color.primary_green);
            reminderDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(greenColor);
            reminderDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTypeface(null, android.graphics.Typeface.BOLD);

            // Style Exit App button: text secondary color, normal weight
            int grayColor = ContextCompat.getColor(this, R.color.text_secondary);
            reminderDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(grayColor);
            reminderDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTypeface(null, android.graphics.Typeface.NORMAL);
        });
    }

    /**
     * Opens the application's notification settings screen.
     */
    private void openNotificationSettings() {
        Log.d("NotificationDebug", "Notification settings opened");
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
            // Final fallback to Application Info Details
            Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            fallback.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(fallback);
        }
    }
}
