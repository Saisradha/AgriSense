package com.jo.agrisenseai;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Smart Water Loss Localization engine.
 * Reads raw moisture + flow readings, detects the leaking boundary,
 * estimates water loss, and writes results to Firebase.
 */
public final class WaterLossEngine {

    public static final String STATUS_NO_LEAKAGE   = "No Leakage";
    public static final String STATUS_POSSIBLE     = "Possible Leakage";
    public static final String STATUS_DETECTED     = "Leakage Detected";

    private static final double EXPECTED_MOISTURE  = 80.0;
    private static final String NODE = "waterLossDetection";

    private WaterLossEngine() {}

    /**
     * Runs the full analysis pipeline and writes the result to Firebase.
     */
    public static WaterLossData analyze(double flowInput,
                                         double northMoisture,
                                         double eastMoisture,
                                         double southMoisture,
                                         double westMoisture) {

        double averageMoisture = (northMoisture + eastMoisture + southMoisture + westMoisture) / 4.0;

        // Boundary detection
        String suspectedBoundary;
        if (eastMoisture > averageMoisture + 20) {
            suspectedBoundary = "East";
        } else if (westMoisture > averageMoisture + 20) {
            suspectedBoundary = "West";
        } else if (northMoisture > averageMoisture + 20) {
            suspectedBoundary = "North";
        } else if (southMoisture > averageMoisture + 20) {
            suspectedBoundary = "South";
        } else {
            suspectedBoundary = "No Leakage";
        }

        // Water loss estimation
        double lossPercent = EXPECTED_MOISTURE - averageMoisture;
        double estimatedLoss = (flowInput * lossPercent) / 100.0;
        if (estimatedLoss < 0) estimatedLoss = 0;

        // Status
        String status;
        if (estimatedLoss <= 100) {
            status = STATUS_NO_LEAKAGE;
        } else if (estimatedLoss <= 250) {
            status = STATUS_POSSIBLE;
        } else {
            status = STATUS_DETECTED;
        }

        WaterLossData result = new WaterLossData(
                flowInput, northMoisture, eastMoisture, southMoisture, westMoisture,
                Math.round(averageMoisture * 10.0) / 10.0,
                Math.round(estimatedLoss * 10.0) / 10.0,
                suspectedBoundary, status);

        FirebaseDatabase.getInstance().getReference(NODE).setValue(result);

        return result;
    }

    // ---------------------------------------------------------------
    // Recommendation builder
    // ---------------------------------------------------------------

    public static String buildRecommendation(WaterLossData d) {
        if (d == null) return "";

        String boundary = d.getSuspectedBoundary();
        double loss = d.getEstimatedLoss();
        String status = d.getStatus();

        if (STATUS_NO_LEAKAGE.equals(status) || "No Leakage".equals(boundary)) {
            return "✅ Farm is operating normally.\n"
                    + "Water Entered: " + (int) d.getFlowInput() + " L\n"
                    + "No significant water loss detected.\n"
                    + "Continue regular monitoring.";
        }

        String icon = loss > 250 ? "🚨" : "⚠";
        return icon + " " + status + "\n\n"
                + "Water Entered: " + (int) d.getFlowInput() + " L\n"
                + "Estimated Loss: " + (int) loss + " L\n\n"
                + "Suspected Boundary:\n" + boundary + " Side\n\n"
                + "Possible Causes:\n"
                + "• Rat Hole\n"
                + "• Bund Damage\n"
                + "• Water Diversion\n\n"
                + "Recommended Action:\n"
                + "Inspect " + boundary + " Boundary Immediately";
    }
}
