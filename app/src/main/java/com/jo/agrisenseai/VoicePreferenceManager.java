package com.jo.agrisenseai;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists Voice Assistant and Auto Speak toggle states in SharedPreferences.
 * <p>
 * Used by:
 * - {@link ProfileFragment} (toggle UI)
 * - {@link VoiceAssistantActivity} (checks auto-speak before calling TTS)
 * - Future: notifications can check isVoiceEnabled() before triggering audio alerts.
 */
public final class VoicePreferenceManager {

    private static final String PREF_FILE = "agrisense_prefs";
    private static final String KEY_VOICE_ENABLED = "voice_enabled";
    private static final String KEY_AUTO_SPEAK = "auto_speak";

    private VoicePreferenceManager() {
    }

    public static boolean isVoiceEnabled(Context context) {
        return prefs(context).getBoolean(KEY_VOICE_ENABLED, true);
    }

    public static void setVoiceEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply();
    }

    public static boolean isAutoSpeak(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_SPEAK, true);
    }

    public static void setAutoSpeak(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_SPEAK, enabled).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }
}
