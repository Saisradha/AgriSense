package com.jo.agrisenseai;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherService {
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/forecast";
    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;

    public WeatherService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getWeatherData(String rawLocation, @NonNull final WeatherCallback callback) {
        String apiKey = BuildConfig.OPENWEATHER_API_KEY;
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY")) {
            callback.onFailure("OpenWeather API key is not configured.");
            return;
        }

        String city = sanitizeLocation(rawLocation);
        String url = BASE_URL + "?q=" + city + "&appid=" + apiKey + "&units=metric";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onFailure("Failed to connect to weather service."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure("Weather API failed with code: " + response.code()));
                    return;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                try {
                    ForecastResponse forecast = gson.fromJson(responseBody, ForecastResponse.class);
                    if (forecast != null && forecast.list != null && !forecast.list.isEmpty()) {
                        ForecastResponse.ForecastItem current = forecast.list.get(0);
                        
                        String description = "Clear";
                        if (current.weather != null && !current.weather.isEmpty()) {
                            description = current.weather.get(0).description;
                        }
                        
                        double temp = current.main != null ? current.main.temp : 0.0;
                        double humidity = current.main != null ? current.main.humidity : 0.0;
                        double windSpeed = current.wind != null ? current.wind.speed : 0.0;

                        // Calculate rain forecast for next 24 hours (next 8 forecast items)
                        String rainForecast = "No rain expected in the next 24 hours.";
                        int limit = Math.min(forecast.list.size(), 8);
                        for (int i = 0; i < limit; i++) {
                            ForecastResponse.ForecastItem item = forecast.list.get(i);
                            if (item.weather != null && !item.weather.isEmpty()) {
                                String mainWeather = item.weather.get(0).main;
                                if ("Rain".equalsIgnoreCase(mainWeather)) {
                                    rainForecast = "Rain forecast: " + item.weather.get(0).description + " expected soon.";
                                    break;
                                }
                            }
                        }

                        WeatherData data = new WeatherData(description, temp, humidity, windSpeed, rainForecast);
                        mainHandler.post(() -> callback.onSuccess(data));
                        return;
                    }
                    mainHandler.post(() -> callback.onFailure("No weather data found for " + city));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure("Failed to parse weather data."));
                }
            }
        });
    }

    private String sanitizeLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return "Hyderabad,IN";
        }
        String loc = location.toLowerCase();
        if (loc.contains("plot") || loc.contains("block") || loc.contains("field") || loc.contains("north") || loc.contains("south")) {
            return "Hyderabad,IN";
        }
        return location.trim();
    }

    // GSON Mapping Classes
    private static class ForecastResponse {
        List<ForecastItem> list;

        static class ForecastItem {
            Main main;
            List<Weather> weather;
            Wind wind;

            static class Main {
                double temp;
                double humidity;
            }

            static class Weather {
                String main;
                String description;
            }

            static class Wind {
                double speed;
            }
        }
    }
}
