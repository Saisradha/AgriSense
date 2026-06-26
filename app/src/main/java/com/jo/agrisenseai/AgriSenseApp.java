package com.jo.agrisenseai;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Custom Application class for initialization tasks.
 * <p>
 * Ensures the preferred theme (Light/Dark Mode) is applied at application startup
 * before any activity is inflated, preventing flickering.
 */
public class AgriSenseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Load theme selection from SharedPreferences
        boolean isDarkMode = ThemePreferenceManager.isDarkMode(this);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
