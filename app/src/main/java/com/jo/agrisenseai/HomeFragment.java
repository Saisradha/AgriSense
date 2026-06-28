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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final double RAW_MOISTURE_MAX = 1023.0;

    // Standard dashboard views
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

    // Sensor readout views
    private TextView soilMoistureValueText;
    private TextView temperatureValueText;
    private TextView humidityValueText;
    private TextView waterLevelValueText;

    // Water loss card
    private TextView wlStatusBadge;
    private TextView wlBoundaryText;
    private TextView wlEstimatedLossText;

    // Greeting / farm selector
    private TextView tvWelcomeGreeting;
    private TextView tvFarmerName;
    private TextView tvSelectedFarm;
    private View cardFarmSelector;

    // ── Pump Mode Selector ─────────────────────────────────────────────────
    private MaterialButton btnWaterNow;
    private MaterialButton btnModeAuto;
    private MaterialButton btnModeManual;
    private TextView tvPumpModeCaption;
    private TextView tvPumpModeInfo;
    private TextView tvLastActivated;
    private TextView tvPumpReason;

    // ── Firebase Listeners ─────────────────────────────────────────────────
    private ValueEventListener dashboardListener;
    private ValueEventListener sensorListener;
    private ValueEventListener unreadListener;
    private ValueEventListener waterLossListener;
    private ValueEventListener farmPumpListener;
    private ValueEventListener pumpStatusListener;
    private ValueEventListener pumpModeListener;

    // ── State ──────────────────────────────────────────────────────────────
    private List<Farm> mUserFarms = new ArrayList<>();
    private Farm mSelectedFarm = null;
    private String currentPumpMode   = "AUTO"; // default: Automatic
    private String currentPumpStatus = "OFF";
    private SensorData lastSensorData = null;  // cached for Smart Irrigation card

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mRefreshRunnable;
    private UserProfile mUserProfile = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // ── Bind views ──────────────────────────────────────────────────────
        farmHealthValueText  = view.findViewById(R.id.farmHealthValueText);
        farmHealthStatusText = view.findViewById(R.id.farmHealthStatusText);
        farmHealthProgress   = view.findViewById(R.id.farmHealthProgress);
        aiRecommendationText = view.findViewById(R.id.aiRecommendationText);
        pumpStatusBadge      = view.findViewById(R.id.pumpStatusBadge);
        pumpSubtitleText     = view.findViewById(R.id.pumpSubtitleText);
        waterSavedValueText  = view.findViewById(R.id.waterSavedValueText);
        energySavedValueText = view.findViewById(R.id.energySavedValueText);
        riskLevelBadge       = view.findViewById(R.id.riskLevelBadge);
        notifUnreadDot       = view.findViewById(R.id.notifUnreadDot);

        wlStatusBadge      = view.findViewById(R.id.wlStatusBadge);
        wlBoundaryText     = view.findViewById(R.id.wlBoundaryText);
        wlEstimatedLossText= view.findViewById(R.id.wlEstimatedLossText);

        soilMoistureValueText = view.findViewById(R.id.soilMoistureValueText);
        temperatureValueText  = view.findViewById(R.id.temperatureValueText);
        humidityValueText     = view.findViewById(R.id.humidityValueText);
        waterLevelValueText   = view.findViewById(R.id.waterLevelValueText);

        tvWelcomeGreeting = view.findViewById(R.id.tvWelcomeGreeting);
        tvFarmerName      = view.findViewById(R.id.tvFarmerName);
        tvSelectedFarm    = view.findViewById(R.id.tvSelectedFarm);
        cardFarmSelector  = view.findViewById(R.id.cardFarmSelector);

        // Pump-mode UI
        btnWaterNow       = view.findViewById(R.id.btnWaterNow);
        btnModeAuto       = view.findViewById(R.id.btnModeAuto);
        btnModeManual     = view.findViewById(R.id.btnModeManual);
        tvPumpModeCaption = view.findViewById(R.id.tvPumpModeCaption);
        tvPumpModeInfo    = view.findViewById(R.id.tvPumpModeInfo);
        tvLastActivated   = view.findViewById(R.id.tvLastActivated);
        tvPumpReason      = view.findViewById(R.id.tvPumpReason);

        // ── Clicks ──────────────────────────────────────────────────────────
        view.findViewById(R.id.notifBellContainer).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationActivity.class)));

        cardFarmSelector.setOnClickListener(v -> showFarmSelectorDialog());

        btnModeAuto.setOnClickListener(v   -> switchPumpMode("AUTO"));
        btnModeManual.setOnClickListener(v -> switchPumpMode("MANUAL"));

        // Water Now acts only in MANUAL mode; sends pumpCommand to Firebase
        btnWaterNow.setOnClickListener(v -> onWaterNowClicked());

        // ── Load data ────────────────────────────────────────────────────────
        loadFarmerProfile();
        loadUserFarms();
        loadDashboardData();
        loadSensorData();
        loadWaterLossData();
        watchUnreadNotifications();
        attachPumpModeListener();
        attachPumpStatusListener();

        // Apply default AUTO mode visuals (no Firebase write yet — only on user click)
        applyModeUI("AUTO");

        // Set dynamic greeting based on time of day
        if (tvWelcomeGreeting != null) {
            tvWelcomeGreeting.setText(getGreeting());
        }

        return view;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove all Firebase listeners to prevent memory leaks
        if (dashboardListener != null)
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_DASHBOARD, dashboardListener);
        if (waterLossListener != null)
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_WATER_LOSS, waterLossListener);
        if (sensorListener != null)
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_SENSOR_DATA, sensorListener);
        if (unreadListener != null)
            FirebaseDatabase.getInstance().getReference("notifications")
                    .removeEventListener(unreadListener);
        if (farmPumpListener != null && mSelectedFarm != null)
            FirebaseHelper.getInstance().removeListenerForFarm(mSelectedFarm.getFarmId(), farmPumpListener);
        if (pumpStatusListener != null)
            FirebaseHelper.getInstance().removeSensorChildListener("pumpStatus", pumpStatusListener);
        if (pumpModeListener != null)
            FirebaseHelper.getInstance().removeSensorChildListener("pumpMode", pumpModeListener);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Pump Mode — AUTO / MANUAL
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Switches pump mode and writes to Firebase so the ESP32 can react.
     * The UI is updated immediately; further UI updates come from the
     * live pumpStatus listener (not from button-click state).
     */
    private void switchPumpMode(String mode) {
        currentPumpMode = mode;
        applyModeUI(mode);

        FirebaseHelper.getInstance().setPumpMode(mode, (error, ref) -> {
            if (getContext() == null) return;
            if (error != null) {
                android.util.Log.e("HomeFragment", "setPumpMode failed: " + error.getMessage());
                Toast.makeText(getContext(), "Could not change pump mode.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Applies mode-specific visual state without writing to Firebase.
     * Called immediately on button press and also on app start.
     */
    private void applyModeUI(String mode) {
        if (getContext() == null) return;
        boolean isAuto = "AUTO".equalsIgnoreCase(mode);

        // Update info row
        if (tvPumpModeInfo != null)
            tvPumpModeInfo.setText(isAuto ? "Automatic" : "Manual");

        // Mode chip visuals — selected chip is filled green, unselected is outlined
        if (btnModeAuto != null && btnModeManual != null) {
            if (isAuto) {
                btnModeAuto.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.primary_green)));
                btnModeAuto.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
                btnModeManual.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                btnModeManual.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_green));
            } else {
                btnModeManual.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.primary_green)));
                btnModeManual.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
                btnModeAuto.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                btnModeAuto.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_green));
            }
        }

        // Caption visibility
        if (tvPumpModeCaption != null)
            tvPumpModeCaption.setVisibility(isAuto ? View.VISIBLE : View.GONE);

        // Water Now button
        if (btnWaterNow != null) {
            if (isAuto) {
                btnWaterNow.setEnabled(false);
                btnWaterNow.setText(getString(R.string.pump_controlled_auto));
                btnWaterNow.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.primary_green)));
            } else {
                btnWaterNow.setEnabled(true);
                // Reflect actual pump state for button text/color
                applyPumpStatusUI(currentPumpStatus);
            }
        }

        // Reason row
        if (tvPumpReason != null)
            tvPumpReason.setText(isAuto ? getString(R.string.pump_reason_ai)
                                        : getString(R.string.pump_reason_manual));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Real-time pumpStatus listener  (always active, drives UI)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attaches a permanent listener to {@code sensorData/pumpStatus}.
     * UI is updated every time the ESP32 changes the pump state — the button
     * state is NEVER set from click events alone.
     */
    private void attachPumpStatusListener() {
        pumpStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null) return;
                String status = snapshot.exists() ? snapshot.getValue(String.class) : "OFF";
                if (status == null) status = "OFF";
                currentPumpStatus = status;
                applyPumpStatusUI(status);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("HomeFragment", "pumpStatus listener cancelled: " + error.getMessage());
            }
        };
        FirebaseHelper.getInstance().listenPumpStatus(pumpStatusListener);
    }

    /**
     * Attaches a permanent listener to {@code sensorData/pumpMode}.
     * Keeps the app in sync if mode is changed from another client or
     * the ESP32 itself resets the mode.
     */
    private void attachPumpModeListener() {
        pumpModeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null) return;
                String mode = snapshot.exists() ? snapshot.getValue(String.class) : "AUTO";
                if (mode == null) mode = "AUTO";
                // Only update UI if mode differs from local state (avoids unnecessary redraws)
                if (!mode.equals(currentPumpMode)) {
                    currentPumpMode = mode;
                    applyModeUI(mode);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("HomeFragment", "pumpMode listener cancelled: " + error.getMessage());
            }
        };
        FirebaseHelper.getInstance().listenPumpMode(pumpModeListener);
    }

    /**
     * Applies the real pump status to all UI elements.
     * This is the single source of truth — called from the Firebase listener.
     */
    private void applyPumpStatusUI(String status) {
        if (getContext() == null) return;
        boolean isOn = "ON".equalsIgnoreCase(status);

        // Status badge
        if (pumpStatusBadge != null) {
            pumpStatusBadge.setText(isOn ? "ON" : "OFF");
            pumpStatusBadge.setTextColor(
                    ContextCompat.getColor(getContext(), isOn ? R.color.status_healthy : R.color.status_critical));
            pumpStatusBadge.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(getContext(),
                            isOn ? R.color.icon_bg_green : R.color.icon_bg_red)));
        }

        // Subtitle
        if (pumpSubtitleText != null)
            pumpSubtitleText.setText(isOn ? "Currently active" : "Currently inactive");

        // Last activated timestamp
        if (tvLastActivated != null && isOn) {
            String ts = new SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
                    .format(new Date(System.currentTimeMillis()));
            tvLastActivated.setText(ts);
        }

        // Reason row — only changes on status transition
        if (tvPumpReason != null)
            tvPumpReason.setText("AUTO".equalsIgnoreCase(currentPumpMode)
                    ? getString(R.string.pump_reason_ai)
                    : getString(R.string.pump_reason_manual));

        // Water Now button — only update in MANUAL mode
        if ("MANUAL".equalsIgnoreCase(currentPumpMode) && btnWaterNow != null) {
            btnWaterNow.setText(isOn ? getString(R.string.pump_turn_off)
                                     : getString(R.string.pump_turn_on));
            btnWaterNow.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(),
                            isOn ? R.color.accent_red : R.color.primary_green)));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Water Now button logic (MANUAL mode only)
    // ══════════════════════════════════════════════════════════════════════

    private void onWaterNowClicked() {
        // Guard: only act in manual mode
        if (!"MANUAL".equalsIgnoreCase(currentPumpMode)) return;
        sendPumpCommand();
    }

    /**
     * Sends pumpCommand ("ON"/"OFF") to {@code sensorData/pumpCommand} for the ESP32 to read.
     *
     * <p>Also writes {@code pumpStatus} directly so the {@link #pumpStatusListener} fires
     * immediately, updating the UI regardless of whether the ESP32 firmware has been updated.
     * If a real ESP32 IS present, it will overwrite {@code pumpStatus} with the confirmed
     * value a moment later — which is harmless since it is the same value.</p>
     */
    private void sendPumpCommand() {
        // Toggle: if currently ON send OFF, and vice versa
        String command = "ON".equalsIgnoreCase(currentPumpStatus) ? "OFF" : "ON";

        btnWaterNow.setEnabled(false);

        // 1. Write pumpCommand — ESP32 reads this and actuates the physical pump
        FirebaseHelper.getInstance().setPumpCommand(command, (cmdError, cmdRef) -> {
            if (getContext() == null) return;
            if (cmdError != null) {
                android.util.Log.e("HomeFragment", "setPumpCommand failed: " + cmdError.getMessage());
                Toast.makeText(getContext(),
                        "Failed to send pump command: " + cmdError.getMessage(),
                        Toast.LENGTH_LONG).show();
                btnWaterNow.setEnabled(true);
            }
        });

        // 2. Also write pumpStatus directly so the pumpStatusListener fires immediately.
        //    This gives instant UI feedback and supports demo mode / pre-update ESP32 firmware.
        //    When the ESP32 IS connected it will write the same value back — no conflict.
        FirebaseHelper.getInstance().setPumpStatusDirect(command, (statusError, statusRef) -> {
            if (getContext() == null) return;
            // Re-enable button after status write completes (success or failure)
            btnWaterNow.setEnabled(true);
            if (statusError != null) {
                android.util.Log.e("HomeFragment", "setPumpStatusDirect failed: " + statusError.getMessage());
            }
            // UI is updated by pumpStatusListener reacting to the sensorData/pumpStatus change
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Smart Irrigation Card — dynamic recommendation from sensor data
    // ══════════════════════════════════════════════════════════════════════

    private void updateSmartIrrigationCard(SensorData data) {
        if (getContext() == null || aiRecommendationText == null || data == null) return;

        double soilPct = toSoilMoisturePercent(data.getSoilMoisture());
        double temp    = data.getTemperature();
        double humidity= data.getHumidity();
        boolean pumpOn = "ON".equalsIgnoreCase(currentPumpStatus);

        String recommendation;
        if (pumpOn) {
            recommendation = "Pump running. Irrigation in progress.";
        } else if (soilPct < 25) {
            recommendation = "Soil is very dry. Irrigation recommended immediately.";
        } else if (soilPct < 40) {
            recommendation = "Soil is dry. Irrigation recommended soon.";
        } else if (soilPct > 75) {
            recommendation = "Soil moisture is optimal. No irrigation needed.";
        } else if (temp > 35) {
            recommendation = "Temperature is high (" + (int) temp + "°C). Monitor closely.";
        } else if (humidity < 30) {
            recommendation = "Humidity is low (" + (int) humidity + "%). Consider irrigation.";
        } else {
            recommendation = "Farm conditions are good. Continue regular monitoring.";
        }
        aiRecommendationText.setText(recommendation);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Farm & Data loading
    // ══════════════════════════════════════════════════════════════════════

    private void loadFarmerProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseHelper.getInstance().getUserProfile(user.getUid(), profile -> {
                if (getContext() == null) return;
                mUserProfile = profile;
                tvFarmerName.setText((profile != null && profile.getName() != null)
                        ? profile.getName() : "Farmer");
            });
        }
    }

    private void loadUserFarms() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseHelper.getInstance().listenUserFarms(user.getUid(), farms -> {
                if (getContext() == null) return;
                mUserFarms = farms;
                if (!farms.isEmpty()) {
                    if (mSelectedFarm == null) {
                        selectFarm(farms.get(0));
                    } else {
                        for (Farm f : farms) {
                            if (f.getFarmId().equals(mSelectedFarm.getFarmId())) {
                                mSelectedFarm = f;
                                tvSelectedFarm.setText(f.getFarmName());
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

        if (farmPumpListener != null && mSelectedFarm != null) {
            FirebaseHelper.getInstance().removeListenerForFarm(mSelectedFarm.getFarmId(), farmPumpListener);
        }

        farmPumpListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null) return;
                String status = snapshot.getValue(String.class);
                if (status != null && mSelectedFarm != null) {
                    mSelectedFarm.setPumpStatus(status);
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
        for (int i = 0; i < mUserFarms.size(); i++) farmNames[i] = mUserFarms.get(i).getFarmName();
        int checkedItem = -1;
        if (mSelectedFarm != null) {
            for (int i = 0; i < mUserFarms.size(); i++) {
                if (mUserFarms.get(i).getFarmId().equals(mSelectedFarm.getFarmId())) {
                    checkedItem = i; break;
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

    private void loadSensorData() {
        sensorListener = FirebaseHelper.getInstance().listenSensorData(data -> {
            if (getContext() == null) return;
            lastSensorData = data;
            applySensorDisplay(data);
            updateSmartIrrigationCard(data);
        });
    }

    private void applySensorDisplay(SensorData data) {
        soilMoistureValueText.setText(toSoilMoisturePercent(data.getSoilMoisture()) + "%");
        temperatureValueText.setText(formatTemperature(data.getTemperature()));
        humidityValueText.setText(Math.round(data.getHumidity()) + "%");
        waterLevelValueText.setText(toWaterLevelPercent(data) + "%");
    }

    private void loadDashboardData() {
        dashboardListener = FirebaseHelper.getInstance().listenDashboard(data -> {
            if (getContext() == null) return;

            farmHealthValueText.setText(data.getFarmHealth() + "%");
            farmHealthStatusText.setText(data.getFarmHealth() >= 70 ? "Healthy" : "Needs Attention");
            farmHealthProgress.setProgress(data.getFarmHealth());

            waterSavedValueText.setText(data.getWaterSaved() + " L");
            energySavedValueText.setText(data.getEnergySaved() + " kWh");

            applyRiskLevel(data.getRiskLevel());
        });
    }

    private void loadWaterLossData() {
        waterLossListener = FirebaseHelper.getInstance().listenWaterLoss(data -> {
            if (getContext() == null) return;
            applyWaterLossCard(data);
        });
    }

    private void applyWaterLossCard(WaterLossData data) {
        String status = data.getStatus() != null ? data.getStatus() : WaterLossEngine.STATUS_NO_LEAKAGE;
        wlStatusBadge.setText(status);
        wlBoundaryText.setText(data.getSuspectedBoundary() != null ? data.getSuspectedBoundary() : "—");
        wlEstimatedLossText.setText((int) data.getEstimatedLoss() + " L");

        int textColorRes, bgColorRes;
        switch (status) {
            case WaterLossEngine.STATUS_DETECTED:
                textColorRes = R.color.status_critical; bgColorRes = R.color.icon_bg_red; break;
            case WaterLossEngine.STATUS_POSSIBLE:
                textColorRes = R.color.status_medium;   bgColorRes = R.color.icon_bg_orange; break;
            default:
                textColorRes = R.color.status_healthy;  bgColorRes = R.color.icon_bg_green; break;
        }
        wlStatusBadge.setTextColor(ContextCompat.getColor(getContext(), textColorRes));
        wlStatusBadge.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(getContext(), bgColorRes)));
    }

    private void applyRiskLevel(String riskLevel) {
        if (riskLevel == null) riskLevel = AIEngine.RISK_LOW;
        int textColorRes, bgColorRes;
        switch (riskLevel) {
            case AIEngine.RISK_HIGH:
                textColorRes = R.color.status_critical; bgColorRes = R.color.icon_bg_red; break;
            case AIEngine.RISK_MEDIUM:
                textColorRes = R.color.status_medium;   bgColorRes = R.color.icon_bg_orange; break;
            default:
                textColorRes = R.color.status_healthy;  bgColorRes = R.color.icon_bg_green; break;
        }
        riskLevelBadge.setText(riskLevel);
        riskLevelBadge.setTextColor(ContextCompat.getColor(getContext(), textColorRes));
        riskLevelBadge.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(getContext(), bgColorRes)));
    }

    private void watchUnreadNotifications() {
        unreadListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null) return;
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

    // ══════════════════════════════════════════════════════════════════════
    // Legacy updatePumpDisplay — kept for backward compat with loadUserFarms
    // Now delegates to applyPumpStatusUI which is the single source of truth
    // ══════════════════════════════════════════════════════════════════════
    private void updatePumpDisplay(String status) {
        applyPumpStatusUI(status);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Auto-refresh timer (triggers AI Engine re-analysis every 20s)
    // ══════════════════════════════════════════════════════════════════════

    private void startAutoRefreshTimer() {
        mRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (getContext() == null) return;
                FirebaseHelper.getInstance().runAIEngine();
                mHandler.postDelayed(this, 20000);
            }
        };
        mHandler.postDelayed(mRefreshRunnable, 20000);
    }

    private void stopAutoRefreshTimer() {
        if (mRefreshRunnable != null) mHandler.removeCallbacks(mRefreshRunnable);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Sensor helpers
    // ══════════════════════════════════════════════════════════════════════

    private static int toSoilMoisturePercent(double rawMoisture) {
        if (rawMoisture <= 0) return 0;
        if (rawMoisture <= 100) return Math.max(0, Math.min(100, (int) Math.round(rawMoisture)));
        int percent = (int) Math.round(100 - (rawMoisture / RAW_MOISTURE_MAX) * 100);
        return Math.max(0, Math.min(100, percent));
    }

    private static String formatTemperature(double temperature) {
        return Math.round(temperature) + "°C";
    }

    private static int toWaterLevelPercent(SensorData data) {
        double light = data.getLightIntensity();
        if (light > 0) return Math.max(0, Math.min(100, (int) Math.round(light / 10)));
        return toSoilMoisturePercent(data.getSoilMoisture());
    }

    private String getGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) {
            return "Good Morning 🌱";
        } else if (hour >= 12 && hour < 17) {
            return "Good Afternoon ☀️";
        } else if (hour >= 17 && hour < 21) {
            return "Good Evening 🍃";
        } else {
            return "Good Night 🌙";
        }
    }
}
