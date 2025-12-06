package com.choicecrafter.studentapp.utils;

import androidx.annotation.Nullable;

import com.choicecrafter.studentapp.models.TaskStats;

/**
 * Detects low-effort responses by looking at the recorded time on task.
 * When a learner answers a question in under a second we treat it as rapid guessing
 * and remove the attempt from scoring.
 */
public final class RapidGuessingDetector {

    private static final int RAPID_THRESHOLD_SECONDS = 1;

    private RapidGuessingDetector() {
        // Utility class
    }

    public static boolean isRapidGuess(@Nullable TaskStats stats) {
        if (stats == null) {
            return false;
        }
        int seconds = resolveSeconds(stats.getTimeSpent());
        return seconds >= 0 && seconds <= RAPID_THRESHOLD_SECONDS;
    }

    public static int resolveSeconds(@Nullable String timeSpent) {
        if (timeSpent == null) {
            return -1;
        }

        String trimmed = timeSpent.trim();
        if (trimmed.isEmpty()) {
            return -1;
        }

        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":");
            if (parts.length == 2) {
                try {
                    int minutes = Integer.parseInt(parts[0]);
                    int seconds = Integer.parseInt(parts[1]);
                    return Math.max(0, minutes * 60 + seconds);
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }

        String numeric = trimmed.replaceAll("[^0-9]", "");
        if (numeric.isEmpty()) {
            return -1;
        }
        try {
            return Math.max(0, Integer.parseInt(numeric));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
