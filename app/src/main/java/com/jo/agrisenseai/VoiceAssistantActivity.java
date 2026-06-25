package com.jo.agrisenseai;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Full-screen Voice Assistant.
 * Flow: mic tap → SpeechManager (STT) → buildAIResponse() → TTSManager (TTS).
 *
 * All AI reasoning still comes from {@link AIEngine} via the cached Firebase
 * dashboard data, so this screen works 100% offline once Firebase has synced.
 *
 * Future upgrades:
 * - Replace buildAIResponse() body to call Gemini / GPT without touching the
 *   rest of the activity.
 * - TTSManager.speak() can be swapped for a neural TTS engine independently.
 */
public class VoiceAssistantActivity extends AppCompatActivity {

    private TextView voiceStatusText;
    private TextView questionText;
    private TextView responseText;
    private FloatingActionButton micButton;
    private MaterialButton btnAskAgain;
    private MaterialCardView questionCard;
    private MaterialCardView responseCard;

    private SpeechManager speechManager;
    private TTSManager ttsManager;

    private boolean isListening = false;

    // ---------------------------------------------------------------
    // Runtime mic permission launcher
    // ---------------------------------------------------------------
    private final ActivityResultLauncher<String> micPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startListening();
                } else {
                    setStatus(getString(R.string.voice_status_mic_denied), false);
                }
            });

    // ---------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_assistant);

        voiceStatusText = findViewById(R.id.voiceStatusText);
        questionText = findViewById(R.id.questionText);
        responseText = findViewById(R.id.responseText);
        micButton = findViewById(R.id.micButton);
        btnAskAgain = findViewById(R.id.btnAskAgain);
        questionCard = findViewById(R.id.questionCard);
        responseCard = findViewById(R.id.responseCard);

        speechManager = new SpeechManager(this);

        ttsManager = new TTSManager(this, () -> {
            // TTS ready — nothing extra needed at init time
        });

        micButton.setOnClickListener(v -> onMicClicked());
        btnAskAgain.setOnClickListener(v -> resetAndListen());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setStatus(getString(R.string.voice_status_idle), false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechManager.shutdown();
        ttsManager.shutdown();
    }

    // ---------------------------------------------------------------
    // Mic interaction
    // ---------------------------------------------------------------

    private void onMicClicked() {
        if (isListening) {
            speechManager.stop();
            ttsManager.stop();
            isListening = false;
            setStatus(getString(R.string.voice_status_idle), false);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startListening();
        }
    }

    private void startListening() {
        questionCard.setVisibility(View.GONE);
        responseCard.setVisibility(View.GONE);
        btnAskAgain.setVisibility(View.GONE);
        isListening = true;

        setStatus(getString(R.string.voice_status_listening), true);

        speechManager.startListening(new SpeechManager.SpeechCallback() {
            @Override
            public void onReadyForSpeech() {
                runOnUiThread(() -> setStatus(getString(R.string.voice_status_listening), true));
            }

            @Override
            public void onResult(String text) {
                isListening = false;
                runOnUiThread(() -> handleSpeechResult(text));
            }

            @Override
            public void onError(int errorCode) {
                isListening = false;
                runOnUiThread(() -> {
                    if (errorCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        setStatus(getString(R.string.voice_status_mic_denied), false);
                    } else {
                        setStatus(getString(R.string.voice_status_error), false);
                    }
                    btnAskAgain.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void resetAndListen() {
        btnAskAgain.setVisibility(View.GONE);
        questionCard.setVisibility(View.GONE);
        responseCard.setVisibility(View.GONE);
        ttsManager.stop();
        startListening();
    }

    // ---------------------------------------------------------------
    // AI response generation
    // ---------------------------------------------------------------

    private void handleSpeechResult(String spokenText) {
        // Show what the user said
        questionText.setText(spokenText);
        questionCard.setVisibility(View.VISIBLE);

        setStatus(getString(R.string.voice_status_thinking), false);

        // Build the AI response from live dashboard data
        String aiResponse = buildAIResponse(spokenText);

        // Show the response
        responseText.setText(aiResponse);
        responseCard.setVisibility(View.VISIBLE);
        btnAskAgain.setVisibility(View.VISIBLE);

        // Speak it if auto-speak is enabled
        if (VoicePreferenceManager.isAutoSpeak(this)) {
            setStatus(getString(R.string.voice_status_speaking), false);
            ttsManager.speak(aiResponse);
            ttsManager.setOnSpeechDoneListener(
                    () -> runOnUiThread(() -> setStatus(getString(R.string.voice_status_idle), false)));
        } else {
            setStatus(getString(R.string.voice_status_idle), false);
        }
    }

    /**
     * Maps the spoken question to an AI response string.
     * Currently returns a fixed response derived from the Firebase dashboard;
     * replace this method body with a Gemini / NLP call in a future phase.
     */
    private String buildAIResponse(String question) {
        // Keyword matching for common farming questions — simple and extensible
        String q = question.toLowerCase();
        boolean isWaterQuestion = q.contains("water") || q.contains("irrigat")
                || q.contains("నీళ్లు") || q.contains("నీరు")
                || q.contains("पानी") || q.contains("தண்ணீர்")
                || q.contains("ನೀರು") || q.contains("വെള്ളം")
                || q.contains("पाणी") || q.contains("জল");
        boolean isHealthQuestion = q.contains("health") || q.contains("healthy")
                || q.contains("ఆరోగ్య") || q.contains("स्वस्थ")
                || q.contains("ஆரோக்கி") || q.contains("ಆರೋಗ್ಯ")
                || q.contains("ആരോഗ്യ") || q.contains("निरोगी") || q.contains("সুস্থ");
        boolean isRiskQuestion = q.contains("risk") || q.contains("danger")
                || q.contains("ప్రమాద") || q.contains("खतरा");

        // Read the latest AI result from Firebase-cached dashboard data
        // (FirebaseHelper already keeps this in sync live)
        // For robustness, we compose the response from string resources so
        // multilingual support applies automatically.
        if (isWaterQuestion) {
            return getString(R.string.assistant_status_text) + ". " +
                    getString(R.string.ai_recommendation_text);
        } else if (isHealthQuestion) {
            return getString(R.string.farm_health_status) + " — " +
                    getString(R.string.farm_health_value) + ". " +
                    getString(R.string.assistant_status_subtext);
        } else if (isRiskQuestion) {
            return getString(R.string.risk_level_title) + ": " +
                    getString(R.string.risk_low) + ". " +
                    getString(R.string.ai_recommendation_text);
        } else {
            // Default greeting + recommendation
            return getString(R.string.voice_greeting) + " " +
                    getString(R.string.ai_recommendation_text);
        }
    }

    // ---------------------------------------------------------------
    // UI helpers
    // ---------------------------------------------------------------

    private void setStatus(String status, boolean listening) {
        voiceStatusText.setText(status);
        micButton.setBackgroundTintList(
                ContextCompat.getColorStateList(this, listening ? R.color.status_critical : R.color.primary_green));
    }
}
