package com.jo.agrisenseai;

/**
 * Represents the raw sensor readings uploaded by the ESP32 to Firebase.
 *
 * <p><b>Firebase → Java field mapping</b></p>
 * <pre>
 * Firebase key   Java field         Notes
 * ─────────────  ─────────────────  ────────────────────────────────────────
 * temperature    temperature        °C, double
 * humidity       humidity           % RH, double
 * averageSoil    soilMoisture       Averaged across soil1-4 by ESP32
 * light          lightIntensity     Lux value from LDR / light sensor
 * pumpStatus     pumpStatus         "ON" or "OFF"
 * soil1          soil1              Individual probe reading (ADC 0-1023)
 * soil2          soil2
 * soil3          soil3
 * soil4          soil4
 * </pre>
 *
 * <p>Note: The ESP32 uses {@code averageSoil} and {@code light} as its key names.
 * {@link FirebaseHelper#buildSensorDataFromSnapshot} reads each child by name and
 * maps them to {@code soilMoisture} and {@code lightIntensity} respectively, so
 * automatic Firebase POJO deserialization ({@code getValue(SensorData.class)}) is
 * NOT used for this class — the field names intentionally differ from the DB keys.</p>
 */
public class SensorData {

    private double temperature;
    private double humidity;
    /** Mapped from Firebase key {@code averageSoil}. */
    private double soilMoisture;
    /** Mapped from Firebase key {@code light}. */
    private double lightIntensity;
    private double farmWaterLevel;
    private String pumpStatus;

    // Individual soil probe readings (soil1–soil4)
    private double soil1;
    private double soil2;
    private double soil3;
    private double soil4;

    /**
     * Placeholder for future rain sensor hardware.
     * Not yet used in AI calculations — reserved for Phase 6+ hardware integration.
     */
    private boolean rainDetected;

    // Required empty constructor for Firebase
    public SensorData() {
    }

    public SensorData(double temperature, double humidity, double soilMoisture,
                      double lightIntensity, double farmWaterLevel, String pumpStatus) {
        this.temperature    = temperature;
        this.humidity       = humidity;
        this.soilMoisture   = soilMoisture;
        this.lightIntensity = lightIntensity;
        this.farmWaterLevel = farmWaterLevel;
        this.pumpStatus     = pumpStatus;
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public double getTemperature()    { return temperature; }
    public double getHumidity()       { return humidity; }
    /** Returns the averaged soil moisture (mapped from Firebase {@code averageSoil}). */
    public double getSoilMoisture()   { return soilMoisture; }
    /** Returns the light intensity in lux (mapped from Firebase {@code light}). */
    public double getLightIntensity() { return lightIntensity; }
    public double getFarmWaterLevel() { return farmWaterLevel; }
    public String getPumpStatus()     { return pumpStatus; }
    public double getSoil1()          { return soil1; }
    public double getSoil2()          { return soil2; }
    public double getSoil3()          { return soil3; }
    public double getSoil4()          { return soil4; }
    public boolean isRainDetected()   { return rainDetected; }

    // ── Setters ──────────────────────────────────────────────────────────

    public void setTemperature(double temperature)       { this.temperature    = temperature; }
    public void setHumidity(double humidity)             { this.humidity       = humidity; }
    public void setSoilMoisture(double soilMoisture)     { this.soilMoisture   = soilMoisture; }
    public void setLightIntensity(double lightIntensity) { this.lightIntensity = lightIntensity; }
    public void setFarmWaterLevel(double farmWaterLevel) { this.farmWaterLevel = farmWaterLevel; }
    public void setPumpStatus(String pumpStatus)         { this.pumpStatus     = pumpStatus; }
    public void setSoil1(double soil1)                   { this.soil1          = soil1; }
    public void setSoil2(double soil2)                   { this.soil2          = soil2; }
    public void setSoil3(double soil3)                   { this.soil3          = soil3; }
    public void setSoil4(double soil4)                   { this.soil4          = soil4; }
    public void setRainDetected(boolean rainDetected)    { this.rainDetected   = rainDetected; }
}