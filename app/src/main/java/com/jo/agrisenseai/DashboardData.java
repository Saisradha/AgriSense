package com.jo.agrisenseai;

/**
 * Represents the summary values shown on the
 * Home and Insights screens.
 */
public class DashboardData {

    private int farmHealth;
    private String pumpStatus;
    private String aiRecommendation;
    private int waterSaved;
    private double energySaved;

    /** Set by AIEngine. One of: "Low", "Medium", "High". */
    private String riskLevel;

    /** Set by AIEngine. Human-readable watering suggestion. */
    private String nextWatering;

    /** Set by AIEngine. Human-readable water requirement estimate. */
    private String waterRequirement;

    // Required empty constructor for Firebase
    public DashboardData() {
    }

    public DashboardData(int farmHealth, String pumpStatus, String aiRecommendation,
                          int waterSaved, double energySaved) {
        this.farmHealth = farmHealth;
        this.pumpStatus = pumpStatus;
        this.aiRecommendation = aiRecommendation;
        this.waterSaved = waterSaved;
        this.energySaved = energySaved;
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

    public int getWaterSaved() {
        return waterSaved;
    }

    public void setWaterSaved(int waterSaved) {
        this.waterSaved = waterSaved;
    }

    public double getEnergySaved() {
        return energySaved;
    }

    public void setEnergySaved(double energySaved) {
        this.energySaved = energySaved;
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
