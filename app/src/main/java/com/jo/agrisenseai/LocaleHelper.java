package com.jo.agrisenseai;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

/**
 * Applies a saved locale to any Context so Android resolves the correct
 * values-xx/strings.xml for all UI text.
 *
 * Usage:
 *   - In every Activity's attachBaseContext, wrap with LocaleHelper.applyLocale().
 *   - When the language changes, call LanguageManager.saveLanguage() then
 *     recreate() the host Activity.
 *
 * Compatible with future Voice Assistant (pass the Locale to TTS engine),
 * hardware LCD (send the locale code over serial), and AI messages
 * (translate AI output to the stored locale).
 */
public final class LocaleHelper {

    private LocaleHelper() {
    }

    /**
     * Returns a new Context configured for the locale stored in SharedPreferences.
     * Call this from Activity.attachBaseContext().
     */
    public static Context applyLocale(Context context) {
        String localeCode = LanguageManager.getSavedLocaleCode(context);
        return applyLocaleCode(context, localeCode);
    }

    /**
     * Returns a new Context configured for the given BCP-47 locale code.
     */
    public static Context applyLocaleCode(Context context, String localeCode) {
        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
}
