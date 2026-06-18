package com.jo.agrisenseai;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Single source of truth for the user's language selection.
 *
 * Stores the BCP-47 locale code in SharedPreferences so the app remembers
 * the choice across restarts.
 *
 * Designed for extensibility:
 * - Future Voice Assistant: pass getLocaleForTTS() to Android TextToSpeech.
 * - Future AI messages: pass getSavedLocaleCode() to the AI API for translation.
 * - Future notifications: pass locale code when building notification text.
 * - Future hardware LCD: send locale code over UART/Bluetooth to the ESP32.
 */
public final class LanguageManager {

    private static final String PREF_FILE = "agrisense_prefs";
    private static final String KEY_LOCALE = "selected_locale";
    private static final String DEFAULT_LOCALE = "en";

    // ---------------------------------------------------------------
    // Language model: code → display name (always shown in native script)
    // ---------------------------------------------------------------

    public static final String[] LOCALE_CODES = {
            "en", "te", "hi", "ta", "kn", "ml", "mr", "bn"
    };

    public static final String[] DISPLAY_NAMES = {
            "English", "తెలుగు", "हिन्दी", "தமிழ்", "ಕನ್ನಡ", "മലയാളം", "मराठी", "বাংলা"
    };

    private LanguageManager() {
    }

    // ---------------------------------------------------------------
    // Read / Write
    // ---------------------------------------------------------------

    public static void saveLanguage(Context context, String localeCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LOCALE, localeCode).apply();
    }

    public static String getSavedLocaleCode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LOCALE, DEFAULT_LOCALE);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Returns the index of the currently saved locale in LOCALE_CODES,
     * or 0 (English) if not found.
     */
    public static int getSavedLocaleIndex(Context context) {
        String saved = getSavedLocaleCode(context);
        for (int i = 0; i < LOCALE_CODES.length; i++) {
            if (LOCALE_CODES[i].equals(saved)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Returns the display name of the currently saved language.
     */
    public static String getSavedDisplayName(Context context) {
        return DISPLAY_NAMES[getSavedLocaleIndex(context)];
    }
}
