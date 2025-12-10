package com.choicecrafter.students.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.choicecrafter.students.models.TaskStats;
import com.choicecrafter.students.models.tasks.Task;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utility helpers for working with task statistics keys.
 */
public final class TaskStatsKeyUtils {

    private static final String KEY_PREFIX = "task";

    private TaskStatsKeyUtils() {
        // Utility class
    }

    /**
     * Builds a stable key for storing statistics about a task. If the task provides an explicit
     * identifier it will be used directly, otherwise the key falls back to a fingerprint derived
     * from the task contents.
     */
    @NonNull
    public static String buildKey(@Nullable Task task) {
        if (task == null) {
            return KEY_PREFIX;
        }
        if (!TextUtils.isEmpty(task.getId())) {
            return task.getId();
        }
        String fingerprintSource = safe(task.getTitle()) + "|" + safe(task.getDescription()) +
                "|" + safe(task.getType()) + "|" + safe(task.getStatus());
        if (fingerprintSource.isEmpty()) {
            fingerprintSource = KEY_PREFIX;
        }
        String fingerprint = UUID.nameUUIDFromBytes(fingerprintSource.getBytes(StandardCharsets.UTF_8)).toString();
        String prefix = safe(task.getTitle());
        if (prefix.isEmpty()) {
            prefix = KEY_PREFIX;
        }
        return prefix + "::" + fingerprint;
    }

    /**
     * Retrieves the statistics for a task from the provided map, looking up using the new key and
     * falling back to legacy keys that were previously used.
     */
    @Nullable
    public static TaskStats findStatsForTask(@Nullable Map<String, TaskStats> statsMap,
                                             @Nullable Task task) {
        if (statsMap == null || statsMap.isEmpty()) {
            return null;
        }
        String key = buildKey(task);
        TaskStats stats = statsMap.get(key);
        if (stats != null) {
            return stats;
        }
        for (String legacyKey : legacyKeys(task)) {
            stats = statsMap.get(legacyKey);
            if (stats != null) {
                return stats;
            }
        }
        return null;
    }

    /**
     * Stores task statistics inside the provided map using the stable key.
     */
    public static void putStats(@Nullable Map<String, TaskStats> statsMap,
                                @Nullable Task task,
                                @Nullable TaskStats stats) {
        if (statsMap == null || task == null || stats == null) {
            return;
        }
        statsMap.put(buildKey(task), stats);
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value != null ? value.trim() : "";
    }

    @NonNull
    private static List<String> legacyKeys(@Nullable Task task) {
        List<String> keys = new ArrayList<>();
        if (task != null && !TextUtils.isEmpty(task.getTitle())) {
            keys.add(task.getTitle());
        }
        return keys;
    }
}
