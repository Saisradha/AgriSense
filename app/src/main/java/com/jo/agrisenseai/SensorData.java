package com.jo.agrisenseai;

/**
 * Represents raw sensor readings.
 * Written by future ESP32/hardware integration.
 * Read by future AI prediction engine.
 */
public class SensorData {

    private double temperature;
    private double humidity;
    private double soilMoisture;
    private double lightIntensity;
    private double farmWaterLevel;
    private String pumpStatus;

    /**
     * Placeholder for future rain sensor hardware.
     * Not yet used in AI calculations - reserved for Phase 6+ hardware integration.
     */
    private boolean rainDetected;

    // Required empty constructor for Firebase
    public SensorData() {
    }

    public SensorData(double temperature, double humidity, double soilMoisture, double lightIntensity,
                      double farmWaterLevel, String pumpStatus) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.soilMoisture = soilMoisture;
        this.lightIntensity = lightIntensity;
        this.farmWaterLevel = farmWaterLevel;
        this.pumpStatus = pumpStatus;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getSoilMoisture() {
        return soilMoisture;
    }

    public void setSoilMoisture(double soilMoisture) {
        this.soilMoisture = soilMoisture;
    }

    public double getLightIntensity() {
        return lightIntensity;
    }

    public void setLightIntensity(double lightIntensity) {
        this.lightIntensity = lightIntensity;
    }

    public double getFarmWaterLevel() {
        return farmWaterLevel;
    }

    public void setFarmWaterLevel(double farmWaterLevel) {
        this.farmWaterLevel = farmWaterLevel;
    }

    public String getPumpStatus() {
        return pumpStatus;
    }

    public void setPumpStatus(String pumpStatus) {
        this.pumpStatus = pumpStatus;
    }

    public boolean isRainDetected() {
        return rainDetected;
    }

    public void setRainDetected(boolean rainDetected) {
        this.rainDetected = rainDetected;
    }
}
