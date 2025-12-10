package com.choicecrafter.students.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.Locale;

public final class BadgePreferences {

    private static final String PREF_NAME = "badge_progress";
    private static final String SUFFIX_COMMENT = "_commented";
    private static final String SUFFIX_CHATBOT = "_chatbot";

    private BadgePreferences() {
    }

    public static void markCommentPosted(Context context, String email) {
        setFlag(context, email, SUFFIX_COMMENT, true);
    }

    public static boolean hasCommented(Context context, String email) {
        return getFlag(context, email, SUFFIX_COMMENT);
    }

    public static void markChatbotDiscussion(Context context, String email) {
        setFlag(context, email, SUFFIX_CHATBOT, true);
    }

    public static boolean hasChatbotDiscussion(Context context, String email) {
        return getFlag(context, email, SUFFIX_CHATBOT);
    }

    private static void setFlag(Context context, String email, String suffix, boolean value) {
        if (context == null) {
            return;
        }
        String key = buildKey(email, suffix);
        if (key == null) {
            return;
        }
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(key, value).apply();
    }

    private static boolean getFlag(Context context, String email, String suffix) {
        if (context == null) {
            return false;
        }
        String key = buildKey(email, suffix);
        if (key == null) {
            return false;
        }
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    private static String buildKey(String email, String suffix) {
        if (TextUtils.isEmpty(email)) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized + suffix;
    }
}
