package com.jo.agrisenseai;

public class FarmTelemetry {
    private final double temperature;
    private final double humidity;
    private final double soilMoisture;
    private final double waterLevel;
    private final String pumpStatus;
    private final String aiPrediction;
    private final String farmName;
    private final WeatherData weatherData;

    public FarmTelemetry(double temperature, double humidity, double soilMoisture, double waterLevel,
                         String pumpStatus, String aiPrediction, String farmName, WeatherData weatherData) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.soilMoisture = soilMoisture;
        this.waterLevel = waterLevel;
        this.pumpStatus = pumpStatus;
        this.aiPrediction = aiPrediction;
        this.farmName = farmName;
        this.weatherData = weatherData;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getSoilMoisture() {
        return soilMoisture;
    }

    public double getWaterLevel() {
        return waterLevel;
    }

    public String getPumpStatus() {
        return pumpStatus;
    }

    public String getAiPrediction() {
        return aiPrediction;
    }

    public String getFarmName() {
        return farmName;
    }

    public WeatherData getWeatherData() {
        return weatherData;
    }
}
