package com.jo.agrisenseai;

/**
 * Offline, rule-based AI Irrigation Engine.
 * <p>
 * Takes raw {@link SensorData} and produces an {@link AIResult} containing
 * the farm health score, pump status, irrigation recommendation, risk level,
 * next watering suggestion, and water requirement estimate.
 * <p>
 * No external services, no ML models - pure deterministic rules so the
 * exact same logic can later run on ESP32 hardware.
 */
public final class AIEngine {

    // Soil moisture thresholds (raw sensor scale, e.g. 0-1023 from a capacitive probe).
    // Higher reading = drier soil for typical soil moisture sensors.
    private static final double SOIL_MOISTURE_HIGH = 900;
    private static final double SOIL_MOISTURE_MEDIUM = 700;

    // Temperature / humidity thresholds for the "hot & dry air" rule.
    private static final double TEMPERATURE_HIGH = 32;
    private static final double HUMIDITY_LOW = 40;

    // Ideal light intensity range (lux) used for the farm health score.
    private static final double LIGHT_IDEAL_MIN = 300;
    private static final double LIGHT_IDEAL_MAX = 1000;

    public static final String RISK_HIGH = "High";
    public static final String RISK_MEDIUM = "Medium";
    public static final String RISK_LOW = "Low";

    public static final String PUMP_ON = "ON";
    public static final String PUMP_OFF = "OFF";

    private AIEngine() {
    }

    /**
     * Analyzes the given sensor readings and returns the AI's decisions.
     */
    public static AIResult analyze(SensorData sensorData) {
        double soilMoisture = sensorData.getSoilMoisture();
        double temperature = sensorData.getTemperature();
        double humidity = sensorData.getHumidity();
        double lightIntensity = sensorData.getLightIntensity();

        // sensorData.isRainDetected() is reserved for future hardware use
        // and is intentionally not part of the decision logic yet.

        String riskLevel;
        String pumpStatus;
        String aiRecommendation;
        String waterRequirement;
        String nextWatering;

        boolean hotAndDryAir = temperature > TEMPERATURE_HIGH && humidity < HUMIDITY_LOW;

        if (soilMoisture > SOIL_MOISTURE_HIGH || hotAndDryAir) {
            riskLevel = RISK_HIGH;
            pumpStatus = PUMP_ON;
            aiRecommendation = "Water Required";
            waterRequirement = "High - approx 500 L";
            nextWatering = "Now";
        } else if (soilMoisture >= SOIL_MOISTURE_MEDIUM) {
            riskLevel = RISK_MEDIUM;
            pumpStatus = PUMP_OFF;
            aiRecommendation = "Moderate Water Need";
            waterRequirement = "Moderate - approx 250 L";
            nextWatering = "In 8 hours";
        } else {
            riskLevel = RISK_LOW;
            pumpStatus = PUMP_OFF;
            aiRecommendation = "Soil Moisture Good";
            waterRequirement = "Not Required";
            nextWatering = "Tomorrow, 6:00 AM";
        }

        int farmHealth = calculateFarmHealth(riskLevel, lightIntensity);

        return new AIResult(farmHealth, pumpStatus, aiRecommendation, riskLevel, nextWatering, waterRequirement);
    }

    /**
     * Derives an overall farm health score (0-100) from the risk level
     * plus a small adjustment for how close light intensity is to the
     * ideal growing range.
     */
    private static int calculateFarmHealth(String riskLevel, double lightIntensity) {
        int baseHealth;

        switch (riskLevel) {
            case RISK_HIGH:
                baseHealth = 60;
                break;
            case RISK_MEDIUM:
                baseHealth = 80;
                break;
            default:
                baseHealth = 95;
                break;
        }

        if (lightIntensity < LIGHT_IDEAL_MIN || lightIntensity > LIGHT_IDEAL_MAX) {
            baseHealth -= 5;
        }

        if (baseHealth < 0) {
            baseHealth = 0;
        }
        if (baseHealth > 100) {
            baseHealth = 100;
        }

        return baseHealth;
    }
}
