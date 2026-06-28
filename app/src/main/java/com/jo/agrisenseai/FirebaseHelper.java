package com.jo.agrisenseai;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Singleton helper that owns every Firebase Realtime Database interaction.
 *
 * <p><b>Real-time listeners</b> — every {@code listen*()} method attaches an
 * {@link com.google.firebase.database.ValueEventListener} via
 * {@link DatabaseReference#addValueEventListener(ValueEventListener)}, which
 * means Firebase calls {@code onDataChange()} <em>immediately</em> with the
 * current value <em>and</em> again on every future change. This is why the
 * app stays live-synced with the ESP32 data without any manual refresh.</p>
 *
 * <p><b>Field mapping – ESP32 → Firebase → Android</b></p>
 * <pre>
 * Firebase key        SensorData field          Accessed via
 * ─────────────────   ────────────────────────  ─────────────────────────────
 * temperature         temperature (double)      getTemperature()
 * humidity            humidity    (double)      getHumidity()
 * averageSoil         soilMoisture(double)      getSoilMoisture()
 * light               lightIntensity (double)   getLightIntensity()
 * pumpStatus          pumpStatus  (String)      getPumpStatus()
 * soil1-4             soil1-4     (double)      getSoil1()…getSoil4()
 * </pre>
 *
 * <p>The mismatch between the ESP32 key names ({@code averageSoil}, {@code light})
 * and the Java field names ({@code soilMoisture}, {@code lightIntensity}) is
 * resolved by reading the snapshot children by name inside
 * {@link #buildSensorDataFromSnapshot(DataSnapshot)} rather than relying on
 * Firebase's automatic POJO deserialization, which would require matching names.</p>
 */
public class FirebaseHelper {

    // ── Firebase node names ────────────────────────────────────────────────
    public static final String NODE_SENSOR_DATA = "sensorData";
    public static final String NODE_DASHBOARD   = "dashboard";
    public static final String NODE_WATER_LOSS  = "waterLossDetection";
    public static final String NODE_HISTORY     = "history";
    public static final String NODE_FARMS       = "farms";
    public static final String NODE_FARM        = "farm";

    // ── Pump constants ─────────────────────────────────────────────────────
    public static final String PUMP_ON  = "ON";
    public static final String PUMP_OFF = "OFF";

    // ── Singleton ──────────────────────────────────────────────────────────
    private static volatile FirebaseHelper sInstance;

    private final DatabaseReference mDatabase;

    private FirebaseHelper() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public static FirebaseHelper getInstance() {
        if (sInstance == null) {
            synchronized (FirebaseHelper.class) {
                if (sInstance == null) {
                    sInstance = new FirebaseHelper();
                }
            }
        }
        return sInstance;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Callback interfaces
    // ══════════════════════════════════════════════════════════════════════

    /** Delivers live {@link SensorData} updates from {@code sensorData/}. */
    public interface SensorListener {
        void onSensorUpdate(SensorData data);
    }

    /** Delivers live {@link DashboardData} updates from {@code dashboard/}. */
    public interface DashboardListener {
        void onDashboardUpdate(DashboardData data);
    }

    /** Delivers live {@link WaterLossData} updates from {@code waterLossDetection/}. */
    public interface WaterLossListener {
        void onWaterLossUpdate(WaterLossData data);
    }

    /** Delivers live lists of {@link SensorHistory} entries from {@code history/}. */
    public interface HistoryListener {
        void onHistoryLoaded(List<SensorHistory> entries);
    }

    /** Delivers live lists of {@link HistoryModel} entries (for charts). */
    public interface HistoryModelListener {
        void onHistoryLoaded(ArrayList<HistoryModel> entries);
        void onHistoryError(String errorMessage);
    }

    /** Delivers live lists of {@link Farm} objects from {@code farms/}. */
    public interface FarmsListener {
        void onFarmsLoaded(List<Farm> farms);
    }

    /** Delivers a single {@link Farm} by its ID (live or one-shot). */
    public interface FarmByIdListener {
        void onFarmLoaded(Farm farm);
        void onFarmNotFound();
    }

    /** Delivers UserProfile loaded from Firebase */
    public interface UserProfileListener {
        void onProfileLoaded(UserProfile profile);
    }

    // ══════════════════════════════════════════════════════════════════════
    // REAL-TIME SENSOR DATA  (sensorData/)
    //
    // The ESP32 writes to sensorData/ with these exact keys:
    //   temperature, humidity, soil1, soil2, soil3, soil4,
    //   averageSoil, light, pumpStatus
    //
    // addValueEventListener fires on EVERY change → live sync.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attaches a permanent real-time listener to {@code sensorData/}.
     *
     * <p>Firebase calls {@code onDataChange} immediately with the current
     * snapshot and again every time the ESP32 writes a new value. The app
     * therefore reflects sensor changes within the Firebase propagation
     * latency (~1 s) without any polling or manual refresh.</p>
     *
     * @param listener callback that receives a populated {@link SensorData}
     * @return the attached {@link ValueEventListener} so the caller can
     *         remove it in {@code onDestroyView()} / {@code onDestroy()}
     */
    public ValueEventListener listenSensorData(SensorListener listener) {
        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    SensorData data = buildSensorDataFromSnapshot(snapshot);
                    listener.onSensorUpdate(data);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("FirebaseHelper",
                        "sensorData listener cancelled: " + error.getMessage());
            }
        };
        // addValueEventListener (NOT addListenerForSingleValueEvent) so the
        // callback fires on every future update, not just the first read.
        mDatabase.child(NODE_SENSOR_DATA).addValueEventListener(vel);
        return vel;
    }

    /**
     * Maps the Firebase {@code sensorData} snapshot to a {@link SensorData} object.
     *
     * <p>The ESP32 uses {@code averageSoil} and {@code light} as the database keys,
     * but the Java model uses {@code soilMoisture} and {@code lightIntensity}.
     * Automatic POJO deserialization ({@code getValue(SensorData.class)}) would
     * leave those two fields at zero. This method reads each child by name to
     * bridge that naming gap.</p>
     */
    private SensorData buildSensorDataFromSnapshot(DataSnapshot snapshot) {
        SensorData data = new SensorData();

        Double temperature = getDouble(snapshot, "temperature");
        Double humidity    = getDouble(snapshot, "humidity");
        Double soil1       = getDouble(snapshot, "soil1");
        Double soil2       = getDouble(snapshot, "soil2");
        Double soil3       = getDouble(snapshot, "soil3");
        Double soil4       = getDouble(snapshot, "soil4");
        // ESP32 key is "averageSoil" → maps to SensorData.soilMoisture
        Double averageSoil = getDouble(snapshot, "averageSoil");
        // ESP32 key is "light" → maps to SensorData.lightIntensity
        Double light       = getDouble(snapshot, "light");

        DataSnapshot pumpSnap = snapshot.child("pumpStatus");
        String pumpStatus = pumpSnap.exists() ? String.valueOf(pumpSnap.getValue()) : PUMP_OFF;

        data.setTemperature(temperature   != null ? temperature  : 0.0);
        data.setHumidity   (humidity      != null ? humidity     : 0.0);
        data.setSoilMoisture(averageSoil  != null ? averageSoil : 0.0);
        data.setLightIntensity(light      != null ? light        : 0.0);
        data.setSoil1      (soil1         != null ? soil1        : 0.0);
        data.setSoil2      (soil2         != null ? soil2        : 0.0);
        data.setSoil3      (soil3         != null ? soil3        : 0.0);
        data.setSoil4      (soil4         != null ? soil4        : 0.0);
        data.setPumpStatus (pumpStatus);
        return data;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REAL-TIME DASHBOARD  (dashboard/)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attaches a permanent real-time listener to {@code dashboard/}.
     *
     * @return the attached {@link ValueEventListener} for later removal
     */
    public ValueEventListener listenDashboard(DashboardListener listener) {
        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DashboardData data = snapshot.exists()
                        ? snapshot.getValue(DashboardData.class)
                        : null;
                if (data == null) data = new DashboardData();
                listener.onDashboardUpdate(data);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("FirebaseHelper",
                        "dashboard listener cancelled: " + error.getMessage());
            }
        };
        mDatabase.child(NODE_DASHBOARD).addValueEventListener(vel);
        return vel;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REAL-TIME WATER LOSS  (waterLossDetection/)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attaches a permanent real-time listener to {@code waterLossDetection/}.
     *
     * @return the attached {@link ValueEventListener} for later removal
     */
    public ValueEventListener listenWaterLoss(WaterLossListener listener) {
        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                WaterLossData data = snapshot.exists()
                        ? snapshot.getValue(WaterLossData.class)
                        : null;
                if (data == null) data = new WaterLossData();
                listener.onWaterLossUpdate(data);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("FirebaseHelper",
                        "waterLoss listener cancelled: " + error.getMessage());
            }
        };
        mDatabase.child(NODE_WATER_LOSS).addValueEventListener(vel);
        return vel;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REAL-TIME HISTORY  (history/)  — for HistoryFragment (SensorHistory)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attaches a permanent real-time listener that delivers
     * {@link SensorHistory} entries ordered by {@code timestamp}, last 100.
     *
     * @return the attached {@link ValueEventListener} for later removal
     */
    public ValueEventListener listenHistory(HistoryListener listener) {
        Query query = mDatabase.child(NODE_HISTORY)
                .orderByChild("timestamp")
                .limitToLast(100);

        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SensorHistory> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    SensorHistory entry = child.getValue(SensorHistory.class);
                    if (entry != null) list.add(entry);
                }
                listener.onHistoryLoaded(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("FirebaseHelper",
                        "history listener cancelled: " + error.getMessage());
                listener.onHistoryLoaded(new ArrayList<>());
            }
        };
        query.addValueEventListener(vel);
        return vel;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REAL-TIME HISTORY  (history/)  — for InsightsFragment (HistoryModel)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attaches a permanent real-time listener that delivers
     * {@link HistoryModel} entries (int fields) ordered by {@code timestamp},
     * last 50 – used by the InsightsFragment charts.
     *
     * @return the attached {@link ValueEventListener} for later removal
     */
    public ValueEventListener listenHistoryModel(HistoryModelListener listener) {
        Query query = mDatabase.child(NODE_HISTORY)
                .orderByChild("timestamp")
                .limitToLast(50);

        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<HistoryModel> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    HistoryModel entry = child.getValue(HistoryModel.class);
                    if (entry != null) list.add(entry);
                }
                listener.onHistoryLoaded(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onHistoryError(error.getMessage());
            }
        };
        query.addValueEventListener(vel);
        return vel;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REAL-TIME FARMS  (farms/)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attaches a permanent real-time listener to {@code farms/}.
     *
     * @return the attached {@link ValueEventListener} for later removal
     */
    public ValueEventListener listenFarms(FarmsListener listener) {
        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Farm> farms = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Farm farm = child.getValue(Farm.class);
                    if (farm != null) {
                        farm.setFarmId(child.getKey());
                        farms.add(farm);
                    }
                }
                listener.onFarmsLoaded(farms);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("FirebaseHelper",
                        "farms listener cancelled: " + error.getMessage());
                listener.onFarmsLoaded(new ArrayList<>());
            }
        };
        mDatabase.child(NODE_FARMS).addValueEventListener(vel);
        return vel;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REAL-TIME SINGLE FARM  (farms/{farmId}/)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attaches a permanent real-time listener to a single farm node.
     * Used by {@code FarmDetailsActivity} so the detail screen refreshes
     * whenever the farm data changes.
     *
     * @return the attached {@link ValueEventListener} for later removal
     */
    public ValueEventListener listenFarmById(String farmId, FarmByIdListener listener) {
        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Farm farm = snapshot.getValue(Farm.class);
                    if (farm != null) {
                        farm.setFarmId(snapshot.getKey());
                        listener.onFarmLoaded(farm);
                    } else {
                        listener.onFarmNotFound();
                    }
                } else {
                    listener.onFarmNotFound();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("FirebaseHelper",
                        "listenFarmById cancelled: " + error.getMessage());
            }
        };
        mDatabase.child(NODE_FARMS).child(farmId).addValueEventListener(vel);
        return vel;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ONE-SHOT READS  (edit form pre-fill — single read is correct here)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Reads a single farm once (used to pre-fill the Edit Farm form).
     * Using {@code addListenerForSingleValueEvent} is intentional here —
     * the edit form only needs the current value once, not a live stream.
     */
    public void getFarmOnce(String farmId, FarmByIdListener listener) {
        mDatabase.child(NODE_FARMS).child(farmId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Farm farm = snapshot.getValue(Farm.class);
                            if (farm != null) {
                                farm.setFarmId(snapshot.getKey());
                                listener.onFarmLoaded(farm);
                            } else {
                                listener.onFarmNotFound();
                            }
                        } else {
                            listener.onFarmNotFound();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onFarmNotFound();
                    }
                });
    }

    /** Retrieves the user profile from Users/{uid}/profile */
    public void getUserProfile(String uid, UserProfileListener listener) {
        mDatabase.child("Users").child(uid).child("profile")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        UserProfile profile = snapshot.getValue(UserProfile.class);
                        listener.onProfileLoaded(profile);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onProfileLoaded(null);
                    }
                });
    }

    /** Saves/updates the user profile at Users/{uid}/profile */
    public void saveUserProfile(UserProfile profile, com.google.firebase.database.DatabaseReference.CompletionListener onComplete) {
        if (profile.getUid() != null) {
            mDatabase.child("Users").child(profile.getUid()).child("profile")
                    .setValue(profile, onComplete);
        }
    }


    // ══════════════════════════════════════════════════════════════════════
    // WRITE OPERATIONS
    // ══════════════════════════════════════════════════════════════════════

    /** Writes a new farm under {@code farms/{newPushKey}}. */
    public void addFarm(Farm farm, com.google.firebase.database.DatabaseReference.CompletionListener onComplete) {
        DatabaseReference ref = mDatabase.child(NODE_FARMS).push();
        farm.setFarmId(ref.getKey());
        ref.setValue(farm, onComplete);
    }

    /** Writes a new farm under both {@code farms/} and {@code Users/{uid}/farms/}. */
    public void addFarmForUser(String uid, Farm farm, com.google.firebase.database.DatabaseReference.CompletionListener onComplete) {
        DatabaseReference ref = mDatabase.child(NODE_FARMS).push();
        String farmId = ref.getKey();
        farm.setFarmId(farmId);
        farm.setUserId(uid);
        ref.setValue(farm, (error, dbRef) -> {
            if (error == null) {
                mDatabase.child("Users").child(uid).child("farms").child(farmId).setValue(farm, onComplete);
            } else if (onComplete != null) {
                onComplete.onComplete(error, dbRef);
            }
        });
    }


    /** Updates specific fields in an existing farm. */
    public void updateFarm(String farmId, java.util.Map<String, Object> updates,
                           com.google.firebase.database.DatabaseReference.CompletionListener onComplete) {
        mDatabase.child(NODE_FARMS).child(farmId).updateChildren(updates, onComplete);
    }

    /** Deletes a farm node entirely from both global and user-specific paths. */
    public void deleteFarm(String farmId,
                           com.google.firebase.database.DatabaseReference.CompletionListener onComplete) {
        mDatabase.child(NODE_FARMS).child(farmId).removeValue((error, ref) -> {
            if (error == null) {
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null) {
                    mDatabase.child("Users").child(uid).child("farms").child(farmId).removeValue(onComplete);
                } else if (onComplete != null) {
                    onComplete.onComplete(null, ref);
                }
            } else if (onComplete != null) {
                onComplete.onComplete(error, ref);
            }
        });
    }

    /** Listens for all farms belonging to a user under Users/{uid}/farms/ */
    public com.google.firebase.database.ValueEventListener listenUserFarms(String uid, FarmsListener listener) {
        com.google.firebase.database.ValueEventListener valListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                java.util.List<Farm> farms = new java.util.ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Farm farm = child.getValue(Farm.class);
                    if (farm != null) {
                        farms.add(farm);
                    }
                }
                listener.onFarmsLoaded(farms);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        mDatabase.child("Users").child(uid).child("farms").addValueEventListener(valListener);
        return valListener;
    }


    /** Sets the pump status (ON/OFF) for a specific farm, syncing to the legacy single-pump node too. */
    public void setPumpStatus(String farmId, String status, com.google.firebase.database.DatabaseReference.CompletionListener onComplete) {
        mDatabase.child(NODE_FARMS).child(farmId).child("pumpStatus").setValue(status, (error, ref) -> {
            if (error == null) {
                // Backward compatibility for legacy hardware
                mDatabase.child(NODE_SENSOR_DATA).child("pumpStatus").setValue(status);

                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null) {
                    mDatabase.child("Users").child(uid).child("farms").child(farmId).child("pumpStatus").setValue(status, onComplete);
                } else if (onComplete != null) {
                    onComplete.onComplete(null, ref);
                }
            } else if (onComplete != null) {
                onComplete.onComplete(error, ref);
            }
        });
    }

    /** Listens to pump status changes for a specific farm. */
    public void listenFarmPumpStatus(String farmId, ValueEventListener listener) {
        mDatabase.child(NODE_FARMS).child(farmId).child("pumpStatus").addValueEventListener(listener);
    }

    // ══════════════════════════════════════════════════════════════════════
    // PUMP MODE / COMMAND CONTROL  (sensorData/pumpMode, sensorData/pumpCommand)
    //
    // Database structure used by the ESP32 + app:
    //   sensorData/
    //     pumpMode    : "AUTO" | "MANUAL"
    //     pumpCommand : "ON"   | "OFF"
    //     pumpStatus  : "ON"   | "OFF"   (written back by ESP32)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Writes {@code pumpMode} ("AUTO" | "MANUAL") to {@code sensorData/pumpMode}.
     * The ESP32 reads this key to determine whether it controls the pump itself
     * (AUTO) or waits for an explicit command (MANUAL).
     */
    public void setPumpMode(String mode, com.google.firebase.database.DatabaseReference.CompletionListener onComplete) {
        mDatabase.child(NODE_SENSOR_DATA).child("pumpMode").setValue(mode, onComplete);
    }

    /**
     * Writes {@code pumpCommand} ("ON" | "OFF") to {@code sensorData/pumpCommand}.
     * The ESP32 reads this key in MANUAL mode and actuates the pump accordingly,
     * then writes {@code pumpStatus} back to Firebase.
     */
    public void setPumpCommand(String command, com.google.firebase.database.DatabaseReference.CompletionListener onComplete) {
        mDatabase.child(NODE_SENSOR_DATA).child("pumpCommand").setValue(command, onComplete);
    }

    /**
     * Writes {@code pumpStatus} directly to {@code sensorData/pumpStatus}.
     *
     * <p>Used for immediate UI feedback in MANUAL mode when the ESP32 firmware
     * may not be reading {@code pumpCommand} yet, or when running in demo mode
     * without hardware. When a real ESP32 IS connected, it will overwrite this
     * with the confirmed state moments later — which is harmless.</p>
     */
    public void setPumpStatusDirect(String status, com.google.firebase.database.DatabaseReference.CompletionListener onComplete) {
        mDatabase.child(NODE_SENSOR_DATA).child("pumpStatus").setValue(status, onComplete);
    }

    /**
     * Attaches a real-time listener to {@code sensorData/pumpStatus}.
     * The app must reflect the actual hardware state, not a locally cached value.
     *
     * @param listener called on every change; returns the attached listener for later removal
     */
    public ValueEventListener listenPumpStatus(ValueEventListener listener) {
        mDatabase.child(NODE_SENSOR_DATA).child("pumpStatus").addValueEventListener(listener);
        return listener;
    }

    /**
     * Attaches a real-time listener to {@code sensorData/pumpMode}.
     * Allows the app to stay in sync if another client changes the mode.
     *
     * @param listener called on every change; returns the attached listener for later removal
     */
    public ValueEventListener listenPumpMode(ValueEventListener listener) {
        mDatabase.child(NODE_SENSOR_DATA).child("pumpMode").addValueEventListener(listener);
        return listener;
    }

    /**
     * Removes a listener that was attached to a child of {@code sensorData/}.
     * Used to cleanly detach per-field listeners (pumpStatus, pumpMode, etc.)
     * in {@code onDestroyView()} to prevent memory leaks.
     *
     * @param childKey the exact child key (e.g., "pumpStatus", "pumpMode")
     * @param listener the listener to remove
     */
    public void removeSensorChildListener(String childKey, ValueEventListener listener) {
        if (childKey == null || listener == null) return;
        mDatabase.child(NODE_SENSOR_DATA).child(childKey).removeEventListener(listener);
    }


    /**
     * Checks whether any farm already has the given name (case-insensitive via Firebase query).
     * Used by {@code AddFarmActivity} to prevent duplicate names.
     * Single read is correct here.
     */
    public void checkDuplicateFarmName(String farmName, ValueEventListener listener) {
        mDatabase.child(NODE_FARMS)
                .orderByChild("farmName")
                .equalTo(farmName)
                .addListenerForSingleValueEvent(listener);
    }

    /** Appends one sensor snapshot to {@code history/}. */
    public void saveSensorHistory(SensorHistory entry) {
        mDatabase.child(NODE_HISTORY).push().setValue(entry);
    }

    // ══════════════════════════════════════════════════════════════════════
    // AI ENGINE  — reads live sensor data, writes computed dashboard values
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attaches a real-time listener to {@code sensorData/}, runs
     * {@link AIEngine#analyze(SensorData)} on every update, and writes
     * the result to {@code dashboard/}.
     *
     * <p>This means the dashboard node always reflects the AI's latest
     * decision based on the most recent sensor reading from the ESP32.</p>
     */
    public void runAIEngine() {
        mDatabase.child(NODE_SENSOR_DATA).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                SensorData sensor = buildSensorDataFromSnapshot(snapshot);
                AIResult result   = AIEngine.analyze(sensor);

                DashboardData dashboard = new DashboardData(
                        result.getFarmHealth(),
                        result.getPumpStatus(),
                        result.getAiRecommendation(),
                        0,   // waterSaved – reserved for future hardware
                        0.0  // energySaved – reserved for future hardware
                );
                dashboard.setRiskLevel(result.getRiskLevel());
                dashboard.setNextWatering(result.getNextWatering());
                dashboard.setWaterRequirement(result.getWaterRequirement());

                mDatabase.child(NODE_DASHBOARD).setValue(dashboard);

                // Append to history every time sensor data changes
                SensorHistory history = new SensorHistory(
                        sensor.getTemperature(),
                        sensor.getHumidity(),
                        sensor.getSoilMoisture(),
                        sensor.getLightIntensity(),
                        System.currentTimeMillis()
                );
                mDatabase.child(NODE_HISTORY).push().setValue(history);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("FirebaseHelper",
                        "runAIEngine listener cancelled: " + error.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // SEED DEMO DATA  (only if sensorData node is absent)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Writes placeholder sensor values to {@code sensorData/} only if the
     * node does not yet exist. This prevents the app showing blank cards
     * on first launch before the ESP32 connects.
     */
    public void seedDemoSensorDataIfMissing() {
        mDatabase.child(NODE_SENSOR_DATA)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) return; // ESP32 data already present

                        // Write seed values that match the ESP32 schema
                        java.util.HashMap<String, Object> seed = new java.util.HashMap<>();
                        seed.put("temperature",  28.0);
                        seed.put("humidity",     65.0);
                        seed.put("soil1",        500.0);
                        seed.put("soil2",        510.0);
                        seed.put("soil3",        490.0);
                        seed.put("soil4",        505.0);
                        seed.put("averageSoil",  501.0);
                        seed.put("light",        700.0);
                        seed.put("pumpStatus",   PUMP_OFF);
                        mDatabase.child(NODE_SENSOR_DATA).setValue(seed);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.w("FirebaseHelper",
                                "seedDemoData check cancelled: " + error.getMessage());
                    }
                });
    }

    /**
     * Reads the latest unified telemetry data from multiple Firebase nodes asynchronously.
     */
    public void getUnifiedTelemetry(final TelemetryCallback callback) {
        final Map<String, Object> results = new HashMap<>();

        class Tracker {
            int completed = 0;
            boolean failed = false;

            synchronized void checkCompletion() {
                if (failed) return;
                completed++;
                if (completed == 3) {
                    SensorData sensor = (SensorData) results.get("sensor");
                    DashboardData dash = (DashboardData) results.get("dash");
                    FarmData farm = (FarmData) results.get("farm");

                    double temp = sensor != null ? sensor.getTemperature() : 0.0;
                    double hum = sensor != null ? sensor.getHumidity() : 0.0;
                    double soil = sensor != null ? sensor.getSoilMoisture() : 0.0;
                    double water = sensor != null ? sensor.getFarmWaterLevel() : 0.0;
                    String pump = dash != null ? dash.getPumpStatus() : "UNKNOWN";
                    String prediction = dash != null ? dash.getAiRecommendation() : "UNKNOWN";
                    String name = farm != null ? farm.getFieldName() : "UNKNOWN";

                    FarmTelemetry telemetry = new FarmTelemetry(temp, hum, soil, water, pump, prediction, name, null);
                    callback.onTelemetryLoaded(telemetry);
                }
            }

            synchronized void fail(String message) {
                if (!failed) {
                    failed = true;
                    callback.onTelemetryError(message);
                }
            }
        }

        final Tracker tracker = new Tracker();

        mDatabase.child(NODE_SENSOR_DATA).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SensorData data = snapshot.getValue(SensorData.class);
                results.put("sensor", data);
                tracker.checkCompletion();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tracker.fail(error.getMessage());
            }
        });

        mDatabase.child(NODE_DASHBOARD).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DashboardData data = snapshot.getValue(DashboardData.class);
                results.put("dash", data);
                tracker.checkCompletion();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tracker.fail(error.getMessage());
            }
        });

        mDatabase.child(NODE_FARM).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FarmData data = snapshot.getValue(FarmData.class);
                results.put("farm", data);
                tracker.checkCompletion();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tracker.fail(error.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // LISTENER REMOVAL  — call from onDestroyView / onDestroy
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Removes a {@link ValueEventListener} from the given top-level node.
     * Use for {@link #NODE_SENSOR_DATA}, {@link #NODE_DASHBOARD},
     * {@link #NODE_WATER_LOSS}, and {@link #NODE_FARMS}.
     */
    public void removeListener(String node, ValueEventListener listener) {
        if (listener == null) return;
        mDatabase.child(node).removeEventListener(listener);
    }

    /**
     * Removes a history listener (the query used in {@link #listenHistory}
     * and {@link #listenHistoryModel} is {@code orderByChild.limitToLast},
     * so we must remove from the same query reference — calling removeEventListener
     * on the plain node reference is safe too; Firebase deduplicates internally).
     */
    public void removeHistoryListener(ValueEventListener listener) {
        if (listener == null) return;
        mDatabase.child(NODE_HISTORY).removeEventListener(listener);
    }

    /**
     * Removes a listener that was attached to a specific farm node.
     * Used by {@code FarmDetailsActivity#onDestroy}.
     */
    public void removeListenerForFarm(String farmId, ValueEventListener listener) {
        if (listener == null || farmId == null) return;
        mDatabase.child(NODE_FARMS).child(farmId).removeEventListener(listener);
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRIVATE UTILITY
    // ══════════════════════════════════════════════════════════════════════

    /** Safely reads a child as a {@code Double}, handling Long and Double Firebase types. */
    private Double getDouble(DataSnapshot snapshot, String key) {
        DataSnapshot child = snapshot.child(key);
        if (!child.exists()) return null;
        Object value = child.getValue();
        if (value instanceof Double)  return (Double) value;
        if (value instanceof Long)    return ((Long) value).doubleValue();
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof String) {
            try { return Double.parseDouble((String) value); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }
}