package com.choicecrafter.studentapp.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/**
 * Lightweight client for executing code snippets using the public Piston API.
 */
public class CodeExecutionService {

    private static final String ENDPOINT = "https://emkc.org/api/v2/piston/execute";

    private static CodeExecutionService instance;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Context applicationContext;

    private final Map<String, LanguageConfig> languageConfigMap = new HashMap<>();

    public interface ExecutionCallback {
        void onSuccess(@NonNull String stdout, @NonNull String stderr);

        void onError(@NonNull String message);
    }

    private CodeExecutionService(@NonNull Context context) {
        this.applicationContext = context.getApplicationContext();
        languageConfigMap.put("python", new LanguageConfig("python", "3.10.0", "Main.py"));
        languageConfigMap.put("java", new LanguageConfig("java", "15.0.2", "Main.java"));
        languageConfigMap.put("cpp", new LanguageConfig("cpp", "10.2.0", "Main.cpp"));
    }

    public static synchronized CodeExecutionService getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new CodeExecutionService(context);
        }
        return instance;
    }

    public void execute(@NonNull String languageKey,
                        @NonNull String sourceCode,
                        @Nullable String stdin,
                        @NonNull ExecutionCallback callback) {
        LanguageConfig config = languageConfigMap.get(languageKey);
        if (config == null) {
            mainHandler.post(() -> callback.onError("Unsupported language: " + languageKey));
            return;
        }
        executorService.execute(() -> performRequest(config, sourceCode, stdin, callback));
    }

    private void performRequest(@NonNull LanguageConfig languageConfig,
                                @NonNull String sourceCode,
                                @Nullable String stdin,
                                @NonNull ExecutionCallback callback) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(ENDPOINT);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            JSONObject payload = buildPayload(languageConfig, sourceCode, stdin);
            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                outputStream.write(body);
            }

            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = responseStream != null ? readStream(responseStream) : "";
            if (responseCode >= 200 && responseCode < 300) {
                JSONObject responseJson = new JSONObject(responseBody);
                JSONObject runObject = responseJson.optJSONObject("run");
                String stdout = runObject != null ? runObject.optString("stdout", "") : "";
                String stderr = runObject != null ? runObject.optString("stderr", "") : "";
                if (TextUtils.isEmpty(stdout) && runObject != null) {
                    stdout = runObject.optString("output", "");
                }
                final String normalizedStdout = stdout != null ? stdout : "";
                final String normalizedStderr = stderr != null ? stderr : "";
                mainHandler.post(() -> callback.onSuccess(normalizedStdout, normalizedStderr));
            } else {
                String errorMessage = extractErrorMessage(responseBody);
                mainHandler.post(() -> callback.onError(errorMessage));
            }
        } catch (IOException | JSONException exception) {
            mainHandler.post(() -> callback.onError(applicationContext.getString(R.string.coding_challenge_run_error)));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JSONObject buildPayload(@NonNull LanguageConfig config,
                                     @NonNull String sourceCode,
                                     @Nullable String stdin) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("language", config.languageId);
        if (!TextUtils.isEmpty(config.version)) {
            payload.put("version", config.version);
        }
        JSONArray files = new JSONArray();
        JSONObject file = new JSONObject();
        file.put("name", config.fileName);
        file.put("content", sourceCode);
        files.put(file);
        payload.put("files", files);
        if (!TextUtils.isEmpty(stdin)) {
            payload.put("stdin", stdin);
        }
        return payload;
    }

    private String extractErrorMessage(@Nullable String responseBody) {
        if (!TextUtils.isEmpty(responseBody)) {
            try {
                JSONObject responseJson = new JSONObject(responseBody);
                JSONObject errorObject = responseJson.optJSONObject("error");
                if (errorObject != null) {
                    String message = errorObject.optString("message");
                    if (!TextUtils.isEmpty(message)) {
                        return message;
                    }
                }
            } catch (JSONException ignored) {
            }
        }
        return applicationContext.getString(R.string.coding_challenge_run_error);
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

    private static class LanguageConfig {
        private final String languageId;
        private final String version;
        private final String fileName;

        private LanguageConfig(String languageId, String version, String fileName) {
            this.languageId = languageId;
            this.version = version;
            this.fileName = fileName;
        }
    }
}
