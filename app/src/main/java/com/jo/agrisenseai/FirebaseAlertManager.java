package com.jo.agrisenseai;

/*
 * FirebaseAlertManager.java
 *
 * Responsibility:
 *   Listens to the Firebase "sensorData" node in real-time and fires
 *   targeted system-tray notifications whenever sensor values cross
 *   critical thresholds.
 *
 * Spam-prevention strategy:
 *   Each alert type maintains a boolean flag (alertXxxActive).
 *
 *   - Flag starts FALSE.
 *   - When a sensor crosses into the DANGER zone → fire notification,
 *     set flag = TRUE.
 *   - While flag is TRUE → do NOT fire again (no spam).
 *   - When sensor value returns to NORMAL → reset flag = FALSE.
 *   - Next time the sensor enters danger → flag fires again (one more time).
 *
 *   This guarantees exactly ONE notification per danger event, with full
 *   recovery detection.
 *
 * Thresholds:
 *   soilMoisture  < 30  → "Low Soil Moisture" alert
 *   temperature   > 40  → "High Temperature" alert
 *   waterLevel    < 20  → "Water Tank Low" alert
 *
 * Architecture:
 *   Called once from MainActivity.onCreate().
 *   Uses FirebaseHelper.listenSensorData() which internally attaches a
 *   persistent ValueEventListener — no lifecycle cleanup needed for a
 *   session-wide watcher.
 *
 * SensorData note:
 *   The existing SensorData model does NOT have a waterLevel field.
 *   We read it directly from the Firebase DataSnapshot using the key
 *   "waterLevel" to stay compatible with both existing and new data.
 */

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public final class FirebaseAlertManager {

    private static final String TAG = "FirebaseAlertManager";

    // -----------------------------------------------------------------------
    // Thresholds — single source of truth for all alert conditions
    // -----------------------------------------------------------------------

    /** Soil moisture percentage below which irrigation is critical. */
    private static final double THRESHOLD_SOIL_MOISTURE_LOW  = 30.0;

    /** Temperature in °C above which crop heat-stress is detected. */
    private static final double THRESHOLD_TEMPERATURE_HIGH   = 40.0;

    /** Water tank level percentage below which refill is required. */
    private static final double THRESHOLD_WATER_LEVEL_LOW    = 20.0;

    // -----------------------------------------------------------------------
    // Spam-prevention state flags
    //
    // Each flag tracks whether we are currently IN an alert condition.
    // "true"  = we already sent a notification for this condition.
    //           Do NOT send again until the sensor recovers.
    // "false" = sensor is in normal range (or just recovered).
    //           Next time it crosses the threshold → send one notification.
    // -----------------------------------------------------------------------

    private static boolean alertSoilMoistureActive  = false;
    private static boolean alertTemperatureActive    = false;
    private static boolean alertWaterLevelActive     = false;

    /**
     * Tracks whether this is the very first data snapshot received from
     * Firebase. On first load we silently initialize the flags so that we
     * do NOT fire notifications for a condition that has existed since
     * before the app opened. Notifications should be for NEW changes only.
     */
    private static boolean firstLoadComplete = false;

    // Private constructor — utility class, no instantiation
    private FirebaseAlertManager() {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Starts the real-time Firebase listener.
     *
     * Call once from {@link MainActivity#onCreate}. The listener runs
     * for the lifetime of the app process — intentional for a smart
     * monitoring app.
     *
     * @param context Application context (used to post notifications and
     *                read SharedPreferences for sound/enabled settings).
     */
    public static void start(final Context context) {
        // We attach directly to Firebase here (rather than via FirebaseHelper)
        // because we need the raw DataSnapshot to read "waterLevel", a field
        // not present in the SensorData POJO.
        FirebaseDatabase.getInstance()
                .getReference(FirebaseHelper.NODE_SENSOR_DATA)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        // ── Guard: Firebase returned an empty/null snapshot ──
                        if (!snapshot.exists()) {
                            Log.w(TAG, "sensorData node is empty — no alerts evaluated.");
                            return;
                        }

                        // ── Read raw values from snapshot ────────────────────
                        // Using Double wrapper type so we can detect null
                        // (missing key in the database) without an exception.
                        Double temperature  = readDouble(snapshot, "temperature");
                        Double soilMoisture = readDouble(snapshot, "soilMoisture");
                        Double waterLevel   = readDouble(snapshot, "waterLevel");

                        // ── First load: initialize flags silently ─────────────
                        // Do not fire notifications for pre-existing conditions.
                        if (!firstLoadComplete) {
                            firstLoadComplete = true;
                            if (soilMoisture != null) {
                                alertSoilMoistureActive = soilMoisture < THRESHOLD_SOIL_MOISTURE_LOW;
                            }
                            if (temperature != null) {
                                alertTemperatureActive = temperature > THRESHOLD_TEMPERATURE_HIGH;
                            }
                            if (waterLevel != null) {
                                alertWaterLevelActive = waterLevel < THRESHOLD_WATER_LEVEL_LOW;
                            }
                            Log.d(TAG, "First load: flags initialized silently. No notifications fired.");
                            return;
                        }

                        // ── Evaluate each alert after the first load ──────────
                        evaluateSoilMoisture(context, soilMoisture);
                        evaluateTemperature(context, temperature);
                        evaluateWaterLevel(context, waterLevel);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Firebase connection was cancelled (e.g. security rule denial,
                        // network drop). Log the error but keep the listener attached
                        // so Firebase will automatically reconnect and retry.
                        Log.e(TAG, "Firebase listener cancelled: " + error.getMessage());
                    }
                });

        Log.d(TAG, "FirebaseAlertManager started — listening to sensorData.");
    }

    // -----------------------------------------------------------------------
    // Per-sensor evaluation methods
    // -----------------------------------------------------------------------

    /**
     * Evaluates soil moisture and manages the spam-prevention flag.
     *
     * Logic:
     *  - If value is null → skip (null-safe: no crash, no false alert)
     *  - If value < 30 AND flag is false → fire alert, set flag = true
     *  - If value < 30 AND flag is true  → do nothing (already notified)
     *  - If value >= 30 AND flag is true → reset flag to false (recovered)
     *  - If value >= 30 AND flag is false → do nothing (normal state)
     */
    private static void evaluateSoilMoisture(Context context, Double soilMoisture) {

        if (soilMoisture == null) {
            Log.w(TAG, "soilMoisture is null in Firebase — skipping alert.");
            return;
        }

        if (soilMoisture < THRESHOLD_SOIL_MOISTURE_LOW) {
            // Sensor is in DANGER zone
            if (!alertSoilMoistureActive) {
                // First time crossing into danger → fire notification
                NotificationHelper.postSoilMoistureAlert(context);
                alertSoilMoistureActive = true;
                Log.d(TAG, "Soil moisture alert FIRED. Value: " + soilMoisture);
            }
            // else: already in danger → suppress (spam prevention)
        } else {
            // Sensor is NORMAL — reset flag so next dip fires again
            if (alertSoilMoistureActive) {
                alertSoilMoistureActive = false;
                Log.d(TAG, "Soil moisture recovered to " + soilMoisture + ". Alert reset.");
            }
        }
    }

    /**
     * Evaluates temperature and manages the spam-prevention flag.
     * Mirrors the same logic as {@link #evaluateSoilMoisture}.
     */
    private static void evaluateTemperature(Context context, Double temperature) {

        if (temperature == null) {
            Log.w(TAG, "temperature is null in Firebase — skipping alert.");
            return;
        }

        if (temperature > THRESHOLD_TEMPERATURE_HIGH) {
            if (!alertTemperatureActive) {
                NotificationHelper.postHighTemperatureAlert(context);
                alertTemperatureActive = true;
                Log.d(TAG, "High temperature alert FIRED. Value: " + temperature);
            }
        } else {
            if (alertTemperatureActive) {
                alertTemperatureActive = false;
                Log.d(TAG, "Temperature recovered to " + temperature + ". Alert reset.");
            }
        }
    }

    /**
     * Evaluates water tank level and manages the spam-prevention flag.
     * Mirrors the same logic as {@link #evaluateSoilMoisture}.
     */
    private static void evaluateWaterLevel(Context context, Double waterLevel) {

        if (waterLevel == null) {
            Log.w(TAG, "waterLevel is null in Firebase — skipping alert.");
            return;
        }

        if (waterLevel < THRESHOLD_WATER_LEVEL_LOW) {
            if (!alertWaterLevelActive) {
                NotificationHelper.postWaterTankAlert(context);
                alertWaterLevelActive = true;
                Log.d(TAG, "Water tank low alert FIRED. Value: " + waterLevel);
            }
        } else {
            if (alertWaterLevelActive) {
                alertWaterLevelActive = false;
                Log.d(TAG, "Water level recovered to " + waterLevel + ". Alert reset.");
            }
        }
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    /**
     * Safely reads a child node as a Double from a DataSnapshot.
     *
     * Returns null if:
     *  - The key does not exist in the snapshot.
     *  - The value stored is not a numeric type.
     *  - Firebase returned null for the key.
     *
     * This avoids NullPointerException and ClassCastException in production.
     *
     * @param snapshot  The parent DataSnapshot (e.g. "sensorData" node)
     * @param key       The child key to read (e.g. "temperature")
     * @return          The double value, or null if unavailable.
     */
    private static Double readDouble(DataSnapshot snapshot, String key) {
        try {
            Object value = snapshot.child(key).getValue();
            if (value == null) return null;
            if (value instanceof Double)  return (Double) value;
            if (value instanceof Long)    return ((Long) value).doubleValue();
            if (value instanceof Integer) return ((Integer) value).doubleValue();
            if (value instanceof String)  return Double.parseDouble((String) value);
            return null;
        } catch (NumberFormatException | ClassCastException e) {
            Log.e(TAG, "Could not parse Firebase value for key '" + key + "': " + e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Testing utilities (package-private, for instrumented tests)
    // -----------------------------------------------------------------------

    /**
     * Resets all state flags. Call this in unit/instrumented tests before
     * each test case to start from a clean state.
     */
    static void resetForTesting() {
        firstLoadComplete       = false;
        alertSoilMoistureActive = false;
        alertTemperatureActive  = false;
        alertWaterLevelActive   = false;
    }
}
