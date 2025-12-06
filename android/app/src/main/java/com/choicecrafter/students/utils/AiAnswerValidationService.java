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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/**
 * Service that evaluates fill-in-the-blank answers with the help of the OpenAI API. The service
 * attempts to be flexible with acceptable answers by allowing semantically equivalent responses to
 * be considered correct even if the text is not an exact match.
 */
public class AiAnswerValidationService {

    private static final String CHAT_COMPLETIONS_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";

    private static AiAnswerValidationService instance;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Context applicationContext;
    private final String apiKey;

    public interface ValidationCallback {
        void onSuccess(ValidationResult result);

        void onError(String errorMessage);
    }

    public static class ValidationResult {
        private final boolean overallCorrect;
        private final List<Boolean> perBlankCorrectness;
        private final String feedback;

        public ValidationResult(boolean overallCorrect, List<Boolean> perBlankCorrectness, String feedback) {
            this.overallCorrect = overallCorrect;
            this.perBlankCorrectness = perBlankCorrectness;
            this.feedback = feedback;
        }

        public boolean isOverallCorrect() {
            return overallCorrect;
        }

        public List<Boolean> getPerBlankCorrectness() {
            return perBlankCorrectness;
        }

        public String getFeedback() {
            return feedback;
        }
    }

    private AiAnswerValidationService(Context context) {
        this.applicationContext = context.getApplicationContext();
        String configuredKey = BuildConfig.OPENAI_API_KEY;
        this.apiKey = configuredKey != null ? configuredKey.trim() : "";
    }

    public static synchronized AiAnswerValidationService getInstance(Context context) {
        if (instance == null) {
            instance = new AiAnswerValidationService(context);
        }
        return instance;
    }

    public void evaluateFillInTheBlank(String sentence, List<String> correctAnswers, List<String> userAnswers,
                                       ValidationCallback callback) {
        if (TextUtils.isEmpty(apiKey)) {
            mainHandler.post(() -> callback.onError(applicationContext.getString(R.string.ai_hint_missing_key)));
            return;
        }

        executorService.execute(() -> performRequest(sentence, correctAnswers, userAnswers, callback));
    }

    private void performRequest(String sentence, List<String> correctAnswers, List<String> userAnswers,
                                ValidationCallback callback) {
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

            JSONObject payload = buildPayload(sentence, correctAnswers, userAnswers);
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
                ValidationResult result = parseValidationResult(responseBody);
                if (result != null) {
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    postError(callback, applicationContext.getString(R.string.ai_answer_validation_error));
                }
            } else {
                String message = extractErrorMessage(responseBody);
                postError(callback, message);
            }
        } catch (IOException | JSONException exception) {
            postError(callback, applicationContext.getString(R.string.ai_answer_validation_error));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JSONObject buildPayload(String sentence, List<String> correctAnswers, List<String> userAnswers) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("model", DEFAULT_MODEL);

        JSONArray messages = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are an assistant that grades fill in the blank questions. " +
                "Mark an answer correct if it is semantically equivalent to the expected answer even if wording differs. " +
                "Return valid JSON only.");
        messages.put(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", buildPrompt(sentence, correctAnswers, userAnswers));
        messages.put(userMessage);

        payload.put("messages", messages);
        payload.put("temperature", 0.2);
        payload.put("max_tokens", 120);
        return payload;
    }

    private String buildPrompt(String sentence, List<String> correctAnswers, List<String> userAnswers) {
        StringBuilder builder = new StringBuilder();
        builder.append("Evaluate the following fill in the blank answers.\n");
        builder.append("Sentence: ").append(sentence).append("\n");
        builder.append("Correct answers (in order): ").append(new JSONArray(correctAnswers).toString()).append("\n");
        builder.append("Learner answers (in order): ").append(new JSONArray(userAnswers).toString()).append("\n");
        builder.append("Consider capitalization, punctuation, and simple stemming differences as correct. ");
        builder.append("Also allow close synonyms or paraphrases that preserve the meaning.\n");
        builder.append("Return ONLY a JSON object with the following structure: ");
        builder.append("{\"overallCorrect\": boolean, \"perBlank\": [boolean,...], \"feedback\": string}. ");
        builder.append("Do not include any additional commentary.");
        return builder.toString();
    }

    private ValidationResult parseValidationResult(String responseBody) throws JSONException {
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
        String content = message.optString("content", null);
        if (TextUtils.isEmpty(content)) {
            return null;
        }

        String jsonPayload = extractJson(content);
        if (TextUtils.isEmpty(jsonPayload)) {
            return null;
        }

        JSONObject resultJson = new JSONObject(jsonPayload);
        boolean overallCorrect = resultJson.optBoolean("overallCorrect", false);

        JSONArray perBlankArray = resultJson.optJSONArray("perBlank");
        List<Boolean> perBlank = new ArrayList<>();
        if (perBlankArray != null) {
            for (int i = 0; i < perBlankArray.length(); i++) {
                perBlank.add(perBlankArray.optBoolean(i, false));
            }
        }

        String feedback = resultJson.optString("feedback", "");
        return new ValidationResult(overallCorrect, perBlank, feedback);
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            return content.substring(start, end + 1);
        }
        return null;
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
        return applicationContext.getString(R.string.ai_answer_validation_error);
    }

    private void postError(ValidationCallback callback, String errorMessage) {
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

