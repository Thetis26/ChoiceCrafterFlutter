package com.choicecrafter.students.admin;

import androidx.annotation.NonNull;

import com.choicecrafter.students.models.NudgePreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Objects;

/**
 * Service that provides administrative operations for {@link NudgePreferences} documents.
 */
public class NudgePreferencesAdminService {

    private final NudgePreferencesStore store;

    public NudgePreferencesAdminService(@NonNull NudgePreferencesStore store) {
        this.store = Objects.requireNonNull(store, "store == null");
    }

    /**
     * Enables or disables all nudge preferences for the provided user.
     */
    public void setAllPreferencesForUser(@NonNull String userEmail, boolean enabled) {
        String normalizedEmail = normalizeEmail(userEmail);
        NudgePreferences preferences = createPreferencesWithState(normalizedEmail, enabled);
        store.save(preferences);
    }

    /**
     * Enables or disables all nudge preferences for a list of users.
     */
    public void setAllPreferencesForUsers(@NonNull Collection<String> userEmails, boolean enabled) {
        Objects.requireNonNull(userEmails, "userEmails == null");
        for (String email : userEmails) {
            if (email == null) {
                continue;
            }
            String trimmed = email.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            setAllPreferencesForUser(trimmed, enabled);
        }
    }

    /**
     * Applies the updates defined in a JSON payload. The payload must contain either a top-level array
     * of user objects or an object with a {@code users} array. Each user entry must contain an
     * {@code email} field and may include an {@code enabled} boolean field to toggle all preferences.
     * Optionally, a {@code preferences} object can specify fine-grained overrides for individual
     * preference flags.
     */
    public void applyChangesFromJson(@NonNull Reader reader) throws IOException {
        Objects.requireNonNull(reader, "reader == null");
        String rawJson = readAll(reader).trim();
        if (rawJson.isEmpty()) {
            return;
        }
        Object parsed;
        try {
            parsed = new JSONTokener(rawJson).nextValue();
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid JSON payload", e);
        }
        JSONArray usersArray;
        if (parsed instanceof JSONArray) {
            usersArray = (JSONArray) parsed;
        } else if (parsed instanceof JSONObject) {
            JSONObject root = (JSONObject) parsed;
            if (!root.has("users")) {
                throw new IllegalArgumentException("JSON payload must contain a 'users' array");
            }
            try {
                usersArray = root.getJSONArray("users");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported JSON payload structure");
        }

        for (int i = 0; i < usersArray.length(); i++) {
            try {
                JSONObject userObject = usersArray.getJSONObject(i);
                String email = normalizeEmail(userObject.getString("email"));
                boolean enabled = userObject.optBoolean("enabled", true);
                NudgePreferences preferences = createPreferencesWithState(email, enabled);
                if (userObject.has("preferences")) {
                    JSONObject overrides = userObject.getJSONObject("preferences");
                    applyOverrides(preferences, overrides);
                }
                store.save(preferences);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static NudgePreferences createPreferencesWithState(String userEmail, boolean enabled) {
        NudgePreferences preferences = NudgePreferences.createDefault(userEmail);
        Boolean value = enabled;
        preferences.setPersonalStatisticsPromptEnabled(value);
        preferences.setCompletedActivityPromptEnabled(value);
        preferences.setColleaguesPromptEnabled(value);
        preferences.setActivityStartedNotificationsEnabled(value);
        preferences.setReminderNotificationsEnabled(value);
        preferences.setColleaguesActivityPageEnabled(value);
        preferences.setDiscussionForumEnabled(value);
        return preferences;
    }

    private static void applyOverrides(NudgePreferences preferences, JSONObject overrides) {
        setIfPresent(overrides, "personalStatisticsPromptEnabled",
                preferences::setPersonalStatisticsPromptEnabled);
        setIfPresent(overrides, "completedActivityPromptEnabled",
                preferences::setCompletedActivityPromptEnabled);
        setIfPresent(overrides, "colleaguesPromptEnabled",
                preferences::setColleaguesPromptEnabled);
        setIfPresent(overrides, "activityStartedNotificationsEnabled",
                preferences::setActivityStartedNotificationsEnabled);
        setIfPresent(overrides, "reminderNotificationsEnabled",
                preferences::setReminderNotificationsEnabled);
        setIfPresent(overrides, "colleaguesActivityPageEnabled",
                preferences::setColleaguesActivityPageEnabled);
        setIfPresent(overrides, "discussionForumEnabled",
                preferences::setDiscussionForumEnabled);
    }

    private static void setIfPresent(JSONObject overrides, String key,
                                     java.util.function.Consumer<Boolean> setter) {
        if (!overrides.has(key) || overrides.isNull(key)) {
            return;
        }
        Object value = null;
        try {
            value = overrides.get(key);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException("Preference '" + key + "' must be a boolean");
        }
        setter.accept((Boolean) value);
    }

    private static String normalizeEmail(String userEmail) {
        String trimmed = Objects.requireNonNull(userEmail, "userEmail == null").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("userEmail must not be blank");
        }
        return trimmed;
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[1024];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, read);
        }
        return builder.toString();
    }
}
