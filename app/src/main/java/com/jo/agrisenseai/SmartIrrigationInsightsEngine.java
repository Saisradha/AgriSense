package com.jo.agrisenseai;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline, trend-based Smart Irrigation Insights Engine.
 *
 * <p>Takes a list of historical sensor entries from Firebase history, evaluates
 * multi-reading patterns over time (rather than a single instantaneous snapshot),
 * and generates actionable recommendations.</p>
 */
public final class SmartIrrigationInsightsEngine {

    private SmartIrrigationInsightsEngine() {
        // Prevent instantiation.
    }

    /**
     * Analyzes historical records to produce a list of individual insight strings.
     *
     * @param history sorted list of history entries from the last 7 days.
     * @return a list of insight strings.
     */
    public static List<String> getInsightsList(List<HistoryModel> history) {
        if (history == null || history.isEmpty()) {
            List<String> fallback = new ArrayList<>();
            fallback.add("No trend data available yet to generate insights.");
            return fallback;
        }

        int totalCount = history.size();
        int dryCount = 0;
        int hotCount = 0;
        double sumSoil = 0;
        double sumTemp = 0;

        int consecutiveDry = 0;
        int maxConsecutiveDry = 0;

        for (HistoryModel m : history) {
            int moisture = m.getSoilMoisture();
            int temp = m.getTemperature();

            sumSoil += moisture;
            sumTemp += temp;

            // Soil is considered dry if sensor value > 700
            if (moisture > 700) {
                dryCount++;
                consecutiveDry++;
                if (consecutiveDry > maxConsecutiveDry) {
                    maxConsecutiveDry = consecutiveDry;
                }
            } else {
                consecutiveDry = 0;
            }

            // High temperature threshold
            if (temp > 32) {
                hotCount++;
            }
        }

        double avgSoil = sumSoil / totalCount;
        double avgTemp = sumTemp / totalCount;

        List<String> insights = new ArrayList<>();

        // 1. Soil remained dry for long periods
        // Flagged if there's a run of 3+ dry readings, or if > 35% of total readings are dry.
        boolean remainedDry = maxConsecutiveDry >= 3 || ((double) dryCount / totalCount) >= 0.35;
        if (remainedDry) {
            insights.add("Soil remained dry for long periods.");
        }

        // 2. Irrigation frequency should increase
        if (avgSoil > 750 || remainedDry) {
            insights.add("Irrigation frequency should increase.");
        }

        // 3. Moisture levels are healthy
        if (avgSoil <= 700 && !remainedDry) {
            insights.add("Moisture levels are healthy.");
        }

        // 4. High temperature increased water demand
        if (hotCount > 0 || avgTemp > 30) {
            insights.add("High temperature increased water demand.");
        }

        // Fallback
        if (insights.isEmpty()) {
            insights.add("Moisture levels and temperatures are normal.");
        }

        return insights;
    }

    /**
     * Analyzes historical records to produce a consolidated recommendations string.
     *
     * @param history sorted list of history entries from the last 7 days.
     * @return a formatted multi-line summary of insights.
     */
    public static String generate(List<HistoryModel> history) {
        List<String> list = getInsightsList(history);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append("• ").append(list.get(i));
            if (i < list.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
