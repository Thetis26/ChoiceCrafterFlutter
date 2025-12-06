package com.choicecrafter.studentapp.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the progress of a single activity stored within a course enrollment document.
 */
public class EnrollmentActivityProgress {
    private String courseId;
    private String activityId;
    private String userId;
    private Map<String, TaskStats> taskStats;
    private Integer highestScore;

    public EnrollmentActivityProgress() {
        this.taskStats = new HashMap<>();
    }

    public EnrollmentActivityProgress(String courseId,
                                      String activityId,
                                      String userId,
                                      Map<String, TaskStats> taskStats) {
        this.courseId = courseId;
        this.activityId = activityId;
        this.userId = userId;
        setTaskStats(taskStats);
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, TaskStats> getTaskStats() {
        if (taskStats == null) {
            taskStats = new HashMap<>();
        }
        return taskStats;
    }

    public void setTaskStats(Map<String, TaskStats> taskStats) {
        this.taskStats = taskStats != null ? taskStats : new HashMap<>();
    }

    public Integer getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(Integer highestScore) {
        this.highestScore = highestScore;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("courseId", courseId);
        map.put("activityId", activityId);
        map.put("userId", userId);
        map.put("taskStats", taskStats != null ? new HashMap<>(taskStats) : new HashMap<>());
        if (highestScore != null) {
            map.put("highestScore", highestScore);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static EnrollmentActivityProgress fromMap(Map<String, Object> map) {
        EnrollmentActivityProgress progress = new EnrollmentActivityProgress();
        if (map == null) {
            return progress;
        }
        progress.setCourseId(String.valueOf(map.getOrDefault("courseId", "")));
        progress.setActivityId(String.valueOf(map.getOrDefault("activityId", "")));
        Object userIdObj = map.get("userId");
        if (userIdObj != null) {
            progress.setUserId(String.valueOf(userIdObj));
        }
        Object taskStatsObj = map.get("taskStats");
        if (taskStatsObj instanceof Map<?, ?> statsMap) {
            Map<String, TaskStats> converted = new HashMap<>();
            for (Map.Entry<?, ?> entry : statsMap.entrySet()) {
                if (entry.getValue() instanceof TaskStats taskStats) {
                    converted.put(String.valueOf(entry.getKey()), taskStats);
                } else if (entry.getValue() instanceof Map<?, ?> nestedMap) {
                    TaskStats taskStats = new TaskStats();
                    Object attemptDateTime = nestedMap.get("attemptDateTime");
                    if (attemptDateTime != null) {
                        taskStats.setAttemptDateTime(String.valueOf(attemptDateTime));
                    }
                    Object timeSpent = nestedMap.get("timeSpent");
                    if (timeSpent != null) {
                        taskStats.setTimeSpent(String.valueOf(timeSpent));
                    }
                    Object retries = nestedMap.get("retries");
                    if (retries instanceof Number) {
                        taskStats.setRetries(((Number) retries).intValue());
                    }
                    Object success = nestedMap.get("success");
                    if (success instanceof Boolean) {
                        taskStats.setSuccess((Boolean) success);
                    }
                    Object hintsUsed = nestedMap.get("hintsUsed");
                    if (hintsUsed instanceof Boolean) {
                        taskStats.setHintsUsed((Boolean) hintsUsed);
                    }
                    Object completionRatio = nestedMap.get("completionRatio");
                    if (completionRatio instanceof Number number) {
                        taskStats.setCompletionRatio(number.doubleValue());
                    }
                    Object scoreRatio = nestedMap.get("scoreRatio");
                    if (scoreRatio instanceof Number number) {
                        taskStats.setScoreRatio(number.doubleValue());
                    }
                    converted.put(String.valueOf(entry.getKey()), taskStats);
                }
            }
            progress.setTaskStats(converted);
        }
        Object highestScoreObj = map.get("highestScore");
        if (highestScoreObj instanceof Number number) {
            progress.setHighestScore(number.intValue());
        }
        return progress;
    }
}
