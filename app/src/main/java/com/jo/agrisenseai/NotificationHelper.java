package com.jo.agrisenseai;

/*
 * NotificationHelper.java
 *
 * Central helper for the entire notification pipeline. Responsibilities:
 *
 *  1. Notification Channel
 *     Creates the "AgriSense Alerts" channel (ID: agrisense_alerts) with
 *     IMPORTANCE_HIGH and vibration enabled. Safe to call multiple times —
 *     Android ignores duplicate createNotificationChannel() calls.
 *
 *  2. SharedPreferences (via NotificationPrefs inner-class pattern)
 *     Stores and loads two user preferences:
 *       "notif_enabled" — master on/off switch
 *       "notif_sound"   — play sound or post silent notification
 *
 *  3. Posting system-tray notifications
 *     All posts go through post() → postSystemNotification() pipeline.
 *     - If alerts_enabled = false → skip immediately (no notification shown)
 *     - If sound_enabled  = false → use NotificationCompat.setSilent(true)
 *     - Click action opens MainActivity (not NotificationActivity) as required
 *
 *  4. Sensor-alert post methods (called by FirebaseAlertManager)
 *       postSoilMoistureAlert()  — soilMoisture < 30
 *       postHighTemperatureAlert() — temperature > 40
 *       postWaterTankAlert()     — waterLevel < 20
 *
 *  5. AI-alert post methods (called by AINotificationWatcher)
 *       notifyWaterRequired()
 *       notifyPumpActivated()
 *       notifyFarmHealthy()
 *       notifyHighRisk()
 *       notifySystem()
 *
 * Notification IDs:
 *   We use static, named integer IDs per alert type so Android replaces
 *   an existing notification of the same type rather than stacking them
 *   infinitely. AI-watcher notifications increment sNextSystemId for
 *   uniqueness (each AI event is a distinct event worth stacking).
 */

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.database.FirebaseDatabase;

import java.util.UUID;

public final class NotificationHelper {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** ID for the "AgriSense Alerts" notification channel. */
    public static final String CHANNEL_ID = "agrisense_alerts";

    /** SharedPreferences file name (shared with SettingsActivity). */
    private static final String PREF_FILE = "agrisense_prefs";

    /** Key: master on/off toggle for all notifications. */
    private static final String KEY_NOTIF_ENABLED = "notifications_enabled";

    /** Key: sound on/off toggle for notifications. */
    private static final String KEY_NOTIF_SOUND   = "notif_sound";

    // -----------------------------------------------------------------------
    // Stable notification IDs for sensor alerts
    //
    // Using stable IDs means Android *replaces* the existing tray card when
    // a second alert of the same type would be posted (e.g., if spam-
    // prevention logic ever changes). This prevents tray flooding.
    // -----------------------------------------------------------------------

    /** Notification ID for "Low Soil Moisture" alert. */
    private static final int NOTIF_ID_SOIL_MOISTURE  = 2001;

    /** Notification ID for "High Temperature" alert. */
    private static final int NOTIF_ID_TEMPERATURE    = 2002;

    /** Notification ID for "Water Tank Low" alert. */
    private static final int NOTIF_ID_WATER_TANK     = 2003;

    /**
     * Counter for AI-watcher notifications.
     * Starts at 1000 to avoid colliding with the stable sensor alert IDs above.
     */
    private static int sNextSystemId = 1000;

    // Private constructor — utility class only
    private NotificationHelper() {}

    // -----------------------------------------------------------------------
    //  1. Notification Channel
    // -----------------------------------------------------------------------

    /**
     * Creates the "AgriSense Alerts" notification channel.
     *
     * Must be called before the first notification is posted.
     * Called from MainActivity.onCreate() so it runs at every app start.
     * Calling multiple times is safe — Android ignores duplicates after
     * the channel is already registered.
     *
     * Android 8+ (API 26+): Required for notifications to appear.
     * Android 7 and below: This method does nothing (NotificationChannel
     * class is unavailable on those API levels, but we use the
     * NotificationManager.class system service which is available and
     * simply returns null on pre-O devices, guarded by the null check).
     *
     * @param context Any valid Context (Application, Activity, or Service)
     */
    public static void createChannel(Context context) {
        // Channel display name shown in System Settings → App Notifications
        CharSequence name = context.getString(R.string.notif_channel_name);
        // Description shown below the channel name in system settings
        String desc = context.getString(R.string.notif_channel_desc);

        // IMPORTANCE_HIGH → shows as heads-up notification + plays sound
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(desc);

        // Enable device vibration for alerts (can be overridden by user in settings)
        channel.enableVibration(true);

        NotificationManager mgr = context.getSystemService(NotificationManager.class);

        // Null check: getSystemService() can theoretically return null
        if (mgr != null) {
            mgr.createNotificationChannel(channel);
        }
    }

