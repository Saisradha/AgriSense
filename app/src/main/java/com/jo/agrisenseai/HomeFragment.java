package com.jo.agrisenseai;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    private ValueEventListener dashboardListener;
    private ValueEventListener sensorListener;
    private ValueEventListener unreadListener;
    private ValueEventListener waterLossListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

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

        view.findViewById(R.id.notifBellContainer).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationActivity.class)));

        loadDashboardData();
        loadSensorData();
        loadWaterLossData();
        watchUnreadNotifications();

        return view;
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

    /** Converts raw ADC moisture (0–1023, higher = drier) or 0–100 scale to a display percent. */
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

            boolean pumpOn = "ON".equalsIgnoreCase(data.getPumpStatus());
            pumpStatusBadge.setText(data.getPumpStatus());
            pumpSubtitleText.setText(pumpOn ? "Currently active" : "Currently inactive");

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
    }
}
