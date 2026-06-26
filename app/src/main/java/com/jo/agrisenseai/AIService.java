package com.jo.agrisenseai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AIService — Calls the Gemini REST API (v1beta) using OkHttp.
 *
 * Endpoint : POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
 * Auth     : API key passed as a query parameter (?key=...)
 *
 * Keeps the last 10 turns of conversation history for ChatGPT-like multi-turn support.
 * The final user message is enriched with live farm telemetry via {@link PromptBuilder}.
 */
public class AIService {

    private static final String TAG = "AIService";

    // Official Gemini v1beta endpoint with gemini-2.0-flash model
    private static final String GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private static final int MAX_HISTORY_TURNS = 10;

    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    public AIService() {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Sends the conversation history (with the latest user message enriched by telemetry)
     * to the Gemini REST API and returns the AI response via {@link AIServiceCallback}.
     *
     * @param history   Full conversation history (ChatMessage list)
     * @param telemetry Live farm sensor + weather telemetry
     * @param callback  Success/failure callback on the main thread
     */
    public void sendMessage(List<ChatMessage> history,
                            FarmTelemetry telemetry,
                            @NonNull AIServiceCallback callback) {

        // --- 1. Validate API key ---
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null) apiKey = "";
        apiKey = apiKey.trim().replace("\"", "").replace("'", "");

        // Log first 6 chars for debugging (mask the rest)
        if (!apiKey.isEmpty()) {
            String masked = apiKey.length() > 6
                    ? apiKey.substring(0, 6) + "..." + (apiKey.length() - 6) + " chars"
                    : apiKey + " (short)";
            Log.d(TAG, "GEMINI_API_KEY loaded: " + masked);
        } else {
            Log.e(TAG, "GEMINI_API_KEY is empty!");
        }

        if (apiKey.isEmpty()) {
            deliverError(callback, "Gemini API key is not configured. Please add a valid GEMINI_API_KEY to local.properties.");
            return;
        }

        // --- 2. Filter history: only USER and AI messages, no loading placeholders ---
        List<ChatMessage> filtered = new ArrayList<>();
        if (history != null) {
            for (ChatMessage msg : history) {
                if ((msg.getType() == ChatMessage.TYPE_USER || msg.getType() == ChatMessage.TYPE_AI)
                        && !msg.isLoading()) {
                    filtered.add(msg);
                }
            }
        }

        // Limit to last MAX_HISTORY_TURNS messages
        int start = Math.max(0, filtered.size() - MAX_HISTORY_TURNS);
        List<ChatMessage> recent = filtered.subList(start, filtered.size());

        // --- 3. Build the JSON request body ---
        String requestJson;
        try {
            requestJson = buildRequestJson(recent, telemetry);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build request JSON: " + e.getMessage(), e);
            deliverError(callback, "Failed to build request: " + e.getMessage());
            return;
        }

        Log.d(TAG, "Sending request with " + recent.size() + " message(s)");

        // --- 4. Build and execute the HTTP request ---
        String url = GEMINI_BASE_URL + "?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")
                .build();

        final String finalApiKey = apiKey;
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network request failed: " + e.getMessage(), e);
                deliverError(callback, "Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "HTTP " + response.code() + " response received");

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Error response body: " + responseBody);
                    String errorMsg = extractErrorMessage(responseBody, response.code());
                    deliverError(callback, errorMsg);
                    return;
                }