    // -----------------------------------------------------------------------
    //  2. SharedPreferences helpers
    //     Used by SettingsActivity and internally before every post()
    // -----------------------------------------------------------------------

    /**
     * Returns true if the user has enabled notifications (default: true).
     * When false, NO notifications are posted by any method in this class.
     */
    public static boolean isEnabled(Context context) {
        boolean val = prefs(context).getBoolean(KEY_NOTIF_ENABLED, true);
        Log.d("NotificationDebug", "Toggle state restored: enabled=" + val);
        return val;
    }

    /**
     * Saves the master enabled/disabled preference.
     * Called by SettingsActivity when the master switch is toggled.
     */
    public static void setEnabled(Context context, boolean enabled) {
        Log.d("NotificationDebug", "Toggle state saved: enabled=" + enabled);
        prefs(context).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply();
    }

    /**
     * Returns true if notification sound is enabled (default: true).
     * When false, silent notifications are posted (no sound, no vibration
     * from the notification itself — only device default for the channel).
     */
    public static boolean isSoundEnabled(Context context) {
        return prefs(context).getBoolean(KEY_NOTIF_SOUND, true);
    }

    /**
     * Saves the sound preference.
     * Called by SettingsActivity when the sound switch is toggled.
     */
    public static void setSoundEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_NOTIF_SOUND, enabled).apply();
    }

    /** Returns the SharedPreferences instance. Internal use only. */
    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    // -----------------------------------------------------------------------
    //  3. Core post methods
    // -----------------------------------------------------------------------

    /**
     * Main entry point for posting a notification.
     *
     * Checks the master alerts_enabled setting before proceeding.
     * Also saves the notification to Firebase for the in-app Notification Center.
     *
     * @param context  App context
     * @param type     One of {@link NotificationModel} TYPE_* constants
     * @param title    Notification title (already localized)
     * @param message  Notification body  (already localized)
     * @param notifId  Stable notification ID (-1 to use auto-incrementing ID)
     */
    public static void post(Context context, String type, String title,
                            String message, int notifId) {
        // Guard: respect the user's master toggle
        if (!isEnabled(context)) return;

        // Save to Firebase so the in-app Notification Center stays in sync
        saveToFirebase(type, title, message);

        // Post to the system tray
        postSystemNotification(context, title, message, notifId);
    }

    /**
     * Overload for AI-watcher calls that use auto-incrementing IDs.
     * Each AI event is a distinct tray card (not replaced).
     */
    public static void post(Context context, String type, String title, String message) {
        post(context, type, title, message, sNextSystemId++);
    }

    /** Convenience overload using string resource IDs. */
    public static void postFromResources(Context context, String type,
                                          int titleResId, int messageResId) {
        post(context, type,
                context.getString(titleResId),
                context.getString(messageResId));
    }

    // -----------------------------------------------------------------------
    //  4. Sensor-alert post methods
    //     Called by FirebaseAlertManager after spam-prevention check passes.
    //     Each uses a STABLE notification ID so Android replaces, not stacks.
    // -----------------------------------------------------------------------

    /**
     * Posts "Low Soil Moisture" alert.
     * Condition: soilMoisture < 30
     * Notification ID: 2001 (stable — replaces previous soil moisture card)
     */
    public static void postSoilMoistureAlert(Context context) {
        post(context,
                NotificationModel.TYPE_SOIL_MOISTURE,
                "Low Soil Moisture",
                "Soil moisture is critically low. Irrigation required immediately.",
                NOTIF_ID_SOIL_MOISTURE);
    }

    /**
     * Posts "High Temperature Alert".
     * Condition: temperature > 40
     * Notification ID: 2002 (stable — replaces previous temperature card)
     */
    public static void postHighTemperatureAlert(Context context) {
        post(context,
                NotificationModel.TYPE_TEMPERATURE,
                "High Temperature Alert",
                "Crop heat stress detected.",
                NOTIF_ID_TEMPERATURE);
    }

    /**
     * Posts "Water Tank Low" alert.
     * Condition: waterLevel < 20
     * Notification ID: 2003 (stable — replaces previous water tank card)
     */
    public static void postWaterTankAlert(Context context) {
        post(context,
                NotificationModel.TYPE_WATER_TANK,
                "Water Tank Low",
                "Please refill the water tank.",
                NOTIF_ID_WATER_TANK);
    }

    // -----------------------------------------------------------------------
    //  5. AI-alert helper methods
    //     Called by AINotificationWatcher when the AI engine changes state.
    // -----------------------------------------------------------------------

    /** Fired when AI engine sets riskLevel to HIGH or MEDIUM. */
    public static void notifyWaterRequired(Context context) {
        postFromResources(context, NotificationModel.TYPE_WATER,
                R.string.notif_type_water, R.string.notif_msg_water);
    }

    /** Fired when pump status changes to ON. */
    public static void notifyPumpActivated(Context context) {
        postFromResources(context, NotificationModel.TYPE_PUMP,
                R.string.notif_type_pump, R.string.notif_msg_pump);
    }

    /** Fired when AI engine sets riskLevel to LOW. */
    public static void notifyFarmHealthy(Context context) {
        postFromResources(context, NotificationModel.TYPE_HEALTHY,
                R.string.notif_type_healthy, R.string.notif_msg_healthy);
    }

    /** Fired when AI engine sets riskLevel to HIGH. */
    public static void notifyHighRisk(Context context) {
        postFromResources(context, NotificationModel.TYPE_RISK,
                R.string.notif_type_risk, R.string.notif_msg_risk);
    }

    /** Fired for generic system-level messages. */
    public static void notifySystem(Context context, String message) {
        post(context, NotificationModel.TYPE_SYSTEM,
                context.getString(R.string.notif_type_system), message);
    }

    // -----------------------------------------------------------------------
    //  Private helpers
    // -----------------------------------------------------------------------

    /**
     * Persists a notification record to Firebase under "notifications/{id}".
     * This populates the in-app Notification Center (NotificationActivity).
     *
     * @param type    Notification type constant
     * @param title   Notification title
     * @param message Notification body
     */
    private static void saveToFirebase(String type, String title, String message) {
        // Generate a short, unique 16-char ID for the Firebase key
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        NotificationModel model = new NotificationModel(
                id, title, message, type, System.currentTimeMillis());
        FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(id)
                .setValue(model);
    }

    /**
     * Builds and posts a notification to the Android system tray.
     *
     * Click action:
     *   Tapping the notification opens MainActivity (as required in spec §12).
     *   FLAG_ACTIVITY_CLEAR_TOP ensures MainActivity is brought to the front
     *   rather than launching a duplicate instance on top of the stack.
     *
     * Sound control:
     *   If isSoundEnabled() returns false, we call builder.setSilent(true).
     *   setSilent(true) overrides the channel's sound setting and posts a
     *   completely silent notification (no sound, no vibration from the
     *   notification itself — even if the channel has vibration enabled).
     *
     * Icon:
     *   Uses R.drawable.ic_bell_badge — already exists in the project and
     *   is a valid monochromatic vector drawable compatible with Android 8+.
     *   On Android 5+ the icon is rendered as a silhouette using the tint
     *   colour from the current theme.
     *
     * @param context  App context
     * @param title    Notification title
     * @param message  Notification body
     * @param notifId  The notification ID to use (stable or incremented)
     */
    private static void postSystemNotification(Context context, String title,
                                                String message, int notifId) {
        // Intent that launches MainActivity when the notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        // FLAG_ACTIVITY_CLEAR_TOP: if MainActivity is already on the stack,
        // bring it to the front and clear activities above it.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // PendingIntent wraps the Intent so the system can fire it later.
        // FLAG_IMMUTABLE: required on Android 12+ (API 31+). The Intent
        // details cannot be changed by the receiving process.
        // FLAG_UPDATE_CURRENT: if a PendingIntent already exists with the
        // same request code, update it with the new Intent extras.
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notifId,    // unique request code per notification
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Build the notification using the NotificationCompat builder
        // so it works on all API levels (minSdk 24 in this project).
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                // Small icon — must be a monochromatic drawable (vector or bitmap).
                // ic_bell_badge is a valid AgriSense vector that meets this requirement.
                .setSmallIcon(R.drawable.ic_bell_badge)
                .setContentTitle(title)
                .setContentText(message)
                // BigTextStyle allows the full message to be visible when
                // the user expands the notification card.
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                // PRIORITY_HIGH → shows as a "heads-up" floating notification
                // (the banner that slides down from the top while the screen is on).
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Auto-cancel: tapping the notification removes it from the tray.
                .setAutoCancel(true)
                // Attach the click action
                .setContentIntent(pendingIntent);

        // Sound control (Requirement §11)
        if (!isSoundEnabled(context)) {
            // setSilent(true) overrides channel defaults to produce a
            // notification with no audible alert and no vibration pattern.
            builder.setSilent(true);
        }

        // Retrieve the NotificationManager system service and post
        NotificationManager mgr = context.getSystemService(NotificationManager.class);
        if (mgr != null) {
            mgr.notify(notifId, builder.build());
        }
    }
}
