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

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.ValueEventListener;

public class InsightsFragment extends Fragment {

    private CircularProgressIndicator insightWaterProgress;
    private TextView insightWaterValueText;
    private CircularProgressIndicator insightCropHealthProgress;
    private TextView insightEnergyValueText;

    // Water Loss views
    private TextView insightWlStatusBadge;
    private TextView insightWlFlowInput;
    private TextView insightWlAvgMoisture;
    private TextView insightWlEstLoss;
    private TextView insightWlNorth;
    private TextView insightWlEast;
    private TextView insightWlSouth;
    private TextView insightWlWest;
    private TextView insightWlBoundary;
    private TextView insightWlRecommendation;

    private ValueEventListener dashboardListener;
    private ValueEventListener waterLossListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        insightWaterProgress     = view.findViewById(R.id.insightWaterProgress);
        insightWaterValueText    = view.findViewById(R.id.insightWaterValueText);
        insightCropHealthProgress = view.findViewById(R.id.insightCropHealthProgress);
        insightEnergyValueText   = view.findViewById(R.id.insightEnergyValueText);

        insightWlStatusBadge    = view.findViewById(R.id.insightWlStatusBadge);
        insightWlFlowInput      = view.findViewById(R.id.insightWlFlowInput);
        insightWlAvgMoisture    = view.findViewById(R.id.insightWlAvgMoisture);
        insightWlEstLoss        = view.findViewById(R.id.insightWlEstLoss);
        insightWlNorth          = view.findViewById(R.id.insightWlNorth);
        insightWlEast           = view.findViewById(R.id.insightWlEast);
        insightWlSouth          = view.findViewById(R.id.insightWlSouth);
        insightWlWest           = view.findViewById(R.id.insightWlWest);
        insightWlBoundary       = view.findViewById(R.id.insightWlBoundary);
        insightWlRecommendation = view.findViewById(R.id.insightWlRecommendation);

        loadInsightsData();
        loadWaterLossData();

        return view;
    }

    private void loadInsightsData() {
        dashboardListener = FirebaseHelper.getInstance().listenDashboard(data -> {
            if (!isAdded()) return;

            int waterPercent = Math.min(data.getWaterSaved() / 5, 100);
            insightWaterProgress.setProgress(waterPercent);
            insightWaterValueText.setText(waterPercent + "%");

            insightCropHealthProgress.setProgress(data.getFarmHealth());

            insightEnergyValueText.setText(data.getEnergySaved() + " kWh");
        });
    }

    private void loadWaterLossData() {
        waterLossListener = FirebaseHelper.getInstance().listenWaterLoss(data -> {
            if (!isAdded()) return;

            insightWlFlowInput.setText((int) data.getFlowInput() + " L");
            insightWlAvgMoisture.setText((int) data.getAverageMoisture() + "%");
            insightWlEstLoss.setText((int) data.getEstimatedLoss() + " L");
            insightWlNorth.setText((int) data.getNorthMoisture() + "%");
            insightWlEast.setText((int) data.getEastMoisture() + "%");
            insightWlSouth.setText((int) data.getSouthMoisture() + "%");
            insightWlWest.setText((int) data.getWestMoisture() + "%");
            insightWlBoundary.setText(data.getSuspectedBoundary() != null ? data.getSuspectedBoundary() : "—");
            insightWlRecommendation.setText(WaterLossEngine.buildRecommendation(data));

            String status = data.getStatus() != null ? data.getStatus() : WaterLossEngine.STATUS_NO_LEAKAGE;
            insightWlStatusBadge.setText(status);

            int textColorRes;
            int bgColorRes;
            switch (status) {
                case WaterLossEngine.STATUS_DETECTED:
                    textColorRes = R.color.status_critical;
                    bgColorRes   = R.color.icon_bg_red;
                    break;
                case WaterLossEngine.STATUS_POSSIBLE:
                    textColorRes = R.color.status_medium;
                    bgColorRes   = R.color.icon_bg_orange;
                    break;
                default:
                    textColorRes = R.color.status_healthy;
                    bgColorRes   = R.color.icon_bg_green;
                    break;
            }
            insightWlStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), textColorRes));
            insightWlStatusBadge.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bgColorRes)));
        });
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
    }
}
