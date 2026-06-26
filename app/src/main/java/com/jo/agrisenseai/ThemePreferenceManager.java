package com.jo.agrisenseai;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists the Dark Mode theme toggle state in SharedPreferences.
 * <p>
 * Shared with VoicePreferenceManager.
 */
public final class ThemePreferenceManager {

    private static final String PREF_FILE = "agrisense_prefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private ThemePreferenceManager() {
    }

    public static boolean isDarkMode(Context context) {
        // Default to false (Light Mode)
        return prefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkMode(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }
}
