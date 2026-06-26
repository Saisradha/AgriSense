package com.jo.agrisenseai;

/**
 * Immutable snapshot of sensor readings stored permanently in the Firebase
 * Realtime Database under the {@code history/} node.
 *
 * <pre>
 * Firebase structure written by {@link FirebaseHelper#saveSensorHistory}:
 *
 * history/
 *   {firebase-push-key}/
 *     temperature  : double   – degrees Celsius
 *     humidity     : double   – relative humidity %
 *     soilMoisture : double   – raw sensor value (0 – 1023)
 *     lightIntensity: double  – lux
 *     timestamp    : long     – epoch milliseconds (device time)
 * </pre>
 *
 * The no-arg constructor and all setters are required by the Firebase SDK
 * for automatic deserialization via reflection.
 */
public class SensorHistory {

    private double temperature;
    private double humidity;
    private double soilMoisture;
    private double lightIntensity;
    private long   timestamp;

    /** Required by Firebase for deserialization. */
    public SensorHistory() {
    }

    public SensorHistory(double temperature,
                         double humidity,
                         double soilMoisture,
                         double lightIntensity,
                         long   timestamp) {
        this.temperature   = temperature;
        this.humidity      = humidity;
        this.soilMoisture  = soilMoisture;
        this.lightIntensity = lightIntensity;
        this.timestamp     = timestamp;
    }

    // ---- Getters --------------------------------------------------------

    public double getTemperature()   { return temperature; }
    public double getHumidity()      { return humidity; }
    public double getSoilMoisture()  { return soilMoisture; }
    public double getLightIntensity(){ return lightIntensity; }
    public long   getTimestamp()     { return timestamp; }

    // ---- Setters (required by Firebase for deserialization) ---------------

    public void setTemperature(double temperature)     { this.temperature   = temperature; }
    public void setHumidity(double humidity)           { this.humidity      = humidity; }
    public void setSoilMoisture(double soilMoisture)   { this.soilMoisture  = soilMoisture; }
    public void setLightIntensity(double lightIntensity){ this.lightIntensity = lightIntensity; }
    public void setTimestamp(long timestamp)            { this.timestamp     = timestamp; }

    @Override
    public String toString() {
        return "SensorHistory{" +
                "timestamp=" + timestamp +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", soilMoisture=" + soilMoisture +
                ", lightIntensity=" + lightIntensity +
                '}';
    }
}
