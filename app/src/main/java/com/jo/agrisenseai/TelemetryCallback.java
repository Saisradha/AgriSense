package com.jo.agrisenseai;

public interface TelemetryCallback {
    void onTelemetryLoaded(FarmTelemetry telemetry);
    void onTelemetryError(String error);
}
