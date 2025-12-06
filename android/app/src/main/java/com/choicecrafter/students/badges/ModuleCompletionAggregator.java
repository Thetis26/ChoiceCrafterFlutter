package com.choicecrafter.studentapp.badges;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility that aggregates completed modules from enrollment progress snapshots.
 */
public class ModuleCompletionAggregator {

    private final Set<String> completedModuleIds = new HashSet<>();
    private int numericCompletedModulesCount;

    public void reset() {
        completedModuleIds.clear();
        numericCompletedModulesCount = 0;
    }

    public void collectFromProgressSummary(@Nullable Map<?, ?> progressSummary) {
        if (progressSummary == null) {
            return;
        }
        addModulesFromSource(progressSummary.get("moduleSnapshots"));
        addModulesFromSource(progressSummary.get("modules"));
        addModulesFromMap(progressSummary.get("moduleProgress"));

        Object completedModulesObj = progressSummary.get("completedModules");
        if (completedModulesObj instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof Map<?, ?> map) {
                    registerCompletedModule(toStringObjectMap(map));
                } else if (item != null) {
                    completedModuleIds.add(String.valueOf(item));
                }
            }
        } else if (completedModulesObj instanceof Number number) {
            numericCompletedModulesCount = Math.max(numericCompletedModulesCount, number.intValue());
        }

        Object modulesCompletedCountObj = progressSummary.get("modulesCompletedCount");
        if (modulesCompletedCountObj instanceof Number number) {
            numericCompletedModulesCount = Math.max(numericCompletedModulesCount, number.intValue());
        }
    }

    private void addModulesFromSource(@Nullable Object source) {
        if (source instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof Map<?, ?> map) {
                    registerCompletedModule(toStringObjectMap(map));
                } else if (item != null) {
                    completedModuleIds.add(String.valueOf(item));
                }
            }
        } else if (source instanceof Map<?, ?> map) {
            addModulesFromMap(map);
        }
    }

    private void addModulesFromMap(@Nullable Object source) {
        if (!(source instanceof Map<?, ?> map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                registerCompletedModule(toStringObjectMap(nested));
            } else if (value instanceof Boolean booleanValue) {
                if (booleanValue) {
                    completedModuleIds.add(String.valueOf(entry.getKey()));
                }
            } else if (value instanceof Number number) {
                if (number.intValue() >= 100) {
                    completedModuleIds.add(String.valueOf(entry.getKey()));
                }
            }
        }
    }

    private void registerCompletedModule(@Nullable Map<String, Object> moduleData) {
        if (moduleData == null || moduleData.isEmpty()) {
            return;
        }
        boolean completed = false;
        Object completedObj = moduleData.get("completed");
        if (completedObj instanceof Boolean booleanValue) {
            completed = booleanValue;
        }
        Object completionObj = moduleData.get("completedPercentage");
        if (!completed && completionObj instanceof Number number) {
            completed = number.intValue() >= 100;
        }
        Object statusObj = moduleData.get("status");
        if (!completed && statusObj != null) {
            String status = String.valueOf(statusObj);
            if ("COMPLETED".equalsIgnoreCase(status)
                    || "COMPLETE".equalsIgnoreCase(status)
                    || "FINISHED".equalsIgnoreCase(status)) {
                completed = true;
            }
        }
        Object progressObj = moduleData.get("progress");
        if (!completed && progressObj instanceof Number number) {
            completed = number.intValue() >= 100;
        }
        if (!completed) {
            return;
        }
        Object moduleId = moduleData.get("moduleId");
        if (moduleId == null) {
            moduleId = moduleData.get("id");
        }
        if (moduleId == null) {
            moduleId = moduleData.get("title");
        }
        if (moduleId != null) {
            completedModuleIds.add(String.valueOf(moduleId));
        } else {
            numericCompletedModulesCount++;
        }
    }

    private Map<String, Object> toStringObjectMap(@Nullable Object source) {
        if (!(source instanceof Map<?, ?> map)) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    public int getCompletedModulesCount() {
        return Math.max(completedModuleIds.size(), numericCompletedModulesCount);
    }

    public Set<String> getCompletedModuleIds() {
        return Collections.unmodifiableSet(completedModuleIds);
    }
}
