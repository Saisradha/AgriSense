package com.jo.agrisenseai;

public class WeatherData {
    private final String description;
    private final double temp;
    private final double humidity;
    private final double windSpeed;
    private final String rainForecast;

    public WeatherData(String description, double temp, double humidity, double windSpeed, String rainForecast) {
        this.description = description;
        this.temp = temp;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.rainForecast = rainForecast;
    }

    public String getDescription() {
        return description;
    }

    public double getTemp() {
        return temp;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public String getRainForecast() {
        return rainForecast;
    }
}
