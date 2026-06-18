package com.jo.agrisenseai;

/**
 * Holds the output of {@link AIEngine#analyze(SensorData)}.
 * <p>
 * This is the single shared contract between the AI engine and:
 * - the Home / My Farm / Insights screens (this phase)
 * - future ESP32 hardware (reads pumpStatus to actuate the relay)
 * - future multilingual support (translates aiRecommendation / riskLevel)
 * - future voice assistant (reads aiRecommendation aloud)
 * - future notifications (triggered by riskLevel changes)
 */
public class AIResult {

    private int farmHealth;
    private String pumpStatus;
    private String aiRecommendation;
    private String riskLevel;
    private String nextWatering;
    private String waterRequirement;

    public AIResult() {
    }

    public AIResult(int farmHealth, String pumpStatus, String aiRecommendation,
                     String riskLevel, String nextWatering, String waterRequirement) {
        this.farmHealth = farmHealth;
        this.pumpStatus = pumpStatus;
        this.aiRecommendation = aiRecommendation;
        this.riskLevel = riskLevel;
        this.nextWatering = nextWatering;
        this.waterRequirement = waterRequirement;
    }

    public int getFarmHealth() {
        return farmHealth;
    }

    public void setFarmHealth(int farmHealth) {
        this.farmHealth = farmHealth;
    }

    public String getPumpStatus() {
        return pumpStatus;
    }

    public void setPumpStatus(String pumpStatus) {
        this.pumpStatus = pumpStatus;
    }

    public String getAiRecommendation() {
        return aiRecommendation;
    }

    public void setAiRecommendation(String aiRecommendation) {
        this.aiRecommendation = aiRecommendation;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getNextWatering() {
        return nextWatering;
    }

    public void setNextWatering(String nextWatering) {
        this.nextWatering = nextWatering;
    }

    public String getWaterRequirement() {
        return waterRequirement;
    }

    public void setWaterRequirement(String waterRequirement) {
        this.waterRequirement = waterRequirement;
    }
}