                // Parse the successful response
                try {
                    String aiText = parseResponseText(responseBody);
                    if (aiText != null && !aiText.trim().isEmpty()) {
                        Log.d(TAG, "Gemini response: " + aiText.substring(0, Math.min(100, aiText.length())) + "...");
                        mainHandler.post(() -> callback.onSuccess(aiText));
                    } else {
                        Log.w(TAG, "Empty text in response: " + responseBody);
                        deliverError(callback, "Empty response received from the AI.");
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse response JSON: " + e.getMessage(), e);
                    deliverError(callback, "Unexpected response format from AI.");
                }
            }
        });
    }

    /**
     * Generates a short, descriptive 3-4 word title for a chat conversation
     * based on the first user message, using the Gemini API.
     */
    public void generateTitle(String userMessage, @NonNull AIServiceCallback callback) {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null) apiKey = "";
        apiKey = apiKey.trim().replace("\"", "").replace("'", "");

        if (apiKey.isEmpty()) {
            deliverError(callback, "API key is not configured.");
            return;
        }

        String prompt = "Suggest a short, descriptive chat title (maximum 3-4 words, do not include quotes, punctuation, markdown, or words like 'Title:') summarizing this user message: \"" + userMessage + "\"";

        String requestJson;
        try {
            JSONArray partsArray = new JSONArray();
            partsArray.put(new JSONObject().put("text", prompt));

            JSONObject contentObj = new JSONObject();
            contentObj.put("role", "user");
            contentObj.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(contentObj);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.5);
            generationConfig.put("maxOutputTokens", 20);

            JSONObject root = new JSONObject();
            root.put("contents", contentsArray);
            root.put("generationConfig", generationConfig);

            requestJson = root.toString();
        } catch (JSONException e) {
            deliverError(callback, "JSON building error: " + e.getMessage());
            return;
        }

        String url = GEMINI_BASE_URL + "?key=" + apiKey;

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                deliverError(callback, e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    deliverError(callback, "HTTP error: " + response.code());
                    return;
                }
                try {
                    String title = parseResponseText(responseBody);
                    if (title != null && !title.trim().isEmpty()) {
                        String cleanTitle = title.trim().replace("\"", "").replace("'", "");
                        mainHandler.post(() -> callback.onSuccess(cleanTitle));
                    } else {
                        deliverError(callback, "Empty response");
                    }
                } catch (JSONException e) {
                    deliverError(callback, e.getMessage());
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the Gemini generateContent JSON payload.
     *
     * Format:
     * {
     *   "contents": [
     *     { "role": "user", "parts": [{ "text": "..." }] },
     *     { "role": "model", "parts": [{ "text": "..." }] },
     *     ...
     *   ]
     * }
     */
    private String buildRequestJson(List<ChatMessage> recent, FarmTelemetry telemetry) throws JSONException {
        JSONArray contents = new JSONArray();

        for (int i = 0; i < recent.size(); i++) {
            ChatMessage msg = recent.get(i);
            String role = msg.getType() == ChatMessage.TYPE_USER ? "user" : "model";

            String text;
            // Enrich the last user message with farm telemetry context
            if (msg.getType() == ChatMessage.TYPE_USER && i == recent.size() - 1) {
                text = PromptBuilder.buildPrompt(msg.getText(), telemetry);
            } else {
                text = msg.getText();
            }

            JSONObject part = new JSONObject();
            part.put("text", text);

            JSONArray parts = new JSONArray();
            parts.put(part);

            JSONObject content = new JSONObject();
            content.put("role", role);
            content.put("parts", parts);

            contents.put(content);
        }

        JSONObject root = new JSONObject();
        root.put("contents", contents);

        // Optional: tune generation parameters
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 1024);
        root.put("generationConfig", generationConfig);

        return root.toString();
    }

    /**
     * Parses the text from a successful Gemini generateContent response.
     *
     * Expected structure:
     * { "candidates": [ { "content": { "parts": [ { "text": "..." } ] } } ] }
     */
    private String parseResponseText(String responseBody) throws JSONException {
        JSONObject json = new JSONObject(responseBody);
        JSONArray candidates = json.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            return null;
        }
        JSONObject candidate = candidates.getJSONObject(0);
        JSONObject content = candidate.optJSONObject("content");
        if (content == null) return null;
        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.length() == 0) return null;
        return parts.getJSONObject(0).optString("text", null);
    }

    /**
     * Extracts a human-readable error message from a Gemini error response body.
     */
    private String extractErrorMessage(String responseBody, int httpCode) {
        try {
            JSONObject json = new JSONObject(responseBody);
            JSONObject error = json.optJSONObject("error");
            if (error != null) {
                String msg = error.optString("message", "");
                if (!msg.isEmpty()) {
                    return "Gemini error (" + httpCode + "): " + msg;
                }
            }
        } catch (JSONException ignored) {
            // Fall through to generic error
        }
        return "Gemini request failed with HTTP " + httpCode + ". Check your API key and model availability.";
    }

    /** Posts an error callback on the main thread. */
    private void deliverError(@NonNull AIServiceCallback callback, String message) {
        Log.e(TAG, "Delivering error: " + message);
        mainHandler.post(() -> callback.onFailure(message));
    }
}
