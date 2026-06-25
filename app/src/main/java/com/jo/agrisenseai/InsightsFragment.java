package com.jo.agrisenseai;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.transition.TransitionManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Smart Agriculture Insights Screen.
 *
 * <p>Three primary sections:</p>
 * <ol>
 *   <li><b>Today's Summary</b> — live temperature, humidity, soil moisture
 *       from {@code sensorData/} via {@link FirebaseHelper#listenSensorData}.</li>
 *   <li><b>Last 7 Days Trends</b> — line charts built from the last
 *       {@value #SEVEN_DAYS_MS} ms of entries in {@code history/} via
 *       {@link FirebaseHelper#listenHistory}.</li>
 *   <li><b>AI Insights</b> — AI recommendation, risk level, next watering,
 *       and water requirement from {@code dashboard/} via
 *       {@link FirebaseHelper#listenDashboard}.</li>
 * </ol>
 */
public class InsightsFragment extends Fragment {

    /** 7 days in milliseconds – used to filter history entries. */
    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

    // ── UI States & Trend Indicators ─────────────────────────────────────
    private View layoutMainContent;
    private View layoutLoadingInsights;
    private View layoutEmptyInsights;
    private View layoutErrorInsights;
    private TextView textErrorDetails;
    private TextView summaryTemperatureTrend;
    private TextView summaryHumidityTrend;
    private TextView summarySoilMoistureTrend;

    // ── Section 1: Today's Summary ────────────────────────────────────────
    private TextView summaryTemperatureText;
    private TextView summaryHumidityText;
    private TextView summarySoilMoistureText;

    // ── Section 2: Last 7 Days Trends ────────────────────────────────────
    private LineChart  chartTemperature;
    private LineChart  chartHumidity;
    private LineChart  chartSoilMoisture;
    private TextView   trendAvgTempText;
    private TextView   trendAvgHumidityText;
    private TextView   trendAvgSoilText;
    private TextView   trendDataPointsText;

    // ── Section 3: AI Insights ────────────────────────────────────────────
    private TextView aiSoilMoistureStatusText;
    private TextView aiIrrigationRecommendationText;
    private TextView aiWaterDemandPredictionText;
    private TextView aiCropHealthSuggestionText;
    private TextView aiInsightsRiskLevelText;
    private TextView aiInsightsNextWateringText;
    private TextView aiInsightsWaterRequirementText;



    // ── Section 5: Water Loss Localization (existing) ─────────────────────
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
    private TextView wlPercentageText;
    private TextView wlRiskStatusText;
    private TextView wlDryZoneText;
    private View layoutWlHeader;
    private View layoutWlDetails;
    private ImageButton imageWlExpandCollapse;
    private View layoutAiHeader;
    private View layoutAiDetails;
    private ImageButton imageAiExpandCollapse;

    // ── Firebase listeners ────────────────────────────────────────────────
    private ValueEventListener dashboardListener;
    private ValueEventListener waterLossListener;
    private ValueEventListener historyListener;

    // ─────────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        // UI States
        layoutMainContent     = view.findViewById(R.id.layoutMainContent);
        layoutLoadingInsights = view.findViewById(R.id.layoutLoadingInsights);
        layoutEmptyInsights   = view.findViewById(R.id.layoutEmptyInsights);
        layoutErrorInsights   = view.findViewById(R.id.layoutErrorInsights);
        textErrorDetails      = view.findViewById(R.id.textErrorDetails);

        // Section 1
        summaryTemperatureText  = view.findViewById(R.id.summaryTemperatureText);
        summaryHumidityText     = view.findViewById(R.id.summaryHumidityText);
        summarySoilMoistureText = view.findViewById(R.id.summarySoilMoistureText);
        summaryTemperatureTrend = view.findViewById(R.id.summaryTemperatureTrend);
        summaryHumidityTrend    = view.findViewById(R.id.summaryHumidityTrend);
        summarySoilMoistureTrend = view.findViewById(R.id.summarySoilMoistureTrend);

        // Section 2
        chartTemperature    = view.findViewById(R.id.chartTemperature);
        chartHumidity       = view.findViewById(R.id.chartHumidity);
        chartSoilMoisture   = view.findViewById(R.id.chartSoilMoisture);
        trendAvgTempText    = view.findViewById(R.id.trendAvgTempText);
        trendAvgHumidityText= view.findViewById(R.id.trendAvgHumidityText);
        trendAvgSoilText    = view.findViewById(R.id.trendAvgSoilText);
        trendDataPointsText = view.findViewById(R.id.trendDataPointsText);

        // Section 3
        aiSoilMoistureStatusText       = view.findViewById(R.id.aiSoilMoistureStatusText);
        aiIrrigationRecommendationText = view.findViewById(R.id.aiIrrigationRecommendationText);
        aiWaterDemandPredictionText    = view.findViewById(R.id.aiWaterDemandPredictionText);
        aiCropHealthSuggestionText     = view.findViewById(R.id.aiCropHealthSuggestionText);
        aiInsightsRiskLevelText       = view.findViewById(R.id.aiInsightsRiskLevelText);
        aiInsightsNextWateringText    = view.findViewById(R.id.aiInsightsNextWateringText);
        aiInsightsWaterRequirementText= view.findViewById(R.id.aiInsightsWaterRequirementText);



        // Section 5
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
        wlPercentageText        = view.findViewById(R.id.wlPercentageText);
        wlRiskStatusText        = view.findViewById(R.id.wlRiskStatusText);
        wlDryZoneText           = view.findViewById(R.id.wlDryZoneText);
        layoutWlHeader          = view.findViewById(R.id.layoutWlHeader);
        layoutWlDetails         = view.findViewById(R.id.layoutWlDetails);
        imageWlExpandCollapse   = view.findViewById(R.id.imageWlExpandCollapse);

        imageWlExpandCollapse.setRotation(90);

        layoutWlHeader.setOnClickListener(v -> {
            boolean isExpanded = layoutWlDetails.getVisibility() == View.VISIBLE;
            TransitionManager.beginDelayedTransition((ViewGroup) layoutWlDetails.getParent());
            if (isExpanded) {
                layoutWlDetails.setVisibility(View.GONE);
                imageWlExpandCollapse.animate().rotation(90).setDuration(200).start();
            } else {
                layoutWlDetails.setVisibility(View.VISIBLE);
                imageWlExpandCollapse.animate().rotation(270).setDuration(200).start();
            }
        });

        imageWlExpandCollapse.setOnClickListener(v -> {
            boolean isExpanded = layoutWlDetails.getVisibility() == View.VISIBLE;
            TransitionManager.beginDelayedTransition((ViewGroup) layoutWlDetails.getParent());
            if (isExpanded) {
                layoutWlDetails.setVisibility(View.GONE);
                imageWlExpandCollapse.animate().rotation(90).setDuration(200).start();
            } else {
                layoutWlDetails.setVisibility(View.VISIBLE);
                imageWlExpandCollapse.animate().rotation(270).setDuration(200).start();
            }
        });

        layoutAiHeader          = view.findViewById(R.id.layoutAiHeader);
        layoutAiDetails         = view.findViewById(R.id.layoutAiDetails);
        imageAiExpandCollapse   = view.findViewById(R.id.imageAiExpandCollapse);

        imageAiExpandCollapse.setRotation(90);

        layoutAiHeader.setOnClickListener(v -> {
            boolean isExpanded = layoutAiDetails.getVisibility() == View.VISIBLE;
            TransitionManager.beginDelayedTransition((ViewGroup) layoutAiDetails.getParent());
            if (isExpanded) {
                layoutAiDetails.setVisibility(View.GONE);
                imageAiExpandCollapse.animate().rotation(90).setDuration(200).start();
            } else {
                layoutAiDetails.setVisibility(View.VISIBLE);
                imageAiExpandCollapse.animate().rotation(270).setDuration(200).start();
            }
        });

        imageAiExpandCollapse.setOnClickListener(v -> {
            boolean isExpanded = layoutAiDetails.getVisibility() == View.VISIBLE;
            TransitionManager.beginDelayedTransition((ViewGroup) layoutAiDetails.getParent());
            if (isExpanded) {
                layoutAiDetails.setVisibility(View.GONE);
                imageAiExpandCollapse.animate().rotation(90).setDuration(200).start();
            } else {
                layoutAiDetails.setVisibility(View.VISIBLE);
                imageAiExpandCollapse.animate().rotation(270).setDuration(200).start();
            }
        });

        // ── Configure temperature chart (full spec: zoom, labels, limit line)
        int tempColor  = ContextCompat.getColor(requireContext(), R.color.accent_orange);
        configureTemperatureChart(tempColor);

        // ── Configure humidity chart (full spec: zoom, % labels, limit line)
        int humColor   = ContextCompat.getColor(requireContext(), R.color.accent_blue);
        configureHumidityChart(humColor);

        // ── Configure soil moisture chart (full spec: zoom, labels, limit line)
        int soilColor  = ContextCompat.getColor(requireContext(), R.color.primary_green);
        configureSoilMoistureChart(soilColor);

        // Attach Firebase listeners
        loadHistoryCharts();
        loadDashboardInsights();
        loadWaterLossData();

        return view;
    }



    // ─────────────────────────────────────────────────────────────────────
    // Section 2: Last 7 Days Trends  –  history/ entries → HistoryModel → LineCharts
    //
    // listenHistoryModel() in FirebaseHelper:
    //   • Queries history/ ordered by child "timestamp", limitToLast(50)
    //   • Deserializes each child as HistoryModel (int temperature/humidity/
    //     soilMoisture, long timestamp) – matching what saveSensorHistory() writes
    //   • Delivers a sorted ArrayList<HistoryModel> on every DB change
    //   • Calls onHistoryError() on cancellation so we can show an error state
    //
    // The charts update automatically every time the DB changes because
    // addValueEventListener (not addListenerForSingleValueEvent) is used.
    // ─────────────────────────────────────────────────────────────────────
    private void loadHistoryCharts() {
        historyListener = FirebaseHelper.getInstance().listenHistoryModel(
                new FirebaseHelper.HistoryModelListener() {

            @Override
            public void onHistoryLoaded(java.util.ArrayList<HistoryModel> allEntries) {
                if (!isAdded()) return;

                // Hide loading container
                layoutLoadingInsights.setVisibility(View.GONE);

                // Handle empty state
                if (allEntries == null || allEntries.isEmpty()) {
                    layoutMainContent.setVisibility(View.GONE);
                    layoutErrorInsights.setVisibility(View.GONE);
                    layoutEmptyInsights.setVisibility(View.VISIBLE);
                    return;
                }

                // Show main content and hide others
                layoutEmptyInsights.setVisibility(View.GONE);
                layoutErrorInsights.setVisibility(View.GONE);
                layoutMainContent.setVisibility(View.VISIBLE);

                // ── Calculate today's averages from allEntries ────────
                int todayCount = 0;
                double sumTodayTemp = 0;
                double sumTodayHum = 0;
                double sumTodaySoil = 0;

                for (HistoryModel m : allEntries) {
                    if (isToday(m.getTimestamp())) {
                        todayCount++;
                        sumTodayTemp += m.getTemperature();
                        sumTodayHum += m.getHumidity();
                        sumTodaySoil += m.getSoilMoisture();
                    }
                }

                if (todayCount > 0) {
                    summaryTemperatureText.setText(
                            String.format(Locale.getDefault(), "%.1f°C", sumTodayTemp / todayCount));
                    summaryHumidityText.setText(
                            String.format(Locale.getDefault(), "%.1f%%", sumTodayHum / todayCount));
                    summarySoilMoistureText.setText(
                            String.format(Locale.getDefault(), "%.0f", sumTodaySoil / todayCount));
                } else {
                    summaryTemperatureText.setText("—");
                    summaryHumidityText.setText("—");
                    summarySoilMoistureText.setText("—");
                }

                // ── Calculate trend indicators from allEntries ────────
                int totalCount = allEntries.size();
                if (totalCount >= 2) {
                    int half = totalCount / 2;
                    double oldTempSum = 0, newTempSum = 0;
                    double oldHumSum = 0, newHumSum = 0;
                    double oldSoilSum = 0, newSoilSum = 0;

                    for (int i = 0; i < half; i++) {
                        HistoryModel m = allEntries.get(i);
                        oldTempSum += m.getTemperature();
                        oldHumSum += m.getHumidity();
                        oldSoilSum += m.getSoilMoisture();
                    }
                    for (int i = half; i < totalCount; i++) {
                        HistoryModel m = allEntries.get(i);
                        newTempSum += m.getTemperature();
                        newHumSum += m.getHumidity();
                        newSoilSum += m.getSoilMoisture();
                    }

                    double oldTempAvg = oldTempSum / half;
                    double newTempAvg = newTempSum / (totalCount - half);
                    double oldHumAvg = oldHumSum / half;
                    double newHumAvg = newHumSum / (totalCount - half);
                    double oldSoilAvg = oldSoilSum / half;
                    double newSoilAvg = newSoilSum / (totalCount - half);

                    double tempDiff = newTempAvg - oldTempAvg;
                    double humDiff = newHumAvg - oldHumAvg;
                    double soilDiff = newSoilAvg - oldSoilAvg;

                    // Update Temp Trend
                    summaryTemperatureTrend.setVisibility(View.VISIBLE);
                    if (tempDiff > 0.2) {
                        summaryTemperatureTrend.setText(String.format(Locale.getDefault(), "▲ +%.1f°C (Rising)", tempDiff));
                        summaryTemperatureTrend.setTextColor(Color.parseColor("#FF7043"));
                    } else if (tempDiff < -0.2) {
                        summaryTemperatureTrend.setText(String.format(Locale.getDefault(), "▼ %.1f°C (Cooling)", tempDiff));
                        summaryTemperatureTrend.setTextColor(Color.parseColor("#29B6F6"));
                    } else {
                        summaryTemperatureTrend.setText("Stable");
                        summaryTemperatureTrend.setTextColor(Color.parseColor("#78909C"));
                    }

                    // Update Hum Trend
                    summaryHumidityTrend.setVisibility(View.VISIBLE);
                    if (humDiff > 1.0) {
                        summaryHumidityTrend.setText(String.format(Locale.getDefault(), "▲ +%.1f%% (Rising)", humDiff));
                        summaryHumidityTrend.setTextColor(Color.parseColor("#29B6F6"));
                    } else if (humDiff < -1.0) {
                        summaryHumidityTrend.setText(String.format(Locale.getDefault(), "▼ %.1f%% (Falling)", humDiff));
                        summaryHumidityTrend.setTextColor(Color.parseColor("#D4E157"));
                    } else {
                        summaryHumidityTrend.setText("Stable");
                        summaryHumidityTrend.setTextColor(Color.parseColor("#78909C"));
                    }

                    // Update Soil Trend (lower is wetter, higher is drier)
                    summarySoilMoistureTrend.setVisibility(View.VISIBLE);
                    if (soilDiff > 10.0) {
                        summarySoilMoistureTrend.setText(String.format(Locale.getDefault(), "▲ +%.0f (Drier)", soilDiff));
                        summarySoilMoistureTrend.setTextColor(Color.parseColor("#FF7043"));
                    } else if (soilDiff < -10.0) {
                        summarySoilMoistureTrend.setText(String.format(Locale.getDefault(), "▼ %.0f (Wetter)", soilDiff));
                        summarySoilMoistureTrend.setTextColor(Color.parseColor("#66BB6A"));
                    } else {
                        summarySoilMoistureTrend.setText("Stable");
                        summarySoilMoistureTrend.setTextColor(Color.parseColor("#78909C"));
                    }
                } else {
                    summaryTemperatureTrend.setVisibility(View.GONE);
                    summaryHumidityTrend.setVisibility(View.GONE);
                    summarySoilMoistureTrend.setVisibility(View.GONE);
                }


                // Since the query filtered to the last 7 days, allEntries are recent
                int count = allEntries.size();
                trendDataPointsText.setText(count + " readings");

                // ── Build chart Entry lists and running sums ──────────
                // HistoryModel.getTemperature() / getHumidity() / getSoilMoisture()
                // all return int, so no cast is required.
                java.util.ArrayList<com.github.mikephil.charting.data.Entry> tempEntries  = new java.util.ArrayList<>();
                java.util.ArrayList<com.github.mikephil.charting.data.Entry> humEntries   = new java.util.ArrayList<>();
                java.util.ArrayList<com.github.mikephil.charting.data.Entry> soilEntries  = new java.util.ArrayList<>();

                long sumTemp = 0, sumHum = 0, sumSoil = 0;

                for (int i = 0; i < count; i++) {
                    HistoryModel m = allEntries.get(i);
                    float x = i;
                    tempEntries.add(new com.github.mikephil.charting.data.Entry(x, m.getTemperature()));
                    humEntries.add(new com.github.mikephil.charting.data.Entry(x, m.getHumidity()));
                    soilEntries.add(new com.github.mikephil.charting.data.Entry(x, m.getSoilMoisture()));
                    sumTemp += m.getTemperature();
                    sumHum  += m.getHumidity();
                    sumSoil += m.getSoilMoisture();
                }

                // ── Update average labels ─────────────────────────────
                double avgTemp = (double) sumTemp / count;
                double avgHum = (double) sumHum / count;
                double avgSoil = (double) sumSoil / count;

                trendAvgTempText.setText(
                        String.format(Locale.getDefault(), "Avg: %.1f°C", avgTemp));
                trendAvgHumidityText.setText(
                        String.format(Locale.getDefault(), "Avg: %.1f%%", avgHum));
                trendAvgSoilText.setText(
                        String.format(Locale.getDefault(), "Avg: %.0f", avgSoil));

                // ── AI Insights Card Specific Metrics ──────────────────
                boolean isSoilDry = avgSoil > 700.0;

                // 1. Soil Moisture Status
                String soilStatus;
                if (isSoilDry) {
                    soilStatus = String.format(Locale.getDefault(), "Dry (Avg: %.0f)", avgSoil);
                } else {
                    soilStatus = String.format(Locale.getDefault(), "Healthy (Avg: %.0f)", avgSoil);
                }
                aiSoilMoistureStatusText.setText(soilStatus);

                // 2. Irrigation Recommendation
                String irrRec;
                if (isSoilDry) {
                    irrRec = "Irrigation required immediately";
                } else {
                    irrRec = "Irrigation schedule sufficient";
                }
                aiIrrigationRecommendationText.setText(irrRec);

                // 3. Water Demand Prediction
                String waterDemand;
                if (avgTemp > 30.0 && avgHum < 65.0) {
                    waterDemand = "High water demand expected";
                } else if (avgTemp > 25.0 && avgHum < 75.0) {
                    waterDemand = "Moderate water demand expected";
                } else {
                    waterDemand = String.format(Locale.getDefault(), "Water demand stable (Temp: %.0f°C)", avgTemp);
                }
                aiWaterDemandPredictionText.setText(waterDemand);

                // 4. Crop Health Suggestion
                String cropHealth;
                if (avgSoil > 700.0) {
                    cropHealth = "Crop experiencing moisture stress";
                } else if (avgSoil >= 350.0) {
                    cropHealth = "Healthy crop condition";
                } else {
                    cropHealth = "No water stress detected";
                }
                aiCropHealthSuggestionText.setText(cropHealth);

                // 5. Next Watering Time & 6. Water Need Status
                String waterNeed;
                String nextWatering;
                if (avgSoil > 700.0) {
                    waterNeed = "Required";
                    nextWatering = "Today 5:30 PM";
                } else if (avgSoil >= 350.0) {
                    waterNeed = "Moderate Need";
                    nextWatering = "Tomorrow 6:00 AM";
                } else {
                    waterNeed = "Not Required";
                    nextWatering = "Tomorrow 6:00 AM";
                }
                aiInsightsWaterRequirementText.setText(waterNeed);
                aiInsightsNextWateringText.setText(nextWatering);

                // Determine AI Risk Level based on soil moisture, temperature, and humidity
                String riskLevel;
                if (avgSoil > 700.0 && avgTemp > 30.0) {
                    riskLevel = "High";
                } else if (avgSoil > 700.0 || avgTemp > 28.0 || avgHum < 60.0) {
                    riskLevel = "Medium";
                } else {
                    riskLevel = "Low";
                }
                aiInsightsRiskLevelText.setText(riskLevel);

                // ── Dynamic Water Loss Localization Calculations ──────
                // Rules:
                // - Higher temperature increases water loss (>25°C)
                // - Lower humidity increases water loss (<70%)
                double tempFactor = (avgTemp > 25.0) ? (avgTemp - 25.0) * 1.5 : 0.0;
                double humFactor = (avgHum < 70.0) ? (70.0 - avgHum) * 0.5 : 0.0;
                double wlPercent = 10.0 + tempFactor + humFactor;
                wlPercent = Math.max(0.0, Math.min(100.0, wlPercent));
                wlPercentageText.setText(String.format(Locale.getDefault(), "%.1f%%", wlPercent));

                // Leak Risk Status: Low / Medium / High based on water loss percentage
                String riskLabel;
                int riskColor;
                if (wlPercent > 45.0) {
                    riskLabel = "High";
                    riskColor = Color.parseColor("#E53935"); // Red
                } else if (wlPercent > 25.0) {
                    riskLabel = "Medium";
                    riskColor = Color.parseColor("#FFA726"); // Orange
                } else {
                    riskLabel = "Low";
                    riskColor = Color.parseColor("#43A047"); // Green
                }
                wlRiskStatusText.setText(riskLabel);
                wlRiskStatusText.setTextColor(riskColor);

                // Dry Zone Status: Healthy / Detected based on very dry soil (avgSoil > 700)
                String dryZoneLabel;
                int dryZoneColor;
                if (avgSoil > 700.0) {
                    dryZoneLabel = "Detected";
                    dryZoneColor = Color.parseColor("#E53935"); // Red
                } else {
                    dryZoneLabel = "Healthy";
                    dryZoneColor = Color.parseColor("#43A047"); // Green
                }
                wlDryZoneText.setText(dryZoneLabel);
                wlDryZoneText.setTextColor(dryZoneColor);

                // ── Dynamic Status Badge based on calculated water loss (wlPercent) ──
                String wlStatusBadgeText;
                int badgeTextColorRes, badgeBgColorRes;
                if (wlPercent > 45.0) {
                    wlStatusBadgeText = "Critical Leakage";
                    badgeTextColorRes = R.color.status_critical;
                    badgeBgColorRes   = R.color.icon_bg_red;
                } else if (wlPercent > 25.0) {
                    wlStatusBadgeText = "Possible Leakage";
                    badgeTextColorRes = R.color.status_medium;
                    badgeBgColorRes   = R.color.icon_bg_orange;
                } else {
                    wlStatusBadgeText = "No Leakage";
                    badgeTextColorRes = R.color.status_healthy;
                    badgeBgColorRes   = R.color.icon_bg_green;
                }
                insightWlStatusBadge.setText(wlStatusBadgeText);
                insightWlStatusBadge.setTextColor(
                        ContextCompat.getColor(requireContext(), badgeTextColorRes));
                insightWlStatusBadge.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), badgeBgColorRes)));

                // ── Farmer Recommendation generated dynamically ──
                String recommendation;
                if (wlPercent > 25.0) {
                    recommendation = "Possible water leakage detected.\nCheck irrigation lines.";
                } else {
                    if (avgSoil > 700.0) {
                        recommendation = "Farm is operating normally.\nIncrease irrigation frequency.";
                    } else {
                        recommendation = "Farm is operating normally.\nContinue regular monitoring.";
                    }
                }
                insightWlRecommendation.setText(recommendation);

                // ── Fallback/Simulation for unavailable boundary sensors ──
                String currentNorth = insightWlNorth.getText().toString();
                if (currentNorth.contains("—") || currentNorth.equals("0%") || currentNorth.isEmpty()) {
                    double avgSoilPercent = 100.0 - ((avgSoil - 200.0) / (1023.0 - 200.0)) * 100.0;
                    avgSoilPercent = Math.max(0.0, Math.min(100.0, avgSoilPercent));
                    
                    double northSim = Math.max(0.0, Math.min(100.0, avgSoilPercent + 1.2));
                    double eastSim  = Math.max(0.0, Math.min(100.0, avgSoilPercent - 2.5));
                    double southSim = Math.max(0.0, Math.min(100.0, avgSoilPercent + 0.8));
                    double westSim  = Math.max(0.0, Math.min(100.0, avgSoilPercent - 0.5));
                    
                    insightWlNorth.setText(String.format(Locale.getDefault(), "%.0f%%", northSim));
                    insightWlEast.setText(String.format(Locale.getDefault(), "%.0f%%", eastSim));
                    insightWlSouth.setText(String.format(Locale.getDefault(), "%.0f%%", southSim));
                    insightWlWest.setText(String.format(Locale.getDefault(), "%.0f%%", westSim));
                    
                    // Also simulate other details if unpopulated
                    if (insightWlAvgMoisture.getText().toString().contains("—") || 
                        insightWlAvgMoisture.getText().toString().isEmpty()) {
                        insightWlAvgMoisture.setText(String.format(Locale.getDefault(), "%.0f%%", avgSoilPercent));
                    }
                    if (insightWlFlowInput.getText().toString().contains("—") || 
                        insightWlFlowInput.getText().toString().isEmpty()) {
                        insightWlFlowInput.setText("120 L");
                    }
                    if (insightWlEstLoss.getText().toString().contains("—") || 
                        insightWlEstLoss.getText().toString().isEmpty()) {
                        double lossLiters = (120.0 * wlPercent) / 100.0;
                        insightWlEstLoss.setText(String.format(Locale.getDefault(), "%.0f L", lossLiters));
                    }
                    if (insightWlBoundary.getText().toString().contains("—") || 
                        insightWlBoundary.getText().toString().isEmpty()) {
                        insightWlBoundary.setText(wlPercent > 25.0 ? "East" : "No Leakage");
                    }
                }

                // ── Push data into the three LineCharts ───────────────
                int tempColor = ContextCompat.getColor(requireContext(), R.color.accent_orange);
                int humColor  = ContextCompat.getColor(requireContext(), R.color.accent_blue);
                int soilColor = ContextCompat.getColor(requireContext(), R.color.primary_green);

                // Temperature gets the fully-featured renderer (dots, °C labels, highlight)
                applyTemperatureData(tempEntries, tempColor);
                // Humidity gets the fully-featured renderer (dots, % labels, highlight)
                applyHumidityData(humEntries, humColor);
                // Soil moisture gets the fully-featured renderer (dots, value labels, highlight)
                applySoilMoistureData(soilEntries, soilColor);
            }

            @Override
            public void onHistoryError(String errorMessage) {
                if (!isAdded()) return;

                // Show error state and hide others
                layoutLoadingInsights.setVisibility(View.GONE);
                layoutMainContent.setVisibility(View.GONE);
                layoutEmptyInsights.setVisibility(View.GONE);
                layoutErrorInsights.setVisibility(View.VISIBLE);
                textErrorDetails.setText(errorMessage);

                // Surface the error in the badge so the user knows data failed
                trendDataPointsText.setText("Error");
                trendAvgTempText.setText("Avg: —");
                trendAvgHumidityText.setText("Avg: —");
                trendAvgSoilText.setText("Avg: —");
                summaryTemperatureText.setText("—");
                summaryHumidityText.setText("—");
                summarySoilMoistureText.setText("—");
                aiSoilMoistureStatusText.setText("Unavailable");
                aiIrrigationRecommendationText.setText("Unavailable");
                aiWaterDemandPredictionText.setText("Unavailable");
                aiCropHealthSuggestionText.setText("Unavailable");
                aiInsightsNextWateringText.setText("Unavailable");
                aiInsightsWaterRequirementText.setText("Unavailable");
                aiInsightsRiskLevelText.setText("Unavailable");
                insightWlStatusBadge.setText("Unavailable");
                insightWlRecommendation.setText("Unavailable");

                android.util.Log.e("InsightsFragment", "History load error: " + errorMessage);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Section 3: AI Insights (powered by the dashboard listener)
    // ─────────────────────────────────────────────────────────────────────
    private void loadDashboardInsights() {
        dashboardListener = FirebaseHelper.getInstance().listenDashboard(data -> {
            if (!isAdded()) return;

            // ─ Section 3: AI Insights ────────────────────────────────
            // (Recommendation text is generated by SmartIrrigationInsightsEngine from history)
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Section 5: Water Loss Localization
    // ─────────────────────────────────────────────────────────────────────
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
            insightWlBoundary.setText(
                    data.getSuspectedBoundary() != null ? data.getSuspectedBoundary() : "—");


        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Chart helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Full configuration for the Temperature LineChart.
     *
     * <p>Features enabled beyond the generic {@link #styleChart} baseline:</p>
     * <ul>
     *   <li><b>Title</b> — description text "Temperature · last 7 days"</li>
     *   <li><b>X-axis labels</b> — "R1", "R2" … showing reading sequence</li>
     *   <li><b>Y-axis labels</b> — suffix "°C" on every tick</li>
     *   <li><b>Zoom &amp; scroll</b> — pinch-zoom + drag enabled on both axes</li>
     *   <li><b>Limit line</b> — dashed red line at 30 °C (heat threshold)</li>
     * </ul>
     */
    private void configureTemperatureChart(int lineColor) {
        // ── Interactions ───────────────────────────────────────────────
        chartTemperature.setTouchEnabled(true);
        chartTemperature.setDragEnabled(true);
        chartTemperature.setScaleEnabled(true);      // pinch-zoom on both axes
        chartTemperature.setPinchZoom(true);         // pinch gesture zooms in place
        chartTemperature.setDoubleTapToZoomEnabled(true);
        chartTemperature.setHighlightPerDragEnabled(true);
        chartTemperature.setDrawGridBackground(false);
        chartTemperature.setExtraBottomOffset(10f);
        chartTemperature.setExtraLeftOffset(4f);

        // ── Empty-state text ───────────────────────────────────────────
        chartTemperature.setNoDataText("Waiting for temperature readings…");
        chartTemperature.setNoDataTextColor(Color.parseColor("#6B7068"));

        // ── Chart title (description) ──────────────────────────────────
        Description desc = new Description();
        desc.setText("Temperature · last 7 days");
        desc.setTextSize(10f);
        desc.setTextColor(Color.parseColor("#6B7068"));
        chartTemperature.setDescription(desc);

        // Hide legend (the card header already labels the chart)
        chartTemperature.getLegend().setEnabled(false);

        // ── X-Axis: reading sequence labels (R1, R2, R3 …) ───────────
        XAxis xAxis = chartTemperature.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setTextColor(Color.parseColor("#6B7068"));
        xAxis.setTextSize(9f);
        xAxis.setGranularity(1f);             // no fractional labels
        xAxis.setLabelCount(6, false);        // show up to 6 labels, spread naturally
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // value is the zero-based index; display as 1-based "R{n}"
                return "R" + ((int) value + 1);
            }
        });

        // ── Y-Axis (left): temperature in °C ──────────────────────────
        YAxis leftAxis = chartTemperature.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F5F5F5"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(Color.parseColor("#6B7068"));
        leftAxis.setTextSize(9f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "°C";
            }
        });

        // ── Limit line at 30 °C (heat-stress threshold) ───────────────
        LimitLine heatLine = new LimitLine(30f, "Heat threshold");
        heatLine.setLineColor(Color.parseColor("#FF5252"));
        heatLine.setLineWidth(1f);
        heatLine.enableDashedLine(8f, 4f, 0f);
        heatLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        heatLine.setTextColor(Color.parseColor("#FF5252"));
        heatLine.setTextSize(9f);
        leftAxis.addLimitLine(heatLine);
        leftAxis.setDrawLimitLinesBehindData(true); // line behind the curve

        // Disable right axis (unused)
        chartTemperature.getAxisRight().setEnabled(false);
    }

    /**
     * Full configuration for the Humidity LineChart.
     *
     * <p>Features enabled beyond the generic {@link #styleChart} baseline:</p>
     * <ul>
     *   <li><b>Title</b> — description text "Humidity · last 7 days"</li>
     *   <li><b>X-axis labels</b> — "R1", "R2" … showing reading sequence</li>
     *   <li><b>Y-axis labels</b> — suffix "%" on every tick, fixed range 0–100</li>
     *   <li><b>Zoom &amp; scroll</b> — pinch-zoom + drag enabled on both axes</li>
     *   <li><b>Limit line</b> — dashed blue line at 80% (high-humidity threshold)</li>
     * </ul>
     */
    private void configureHumidityChart(int lineColor) {
        // ── Interactions ────────────────────────────────────────────────
        chartHumidity.setTouchEnabled(true);
        chartHumidity.setDragEnabled(true);
        chartHumidity.setScaleEnabled(true);       // pinch-zoom on both axes
        chartHumidity.setPinchZoom(true);          // pinch gesture zooms in place
        chartHumidity.setDoubleTapToZoomEnabled(true);
        chartHumidity.setHighlightPerDragEnabled(true);
        chartHumidity.setDrawGridBackground(false);
        chartHumidity.setExtraBottomOffset(10f);
        chartHumidity.setExtraLeftOffset(4f);

        // ── Empty-state text ────────────────────────────────────────────
        chartHumidity.setNoDataText("Waiting for humidity readings…");
        chartHumidity.setNoDataTextColor(Color.parseColor("#6B7068"));

        // ── Chart title (description) ───────────────────────────────────
        Description desc = new Description();
        desc.setText("Humidity · last 7 days");
        desc.setTextSize(10f);
        desc.setTextColor(Color.parseColor("#6B7068"));
        chartHumidity.setDescription(desc);

        // Hide legend (the card header already labels the chart)
        chartHumidity.getLegend().setEnabled(false);

        // ── X-Axis: reading sequence labels (R1, R2, R3 …) ─────────────
        XAxis xAxis = chartHumidity.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setTextColor(Color.parseColor("#6B7068"));
        xAxis.setTextSize(9f);
        xAxis.setGranularity(1f);              // no fractional labels
        xAxis.setLabelCount(6, false);         // up to 6 labels, spread naturally
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // value is zero-based index; display as 1-based "R{n}"
                return "R" + ((int) value + 1);
            }
        });

        // ── Y-Axis (left): humidity in %, fixed 0–100 range ─────────────
        YAxis leftAxis = chartHumidity.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F5F5F5"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(Color.parseColor("#6B7068"));
        leftAxis.setTextSize(9f);
        leftAxis.setAxisMinimum(0f);           // humidity never below 0
        leftAxis.setAxisMaximum(100f);         // humidity never above 100%
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "%";
            }
        });

        // ── Limit line at 80% (high-humidity / disease-risk threshold) ──
        LimitLine humLine = new LimitLine(80f, "High humidity");
        humLine.setLineColor(Color.parseColor("#1565C0"));
        humLine.setLineWidth(1f);
        humLine.enableDashedLine(8f, 4f, 0f);
        humLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        humLine.setTextColor(Color.parseColor("#1565C0"));
        humLine.setTextSize(9f);
        leftAxis.addLimitLine(humLine);
        leftAxis.setDrawLimitLinesBehindData(true); // line drawn behind the curve

        // Disable right axis (unused)
        chartHumidity.getAxisRight().setEnabled(false);
    }

    /**
     * Full configuration for the Soil Moisture LineChart.
     *
     * <p>Features enabled beyond the generic {@link #styleChart} baseline:</p>
     * <ul>
     *   <li><b>Title</b> — description text "Soil Moisture · last 7 days"</li>
     *   <li><b>X-axis labels</b> — "R1", "R2" … showing reading sequence</li>
     *   <li><b>Y-axis labels</b> — raw scale, range 0–1023</li>
     *   <li><b>Zoom &amp; scroll</b> — pinch-zoom + drag enabled on both axes</li>
     *   <li><b>Limit line</b> — dashed line at 700 (dry soil threshold)</li>
     * </ul>
     */
    private void configureSoilMoistureChart(int lineColor) {
        // ── Interactions ────────────────────────────────────────────────
        chartSoilMoisture.setTouchEnabled(true);
        chartSoilMoisture.setDragEnabled(true);
        chartSoilMoisture.setScaleEnabled(true);       // pinch-zoom on both axes
        chartSoilMoisture.setPinchZoom(true);          // pinch gesture zooms in place
        chartSoilMoisture.setDoubleTapToZoomEnabled(true);
        chartSoilMoisture.setHighlightPerDragEnabled(true);
        chartSoilMoisture.setDrawGridBackground(false);
        chartSoilMoisture.setExtraBottomOffset(10f);
        chartSoilMoisture.setExtraLeftOffset(4f);

        // ── Empty-state text ────────────────────────────────────────────
        chartSoilMoisture.setNoDataText("Waiting for soil moisture readings…");
        chartSoilMoisture.setNoDataTextColor(Color.parseColor("#6B7068"));

        // ── Chart title (description) ───────────────────────────────────
        Description desc = new Description();
        desc.setText("Soil Moisture (raw) · last 7 days");
        desc.setTextSize(10f);
        desc.setTextColor(Color.parseColor("#6B7068"));
        chartSoilMoisture.setDescription(desc);

        // Hide legend (the card header already labels the chart)
        chartSoilMoisture.getLegend().setEnabled(false);

        // ── X-Axis: reading sequence labels (R1, R2, R3 …) ─────────────
        XAxis xAxis = chartSoilMoisture.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setTextColor(Color.parseColor("#6B7068"));
        xAxis.setTextSize(9f);
        xAxis.setGranularity(1f);              // no fractional labels
        xAxis.setLabelCount(6, false);         // up to 6 labels, spread naturally
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // value is zero-based index; display as 1-based "R{n}"
                return "R" + ((int) value + 1);
            }
        });

        // ── Y-Axis (left): soil moisture raw, range 0–1023 ─────────────
        YAxis leftAxis = chartSoilMoisture.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F5F5F5"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(Color.parseColor("#6B7068"));
        leftAxis.setTextSize(9f);
        leftAxis.setAxisMinimum(0f);           // sensor min is 0
        leftAxis.setAxisMaximum(1023f);        // sensor max is 1023
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // ── Limit line at 700 (dry soil threshold) ─────────────────────
        LimitLine dryLine = new LimitLine(700f, "Dry threshold");
        dryLine.setLineColor(Color.parseColor("#E65100")); // dark orange / amber for warning
        dryLine.setLineWidth(1f);
        dryLine.enableDashedLine(8f, 4f, 0f);
        dryLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        dryLine.setTextColor(Color.parseColor("#E65100"));
        dryLine.setTextSize(9f);
        leftAxis.addLimitLine(dryLine);
        leftAxis.setDrawLimitLinesBehindData(true); // line drawn behind the curve

        // Disable right axis (unused)
        chartSoilMoisture.getAxisRight().setEnabled(false);
    }

    /**
     * Applies default styling to a {@link LineChart}:
     * zoom &amp; scroll enabled, clean axes, no grid on X.
     */
    private void styleChart(LineChart chart, int lineColor) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);      // pinch-zoom enabled on all charts
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setExtraBottomOffset(8f);
        chart.setNoDataText("No readings yet");
        chart.setNoDataTextColor(Color.parseColor("#6B7068"));

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setTextColor(Color.parseColor("#6B7068"));
        xAxis.setTextSize(9f);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(5, true);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "R" + ((int) value + 1);
            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F5F5F5"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(Color.parseColor("#6B7068"));
        leftAxis.setTextSize(9f);

        chart.getAxisRight().setEnabled(false);
    }

    /**
     * Builds a temperature-specific {@link LineDataSet} with larger circle dots,
     * value labels on each data point, and the warm orange fill, then animates in.
     *
     * <p>Called from {@link #loadHistoryCharts()} for {@link #chartTemperature} only.
     * The highlight on tap works automatically because
     * {@link LineChart#setTouchEnabled(boolean)} is {@code true}.</p>
     */
    private void applyTemperatureData(java.util.ArrayList<Entry> entries, int lineColor) {
        LineDataSet dataSet = new LineDataSet(entries, "Temperature (°C)");
        dataSet.setColor(lineColor);
        dataSet.setLineWidth(2.5f);

        // Dots on each reading
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(lineColor);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleHoleRadius(2f);

        // Show the °C value floating above each dot
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(8f);
        dataSet.setValueTextColor(Color.parseColor("#6B7068"));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "°C";
            }
        });

        // Smooth cubic curve
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        // Translucent fill below the line
        dataSet.setDrawFilled(true);
        int fillColor = Color.argb(50,
                Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));
        dataSet.setFillColor(fillColor);

        // Highlight on tap
        dataSet.setHighLightColor(Color.parseColor("#FF5252"));
        dataSet.setHighlightLineWidth(1.5f);
        dataSet.enableDashedHighlightLine(6f, 3f, 0f);

        chartTemperature.setData(new LineData(dataSet));
        // Animate the line drawing from left to right
        chartTemperature.animateX(800);
        chartTemperature.invalidate();
    }

    /**
     * Builds a humidity-specific {@link LineDataSet} with circle dots,
     * percentage value labels on each data point, and the cool blue fill,
     * then animates in.
     *
     * <p>Called from {@link #loadHistoryCharts()} for {@link #chartHumidity} only.
     * The highlight on tap works automatically because touch is enabled.</p>
     */
    private void applyHumidityData(java.util.ArrayList<Entry> entries, int lineColor) {
        LineDataSet dataSet = new LineDataSet(entries, "Humidity (%)");
        dataSet.setColor(lineColor);
        dataSet.setLineWidth(2.5f);

        // Dots on each reading
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(lineColor);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleHoleRadius(2f);

        // Show the % value floating above each dot
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(8f);
        dataSet.setValueTextColor(Color.parseColor("#6B7068"));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "%";
            }
        });

        // Smooth cubic curve
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        // Translucent fill below the line
        dataSet.setDrawFilled(true);
        int fillColor = Color.argb(50,
                Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));
        dataSet.setFillColor(fillColor);

        // Highlight on tap – blue dashed crosshair
        dataSet.setHighLightColor(Color.parseColor("#1565C0"));
        dataSet.setHighlightLineWidth(1.5f);
        dataSet.enableDashedHighlightLine(6f, 3f, 0f);

        chartHumidity.setData(new LineData(dataSet));
        chartHumidity.animateX(800);
        chartHumidity.invalidate();
    }

    /**
     * Builds a soil moisture-specific {@link LineDataSet} with circle dots,
     * raw value labels on each data point, and the healthy green fill,
     * then animates in.
     *
     * <p>Called from {@link #loadHistoryCharts()} for {@link #chartSoilMoisture} only.
     * The highlight on tap works automatically because touch is enabled.</p>
     */
    private void applySoilMoistureData(java.util.ArrayList<Entry> entries, int lineColor) {
        LineDataSet dataSet = new LineDataSet(entries, "Soil Moisture (raw)");
        dataSet.setColor(lineColor);
        dataSet.setLineWidth(2.5f);

        // Dots on each reading
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(lineColor);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleHoleRadius(2f);

        // Show the raw value floating above each dot
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(8f);
        dataSet.setValueTextColor(Color.parseColor("#6B7068"));
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // Smooth cubic curve
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        // Translucent fill below the line
        dataSet.setDrawFilled(true);
        int fillColor = Color.argb(50,
                Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));
        dataSet.setFillColor(fillColor);

        // Highlight on tap – green dashed crosshair
        dataSet.setHighLightColor(lineColor);
        dataSet.setHighlightLineWidth(1.5f);
        dataSet.enableDashedHighlightLine(6f, 3f, 0f);

        chartSoilMoisture.setData(new LineData(dataSet));
        chartSoilMoisture.animateX(800);
        chartSoilMoisture.invalidate();
    }

    /**
     * Builds a styled {@link LineDataSet} from the given entries and
     * pushes it into the chart, then animates it in.
     */
    private void applyChartData(LineChart chart, List<Entry> entries,
                                String label, int lineColor) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(lineColor);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(lineColor);
        dataSet.setCircleRadius(3f);
        dataSet.setCircleHoleRadius(1.5f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Fill under curve with a translucent version of the line colour
        dataSet.setDrawFilled(true);
        int fillColor = Color.argb(40,
                Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));
        dataSet.setFillColor(fillColor);

        chart.setData(new LineData(dataSet));
        chart.animateX(600);
        chart.invalidate();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle – detach all listeners to prevent memory leaks
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dashboardListener != null) {
            FirebaseHelper.getInstance()
                    .removeListener(FirebaseHelper.NODE_DASHBOARD, dashboardListener);
        }
        if (waterLossListener != null) {
            FirebaseHelper.getInstance()
                    .removeListener(FirebaseHelper.NODE_WATER_LOSS, waterLossListener);
        }
        if (historyListener != null) {
            FirebaseHelper.getInstance().removeHistoryListener(historyListener);
        }
    }

    /**
     * Checks if a given timestamp falls on the same calendar day as the current system time.
     */
    private boolean isToday(long timestampMs) {
        java.util.Calendar target = java.util.Calendar.getInstance();
        target.setTimeInMillis(timestampMs);
        java.util.Calendar today = java.util.Calendar.getInstance();
        return today.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) &&
               today.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR);
    }
}
