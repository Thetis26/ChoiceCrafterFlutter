package com.choicecrafter.students.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Helper for persisting the activities a user has liked so the UI can
 * reflect their previous reactions locally.
 */
public final class LikePreferences {

    private static final String PREF_NAME = "liked_activities";
    private static final String KEY_PREFIX = "likes_";
    private static final String DEFAULT_USER_ID = "anonymous";

    private LikePreferences() {
        // Utility class
    }

    public static boolean hasLikedActivity(@NonNull Context context,
                                           @Nullable String userId,
                                           @Nullable String activityId) {
        if (TextUtils.isEmpty(activityId)) {
            return false;
        }
        SharedPreferences preferences = getPreferences(context);
        Set<String> likedActivities = preferences.getStringSet(buildKey(userId), Collections.emptySet());
        return likedActivities != null && likedActivities.contains(activityId);
    }

    public static void markActivityLiked(@NonNull Context context,
                                         @Nullable String userId,
                                         @Nullable String activityId) {
        if (TextUtils.isEmpty(activityId)) {
            return;
        }
        SharedPreferences preferences = getPreferences(context);
        String key = buildKey(userId);
        Set<String> likedActivities = new HashSet<>(preferences.getStringSet(key, Collections.emptySet()));
        if (likedActivities.add(activityId)) {
            preferences.edit().putStringSet(key, likedActivities).apply();
        }
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static String buildKey(@Nullable String userId) {
        String normalizedUserId = TextUtils.isEmpty(userId)
                ? DEFAULT_USER_ID
                : userId.toLowerCase(Locale.US);
        return KEY_PREFIX + normalizedUserId;
    }
}
