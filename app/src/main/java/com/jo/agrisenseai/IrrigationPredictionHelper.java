package com.jo.agrisenseai;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class IrrigationPredictionHelper {

    public static class PredictionResult {
        public final String estimatedTimeLabel;
        public final String aiAnalysisText;
        public final String priority; // Very High, High, Medium, Low, None
        public final String recommendation; // Pump ON, Monitor, No Irrigation Required

        public PredictionResult(String estimatedTimeLabel, String aiAnalysisText, String priority, String recommendation) {
            this.estimatedTimeLabel = estimatedTimeLabel;
            this.aiAnalysisText = aiAnalysisText;
            this.priority = priority;
            this.recommendation = recommendation;
        }
    }

    public static PredictionResult calculateNextWatering(
            long currentTime,
            double soilMoisture,
            double temperature,
            double humidity,
            double farmWaterLevel,
            String cropType,
            int threshold) {

        double moistureDeficit = threshold - soilMoisture;

        // Crop multiplier
        double multiplier = 1.0;
        if (cropType != null) {
            switch (cropType.trim().toLowerCase(Locale.US)) {
                case "rice":
                    multiplier = 0.7;
                    break;
                case "tomato":
                    multiplier = 0.9;
                    break;
                case "wheat":
                    multiplier = 1.0;
                    break;
                case "cotton":
                    multiplier = 1.3;
                    break;
                default:
                    multiplier = 1.0;
                    break;
            }
        }

        double baseHours = 24.0;
        String priority = "None";
        String recommendation = "No Irrigation Required";
        String evapLevel = "Low";

        boolean isWaterLevelLow = farmWaterLevel < 35.0;

        if (soilMoisture >= threshold) {
            baseHours = 24.0;
            priority = "None";
            recommendation = "No Irrigation Required";
            evapLevel = "Low";
        } else {
            // Determine Evaporation Level for AI Analysis output
            if (temperature > 35 && humidity < 45) {
                evapLevel = "Very High";
            } else if (temperature > 32 && humidity < 55) {
                evapLevel = "High";
            } else if (temperature > 28) {
                evapLevel = "Medium";
            } else {
                evapLevel = "Low";
            }

            // AI Priority calculation logic
            // Very High Priority
            if (temperature > 35 && moistureDeficit >= 150 && humidity <= 45 && isWaterLevelLow) {
                baseHours = 1.0;
                priority = "Very High";
                recommendation = "Pump ON";
            }
            // High Priority
            else if (temperature > 32 && moistureDeficit >= 100 && humidity <= 50) {
                baseHours = 2.0;
                priority = "High";
                recommendation = "Pump ON";
            }
            // Medium Priority
            else if (temperature > 28 && moistureDeficit > 0) {
                baseHours = 4.0;
                priority = "Medium";
                recommendation = "Pump ON";
            }
            // Low Priority (soil moisture near threshold, deficit > 0)
            else {
                baseHours = 8.0;
                priority = "Low";
                recommendation = "Monitor";
            }
        }

        // Apply Crop Multiplier to baseline hours
        double finalHours = baseHours * multiplier;

        // Calculate targeted timestamp
        long nextWateringMillis = currentTime + (long) (finalHours * 60.0 * 60.0 * 1000.0);

        // Format next watering time
        String timeLabel = formatWateringTime(currentTime, nextWateringMillis);

        // Build AI Analysis description
        String aiAnalysisText;
        if (soilMoisture >= threshold) {
            aiAnalysisText = "Soil Moisture Sufficient\nNo Irrigation Required";
        } else {
            aiAnalysisText = String.format(Locale.US,
                    "Moisture Deficit = %.0f\nEvaporation = %s\nFarm Water Level = %.0f%%",
                    moistureDeficit,
                    evapLevel,
                    farmWaterLevel);
        }

        return new PredictionResult(timeLabel, aiAnalysisText, priority, recommendation);
    }

    private static String formatWateringTime(long currentMillis, long targetMillis) {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(currentMillis);

        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(targetMillis);

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String formattedTime = timeFormat.format(new Date(targetMillis));

        if (current.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                current.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return "Today " + formattedTime;
        }

        // Check if tomorrow
        Calendar tomorrow = (Calendar) current.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        if (tomorrow.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return "Tomorrow " + formattedTime;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault());
        return dateFormat.format(new Date(targetMillis));
    }
}
