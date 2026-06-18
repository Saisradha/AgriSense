package com.jo.agrisenseai;

/**
 * Firebase model for the waterLossDetection node.
 * Written by WaterLossEngine.analyze() and the simulator buttons.
 * Read by HomeFragment and InsightsFragment.
 */
public class WaterLossData {

    private double flowInput;
    private double northMoisture;
    private double eastMoisture;
    private double southMoisture;
    private double westMoisture;
    private double averageMoisture;
    private double estimatedLoss;
    private String suspectedBoundary;
    private String status;

    // Required empty constructor for Firebase
    public WaterLossData() {
    }

    public WaterLossData(double flowInput, double northMoisture, double eastMoisture,
                          double southMoisture, double westMoisture,
                          double averageMoisture, double estimatedLoss,
                          String suspectedBoundary, String status) {
        this.flowInput = flowInput;
        this.northMoisture = northMoisture;
        this.eastMoisture = eastMoisture;
        this.southMoisture = southMoisture;
        this.westMoisture = westMoisture;
        this.averageMoisture = averageMoisture;
        this.estimatedLoss = estimatedLoss;
        this.suspectedBoundary = suspectedBoundary;
        this.status = status;
    }

    public double getFlowInput() { return flowInput; }
    public void setFlowInput(double flowInput) { this.flowInput = flowInput; }

    public double getNorthMoisture() { return northMoisture; }
    public void setNorthMoisture(double northMoisture) { this.northMoisture = northMoisture; }

    public double getEastMoisture() { return eastMoisture; }
    public void setEastMoisture(double eastMoisture) { this.eastMoisture = eastMoisture; }

    public double getSouthMoisture() { return southMoisture; }
    public void setSouthMoisture(double southMoisture) { this.southMoisture = southMoisture; }

    public double getWestMoisture() { return westMoisture; }
    public void setWestMoisture(double westMoisture) { this.westMoisture = westMoisture; }

    public double getAverageMoisture() { return averageMoisture; }
    public void setAverageMoisture(double averageMoisture) { this.averageMoisture = averageMoisture; }

    public double getEstimatedLoss() { return estimatedLoss; }
    public void setEstimatedLoss(double estimatedLoss) { this.estimatedLoss = estimatedLoss; }

    public String getSuspectedBoundary() { return suspectedBoundary; }
    public void setSuspectedBoundary(String suspectedBoundary) { this.suspectedBoundary = suspectedBoundary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
