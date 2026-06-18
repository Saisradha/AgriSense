package com.jo.agrisenseai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Thin wrapper around Android {@link SpeechRecognizer}.
 * <p>
 * Resolves the correct BCP-47 language tag from the user's saved locale and
 * starts recognition. The host Activity supplies a {@link SpeechCallback} to
 * receive the transcribed text or an error code.
 * <p>
 * Future upgrades (cloud STT, Whisper, etc.) only need to replace startListening().
 * No other class needs to change.
 */
public class SpeechManager {

    /** Maps LanguageManager locale codes to BCP-47 tags accepted by SpeechRecognizer. */
    private static final HashMap<String, String> STT_LOCALE_MAP = new HashMap<>();

    static {
        STT_LOCALE_MAP.put("en", "en-IN");
        STT_LOCALE_MAP.put("te", "te-IN");
        STT_LOCALE_MAP.put("hi", "hi-IN");
        STT_LOCALE_MAP.put("ta", "ta-IN");
        STT_LOCALE_MAP.put("kn", "kn-IN");
        STT_LOCALE_MAP.put("ml", "ml-IN");
        STT_LOCALE_MAP.put("mr", "mr-IN");
        STT_LOCALE_MAP.put("bn", "bn-IN");
    }

    public interface SpeechCallback {
        void onResult(String text);
        void onError(int errorCode);
        void onReadyForSpeech();
    }

    private SpeechRecognizer recognizer;
    private final Context context;

    public SpeechManager(Context context) {
        this.context = context;
    }

    /**
     * Starts listening in the user's saved language.
     * Falls back to en-IN if the locale code is unknown.
     */
    public void startListening(SpeechCallback callback) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (callback != null) callback.onError(SpeechRecognizer.ERROR_CLIENT);
            return;
        }

        stop(); // stop any ongoing session

        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                if (callback != null) callback.onReadyForSpeech();
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    if (callback != null) callback.onResult(matches.get(0));
                } else {
                    if (callback != null) callback.onError(SpeechRecognizer.ERROR_NO_MATCH);
                }
            }

            @Override
            public void onError(int error) {
                if (callback != null) callback.onError(error);
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        String localeCode = LanguageManager.getSavedLocaleCode(context);
        String languageTag = STT_LOCALE_MAP.getOrDefault(localeCode, "en-IN");

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        recognizer.startListening(intent);
    }

    public void stop() {
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer.destroy();
            recognizer = null;
        }
    }

    /** Call from Activity.onDestroy() to release recognizer resources. */
    public void shutdown() {
        stop();
    }
}
