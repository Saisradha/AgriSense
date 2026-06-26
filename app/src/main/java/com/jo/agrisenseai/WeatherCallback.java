package com.jo.agrisenseai;

public interface WeatherCallback {
    void onSuccess(WeatherData weatherData);
    void onFailure(String error);
}
