package com.jo.agrisenseai;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.ValueEventListener;

public class InsightsFragment extends Fragment {

    private CircularProgressIndicator insightWaterProgress;
    private TextView insightWaterValueText;
    private CircularProgressIndicator insightCropHealthProgress;
    private TextView insightEnergyValueText;

    private ValueEventListener dashboardListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        insightWaterProgress = view.findViewById(R.id.insightWaterProgress);
        insightWaterValueText = view.findViewById(R.id.insightWaterValueText);
        insightCropHealthProgress = view.findViewById(R.id.insightCropHealthProgress);
        insightEnergyValueText = view.findViewById(R.id.insightEnergyValueText);

        loadInsightsData();

        return view;
    }

    private void loadInsightsData() {
        dashboardListener = FirebaseHelper.getInstance().listenDashboard(data -> {
            if (!isAdded()) return;

            // Water saved -> shown as a percentage style stat in the circular indicator
            int waterPercent = Math.min(data.getWaterSaved() / 5, 100);
            insightWaterProgress.setProgress(waterPercent);
            insightWaterValueText.setText(waterPercent + "%");

            // Farm health reused as Crop Health indicator
            insightCropHealthProgress.setProgress(data.getFarmHealth());

            insightEnergyValueText.setText(data.getEnergySaved() + " kWh");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dashboardListener != null) {
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_DASHBOARD, dashboardListener);
        }
    }
}
