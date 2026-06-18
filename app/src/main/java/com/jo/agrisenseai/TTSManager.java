package com.jo.agrisenseai;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.HashMap;
import java.util.Locale;

/**
 * Thin wrapper around Android {@link TextToSpeech}.
 * <p>
 * Resolves the correct {@link Locale} from the user's saved language (Phase 6)
 * and speaks AI responses aloud. Falls back to English when the device TTS
 * engine does not support the chosen locale.
 * <p>
 * Future voice upgrades (e.g. neural TTS, Gemini audio) can replace the speak()
 * implementation without touching any other class.
 */
public class TTSManager {

    /**
     * Maps LanguageManager locale codes to Android Locale objects.
     * Kannada (kn) and Bengali (bn) are not widely supported by device TTS —
     * they fall back to English automatically via the LANG_NOT_SUPPORTED check.
     */
    private static final HashMap<String, Locale> LOCALE_MAP = new HashMap<>();

    static {
        LOCALE_MAP.put("en", Locale.ENGLISH);
        LOCALE_MAP.put("te", new Locale("te", "IN"));
        LOCALE_MAP.put("hi", new Locale("hi", "IN"));
        LOCALE_MAP.put("ta", new Locale("ta", "IN"));
        LOCALE_MAP.put("kn", new Locale("kn", "IN"));
        LOCALE_MAP.put("ml", new Locale("ml", "IN"));
        LOCALE_MAP.put("mr", new Locale("mr", "IN"));
        LOCALE_MAP.put("bn", new Locale("bn", "IN"));
    }

    private TextToSpeech tts;
    private boolean ready = false;
    private OnSpeechDoneListener doneListener;

    public interface OnSpeechDoneListener {
        void onDone();
    }

    public TTSManager(Context context, Runnable onInitialized) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                applyLocale(LanguageManager.getSavedLocaleCode(context));
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) {}
                    @Override
                    public void onDone(String utteranceId) {
                        if (doneListener != null) doneListener.onDone();
                    }
                    @Override public void onError(String utteranceId) {
                        if (doneListener != null) doneListener.onDone();
                    }
                });
                ready = true;
                if (onInitialized != null) onInitialized.run();
            }
        });
    }

    /** Applies (or re-applies) the locale; call again after a language change. */
    public void applyLocale(String localeCode) {
        if (!ready) return;
        Locale locale = LOCALE_MAP.getOrDefault(localeCode, Locale.ENGLISH);
        int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
            tts.setLanguage(Locale.ENGLISH);
        }
    }

    /**
     * Speaks {@code text} aloud. Safe to call before TTS is ready — it will be
     * silently ignored. Uses QUEUE_FLUSH so any ongoing speech is interrupted.
     */
    public void speak(String text) {
        if (!ready || tts == null) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "agrisense_utterance");
    }

    public void stop() {
        if (tts != null) tts.stop();
    }

    public boolean isReady() {
        return ready;
    }

    public void setOnSpeechDoneListener(OnSpeechDoneListener listener) {
        this.doneListener = listener;
    }

    /** Call from Activity.onDestroy() to release engine resources. */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            ready = false;
        }
    }
}
