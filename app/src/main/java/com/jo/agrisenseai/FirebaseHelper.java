package com.jo.agrisenseai;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized access point for all Firebase Realtime Database operations.
 * Keeps Firebase logic out of Fragments/Activities.
 *
 * Database structure:
 *   sensorData/  -> temperature, humidity, soilMoisture, lightIntensity
 *   dashboard/   -> farmHealth, pumpStatus, aiRecommendation, waterSaved, energySaved
 *   farm/        -> fieldName, fieldStatus, nextWatering
 *   history/     -> {pushKey} -> temperature, humidity, soilMoisture, timestamp
 *                   Every sensor update is permanently archived here.
 */
public class FirebaseHelper {

    public static final String NODE_SENSOR_DATA  = "sensorData";
    public static final String NODE_HISTORY      = "history";
    public static final String NODE_DASHBOARD    = "dashboard";
    public static final String NODE_FARM         = "farm";
    public static final String NODE_WATER_LOSS   = "waterLossDetection";
    public static final String PUMP_ON           = "ON";

    /** Max number of history entries returned by listenHistory(). */
    private static final int HISTORY_LIMIT = 50;

    private final DatabaseReference rootRef;

    private static FirebaseHelper instance;

    private FirebaseHelper() {
        rootRef = FirebaseDatabase.getInstance().getReference();
    }

    public static FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // ---------------------------------------------------------
    // Listener interfaces
    // ---------------------------------------------------------

    public interface DashboardListener {
        void onDashboardUpdate(DashboardData data);
    }

    public interface FarmListener {
        void onFarmUpdate(FarmData data);
    }

    public interface SensorListener {
        void onSensorUpdate(SensorData data);
    }

    /** Used by HomeFragment and InsightsFragment for water loss card. */
    public interface WaterLossListener {
        void onWaterLossUpdate(WaterLossData data);
    }

    /**
     * Delivers the most recent {@value #HISTORY_LIMIT} history entries
     * ordered by timestamp (oldest first) whenever the history node changes.
     * Deserializes entries as {@link SensorHistory} (used by HistoryFragment).
     */
    public interface HistoryListener {
        void onHistoryUpdate(java.util.List<SensorHistory> entries);
    }

    /**
     * Delivers the most recent {@value #HISTORY_LIMIT} history entries
     * ordered by timestamp (oldest first) whenever the history node changes.
     * Deserializes entries as {@link HistoryModel} (used by InsightsFragment).
     *
     * <p>Called on the main thread; safe to update UI directly inside
     * {@link HistoryModelListener#onHistoryLoaded(java.util.ArrayList)}.</p>
     */
    public interface HistoryModelListener {
        /** @param entries non-null, may be empty, sorted oldest-to-newest. */
        void onHistoryLoaded(java.util.ArrayList<HistoryModel> entries);

        /**
         * Called when Firebase cancels the listener (e.g. permission denied,
         * network failure). The UI should display an appropriate error state.
         *
         * @param errorMessage human-readable description of the failure
         */
        void onHistoryError(String errorMessage);
    }

    // ---------------------------------------------------------
    // READ (real-time listeners)
    // ---------------------------------------------------------

