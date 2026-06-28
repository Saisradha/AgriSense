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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Smart Agriculture Insights Screen.
 *
 * <p>Three primary sections:</p>
 * <ol>
 *   <li><b>Today's Summary</b> — live temperature, humidity, soil moisture
 *       from {@code sensorData/} via {@link FirebaseHelper#listenSensorData}.</li>
 *   <li><b>Last 7 Days Trends</b> — line charts built from aggregated daily
 *       averages from {@code history/}.</li>
 *   <li><b>AI Insights</b> — AI recommendation, risk level, next watering,
 *       and water requirement from {@code dashboard/}.</li>
 * </ol>
 *
 * <p><b>Loading fix</b>: The previous implementation attached a dashboard listener
 * that did nothing, leaking it permanently. It also showed an infinite spinner
 * when Firebase returned an empty snapshot because the loading state was only
 * cleared inside the non-empty branch. Both bugs are fixed here.</p>
 *
 * <p><b>Graph fix</b>: Raw readings (up to 50 data points) are now aggregated
 * into daily averages before being rendered. Each chart shows one point per
 * day with a day-name X-axis label.</p>
 */
public class InsightsFragment extends Fragment {

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

    // ── Section 5: Water Loss Localization ────────────────────────────────
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
    // NOTE: dashboardListener removed — it created a permanent Firebase listener
    // that did nothing, causing a resource leak. Dashboard data is not needed
    // on this screen since all metrics are computed from history entries.
    private ValueEventListener waterLossListener;
    private ValueEventListener historyListener;

    // ── Aggregation cache (avoid redundant re-renders) ───────────────────
    private int lastRenderedEntryCount = -1;
    private boolean chartsAnimated = false;

    // ─────────────────────────────────────────────────────────────────────

    /** Simple holder for one day's aggregated sensor averages. */
    private static class DayAverage {
        final String label;    // e.g., "Mon", "Jun 22"
        final float temp;
        final float humidity;
        final float soil;
        DayAverage(String label, float temp, float humidity, float soil) {
            this.label = label; this.temp = temp;
            this.humidity = humidity; this.soil = soil;
        }
    }

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
        summaryTemperatureText   = view.findViewById(R.id.summaryTemperatureText);
        summaryHumidityText      = view.findViewById(R.id.summaryHumidityText);
        summarySoilMoistureText  = view.findViewById(R.id.summarySoilMoistureText);
        summaryTemperatureTrend  = view.findViewById(R.id.summaryTemperatureTrend);
        summaryHumidityTrend     = view.findViewById(R.id.summaryHumidityTrend);
        summarySoilMoistureTrend = view.findViewById(R.id.summarySoilMoistureTrend);

        // Section 2
        chartTemperature     = view.findViewById(R.id.chartTemperature);
        chartHumidity        = view.findViewById(R.id.chartHumidity);
        chartSoilMoisture    = view.findViewById(R.id.chartSoilMoisture);
        trendAvgTempText     = view.findViewById(R.id.trendAvgTempText);
        trendAvgHumidityText = view.findViewById(R.id.trendAvgHumidityText);
        trendAvgSoilText     = view.findViewById(R.id.trendAvgSoilText);
        trendDataPointsText  = view.findViewById(R.id.trendDataPointsText);

        // Section 3
        aiSoilMoistureStatusText       = view.findViewById(R.id.aiSoilMoistureStatusText);
        aiIrrigationRecommendationText = view.findViewById(R.id.aiIrrigationRecommendationText);
        aiWaterDemandPredictionText    = view.findViewById(R.id.aiWaterDemandPredictionText);
        aiCropHealthSuggestionText     = view.findViewById(R.id.aiCropHealthSuggestionText);
        aiInsightsRiskLevelText        = view.findViewById(R.id.aiInsightsRiskLevelText);
        aiInsightsNextWateringText     = view.findViewById(R.id.aiInsightsNextWateringText);
        aiInsightsWaterRequirementText = view.findViewById(R.id.aiInsightsWaterRequirementText);

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

        layoutWlHeader.setOnClickListener(v -> toggleSection(layoutWlDetails, imageWlExpandCollapse));
        imageWlExpandCollapse.setOnClickListener(v -> toggleSection(layoutWlDetails, imageWlExpandCollapse));

        layoutAiHeader        = view.findViewById(R.id.layoutAiHeader);
        layoutAiDetails       = view.findViewById(R.id.layoutAiDetails);
        imageAiExpandCollapse = view.findViewById(R.id.imageAiExpandCollapse);

        imageAiExpandCollapse.setRotation(90);

        layoutAiHeader.setOnClickListener(v -> toggleSection(layoutAiDetails, imageAiExpandCollapse));
        imageAiExpandCollapse.setOnClickListener(v -> toggleSection(layoutAiDetails, imageAiExpandCollapse));

        // Configure charts
        configureTemperatureChart(ContextCompat.getColor(requireContext(), R.color.accent_orange));
        configureHumidityChart(ContextCompat.getColor(requireContext(), R.color.accent_blue));
        configureSoilMoistureChart(ContextCompat.getColor(requireContext(), R.color.primary_green));

        // Attach Firebase listeners
        // NOTE: loadDashboardInsights() intentionally removed — it previously
        // created a listener that did nothing, leaking a permanent Firebase handle.
        loadHistoryCharts();
        loadWaterLossData();

        return view;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Expand/collapse helper (replaces duplicated inline code)
    // ─────────────────────────────────────────────────────────────────────
    private void toggleSection(View detailsView, ImageButton chevron) {
        boolean isExpanded = detailsView.getVisibility() == View.VISIBLE;
        TransitionManager.beginDelayedTransition((ViewGroup) detailsView.getParent());
        if (isExpanded) {
            detailsView.setVisibility(View.GONE);
            chevron.animate().rotation(90).setDuration(200).start();
        } else {
            detailsView.setVisibility(View.VISIBLE);
            chevron.animate().rotation(270).setDuration(200).start();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Data aggregation — group raw readings into daily averages
    //
    // Input : raw HistoryModel list (up to 50 recent readings)
    // Output: list of DayAverage, one per calendar day, newest last
    //         max 7 entries (last 7 calendar days)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Groups raw history entries by calendar day and returns daily averages.
     * Result is ordered oldest-to-newest with at most {@code maxDays} entries.
     */
    private List<DayAverage> aggregateByDay(ArrayList<HistoryModel> entries, int maxDays) {
        // Use a LinkedHashMap keyed by "yyyyMMdd" to preserve insertion order
        LinkedHashMap<String, float[]> dayBuckets = new LinkedHashMap<>();
        // float[4] = {sumTemp, sumHum, sumSoil, count}
        SimpleDateFormat dayKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        SimpleDateFormat dayLabel = new SimpleDateFormat("EEE", Locale.getDefault()); // Mon, Tue…

        for (HistoryModel m : entries) {
            if (m == null) continue;
            String key = dayKey.format(new java.util.Date(m.getTimestamp()));
            float[] bucket = dayBuckets.get(key);
            if (bucket == null) {
                bucket = new float[]{0, 0, 0, 0};
                dayBuckets.put(key, bucket);
            }
            bucket[0] += m.getTemperature();
            bucket[1] += m.getHumidity();
            bucket[2] += m.getSoilMoisture();
            bucket[3] += 1;
        }

        // Convert buckets to DayAverage list (keep only the latest maxDays)
        List<String> keys = new ArrayList<>(dayBuckets.keySet());
        int start = Math.max(0, keys.size() - maxDays);
        List<DayAverage> result = new ArrayList<>();

        SimpleDateFormat parser = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        for (int i = start; i < keys.size(); i++) {
            String k = keys.get(i);
            float[] b = dayBuckets.get(k);
            if (b == null || b[3] == 0) continue;
            String label;
            try {
                label = dayLabel.format(parser.parse(k));
            } catch (Exception ex) {
                label = k.substring(6); // fallback: day-of-month digits
            }
            result.add(new DayAverage(label, b[0] / b[3], b[1] / b[3], b[2] / b[3]));
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Section 2 — History charts (fixed loading + aggregated data)
    // ─────────────────────────────────────────────────────────────────────
    private void loadHistoryCharts() {
        historyListener = FirebaseHelper.getInstance().listenHistoryModel(
                new FirebaseHelper.HistoryModelListener() {

            @Override
            public void onHistoryLoaded(java.util.ArrayList<HistoryModel> allEntries) {
                if (getContext() == null) return;
                try {
                    // ── FIX: Always hide loading immediately (was only hidden inside
                    // the non-empty branch, causing infinite spinner on empty data).
                    layoutLoadingInsights.setVisibility(View.GONE);

                    // Handle empty state
                    if (allEntries == null || allEntries.isEmpty()) {
                        layoutMainContent.setVisibility(View.GONE);
                        layoutErrorInsights.setVisibility(View.GONE);
                        layoutEmptyInsights.setVisibility(View.VISIBLE);
                        trendDataPointsText.setText(getString(R.string.no_history_data));
                        return;
                    }

                    // Show main content
                    layoutEmptyInsights.setVisibility(View.GONE);
                    layoutErrorInsights.setVisibility(View.GONE);
                    layoutMainContent.setVisibility(View.VISIBLE);

                    // ── Skip redundant re-render if data size hasn't changed ──
                    if (allEntries.size() == lastRenderedEntryCount) return;
                    lastRenderedEntryCount = allEntries.size();

                    // ── Aggregate raw readings into daily averages (max 7 days) ──
                    List<DayAverage> dailyAvgs = aggregateByDay(allEntries, 7);
                    int dayCount = dailyAvgs.size();

                    trendDataPointsText.setText(dayCount + " days of data");

                    // ── Today's Summary — use today's bucket if available ─────
                    boolean todayFound = false;
                    Calendar today = Calendar.getInstance();
                    SimpleDateFormat dayKeyFmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                    String todayKey = dayKeyFmt.format(today.getTime());

                    double sumTodayTemp = 0, sumTodayHum = 0, sumTodaySoil = 0;
                    int todayCount = 0;
                    for (HistoryModel m : allEntries) {
                        if (m == null) continue;
                        String k;
                        try { k = dayKeyFmt.format(new java.util.Date(m.getTimestamp())); }
                        catch (Exception e) { continue; }
                        if (k.equals(todayKey)) {
                            sumTodayTemp += m.getTemperature();
                            sumTodayHum  += m.getHumidity();
                            sumTodaySoil += m.getSoilMoisture();
                            todayCount++;
                            todayFound = true;
                        }
                    }
                    if (todayFound && todayCount > 0) {
                        summaryTemperatureText.setText(
                                String.format(Locale.getDefault(), "%.1f°C", sumTodayTemp / todayCount));
                        summaryHumidityText.setText(
                                String.format(Locale.getDefault(), "%.1f%%", sumTodayHum / todayCount));
                        summarySoilMoistureText.setText(
                                String.format(Locale.getDefault(), "%.0f", sumTodaySoil / todayCount));
                    } else if (dayCount > 0) {
                        // Fall back to most recent day's data
                        DayAverage latest = dailyAvgs.get(dayCount - 1);
                        summaryTemperatureText.setText(
                                String.format(Locale.getDefault(), "%.1f°C", latest.temp));
                        summaryHumidityText.setText(
                                String.format(Locale.getDefault(), "%.1f%%", latest.humidity));
                        summarySoilMoistureText.setText(
                                String.format(Locale.getDefault(), "%.0f", latest.soil));
                    } else {
                        summaryTemperatureText.setText("—");
                        summaryHumidityText.setText("—");
                        summarySoilMoistureText.setText("—");
                    }

                    // ── Trend indicators (compare first half vs second half) ──
                    int totalCount = allEntries.size();
                    if (totalCount >= 2) {
                        int half = totalCount / 2;
                        double oldTempSum = 0, newTempSum = 0;
                        double oldHumSum  = 0, newHumSum  = 0;
                        double oldSoilSum = 0, newSoilSum = 0;
                        for (int i = 0; i < half; i++) {
                            HistoryModel m = allEntries.get(i);
                            if (m == null) continue;
                            oldTempSum += m.getTemperature();
                            oldHumSum  += m.getHumidity();
                            oldSoilSum += m.getSoilMoisture();
                        }
                        for (int i = half; i < totalCount; i++) {
                            HistoryModel m = allEntries.get(i);
                            if (m == null) continue;
                            newTempSum += m.getTemperature();
                            newHumSum  += m.getHumidity();
                            newSoilSum += m.getSoilMoisture();
                        }
                        applyTrendIndicator(summaryTemperatureTrend,
                                newTempSum / (totalCount - half) - oldTempSum / half,
                                0.2, "°C", "Rising", "Cooling", "#FF7043", "#29B6F6");
                        applyTrendIndicator(summaryHumidityTrend,
                                newHumSum / (totalCount - half) - oldHumSum / half,
                                1.0, "%", "Rising", "Falling", "#29B6F6", "#D4E157");
                        applyTrendIndicator(summarySoilMoistureTrend,
                                newSoilSum / (totalCount - half) - oldSoilSum / half,
                                10.0, "", "Drier", "Wetter", "#FF7043", "#66BB6A");
                    } else {
                        summaryTemperatureTrend.setVisibility(View.GONE);
                        summaryHumidityTrend.setVisibility(View.GONE);
                        summarySoilMoistureTrend.setVisibility(View.GONE);
                    }

                    // ── Build chart entries from aggregated daily averages ────
                    final String[] xLabels = new String[dayCount];
                    ArrayList<Entry> tempEntries  = new ArrayList<>();
                    ArrayList<Entry> humEntries   = new ArrayList<>();
                    ArrayList<Entry> soilEntries  = new ArrayList<>();

                    double sumTemp = 0, sumHum = 0, sumSoil = 0;
                    for (int i = 0; i < dayCount; i++) {
                        DayAverage d = dailyAvgs.get(i);
                        xLabels[i] = d.label;
                        tempEntries.add(new Entry(i, d.temp));
                        humEntries.add(new Entry(i, d.humidity));
                        soilEntries.add(new Entry(i, d.soil));
                        sumTemp += d.temp;
                        sumHum  += d.humidity;
                        sumSoil += d.soil;
                    }

                    double avgTemp = dayCount > 0 ? sumTemp / dayCount : 0;
                    double avgHum  = dayCount > 0 ? sumHum  / dayCount : 0;
                    double avgSoil = dayCount > 0 ? sumSoil / dayCount : 0;

                    trendAvgTempText.setText(String.format(Locale.getDefault(), "Avg: %.1f°C", avgTemp));
                    trendAvgHumidityText.setText(String.format(Locale.getDefault(), "Avg: %.1f%%", avgHum));
                    trendAvgSoilText.setText(String.format(Locale.getDefault(), "Avg: %.0f", avgSoil));

                    // ── Update chart X-axis formatters with day labels ────────
                    setChartXLabels(chartTemperature, xLabels);
                    setChartXLabels(chartHumidity, xLabels);
                    setChartXLabels(chartSoilMoisture, xLabels);

                    // ── Push data to charts (animate only first time) ─────────
                    int tempColor = ContextCompat.getColor(getContext(), R.color.accent_orange);
                    int humColor  = ContextCompat.getColor(getContext(), R.color.accent_blue);
                    int soilColor = ContextCompat.getColor(getContext(), R.color.primary_green);

                    applyTemperatureData(tempEntries, tempColor);
                    applyHumidityData(humEntries, humColor);
                    applySoilMoistureData(soilEntries, soilColor);

                    // ── AI Insights computed from aggregated averages ─────────
                    boolean isSoilDry = avgSoil > 700.0;

                    aiSoilMoistureStatusText.setText(isSoilDry
                            ? String.format(Locale.getDefault(), "Dry (Avg: %.0f)", avgSoil)
                            : String.format(Locale.getDefault(), "Healthy (Avg: %.0f)", avgSoil));

                    aiIrrigationRecommendationText.setText(isSoilDry
                            ? "Irrigation required immediately"
                            : "Irrigation schedule sufficient");

                    aiWaterDemandPredictionText.setText(
                            avgTemp > 30.0 && avgHum < 65.0 ? "High water demand expected" :
                            avgTemp > 25.0 && avgHum < 75.0 ? "Moderate water demand expected" :
                            String.format(Locale.getDefault(), "Water demand stable (Temp: %.0f°C)", avgTemp));

                    aiCropHealthSuggestionText.setText(
                            avgSoil > 700.0 ? "Crop experiencing moisture stress" :
                            avgSoil >= 350.0 ? "Healthy crop condition" :
                            "No water stress detected");

                    String waterNeed, nextWatering;
                    if (avgSoil > 700.0) {
                        waterNeed = "Required"; nextWatering = "Today 5:30 PM";
                    } else if (avgSoil >= 350.0) {
                        waterNeed = "Moderate Need"; nextWatering = "Tomorrow 6:00 AM";
                    } else {
                        waterNeed = "Not Required"; nextWatering = "Tomorrow 6:00 AM";
                    }
                    aiInsightsWaterRequirementText.setText(waterNeed);
                    aiInsightsNextWateringText.setText(nextWatering);

                    String riskLevel = (avgSoil > 700.0 && avgTemp > 30.0) ? "High" :
                                       (avgSoil > 700.0 || avgTemp > 28.0 || avgHum < 60.0) ? "Medium" : "Low";
                    aiInsightsRiskLevelText.setText(riskLevel);

                    // ── Water loss computed from averages ─────────────────────
                    double tempFactor = avgTemp > 25.0 ? (avgTemp - 25.0) * 1.5 : 0.0;
                    double humFactor  = avgHum  < 70.0 ? (70.0 - avgHum) * 0.5 : 0.0;
                    double wlPercent  = Math.max(0.0, Math.min(100.0, 10.0 + tempFactor + humFactor));
                    wlPercentageText.setText(String.format(Locale.getDefault(), "%.1f%%", wlPercent));

                    String riskLabel;
                    int riskColor;
                    if (wlPercent > 45.0) {
                        riskLabel = "High"; riskColor = Color.parseColor("#E53935");
                    } else if (wlPercent > 25.0) {
                        riskLabel = "Medium"; riskColor = Color.parseColor("#FFA726");
                    } else {
                        riskLabel = "Low"; riskColor = Color.parseColor("#43A047");
                    }
                    wlRiskStatusText.setText(riskLabel);
                    wlRiskStatusText.setTextColor(riskColor);

                    String dryZoneLabel;
                    int dryZoneColor;
                    if (avgSoil > 700.0) {
                        dryZoneLabel = "Detected"; dryZoneColor = Color.parseColor("#E53935");
                    } else {
                        dryZoneLabel = "Healthy";  dryZoneColor = Color.parseColor("#43A047");
                    }
                    wlDryZoneText.setText(dryZoneLabel);
                    wlDryZoneText.setTextColor(dryZoneColor);

                    String wlStatusBadgeText;
                    int badgeTextColorRes, badgeBgColorRes;
                    if (wlPercent > 45.0) {
                        wlStatusBadgeText = "Critical Leakage";
                        badgeTextColorRes = R.color.status_critical; badgeBgColorRes = R.color.icon_bg_red;
                    } else if (wlPercent > 25.0) {
                        wlStatusBadgeText = "Possible Leakage";
                        badgeTextColorRes = R.color.status_medium;   badgeBgColorRes = R.color.icon_bg_orange;
                    } else {
                        wlStatusBadgeText = "No Leakage";
                        badgeTextColorRes = R.color.status_healthy;  badgeBgColorRes = R.color.icon_bg_green;
                    }
                    insightWlStatusBadge.setText(wlStatusBadgeText);
                    insightWlStatusBadge.setTextColor(
                            ContextCompat.getColor(getContext(), badgeTextColorRes));
                    insightWlStatusBadge.setBackgroundTintList(
                            ColorStateList.valueOf(ContextCompat.getColor(getContext(), badgeBgColorRes)));

                    insightWlRecommendation.setText(wlPercent > 25.0
                            ? "Possible water leakage detected.\nCheck irrigation lines."
                            : (avgSoil > 700.0 ? "Farm operating normally.\nIncrease irrigation frequency."
                                               : "Farm operating normally.\nContinue regular monitoring."));

                    // Simulate boundary sensor values from average soil
                    double avgSoilPercent = Math.max(0, Math.min(100,
                            100.0 - ((avgSoil - 200.0) / (1023.0 - 200.0)) * 100.0));
                    insightWlNorth.setText(String.format(Locale.getDefault(), "%.0f%%", Math.min(100, avgSoilPercent + 1.2)));
                    insightWlEast.setText( String.format(Locale.getDefault(), "%.0f%%", Math.max(0,  avgSoilPercent - 2.5)));
                    insightWlSouth.setText(String.format(Locale.getDefault(), "%.0f%%", Math.min(100, avgSoilPercent + 0.8)));
                    insightWlWest.setText( String.format(Locale.getDefault(), "%.0f%%", Math.max(0,  avgSoilPercent - 0.5)));
                    insightWlAvgMoisture.setText(String.format(Locale.getDefault(), "%.0f%%", avgSoilPercent));
                    insightWlFlowInput.setText("120 L");
                    double lossLiters = 120.0 * wlPercent / 100.0;
                    insightWlEstLoss.setText(String.format(Locale.getDefault(), "%.0f L", lossLiters));
                    insightWlBoundary.setText(wlPercent > 25.0 ? "East" : "No Leakage");
                } catch (Exception e) {
                    android.util.Log.e("InsightsFragment", "Crash in onHistoryLoaded: " + e.getMessage(), e);
                    // Hide loading and show error screen
                    layoutLoadingInsights.setVisibility(View.GONE);
                    layoutMainContent.setVisibility(View.GONE);
                    layoutEmptyInsights.setVisibility(View.GONE);
                    layoutErrorInsights.setVisibility(View.VISIBLE);
                    if (textErrorDetails != null) {
                        textErrorDetails.setText("Error rendering insights: " + e.toString());
                    }
                }
            }

            @Override
            public void onHistoryError(String errorMessage) {
                if (getContext() == null) return;
                // Always clear loading on error
                layoutLoadingInsights.setVisibility(View.GONE);
                layoutMainContent.setVisibility(View.GONE);
                layoutEmptyInsights.setVisibility(View.GONE);
                layoutErrorInsights.setVisibility(View.VISIBLE);
                if (textErrorDetails != null) textErrorDetails.setText(errorMessage);

                trendDataPointsText.setText("Error");
                trendAvgTempText.setText("Avg: —");
                trendAvgHumidityText.setText("Avg: —");
                trendAvgSoilText.setText("Avg: —");
                summaryTemperatureText.setText("—");
                summaryHumidityText.setText("—");
                summarySoilMoistureText.setText("—");
                if (aiSoilMoistureStatusText != null) aiSoilMoistureStatusText.setText("Unavailable");
                if (aiIrrigationRecommendationText != null) aiIrrigationRecommendationText.setText("Unavailable");
                if (aiWaterDemandPredictionText != null) aiWaterDemandPredictionText.setText("Unavailable");
                if (aiCropHealthSuggestionText != null) aiCropHealthSuggestionText.setText("Unavailable");
                if (aiInsightsNextWateringText != null) aiInsightsNextWateringText.setText("Unavailable");
                if (aiInsightsWaterRequirementText != null) aiInsightsWaterRequirementText.setText("Unavailable");
                if (aiInsightsRiskLevelText != null) aiInsightsRiskLevelText.setText("Unavailable");

                android.util.Log.e("InsightsFragment", "History load error: " + errorMessage);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Trend indicator helper
    // ─────────────────────────────────────────────────────────────────────
    private void applyTrendIndicator(TextView tv, double diff, double threshold,
                                     String unit, String upLabel, String downLabel,
                                     String upColor, String downColor) {
        tv.setVisibility(View.VISIBLE);
        if (diff > threshold) {
            tv.setText(String.format(Locale.getDefault(), "▲ +%.1f%s (%s)", diff, unit, upLabel));
            tv.setTextColor(Color.parseColor(upColor));
        } else if (diff < -threshold) {
            tv.setText(String.format(Locale.getDefault(), "▼ %.1f%s (%s)", diff, unit, downLabel));
            tv.setTextColor(Color.parseColor(downColor));
        } else {
            tv.setText("Stable");
            tv.setTextColor(Color.parseColor("#78909C"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Section 5: Water Loss Localization
    // ─────────────────────────────────────────────────────────────────────
    private void loadWaterLossData() {
        waterLossListener = FirebaseHelper.getInstance().listenWaterLoss(data -> {
            if (getContext() == null) return;
            try {
                insightWlFlowInput.setText((int) data.getFlowInput() + " L");
                insightWlAvgMoisture.setText((int) data.getAverageMoisture() + "%");
                insightWlEstLoss.setText((int) data.getEstimatedLoss() + " L");
                insightWlNorth.setText((int) data.getNorthMoisture() + "%");
                insightWlEast.setText((int) data.getEastMoisture() + "%");
                insightWlSouth.setText((int) data.getSouthMoisture() + "%");
                insightWlWest.setText((int) data.getWestMoisture() + "%");
                insightWlBoundary.setText(
                        data.getSuspectedBoundary() != null ? data.getSuspectedBoundary() : "—");
            } catch (Exception e) {
                android.util.Log.w("InsightsFragment", "WaterLoss update error: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Chart helpers — X-axis label injection
    // ─────────────────────────────────────────────────────────────────────

    /** Updates the X-axis formatter of a chart to show day labels. */
    private void setChartXLabels(LineChart chart, String[] labels) {
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < labels.length) return labels[idx];
                return "";
            }
        });
        xAxis.setLabelCount(labels.length, false);
        xAxis.setGranularity(1f);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Chart configuration
    // ─────────────────────────────────────────────────────────────────────

    private void configureTemperatureChart(int lineColor) {
        chartTemperature.setTouchEnabled(true);
        chartTemperature.setDragEnabled(true);
        chartTemperature.setScaleEnabled(true);
        chartTemperature.setPinchZoom(true);
        chartTemperature.setDoubleTapToZoomEnabled(true);
        chartTemperature.setHighlightPerDragEnabled(true);
        chartTemperature.setDrawGridBackground(false);
        chartTemperature.setExtraBottomOffset(10f);
        chartTemperature.setExtraLeftOffset(4f);
        chartTemperature.setNoDataText("Waiting for temperature readings…");
        chartTemperature.setNoDataTextColor(Color.parseColor("#6B7068"));

        Description desc = new Description();
        desc.setText("Temperature · daily avg");
        desc.setTextSize(10f);
        desc.setTextColor(Color.parseColor("#6B7068"));
        chartTemperature.setDescription(desc);
        chartTemperature.getLegend().setEnabled(false);

        XAxis xAxis = chartTemperature.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setTextColor(Color.parseColor("#6B7068"));
        xAxis.setTextSize(9f);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7, false);

        YAxis leftAxis = chartTemperature.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F5F5F5"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(Color.parseColor("#6B7068"));
        leftAxis.setTextSize(9f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) { return (int) v + "°C"; }
        });

        LimitLine heatLine = new LimitLine(30f, "Heat threshold");
        heatLine.setLineColor(Color.parseColor("#FF5252"));
        heatLine.setLineWidth(1f);
        heatLine.enableDashedLine(8f, 4f, 0f);
        heatLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        heatLine.setTextColor(Color.parseColor("#FF5252"));
        heatLine.setTextSize(9f);
        leftAxis.addLimitLine(heatLine);
        leftAxis.setDrawLimitLinesBehindData(true);
        chartTemperature.getAxisRight().setEnabled(false);
    }

    private void configureHumidityChart(int lineColor) {
        chartHumidity.setTouchEnabled(true);
        chartHumidity.setDragEnabled(true);
        chartHumidity.setScaleEnabled(true);
        chartHumidity.setPinchZoom(true);
        chartHumidity.setDoubleTapToZoomEnabled(true);
        chartHumidity.setHighlightPerDragEnabled(true);
        chartHumidity.setDrawGridBackground(false);
        chartHumidity.setExtraBottomOffset(10f);
        chartHumidity.setExtraLeftOffset(4f);
        chartHumidity.setNoDataText("Waiting for humidity readings…");
        chartHumidity.setNoDataTextColor(Color.parseColor("#6B7068"));

        Description desc = new Description();
        desc.setText("Humidity · daily avg");
        desc.setTextSize(10f);
        desc.setTextColor(Color.parseColor("#6B7068"));
        chartHumidity.setDescription(desc);
        chartHumidity.getLegend().setEnabled(false);

        XAxis xAxis = chartHumidity.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setTextColor(Color.parseColor("#6B7068"));
        xAxis.setTextSize(9f);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7, false);

        YAxis leftAxis = chartHumidity.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F5F5F5"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(Color.parseColor("#6B7068"));
        leftAxis.setTextSize(9f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) { return (int) v + "%"; }
        });

        LimitLine humLine = new LimitLine(80f, "High humidity");
        humLine.setLineColor(Color.parseColor("#1565C0"));
        humLine.setLineWidth(1f);
        humLine.enableDashedLine(8f, 4f, 0f);
        humLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        humLine.setTextColor(Color.parseColor("#1565C0"));
        humLine.setTextSize(9f);
        leftAxis.addLimitLine(humLine);
        leftAxis.setDrawLimitLinesBehindData(true);
        chartHumidity.getAxisRight().setEnabled(false);
    }

    private void configureSoilMoistureChart(int lineColor) {
        chartSoilMoisture.setTouchEnabled(true);
        chartSoilMoisture.setDragEnabled(true);
        chartSoilMoisture.setScaleEnabled(true);
        chartSoilMoisture.setPinchZoom(true);
        chartSoilMoisture.setDoubleTapToZoomEnabled(true);
        chartSoilMoisture.setHighlightPerDragEnabled(true);
        chartSoilMoisture.setDrawGridBackground(false);
        chartSoilMoisture.setExtraBottomOffset(10f);
        chartSoilMoisture.setExtraLeftOffset(4f);
        chartSoilMoisture.setNoDataText("Waiting for soil moisture readings…");
        chartSoilMoisture.setNoDataTextColor(Color.parseColor("#6B7068"));

        Description desc = new Description();
        desc.setText("Soil Moisture · daily avg");
        desc.setTextSize(10f);
        desc.setTextColor(Color.parseColor("#6B7068"));
        chartSoilMoisture.setDescription(desc);
        chartSoilMoisture.getLegend().setEnabled(false);

        XAxis xAxis = chartSoilMoisture.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.parseColor("#E0E0E0"));
        xAxis.setTextColor(Color.parseColor("#6B7068"));
        xAxis.setTextSize(9f);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7, false);

        YAxis leftAxis = chartSoilMoisture.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F5F5F5"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(Color.parseColor("#6B7068"));
        leftAxis.setTextSize(9f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1023f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) { return String.valueOf((int) v); }
        });

        LimitLine dryLine = new LimitLine(700f, "Dry threshold");
        dryLine.setLineColor(Color.parseColor("#E65100"));
        dryLine.setLineWidth(1f);
        dryLine.enableDashedLine(8f, 4f, 0f);
        dryLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        dryLine.setTextColor(Color.parseColor("#E65100"));
        dryLine.setTextSize(9f);
        leftAxis.addLimitLine(dryLine);
        leftAxis.setDrawLimitLinesBehindData(true);
        chartSoilMoisture.getAxisRight().setEnabled(false);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Chart data application — animate only on first render
    // ─────────────────────────────────────────────────────────────────────

    private void applyTemperatureData(ArrayList<Entry> entries, int lineColor) {
        LineDataSet dataSet = new LineDataSet(entries, "Temperature (°C)");
        styleDataSet(dataSet, lineColor, "#FF5252");
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) { return (int) v + "°C"; }
        });
        chartTemperature.setData(new LineData(dataSet));
        chartTemperature.invalidate();
    }

    private void applyHumidityData(ArrayList<Entry> entries, int lineColor) {
        LineDataSet dataSet = new LineDataSet(entries, "Humidity (%)");
        styleDataSet(dataSet, lineColor, "#1565C0");
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) { return (int) v + "%"; }
        });
        chartHumidity.setData(new LineData(dataSet));
        chartHumidity.invalidate();
    }

    private void applySoilMoistureData(ArrayList<Entry> entries, int lineColor) {
        LineDataSet dataSet = new LineDataSet(entries, "Soil Moisture (raw)");
        styleDataSet(dataSet, lineColor, null);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) { return String.valueOf((int) v); }
        });
        chartSoilMoisture.setData(new LineData(dataSet));
        chartSoilMoisture.invalidate();
        chartsAnimated = true;
    }


    /** Applies common visual styling to a LineDataSet. highlightHex may be null to skip highlight. */
    private void styleDataSet(LineDataSet ds, int lineColor, String highlightHex) {
        ds.setColor(lineColor);
        ds.setLineWidth(2.5f);
        ds.setDrawCircles(true);
        ds.setCircleColor(lineColor);
        ds.setCircleRadius(4f);
        ds.setCircleHoleColor(Color.WHITE);
        ds.setCircleHoleRadius(2f);
        ds.setDrawValues(true);
        ds.setValueTextSize(8f);
        ds.setValueTextColor(Color.parseColor("#6B7068"));
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setCubicIntensity(0.2f);
        ds.setDrawFilled(true);
        int fill = Color.argb(50, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor));
        ds.setFillColor(fill);
        if (highlightHex != null) {
            ds.setHighLightColor(Color.parseColor(highlightHex));
            ds.setHighlightLineWidth(1.5f);
            ds.enableDashedHighlightLine(6f, 3f, 0f);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle — detach all listeners to prevent memory leaks
    // ─────────────────────────────────────────────────────────────────────
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // NOTE: No dashboardListener to remove here (it was deleted).
        if (waterLossListener != null)
            FirebaseHelper.getInstance()
                    .removeListener(FirebaseHelper.NODE_WATER_LOSS, waterLossListener);
        if (historyListener != null)
            FirebaseHelper.getInstance().removeHistoryListener(historyListener);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────
    private boolean isToday(long timestampMs) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestampMs);
        Calendar today = Calendar.getInstance();
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
    }
}
