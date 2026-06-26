package com.jo.agrisenseai;

/**
 * Model class representing a single sensor history entry stored permanently
 * in the Firebase Realtime Database under the {@code history/} node.
 *
 * <p>Firebase database structure:</p>
 * <pre>
 * history/
 *   {pushKey}/
 *     temperature  : int   – degrees Celsius
 *     humidity     : int   – relative humidity (%)
 *     soilMoisture : int   – raw sensor value (0 – 1023)
 *     timestamp    : long  – epoch milliseconds (device time)
 * </pre>
 *
 * <p>The no-arg constructor and all setters are required by the Firebase SDK
 * for automatic deserialization via reflection.</p>
 */
public class HistoryModel {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private int  temperature;
    private int  humidity;
    private int  soilMoisture;
    private long timestamp;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Required empty constructor for Firebase deserialization.
     * Do not remove — Firebase uses reflection to instantiate this class.
     */
    public HistoryModel() {
    }

    /**
     * Creates a fully-populated {@code HistoryModel}.
     *
     * @param temperature  sensor temperature reading in degrees Celsius
     * @param humidity     relative humidity percentage (0–100)
     * @param soilMoisture raw soil-moisture sensor value (0–1023)
     * @param timestamp    epoch milliseconds when the reading was recorded
     */
    public HistoryModel(int temperature, int humidity, int soilMoisture, long timestamp) {
        this.temperature  = temperature;
        this.humidity     = humidity;
        this.soilMoisture = soilMoisture;
        this.timestamp    = timestamp;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the temperature reading in degrees Celsius.
     */
    public int getTemperature() {
        return temperature;
    }

    /**
     * Returns the relative humidity percentage (0–100).
     */
    public int getHumidity() {
        return humidity;
    }

    /**
     * Returns the raw soil-moisture sensor value (0–1023).
     */
    public int getSoilMoisture() {
        return soilMoisture;
    }

    /**
     * Returns the epoch-millisecond timestamp when the reading was recorded.
     */
    public long getTimestamp() {
        return timestamp;
    }

    // -------------------------------------------------------------------------
    // Setters  (required by Firebase for deserialization)
    // -------------------------------------------------------------------------

    /**
     * Sets the temperature reading in degrees Celsius.
     *
     * @param temperature temperature value
     */
    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    /**
     * Sets the relative humidity percentage (0–100).
     *
     * @param humidity humidity value
     */
    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    /**
     * Sets the raw soil-moisture sensor value (0–1023).
     *
     * @param soilMoisture soil moisture value
     */
    public void setSoilMoisture(int soilMoisture) {
        this.soilMoisture = soilMoisture;
    }

    /**
     * Sets the epoch-millisecond timestamp when the reading was recorded.
     *
     * @param timestamp epoch milliseconds
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "HistoryModel{" +
                "temperature=" + temperature +
                ", humidity=" + humidity +
                ", soilMoisture=" + soilMoisture +
                ", timestamp=" + timestamp +
                '}';
    }
}
