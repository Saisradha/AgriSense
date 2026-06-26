package com.jo.agrisenseai;

/**
 * Representing the database schema for a farm item.
 */
public class Farm {

    private String farmId;
    private String farmName;
    private String location;
    private String totalAcres;
    private String cropType;
    private int moistureThreshold;
    private String irrigationSchedule;
    private String notes;
    private long createdAt;
    private String healthStatus;
    private String nextWatering;
    private int soilMoisture;
    private String userId;
    private String soilType;
    private String plantingDate;
    private String pumpStatus;

    // Required empty constructor for Firebase
    public Farm() {
    }

    public Farm(String farmId, String farmName, String location, String totalAcres, String cropType,
                int moistureThreshold, String irrigationSchedule, String notes, long createdAt,
                String healthStatus, String nextWatering, int soilMoisture) {
        this.farmId = farmId;
        this.farmName = farmName;
        this.location = location;
        this.totalAcres = totalAcres;
        this.cropType = cropType;
        this.moistureThreshold = moistureThreshold;
        this.irrigationSchedule = irrigationSchedule;
        this.notes = notes;
        this.createdAt = createdAt;
        this.healthStatus = healthStatus;
        this.nextWatering = nextWatering;
        this.soilMoisture = soilMoisture;
    }

    public Farm(String farmId, String farmName, String location, String totalAcres, String cropType,
                int moistureThreshold, String irrigationSchedule, String notes, long createdAt,
                String healthStatus, String nextWatering, int soilMoisture, String userId,
                String soilType, String plantingDate, String pumpStatus) {
        this.farmId = farmId;
        this.farmName = farmName;
        this.location = location;
        this.totalAcres = totalAcres;
        this.cropType = cropType;
        this.moistureThreshold = moistureThreshold;
        this.irrigationSchedule = irrigationSchedule;
        this.notes = notes;
        this.createdAt = createdAt;
        this.healthStatus = healthStatus;
        this.nextWatering = nextWatering;
        this.soilMoisture = soilMoisture;
        this.userId = userId;
        this.soilType = soilType;
        this.plantingDate = plantingDate;
        this.pumpStatus = pumpStatus;
    }

    // Getters and Setters

    public String getFarmId() {
        return farmId;
    }

    public void setFarmId(String farmId) {
        this.farmId = farmId;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTotalAcres() {
        return totalAcres;
    }

    public void setTotalAcres(String totalAcres) {
        this.totalAcres = totalAcres;
    }

    public String getCropType() {
        return cropType;
    }

    public void setCropType(String cropType) {
        this.cropType = cropType;
    }

    public int getMoistureThreshold() {
        return moistureThreshold;
    }

    public void setMoistureThreshold(int moistureThreshold) {
        this.moistureThreshold = moistureThreshold;
    }

    public String getIrrigationSchedule() {
        return irrigationSchedule;
    }

    public void setIrrigationSchedule(String irrigationSchedule) {
        this.irrigationSchedule = irrigationSchedule;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getNextWatering() {
        return nextWatering;
    }

    public void setNextWatering(String nextWatering) {
        this.nextWatering = nextWatering;
    }

    public int getSoilMoisture() {
        return soilMoisture;
    }

    public void setSoilMoisture(int soilMoisture) {
        this.soilMoisture = soilMoisture;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSoilType() {
        return soilType;
    }

    public void setSoilType(String soilType) {
        this.soilType = soilType;
    }

    public String getPlantingDate() {
        return plantingDate;
    }

    public void setPlantingDate(String plantingDate) {
        this.plantingDate = plantingDate;
    }

    public String getPumpStatus() {
        return pumpStatus;
    }

    public void setPumpStatus(String pumpStatus) {
        this.pumpStatus = pumpStatus;
    }
}
