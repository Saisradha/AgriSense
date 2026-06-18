package com.jo.agrisenseai;

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

import com.google.firebase.database.ValueEventListener;

public class MyFarmFragment extends Fragment {

    private TextView fieldNameText;
    private TextView fieldStatusBadge;
    private TextView fieldMoistureText;
    private TextView fieldWateringText;

    private ValueEventListener farmListener;
    private ValueEventListener sensorListener;
    private ValueEventListener dashboardListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_farm, container, false);

        fieldNameText = view.findViewById(R.id.fieldNameText);
        fieldStatusBadge = view.findViewById(R.id.fieldStatusBadge);
        fieldMoistureText = view.findViewById(R.id.fieldMoistureText);
        fieldWateringText = view.findViewById(R.id.fieldWateringText);

        loadFarmData();
        loadSensorData();
        loadAiStatus();

        return view;
    }

    /** Field name and AI-driven "Next Watering" suggestion. */
    private void loadFarmData() {
        farmListener = FirebaseHelper.getInstance().listenFarm(data -> {
            if (!isAdded()) return;

            fieldNameText.setText(data.getFieldName());
            fieldWateringText.setText("Next Watering: " + data.getNextWatering());
        });
    }

    /** Live soil moisture reading from the sensor node. */
    private void loadSensorData() {
        sensorListener = FirebaseHelper.getInstance().listenSensorData(data -> {
            if (!isAdded()) return;

            fieldMoistureText.setText("Soil Moisture Level: " + (int) data.getSoilMoisture());
        });
    }

    /** Field status badge driven by the AI engine's risk level. */
    private void loadAiStatus() {
        dashboardListener = FirebaseHelper.getInstance().listenDashboard(data -> {
            if (!isAdded()) return;

            String riskLevel = data.getRiskLevel();
            if (riskLevel == null) {
                riskLevel = AIEngine.RISK_LOW;
            }

            String statusText;
            int textColorRes;
            int bgColorRes;

            switch (riskLevel) {
                case AIEngine.RISK_HIGH:
                    statusText = "Critical";
                    textColorRes = R.color.status_critical;
                    bgColorRes = R.color.icon_bg_red;
                    break;
                case AIEngine.RISK_MEDIUM:
                    statusText = "Medium";
                    textColorRes = R.color.status_medium;
                    bgColorRes = R.color.icon_bg_orange;
                    break;
                default:
                    statusText = "Healthy";
                    textColorRes = R.color.status_healthy;
                    bgColorRes = R.color.icon_bg_green;
                    break;
            }

            fieldStatusBadge.setText(statusText);
            fieldStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), textColorRes));
            fieldStatusBadge.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bgColorRes)));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (farmListener != null) {
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_FARM, farmListener);
        }
        if (sensorListener != null) {
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_SENSOR_DATA, sensorListener);
        }
        if (dashboardListener != null) {
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_DASHBOARD, dashboardListener);
        }
    }
}
