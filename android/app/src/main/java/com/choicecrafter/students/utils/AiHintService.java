package com.choicecrafter.studentapp.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.choicecrafter.studentapp.BuildConfig;
import com.choicecrafter.studentapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/**
 * Utility responsible for generating AI-powered hints using the OpenAI API.
 */
public class AiHintService {

    private static final String CHAT_COMPLETIONS_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";

    private static AiHintService instance;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Context applicationContext;
    private final String apiKey;

    public interface HintCallback {
        void onSuccess(String hint);

        void onError(String errorMessage);
    }

    private AiHintService(Context context) {
        this.applicationContext = context.getApplicationContext();
        String configuredKey = BuildConfig.OPENAI_API_KEY;
        this.apiKey = configuredKey != null ? configuredKey.trim() : "";
    }

    public static synchronized AiHintService getInstance(Context context) {
        if (instance == null) {
            instance = new AiHintService(context);
        }
        return instance;
    }

    public void requestHint(String prompt, HintCallback callback) {
        if (TextUtils.isEmpty(apiKey)) {
            mainHandler.post(() -> callback.onError(applicationContext.getString(R.string.ai_hint_missing_key)));
            return;
        }

        executorService.execute(() -> performRequest(prompt,
                "You are a helpful tutor. Provide concise hints that guide the learner without revealing the final answer.",
                0.7,
                120,
                callback));
    }

    public void requestSolution(String prompt, HintCallback callback) {
        if (TextUtils.isEmpty(apiKey)) {
            mainHandler.post(() -> callback.onError(applicationContext.getString(R.string.ai_hint_missing_key)));
            return;
        }

        executorService.execute(() -> performRequest(prompt,
                "You are an expert developer. Provide a correct, efficient, and clean solution that can be pasted directly into a code editor.",
                0.35,
                320,
                callback));
    }

    private void performRequest(String prompt,
                                String systemInstruction,
                                double temperature,
                                int maxTokens,
                                HintCallback callback) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(CHAT_COMPLETIONS_ENDPOINT);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);

            JSONObject payload = buildPayload(prompt, systemInstruction, temperature, maxTokens);
            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                outputStream.write(body);
            }

            int responseCode = connection.getResponseCode();
            InputStream stream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = stream != null ? readStream(stream) : "";

            if (responseCode >= 200 && responseCode < 300) {
                String hint = parseHint(responseBody);
                if (!TextUtils.isEmpty(hint)) {
                    mainHandler.post(() -> callback.onSuccess(hint.trim()));
                } else {
                    postError(callback, applicationContext.getString(R.string.ai_hint_error));
                }
            } else {
                String message = extractErrorMessage(responseBody);
                postError(callback, message);
            }
        } catch (IOException | JSONException exception) {
            postError(callback, applicationContext.getString(R.string.ai_hint_error));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JSONObject buildPayload(String prompt,
                                    String systemInstruction,
                                    double temperature,
                                    int maxTokens) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("model", DEFAULT_MODEL);

        JSONArray messages = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemInstruction);
        messages.put(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(userMessage);

        payload.put("messages", messages);
        payload.put("temperature", temperature);
        payload.put("max_tokens", maxTokens);
        return payload;
    }

    private String parseHint(String responseBody) throws JSONException {
        JSONObject response = new JSONObject(responseBody);
        JSONArray choices = response.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return null;
        }
        JSONObject choice = choices.optJSONObject(0);
        if (choice == null) {
            return null;
        }
        JSONObject message = choice.optJSONObject("message");
        if (message == null) {
            return null;
        }
        return message.optString("content", null);
    }

    private String extractErrorMessage(String responseBody) {
        if (!TextUtils.isEmpty(responseBody)) {
            try {
                JSONObject response = new JSONObject(responseBody);
                JSONObject error = response.optJSONObject("error");
                if (error != null) {
                    String message = error.optString("message");
                    if (!TextUtils.isEmpty(message)) {
                        return message;
                    }
                }
            } catch (JSONException ignored) {
                // Ignore parsing error and fall through to generic message
            }
        }
        return applicationContext.getString(R.string.ai_hint_error);
    }

    private void postError(HintCallback callback, String errorMessage) {
        mainHandler.post(() -> callback.onError(errorMessage));
    }

    private String readStream(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}

