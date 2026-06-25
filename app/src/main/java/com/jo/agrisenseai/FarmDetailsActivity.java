package com.jo.agrisenseai;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FarmDetailsActivity extends AppCompatActivity {

    private String farmId;
    private ValueEventListener farmListener;
    private ValueEventListener sensorListener;

    private Farm currentFarm;
    private SensorData currentSensorData;

    // Views
    private TextView tvFarmName, tvFarmLocation, tvHealthBadge;
    private TextView tvCropType, tvArea, tvSoilMoisture;
    private TextView tvNextWatering, tvMoistureThreshold, tvIrrigationSchedule;
    private TextView tvCreatedAt, tvNotes;
    
    // New Sensor and AI Views
    private TextView tvTemperature, tvHumidity, tvWaterLevel, tvAiStatus, tvPumpStatus;
    
    private MaterialButton btnEditFarm, btnDeleteFarm;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farm_details);

        farmId = getIntent().getStringExtra("farmId");
        if (farmId == null) {
            finish();
            return;
        }

        // Bind views
        tvFarmName           = findViewById(R.id.tvFarmName);
        tvFarmLocation       = findViewById(R.id.tvFarmLocation);
        tvHealthBadge        = findViewById(R.id.tvHealthBadge);
        tvCropType           = findViewById(R.id.tvCropType);
        tvArea               = findViewById(R.id.tvArea);
        tvSoilMoisture       = findViewById(R.id.tvSoilMoisture);
        tvNextWatering       = findViewById(R.id.tvNextWatering);
        tvMoistureThreshold  = findViewById(R.id.tvMoistureThreshold);
        tvIrrigationSchedule = findViewById(R.id.tvIrrigationSchedule);
        tvCreatedAt          = findViewById(R.id.tvCreatedAt);
        tvNotes              = findViewById(R.id.tvNotes);
        
        // New Sensor & AI bindings
        tvTemperature        = findViewById(R.id.tvTemperature);
        tvHumidity           = findViewById(R.id.tvHumidity);
        tvWaterLevel         = findViewById(R.id.tvWaterLevel);
        tvAiStatus           = findViewById(R.id.tvAiStatus);
        tvPumpStatus         = findViewById(R.id.tvPumpStatus);
        
        btnEditFarm          = findViewById(R.id.btnEditFarm);
        btnDeleteFarm        = findViewById(R.id.btnDeleteFarm);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finishWithTransition());

        // Edit button → launch EditFarmActivity
        btnEditFarm.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditFarmActivity.class);
            intent.putExtra("farmId", farmId);
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                    this, android.R.anim.fade_in, android.R.anim.fade_out);
            startActivity(intent, options.toBundle());
        });

        // Delete button → confirmation dialog
        btnDeleteFarm.setOnClickListener(v -> showDeleteDialog());

        // Start real-time listeners for farm and global sensor data
        listenForFarm();
        listenForSensors();
    }

    private void listenForFarm() {
        farmListener = FirebaseHelper.getInstance().listenFarmById(farmId, new FirebaseHelper.FarmByIdListener() {
            @Override
            public void onFarmLoaded(Farm farm) {
                currentFarm = farm;
                updatePredictionAndUi();
            }

            @Override
            public void onFarmNotFound() {
                // Farm was deleted → return to My Farm
                finish();
            }
        });
    }

    private void listenForSensors() {
        sensorListener = FirebaseHelper.getInstance().listenSensorData(new FirebaseHelper.SensorListener() {
            @Override
            public void onSensorUpdate(SensorData data) {
                currentSensorData = data;
                updatePredictionAndUi();
            }
        });
    }

    private void updatePredictionAndUi() {
        if (currentFarm == null) {
            return;
        }

        // 1. Populate basic farm details
        tvFarmName.setText(currentFarm.getFarmName() != null ? currentFarm.getFarmName() : "—");
        tvFarmLocation.setText(currentFarm.getLocation() != null ? currentFarm.getLocation() : "—");
        tvCropType.setText(currentFarm.getCropType() != null ? currentFarm.getCropType() : "—");
        tvArea.setText(currentFarm.getTotalAcres() != null ? currentFarm.getTotalAcres() : "—");
        tvMoistureThreshold.setText(String.valueOf(currentFarm.getMoistureThreshold()));
        tvIrrigationSchedule.setText(currentFarm.getIrrigationSchedule() != null ? currentFarm.getIrrigationSchedule() : "—");
        tvNotes.setText(currentFarm.getNotes() != null && !currentFarm.getNotes().isEmpty() ? currentFarm.getNotes() : "No notes");

        // Format created date
        if (currentFarm.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
            tvCreatedAt.setText(sdf.format(new Date(currentFarm.getCreatedAt())));
        } else {
            tvCreatedAt.setText("—");
        }

        String status = currentFarm.getHealthStatus() != null ? currentFarm.getHealthStatus() : "Healthy";

        // 2. Populate sensor and AI irrigation prediction values if loaded
        if (currentSensorData != null) {
            double soilMoisture = currentSensorData.getSoilMoisture();
            int moistureThreshold = currentFarm.getMoistureThreshold();
            double moistureDeficit = moistureThreshold - soilMoisture;

            // Calculate health status dynamically from real sensor readings
            if (soilMoisture >= moistureThreshold) {
                status = "Healthy";
            } else if (moistureDeficit <= 50) {
                status = "Monitor";
            } else if (moistureDeficit < 150) {
                status = "Water Required";
            } else {
                status = "Critical";
            }

            tvSoilMoisture.setText(String.format(Locale.getDefault(), "%.0f", soilMoisture));
            tvTemperature.setText(String.format(Locale.getDefault(), "%.1f°C", currentSensorData.getTemperature()));
            tvHumidity.setText(String.format(Locale.getDefault(), "%.1f%%", currentSensorData.getHumidity()));
            tvWaterLevel.setText(String.format(Locale.getDefault(), "%.0f%%", currentSensorData.getFarmWaterLevel()));
            tvPumpStatus.setText(currentSensorData.getPumpStatus() != null ? currentSensorData.getPumpStatus() : "—");

            if ("ON".equalsIgnoreCase(currentSensorData.getPumpStatus())) {
                tvPumpStatus.setTextColor(ContextCompat.getColor(this, R.color.status_healthy));
            } else {
                tvPumpStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }

            // Calculate estimated next watering
            IrrigationPredictionHelper.PredictionResult prediction = IrrigationPredictionHelper.calculateNextWatering(
                    System.currentTimeMillis(),
                    soilMoisture,
                    currentSensorData.getTemperature(),
                    currentSensorData.getHumidity(),
                    currentSensorData.getFarmWaterLevel(),
                    currentFarm.getCropType(),
                    moistureThreshold
            );

            tvNextWatering.setText(prediction.estimatedTimeLabel);
            tvAiStatus.setText(prediction.aiAnalysisText);

            // Sync dynamic values back to Firebase farm node so RecyclerView stays in sync
            boolean nextWateringChanged = !prediction.estimatedTimeLabel.equals(currentFarm.getNextWatering());
            boolean healthStatusChanged = !status.equals(currentFarm.getHealthStatus());
            boolean moistureChanged = (int) soilMoisture != currentFarm.getSoilMoisture();

            if (nextWateringChanged || healthStatusChanged || moistureChanged) {
                Map<String, Object> syncUpdates = new HashMap<>();
                syncUpdates.put("nextWatering", prediction.estimatedTimeLabel);
                syncUpdates.put("healthStatus", status);
                syncUpdates.put("soilMoisture", (int) soilMoisture);
                
                FirebaseHelper.getInstance().updateFarm(farmId, syncUpdates, null);
            }
        } else {
            // Fallback before sensor data loads
            tvSoilMoisture.setText(String.valueOf(currentFarm.getSoilMoisture()));
            tvNextWatering.setText(currentFarm.getNextWatering() != null ? currentFarm.getNextWatering() : "—");
            tvTemperature.setText("—");
            tvHumidity.setText("—");
            tvWaterLevel.setText("—");
            tvAiStatus.setText("Waiting for sensor data...");
            tvPumpStatus.setText("—");
        }

        // Set Health Badge text and background styling dynamically
        tvHealthBadge.setText(status);

        int textColorRes;
        int bgColorRes;
        switch (status) {
            case "Critical":
                textColorRes = R.color.status_critical;
                bgColorRes   = R.color.icon_bg_red;
                break;
            case "Water Required":
            case "High":
                textColorRes = R.color.status_medium;
                bgColorRes   = R.color.icon_bg_orange;
                break;
            case "Monitor":
            case "Medium":
                textColorRes = R.color.status_medium;
                bgColorRes   = R.color.icon_bg_orange;
                break;
            default:
                textColorRes = R.color.status_healthy;
                bgColorRes   = R.color.icon_bg_green;
                break;
        }
        tvHealthBadge.setTextColor(ContextCompat.getColor(this, textColorRes));
        tvHealthBadge.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(this, bgColorRes)));
    }

    private void showDeleteDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Farm?")
                .setMessage("Are you sure you want to permanently delete this farm? This action cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> performDelete())
                .show();
    }

    private void performDelete() {
        btnDeleteFarm.setEnabled(false);
        FirebaseHelper.getInstance().deleteFarm(farmId, (error, ref) -> {
            if (error == null) {
                showSnackbar("Farm Deleted Successfully");
                // Small delay to show Snackbar before finishing
                findViewById(android.R.id.content).postDelayed(this::finishWithTransition, 1200);
            } else {
                btnDeleteFarm.setEnabled(true);
                showSnackbar("Error deleting farm: " + error.getMessage());
            }
        });
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.primary_green))
                .setTextColor(ContextCompat.getColor(this, R.color.white))
                .show();
    }

    private void finishWithTransition() {
        supportFinishAfterTransition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (farmListener != null && farmId != null) {
            FirebaseHelper.getInstance().removeListenerForFarm(farmId, farmListener);
        }
        if (sensorListener != null) {
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_SENSOR_DATA, sensorListener);
        }
    }
}
