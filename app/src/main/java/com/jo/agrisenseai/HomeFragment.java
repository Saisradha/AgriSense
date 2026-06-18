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

    private ValueEventListener dashboardListener;
    private ValueEventListener unreadListener;

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

        // Bell icon → open Notification Center
        view.findViewById(R.id.notifBellContainer).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationActivity.class)));

        loadDashboardData();
        watchUnreadNotifications();

        return view;
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

    /**
     * Updates the Risk Level badge text and colors based on the AI's
     * riskLevel output ("Low", "Medium", "High").
     */
    private void applyRiskLevel(String riskLevel) {
        if (riskLevel == null) {
            riskLevel = AIEngine.RISK_LOW;
        }

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

    /** Shows a red dot on the bell when there are unread notifications. */
    private void watchUnreadNotifications() {
        unreadListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                boolean hasUnread = false;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean read = child.child("read").getValue(Boolean.class);
                    if (read == null || !read) {
                        hasUnread = true;
                        break;
                    }
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
        if (unreadListener != null) {
            FirebaseDatabase.getInstance().getReference("notifications")
                    .removeEventListener(unreadListener);
        }
    }
}
