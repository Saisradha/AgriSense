package com.jo.agrisenseai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;

import com.google.firebase.database.FirebaseDatabase;

import java.util.UUID;

/**
 * Central helper for:
 * 1. Creating the Android notification channel (once, at app start).
 * 2. Posting system tray notifications.
 * 3. Writing {@link NotificationModel} records to Firebase so the
 *    in-app Notification Center always reflects the full history.
 *
 * Called by {@link AIEngine} after a risk change, or by future hardware
 * events sent from ESP32 via Firebase triggers.
 *
 * Future multilingual support: pass a locale code to choose the right
 * string resource when building title/message before calling post().
 */
public final class NotificationHelper {

    public static final String CHANNEL_ID   = "agrisense_alerts";
    private static final String PREF_FILE   = "agrisense_prefs";
    private static final String KEY_NOTIF_ENABLED = "notif_enabled";
    private static final String KEY_NOTIF_SOUND   = "notif_sound";

    private static int sNextSystemId = 1000;

    private NotificationHelper() {}

    // ---------------------------------------------------------------
    // Channel (call once from MainActivity.onCreate)
    // ---------------------------------------------------------------

    public static void createChannel(Context context) {
        CharSequence name = context.getString(R.string.notif_channel_name);
        String desc = context.getString(R.string.notif_channel_desc);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(desc);
        channel.enableVibration(true);
        NotificationManager mgr = context.getSystemService(NotificationManager.class);
        if (mgr != null) mgr.createNotificationChannel(channel);
    }

    // ---------------------------------------------------------------
    // Preference helpers (used by ProfileFragment toggles)
    // ---------------------------------------------------------------

    public static boolean isEnabled(Context context) {
        return prefs(context).getBoolean(KEY_NOTIF_ENABLED, true);
    }

    public static void setEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply();
    }

    public static boolean isSoundEnabled(Context context) {
        return prefs(context).getBoolean(KEY_NOTIF_SOUND, true);
    }

    public static void setSoundEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_NOTIF_SOUND, enabled).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------
    // Post a notification (system tray + Firebase)
    // ---------------------------------------------------------------

    /**
     * Posts a system tray notification and saves it to Firebase.
     * Safe to call from any thread.
     *
     * @param context app context
     * @param type    one of {@link NotificationModel} TYPE_* constants
     * @param title   notification title (already in user's language)
     * @param message notification body (already in user's language)
     */
    public static void post(Context context, String type, String title, String message) {
        if (!isEnabled(context)) return;

        // 1. Save to Firebase for the in-app Notification Center
        saveToFirebase(type, title, message);

        // 2. Post system tray notification
        postSystemNotification(context, title, message);
    }

    /** Convenience overload using string resource IDs for title and message. */
    public static void postFromResources(Context context, String type,
                                          int titleResId, int messageResId) {
        post(context, type,
                context.getString(titleResId),
                context.getString(messageResId));
    }

    // ---------------------------------------------------------------
    // AI-triggered helper methods (called from AINotificationWatcher)
    // ---------------------------------------------------------------

    public static void notifyWaterRequired(Context context) {
        postFromResources(context, NotificationModel.TYPE_WATER,
                R.string.notif_type_water, R.string.notif_msg_water);
    }

    public static void notifyPumpActivated(Context context) {
        postFromResources(context, NotificationModel.TYPE_PUMP,
                R.string.notif_type_pump, R.string.notif_msg_pump);
    }

    public static void notifyFarmHealthy(Context context) {
        postFromResources(context, NotificationModel.TYPE_HEALTHY,
                R.string.notif_type_healthy, R.string.notif_msg_healthy);
    }

    public static void notifyHighRisk(Context context) {
        postFromResources(context, NotificationModel.TYPE_RISK,
                R.string.notif_type_risk, R.string.notif_msg_risk);
    }

    public static void notifySystem(Context context, String message) {
        post(context, NotificationModel.TYPE_SYSTEM,
                context.getString(R.string.notif_type_system), message);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private static void saveToFirebase(String type, String title, String message) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        NotificationModel model = new NotificationModel(id, title, message, type,
                System.currentTimeMillis());
        FirebaseDatabase.getInstance().getReference("notifications")
                .child(id)
                .setValue(model);
    }

    private static void postSystemNotification(Context context, String title, String message) {
        Intent intent = new Intent(context, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell_badge)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pending);

        if (!isSoundEnabled(context)) {
            builder.setSilent(true);
        }

        NotificationManager mgr = context.getSystemService(NotificationManager.class);
        if (mgr != null) mgr.notify(sNextSystemId++, builder.build());
    }
}
