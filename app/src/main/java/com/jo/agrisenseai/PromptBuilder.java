package com.jo.agrisenseai;

public class PromptBuilder {

    public static String buildPrompt(String userQuestion, FarmTelemetry telemetry) {
        if (telemetry == null) {
            return "You are AgriSense AI.\n\n" +
                   "User Question: " + userQuestion + "\n\n" +
                   "Generate a professional farming recommendation.";
        }

        double temp = telemetry.getTemperature();
        double humidity = telemetry.getHumidity();
        double soilMoisture = telemetry.getSoilMoisture();
        double waterLevel = telemetry.getWaterLevel();
        String pumpStatus = telemetry.getPumpStatus();
        String aiPrediction = telemetry.getAiPrediction();
        String farmName = telemetry.getFarmName();

        // Calculate moisture percentage
        int moisturePercent = toSoilMoisturePercent(soilMoisture);

        // Weather fields
        String weatherDesc = "Not Available";
        WeatherData weather = telemetry.getWeatherData();
        if (weather != null) {
            weatherDesc = weather.getDescription() + " (" + Math.round(weather.getTemp()) + "°C, " + Math.round(weather.getHumidity()) + "% Humidity)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("You are AgriSense AI.\n\n");
        sb.append("Current Farm Data:\n\n");
        sb.append("Temperature: ").append(Math.round(temp)).append("°C\n");
        sb.append("Humidity: ").append(Math.round(humidity)).append("%\n");
        sb.append("Soil Moisture: ").append(moisturePercent).append("% (Raw value: ").append(Math.round(soilMoisture)).append(")\n");
        sb.append("Water Level: ").append(Math.round(waterLevel)).append("%\n");
        sb.append("Pump Status: ").append(pumpStatus).append("\n");
        sb.append("Weather: ").append(weatherDesc).append("\n");
        sb.append("AI Prediction: ").append(aiPrediction).append("\n");
        sb.append("Farm Name: ").append(farmName).append("\n\n");
        sb.append("User Question: ").append(userQuestion).append("\n\n");
        sb.append("Generate a professional farming recommendation.");

        return sb.toString();
    }

    private static int toSoilMoisturePercent(double rawMoisture) {
        if (rawMoisture <= 0) {
            return 0;
        }
        if (rawMoisture <= 100) {
            return Math.max(0, Math.min(100, (int) Math.round(rawMoisture)));
        }
        int percent = (int) Math.round(100 - (rawMoisture / 1023.0) * 100);
        return Math.max(0, Math.min(100, percent));
    }
}
