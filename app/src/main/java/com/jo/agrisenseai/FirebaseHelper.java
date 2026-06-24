package com.jo.agrisenseai;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
 */
public class FirebaseHelper {

    public static final String NODE_SENSOR_DATA = "sensorData";
    public static final String NODE_DASHBOARD = "dashboard";
    public static final String NODE_FARM = "farm";
    public static final String NODE_FARMS = "farms";
    public static final String NODE_WATER_LOSS = "waterLossDetection";
    public static final String PUMP_ON = "ON";

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

    public interface FarmsListener {
        void onFarmsUpdate(java.util.List<Farm> farms);
    }

    public interface FarmByIdListener {
        void onFarmLoaded(Farm farm);
        void onFarmNotFound();
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

    public void removeListener(String node, ValueEventListener listener) {
        rootRef.child(node).removeEventListener(listener);
    }

    /** Remove a real-time listener attached to a specific farm node. */
    public void removeListenerForFarm(String farmId, ValueEventListener listener) {
        rootRef.child(NODE_FARMS).child(farmId).removeEventListener(listener);
    }


    /** Listen for all farms dynamically in real-time. */
    public ValueEventListener listenFarms(final FarmsListener listener) {
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                java.util.List<Farm> farms = new java.util.ArrayList<>();
                for (DataSnapshot farmSnapshot : snapshot.getChildren()) {
                    Farm farm = farmSnapshot.getValue(Farm.class);
                    if (farm != null) {
                        if (farm.getFarmId() == null || farm.getFarmId().isEmpty()) {
                            farm.setFarmId(farmSnapshot.getKey());
                        }
                        farms.add(farm);
                    }
                }
                listener.onFarmsUpdate(farms);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        rootRef.child(NODE_FARMS).addValueEventListener(valueEventListener);
        return valueEventListener;
    }

    /** Add a new farm with a generated key. */
    public void addFarm(Farm farm, com.google.firebase.database.DatabaseReference.CompletionListener listener) {
        DatabaseReference farmsReference = rootRef.child(NODE_FARMS);
        String key = farmsReference.push().getKey();
        if (key != null) {
            farm.setFarmId(key);
            farmsReference.child(key).setValue(farm, listener);
        }
    }

    /** Query database to check if a farm name already exists. */
    public void checkDuplicateFarmName(String farmName, ValueEventListener listener) {
        rootRef.child(NODE_FARMS).orderByChild("farmName").equalTo(farmName).addListenerForSingleValueEvent(listener);
    }

    /**
     * Real-time listener for a single farm node.
     * Used by FarmDetailsActivity to keep the UI in sync automatically.
     */
    public ValueEventListener listenFarmById(String farmId, final FarmByIdListener listener) {
        ValueEventListener vel = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Farm farm = snapshot.getValue(Farm.class);
                    if (farm != null) {
                        if (farm.getFarmId() == null || farm.getFarmId().isEmpty()) {
                            farm.setFarmId(snapshot.getKey());
                        }
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
        };
        rootRef.child(NODE_FARMS).child(farmId).addValueEventListener(vel);
        return vel;
    }

    /** Partial update of an existing farm using updateChildren(). */
    public void updateFarm(String farmId, Map<String, Object> updates,
                           DatabaseReference.CompletionListener listener) {
        rootRef.child(NODE_FARMS).child(farmId).updateChildren(updates, listener);
    }

    /** Permanently remove a farm record. */
    public void deleteFarm(String farmId, DatabaseReference.CompletionListener listener) {
        rootRef.child(NODE_FARMS).child(farmId).removeValue(listener);
    }

    /** Fetch a single farm once (no continuous listener). Used by EditFarmActivity. */
    public void getFarmOnce(String farmId, final FarmByIdListener listener) {
        rootRef.child(NODE_FARMS).child(farmId)
               .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Farm farm = snapshot.getValue(Farm.class);
                    if (farm != null) {
                        if (farm.getFarmId() == null || farm.getFarmId().isEmpty()) {
                            farm.setFarmId(snapshot.getKey());
                        }
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
     * Safe to call once for the lifetime of the app (e.g. from MainActivity).
     */
    public ValueEventListener runAIEngine() {
        return listenSensorData(sensorData -> {
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
        dashboardUpdates.put("farmHealth", result.getFarmHealth());
        dashboardUpdates.put("pumpStatus", result.getPumpStatus());
        dashboardUpdates.put("aiRecommendation", result.getAiRecommendation());
        dashboardUpdates.put("riskLevel", result.getRiskLevel());
        dashboardUpdates.put("nextWatering", result.getNextWatering());
        dashboardUpdates.put("waterRequirement", result.getWaterRequirement());
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

    public void updateSensorData(SensorData data) {
        rootRef.child(NODE_SENSOR_DATA).setValue(data);
    }

    /** Convenience method: toggle pump status only. */
    public void setPumpStatus(String status) {
        rootRef.child(NODE_DASHBOARD).child("pumpStatus").setValue(status);
    }
}
