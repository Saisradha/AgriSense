package com.jo.agrisenseai;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final double RAW_MOISTURE_MAX = 1023.0;

    private TextView farmHealthValueText;
    private TextView farmHealthStatusText;
    private CircularProgressIndicator farmHealthProgress;
    private TextView aiRecommendationText;
    private TextView pumpStatusBadge;
    private TextView pumpSubtitleText;
    private TextView waterSavedValueText;
    private TextView energySavedValueText;
    private TextView riskLevelBadge;
    private View notifUnreadDot;

    private TextView soilMoistureValueText;
    private TextView temperatureValueText;
    private TextView humidityValueText;
    private TextView waterLevelValueText;

    private TextView wlStatusBadge;
    private TextView wlBoundaryText;
    private TextView wlEstimatedLossText;

    // Farm selector & Greeting views
    private TextView tvWelcomeGreeting;
    private TextView tvFarmerName;
    private TextView tvSelectedFarm;
    private View cardFarmSelector;
    private MaterialButton btnWaterNow;

    private ValueEventListener dashboardListener;
    private ValueEventListener sensorListener;
    private ValueEventListener unreadListener;
    private ValueEventListener waterLossListener;
    private ValueEventListener farmPumpListener;

    private List<Farm> mUserFarms = new ArrayList<>();
    private Farm mSelectedFarm = null;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mRefreshRunnable;
    private UserProfile mUserProfile = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Bind standard views
        farmHealthValueText = view.findViewById(R.id.farmHealthValueText);
        farmHealthStatusText = view.findViewById(R.id.farmHealthStatusText);
        farmHealthProgress = view.findViewById(R.id.farmHealthProgress);
        aiRecommendationText = view.findViewById(R.id.aiRecommendationText);
        pumpStatusBadge = view.findViewById(R.id.pumpStatusBadge);
        pumpSubtitleText = view.findViewById(R.id.pumpSubtitleText);
        waterSavedValueText = view.findViewById(R.id.waterSavedValueText);
        energySavedValueText = view.findViewById(R.id.energySavedValueText);
        riskLevelBadge = view.findViewById(R.id.riskLevelBadge);
        notifUnreadDot = view.findViewById(R.id.notifUnreadDot);

        wlStatusBadge = view.findViewById(R.id.wlStatusBadge);
        wlBoundaryText = view.findViewById(R.id.wlBoundaryText);
        wlEstimatedLossText = view.findViewById(R.id.wlEstimatedLossText);

        soilMoistureValueText = view.findViewById(R.id.soilMoistureValueText);
        temperatureValueText = view.findViewById(R.id.temperatureValueText);
        humidityValueText = view.findViewById(R.id.humidityValueText);
        waterLevelValueText = view.findViewById(R.id.waterLevelValueText);

        // Bind new greeting and selector views
        tvWelcomeGreeting = view.findViewById(R.id.tvWelcomeGreeting);
        tvFarmerName = view.findViewById(R.id.tvFarmerName);
        tvSelectedFarm = view.findViewById(R.id.tvSelectedFarm);
        cardFarmSelector = view.findViewById(R.id.cardFarmSelector);
        btnWaterNow = view.findViewById(R.id.btnWaterNow);

        // Notification Bell Click
        view.findViewById(R.id.notifBellContainer).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationActivity.class)));

        // Farm Selection Click
        cardFarmSelector.setOnClickListener(v -> showFarmSelectorDialog());

        // Water Now checks connection first
        btnWaterNow.setOnClickListener(v -> onWaterNowClicked());

        // Load data
        loadFarmerProfile();
        loadUserFarms();
        loadDashboardData();
        loadSensorData();
        loadWaterLossData();
        watchUnreadNotifications();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoRefreshTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefreshTimer();
    }

    private void loadFarmerProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseHelper.getInstance().getUserProfile(user.getUid(), profile -> {
                if (!isAdded()) return;
                mUserProfile = profile;
                if (profile != null && profile.getName() != null) {
                    tvFarmerName.setText(profile.getName());
                } else {
                    tvFarmerName.setText("Farmer");
                }
            });
        }
    }

    private void loadUserFarms() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseHelper.getInstance().listenUserFarms(user.getUid(), farms -> {
                if (!isAdded()) return;
                mUserFarms = farms;
                if (!farms.isEmpty()) {
                    if (mSelectedFarm == null) {
                        selectFarm(farms.get(0));
                    } else {
                        // Refresh current selected farm data from the list
                        for (Farm f : farms) {
                            if (f.getFarmId().equals(mSelectedFarm.getFarmId())) {
                                mSelectedFarm = f;
                                tvSelectedFarm.setText(f.getFarmName());
                                updatePumpDisplay(f.getPumpStatus());
                                break;
                            }
                        }
                    }
                } else {
                    tvSelectedFarm.setText("No Farms Linked");
                    mSelectedFarm = null;
                }
            });
        }
    }

    private void selectFarm(Farm farm) {
        mSelectedFarm = farm;
        tvSelectedFarm.setText(farm.getFarmName());

        // Clean up previous pump listener if any
        if (farmPumpListener != null && mSelectedFarm != null) {
            FirebaseHelper.getInstance().removeListenerForFarm(mSelectedFarm.getFarmId(), farmPumpListener);
        }

        // Add real-time listener to this farm's pump status
        farmPumpListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                String status = snapshot.getValue(String.class);
                if (status != null && mSelectedFarm != null) {
                    mSelectedFarm.setPumpStatus(status);
                    updatePumpDisplay(status);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseHelper.getInstance().listenFarmPumpStatus(farm.getFarmId(), farmPumpListener);
    }

    private void showFarmSelectorDialog() {
        if (mUserFarms.isEmpty()) {
            Toast.makeText(requireContext(), "No farms configured. Add fields in My Farm tab.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] farmNames = new String[mUserFarms.size()];
        for (int i = 0; i < mUserFarms.size(); i++) {
            farmNames[i] = mUserFarms.get(i).getFarmName();
        }

        int checkedItem = -1;
        if (mSelectedFarm != null) {
            for (int i = 0; i < mUserFarms.size(); i++) {
                if (mUserFarms.get(i).getFarmId().equals(mSelectedFarm.getFarmId())) {
                    checkedItem = i;
                    break;
                }
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Active Farm")
                .setSingleChoiceItems(farmNames, checkedItem, (dialog, which) -> {
                    selectFarm(mUserFarms.get(which));
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Switched to " + mSelectedFarm.getFarmName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onWaterNowClicked() {
        if (mUserProfile == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseHelper.getInstance().getUserProfile(user.getUid(), profile -> {
                    mUserProfile = profile;
                    runWaterNowFlow();
                });
            } else {
                Toast.makeText(requireContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            }
        } else {
            runWaterNowFlow();
        }
    }

    private void runWaterNowFlow() {
        boolean isConnected = mUserProfile != null && (mUserProfile.isHasDevice() || mUserProfile.isDemoMode());

        if (isConnected) {
            showFarmSelectorForWatering();
        } else {
            new AlertDialog.Builder(requireContext())
                    .setTitle("AgriSense Device Required")
                    .setMessage("AgriSense ESP32 device is not connected. Please connect your hardware to enable automated irrigation controls, or run in Demo Mode.")
                    .setPositiveButton("Connect Device", (dialog, which) -> {
                        Intent intent = new Intent(requireContext(), DeviceCheckActivity.class);
                        startActivity(intent);
                    })
                    .setNeutralButton("Use Demo Mode", (dialog, which) -> {
                        enableDemoModeAndSelectFarm();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void enableDemoModeAndSelectFarm() {
        if (mUserProfile == null) return;
        mUserProfile.setDemoMode(true);
        mUserProfile.setHasDevice(false);

        FirebaseHelper.getInstance().saveUserProfile(mUserProfile, (error, ref) -> {
            if (!isAdded()) return;
            if (error == null) {
                Toast.makeText(requireContext(), "Simulated Demo Mode active", Toast.LENGTH_SHORT).show();
                showFarmSelectorForWatering();
            } else {
                Toast.makeText(requireContext(), "Failed to activate Demo Mode: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFarmSelectorForWatering() {
        if (mUserFarms.isEmpty()) {
            Toast.makeText(requireContext(), "No farms configured. Add fields in My Farm tab.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] farmNames = new String[mUserFarms.size()];
        for (int i = 0; i < mUserFarms.size(); i++) {
            farmNames[i] = mUserFarms.get(i).getFarmName();
        }

        int checkedItem = -1;
        if (mSelectedFarm != null) {
            for (int i = 0; i < mUserFarms.size(); i++) {
                if (mUserFarms.get(i).getFarmId().equals(mSelectedFarm.getFarmId())) {
                    checkedItem = i;
                    break;
                }
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Field to Water")
                .setSingleChoiceItems(farmNames, checkedItem, (dialog, which) -> {
                    Farm chosenFarm = mUserFarms.get(which);
                    selectFarm(chosenFarm);
                    dialog.dismiss();
                    toggleActivePumpForFarm(chosenFarm);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleActivePumpForFarm(Farm farm) {
        String currentStatus = farm.getPumpStatus();
        String targetStatus = "ON".equalsIgnoreCase(currentStatus) ? "OFF" : "ON";

        btnWaterNow.setEnabled(false);
        FirebaseHelper.getInstance().setPumpStatus(farm.getFarmId(), targetStatus, (error, ref) -> {
            if (!isAdded()) return;
            btnWaterNow.setEnabled(true);
            if (error == null) {
                farm.setPumpStatus(targetStatus);
                updatePumpDisplay(targetStatus);
                Toast.makeText(requireContext(), "Pump switched " + targetStatus + " for " + farm.getFarmName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to control pump: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void toggleActivePump() {
        if (mSelectedFarm == null) {
            Toast.makeText(requireContext(), "No active farm selected to irrigate", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentStatus = mSelectedFarm.getPumpStatus();
        String targetStatus = "ON".equalsIgnoreCase(currentStatus) ? "OFF" : "ON";

        btnWaterNow.setEnabled(false);
        FirebaseHelper.getInstance().setPumpStatus(mSelectedFarm.getFarmId(), targetStatus, (error, ref) -> {
            if (!isAdded()) return;
            btnWaterNow.setEnabled(true);
            if (error == null) {
                mSelectedFarm.setPumpStatus(targetStatus);
                updatePumpDisplay(targetStatus);
                Toast.makeText(requireContext(), "Pump switched " + targetStatus, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to control pump: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updatePumpDisplay(String status) {
        boolean isOn = "ON".equalsIgnoreCase(status);
        pumpStatusBadge.setText(isOn ? "ON" : "OFF");
        pumpSubtitleText.setText(isOn ? "Currently active" : "Currently inactive");

        pumpStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), isOn ? R.color.status_healthy : R.color.status_critical));
        pumpStatusBadge.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), isOn ? R.color.icon_bg_green : R.color.icon_bg_red)));

        btnWaterNow.setText(isOn ? "Stop Irrigation" : "Water Now");
        if (isOn) {
            btnWaterNow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.accent_red)));
        } else {
            btnWaterNow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_green)));
        }
    }

    private void loadSensorData() {
        sensorListener = FirebaseHelper.getInstance().listenSensorData(data -> {
            if (!isAdded()) return;
            applySensorDisplay(data);
        });
    }

    private void applySensorDisplay(SensorData data) {
        soilMoistureValueText.setText(toSoilMoisturePercent(data.getSoilMoisture()) + "%");
        temperatureValueText.setText(formatTemperature(data.getTemperature()));
        humidityValueText.setText(Math.round(data.getHumidity()) + "%");
        waterLevelValueText.setText(toWaterLevelPercent(data) + "%");
    }

    private static int toSoilMoisturePercent(double rawMoisture) {
        if (rawMoisture <= 0) {
            return 0;
        }
        if (rawMoisture <= 100) {
            return Math.max(0, Math.min(100, (int) Math.round(rawMoisture)));
        }
        int percent = (int) Math.round(100 - (rawMoisture / RAW_MOISTURE_MAX) * 100);
        return Math.max(0, Math.min(100, percent));
    }

    private static String formatTemperature(double temperature) {
        return Math.round(temperature) + "°C";
    }

    private static int toWaterLevelPercent(SensorData data) {
        double light = data.getLightIntensity();
        if (light > 0) {
            return Math.max(0, Math.min(100, (int) Math.round(light / 10)));
        }
        return toSoilMoisturePercent(data.getSoilMoisture());
    }

    private void loadDashboardData() {
        dashboardListener = FirebaseHelper.getInstance().listenDashboard(data -> {
            if (!isAdded()) return;

            farmHealthValueText.setText(data.getFarmHealth() + "%");
            farmHealthStatusText.setText(data.getFarmHealth() >= 70 ? "Healthy" : "Needs Attention");
            farmHealthProgress.setProgress(data.getFarmHealth());

            aiRecommendationText.setText(data.getAiRecommendation());

            // If we don't have individual farm pump sync, fallback to global legacy state
            if (mSelectedFarm == null) {
                boolean pumpOn = "ON".equalsIgnoreCase(data.getPumpStatus());
                pumpStatusBadge.setText(data.getPumpStatus());
                pumpSubtitleText.setText(pumpOn ? "Currently active" : "Currently inactive");
                updatePumpDisplay(data.getPumpStatus());
            }

            waterSavedValueText.setText(data.getWaterSaved() + " L");
            energySavedValueText.setText(data.getEnergySaved() + " kWh");

            applyRiskLevel(data.getRiskLevel());
        });
    }

    private void loadWaterLossData() {
        waterLossListener = FirebaseHelper.getInstance().listenWaterLoss(data -> {
            if (!isAdded()) return;
            applyWaterLossCard(data);
        });
    }

    private void applyWaterLossCard(WaterLossData data) {
        String status = data.getStatus() != null ? data.getStatus() : WaterLossEngine.STATUS_NO_LEAKAGE;
        wlStatusBadge.setText(status);
        wlBoundaryText.setText(data.getSuspectedBoundary() != null ? data.getSuspectedBoundary() : "—");
        wlEstimatedLossText.setText((int) data.getEstimatedLoss() + " L");

        int textColorRes;
        int bgColorRes;
        switch (status) {
            case WaterLossEngine.STATUS_DETECTED:
                textColorRes = R.color.status_critical;
                bgColorRes = R.color.icon_bg_red;
                break;
            case WaterLossEngine.STATUS_POSSIBLE:
                textColorRes = R.color.status_medium;
                bgColorRes = R.color.icon_bg_orange;
                break;
            default:
                textColorRes = R.color.status_healthy;
                bgColorRes = R.color.icon_bg_green;
                break;
        }
        wlStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), textColorRes));
        wlStatusBadge.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bgColorRes)));
    }

    private void applyRiskLevel(String riskLevel) {
        if (riskLevel == null) riskLevel = AIEngine.RISK_LOW;

        int textColorRes;
        int bgColorRes;
        switch (riskLevel) {
            case AIEngine.RISK_HIGH:
                textColorRes = R.color.status_critical;
                bgColorRes = R.color.icon_bg_red;
                break;
            case AIEngine.RISK_MEDIUM:
                textColorRes = R.color.status_medium;
                bgColorRes = R.color.icon_bg_orange;
                break;
            default:
                textColorRes = R.color.status_healthy;
                bgColorRes = R.color.icon_bg_green;
                break;
        }
        riskLevelBadge.setText(riskLevel);
        riskLevelBadge.setTextColor(ContextCompat.getColor(requireContext(), textColorRes));
        riskLevelBadge.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bgColorRes)));
    }

    private void watchUnreadNotifications() {
        unreadListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                boolean hasUnread = false;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean read = child.child("read").getValue(Boolean.class);
                    if (read == null || !read) { hasUnread = true; break; }
                }
                notifUnreadDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseDatabase.getInstance().getReference("notifications")
                .addValueEventListener(unreadListener);
    }

    private void startAutoRefreshTimer() {
        mRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                // Force AI Engine recommendation refresh on Firebase
                FirebaseHelper.getInstance().runAIEngine();
                mHandler.postDelayed(this, 20000); // 20 seconds loop
            }
        };
        mHandler.postDelayed(mRefreshRunnable, 20000);
    }

    private void stopAutoRefreshTimer() {
        if (mRefreshRunnable != null) {
            mHandler.removeCallbacks(mRefreshRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dashboardListener != null) {
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_DASHBOARD, dashboardListener);
        }
        if (waterLossListener != null) {
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_WATER_LOSS, waterLossListener);
        }
        if (sensorListener != null) {
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_SENSOR_DATA, sensorListener);
        }
        if (unreadListener != null) {
            FirebaseDatabase.getInstance().getReference("notifications")
                    .removeEventListener(unreadListener);
        }
        if (farmPumpListener != null && mSelectedFarm != null) {
            FirebaseHelper.getInstance().removeListenerForFarm(mSelectedFarm.getFarmId(), farmPumpListener);
        }
    }
}