    /** Used by Home & Insights screens. */
    public ValueEventListener listenDashboard(final DashboardListener listener) {
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DashboardData data = snapshot.getValue(DashboardData.class);
                if (data != null) {
                    listener.onDashboardUpdate(data);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Intentionally left blank for this phase.
            }
        };
        rootRef.child(NODE_DASHBOARD).addValueEventListener(valueEventListener);
        return valueEventListener;
    }

    /** Used by My Farm screen. */
    public ValueEventListener listenFarm(final FarmListener listener) {
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FarmData data = snapshot.getValue(FarmData.class);
                if (data != null) {
                    listener.onFarmUpdate(data);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Intentionally left blank for this phase.
            }
        };
        rootRef.child(NODE_FARM).addValueEventListener(valueEventListener);
        return valueEventListener;
    }

    /** Reserved for future AI engine / sensor display use. */
    public ValueEventListener listenSensorData(final SensorListener listener) {
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SensorData data = snapshot.getValue(SensorData.class);
                if (data != null) {
                    listener.onSensorUpdate(data);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Intentionally left blank for this phase.
            }
        };
        rootRef.child(NODE_SENSOR_DATA).addValueEventListener(valueEventListener);
        return valueEventListener;
    }

    /** Used by HomeFragment and InsightsFragment. */
    public ValueEventListener listenWaterLoss(final WaterLossListener listener) {
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                WaterLossData data = snapshot.getValue(WaterLossData.class);
                if (data != null) {
                    listener.onWaterLossUpdate(data);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        rootRef.child(NODE_WATER_LOSS).addValueEventListener(valueEventListener);
        return valueEventListener;
    }

    /**
     * Listens to the most recent {@value #HISTORY_LIMIT} entries under the
     * {@code history} node, ordered by the {@code timestamp} field.
     * <p>
     * The list delivered to {@link HistoryListener#onHistoryUpdate} is sorted
     * oldest-to-newest so the UI can display it in chronological order.
     * <p>
     * Used by {@link HistoryFragment}.
     *
     * @return the attached {@link ValueEventListener}; pass it to
     *         {@link #removeHistoryListener(ValueEventListener)} when done.
     */
    public ValueEventListener listenHistory(final HistoryListener listener) {
        Query query = rootRef.child(NODE_HISTORY)
                .orderByChild("timestamp")
                .limitToLast(HISTORY_LIMIT);

        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                java.util.List<SensorHistory> entries = new java.util.ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SensorHistory entry = child.getValue(SensorHistory.class);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
                listener.onHistoryUpdate(entries);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Intentionally left blank for this phase.
            }
        };
        query.addValueEventListener(valueEventListener);
        return valueEventListener;
    }

    /** Detaches a history listener returned by {@link #listenHistory}. */
    public void removeHistoryListener(ValueEventListener listener) {
        rootRef.child(NODE_HISTORY).removeEventListener(listener);
    }

    /**
     * Attaches a real-time listener to the {@code history/} node that:
     * <ol>
     *   <li>Queries the last {@value #HISTORY_LIMIT} entries ordered by the
     *       child field {@code timestamp} (matches how
     *       {@link #saveSensorHistory(SensorData)} writes them).</li>
     *   <li>Deserializes each child snapshot as a {@link HistoryModel}
     *       (fields: {@code temperature int}, {@code humidity int},
     *       {@code soilMoisture int}, {@code timestamp long}).</li>
     *   <li>Accumulates results into an {@link java.util.ArrayList} that is
     *       sorted oldest-to-newest before delivery.</li>
     *   <li>Calls {@link HistoryModelListener#onHistoryError(String)} and
     *       logs to Logcat when Firebase cancels the query.</li>
     * </ol>
     *
     * <p>The listener fires immediately with cached data (if available) and
     * again every time any child under {@code history/} is added or changed,
     * so the charts update automatically on every new sensor reading.</p>
     *
     * @param listener the callback to receive updates or errors
     * @return the attached {@link ValueEventListener}; pass it to
     *         {@link #removeHistoryListener(ValueEventListener)} when done.
     */
    public ValueEventListener listenHistoryModel(final HistoryModelListener listener) {
        // Query only entries from the last 7 days (7 days in milliseconds)
        long cutoff = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        Query query = rootRef.child(NODE_HISTORY)
                .orderByChild("timestamp")
                .startAt(cutoff);

        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                java.util.ArrayList<HistoryModel> entries = new java.util.ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        HistoryModel model = child.getValue(HistoryModel.class);
                        if (model != null) {
                            entries.add(model);
                        }
                    } catch (Exception e) {
                        // Skip malformed entries rather than crashing.
                        android.util.Log.w("FirebaseHelper",
                                "Skipping malformed history entry key=" + child.getKey(), e);
                    }
                }

                // Firebase orderByChild("timestamp") already returns children
                // sorted oldest-to-newest, so no additional sort is needed.
                android.util.Log.d("FirebaseHelper",
                        "listenHistoryModel: delivered " + entries.size() + " HistoryModel entries");

                listener.onHistoryLoaded(entries);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String msg = "history/ query cancelled: "
                        + error.getMessage() + " (code " + error.getCode() + ")";
                android.util.Log.e("FirebaseHelper", msg);
                listener.onHistoryError(msg);
            }
        };

        query.addValueEventListener(vel);
        return vel;
    }

    public void removeListener(String node, ValueEventListener listener) {
        rootRef.child(node).removeEventListener(listener);
    }

    // ---------------------------------------------------------
    // AI ENGINE INTEGRATION
    // ---------------------------------------------------------

    /**
     * Starts the AI Irrigation Engine.
     * <p>
     * Every time sensorData changes, runs {@link AIEngine#analyze(SensorData)}
     * and writes the result back to the database so all screens (and future
     * hardware/voice/notification features) stay in sync automatically.
     * <p>
     * Additionally, every sensor update is permanently archived in the
     * {@code history} node via {@link #saveSensorHistory(SensorData)}.
     * <p>
     * Safe to call once for the lifetime of the app (e.g. from MainActivity).
     */
    public ValueEventListener runAIEngine() {
        return listenSensorData(sensorData -> {
            // Archive every reading permanently to history/
            saveSensorHistory(sensorData);

            AIResult result = AIEngine.analyze(sensorData);
            applyAIResult(result);
        });
    }

    /**
     * Writes an {@link AIResult} to the database.
     * Uses partial updates so existing fields (waterSaved, energySaved,
     * fieldName, fieldStatus, etc.) are preserved.
     */
    public void applyAIResult(AIResult result) {
        Map<String, Object> dashboardUpdates = new HashMap<>();
        dashboardUpdates.put("farmHealth",        result.getFarmHealth());
        dashboardUpdates.put("pumpStatus",         result.getPumpStatus());
        dashboardUpdates.put("aiRecommendation",   result.getAiRecommendation());
        dashboardUpdates.put("riskLevel",          result.getRiskLevel());
        dashboardUpdates.put("nextWatering",       result.getNextWatering());
        dashboardUpdates.put("waterRequirement",   result.getWaterRequirement());
        rootRef.child(NODE_DASHBOARD).updateChildren(dashboardUpdates);

        // Keep the My Farm screen's "Next Watering" in sync with the AI decision.
        rootRef.child(NODE_FARM).child("nextWatering").setValue(result.getNextWatering());
    }

    // ---------------------------------------------------------
    // WRITE (used by "Water Now" button now, hardware later)
    // ---------------------------------------------------------

    public void updateDashboard(DashboardData data) {
        rootRef.child(NODE_DASHBOARD).setValue(data);
    }

    public void updateFarmData(FarmData data) {
        rootRef.child(NODE_FARM).setValue(data);
    }

    /**
     * Writes current sensor readings to {@code sensorData/} (overwriting the
     * live values) and simultaneously stores a permanent copy into
     * {@code history/} keyed by the current epoch-millisecond timestamp.
     *
     * <pre>
     * history/
     *   {System.currentTimeMillis()}/      ← timestamp string key, never overwritten
     *     temperature  : int
     *     humidity     : int
     *     soilMoisture : int
     *     timestamp    : long  (same epoch millis as the key)
     * </pre>
     *
     * Existing functionality (sensorData node, AI engine re-trigger,
     * prediction, pump status) is completely unaffected; the history
     * write is purely additive.
     */
    public void updateSensorData(SensorData data) {
        // 1. Overwrite the current live reading (existing behaviour).
        rootRef.child(NODE_SENSOR_DATA).setValue(data);

        // 2. Archive a permanent snapshot in history/ (new behaviour).
        saveSensorHistory(data);
    }

    /**
     * Stores a single {@link HistoryModel} entry permanently in
     * {@code history/} using {@link System#currentTimeMillis()} as the node key.
     *
     * <p>Using the timestamp string as the key means:</p>
     * <ul>
     *   <li>Old records are <b>never overwritten</b> — each key is unique to
     *       the millisecond.</li>
     *   <li>Entries are naturally ordered chronologically when queried with
     *       {@code orderByKey()}.</li>
     * </ul>
     *
     * <p>Called from both {@link #updateSensorData(SensorData)} (manual writes)
     * and {@link #runAIEngine()} (every real-time sensor change), so every
     * sensor update is captured regardless of its source.</p>
     *
     * @param data the current sensor readings to archive
     */
    public void saveSensorHistory(SensorData data) {
        long now = System.currentTimeMillis();

        // Use the epoch-millisecond timestamp as the node key so that:
        //  - each entry is permanently stored (no overwrite)
        //  - records are implicitly sorted oldest-to-newest by key
        String timestampKey = String.valueOf(now);

        HistoryModel historyModel = new HistoryModel(
                (int) data.getTemperature(),
                (int) data.getHumidity(),
                (int) data.getSoilMoisture(),
                now
        );

        rootRef.child(NODE_HISTORY)
                .child(timestampKey)
                .setValue(historyModel);
    }

    /** Convenience method: toggle pump status only. */
    public void setPumpStatus(String status) {
        rootRef.child(NODE_DASHBOARD).child("pumpStatus").setValue(status);
    }
}
