package com.choicecrafter.studentapp.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseProgress {
    private int totalActivities;
    private int activitiesStarted;
    private int totalTasks;
    private int attemptedTasks;
    private int completedTasks;
    private double completionPercentage;
    private List<Map<String, Object>> activitySnapshots = new ArrayList<>();
    private Map<String, ModuleProgress> moduleProgress = new HashMap<>();
    private int earnedXp;
    private int totalXp;

    public CourseProgress() {
    }

    public CourseProgress(int totalActivities,
                          int activitiesStarted,
                          int totalTasks,
                          int attemptedTasks,
                          int completedTasks,
                          double completionPercentage) {
        this.totalActivities = totalActivities;
        this.activitiesStarted = activitiesStarted;
        this.totalTasks = totalTasks;
        this.attemptedTasks = attemptedTasks;
        this.completedTasks = completedTasks;
        this.completionPercentage = completionPercentage;
    }

    public int getTotalActivities() {
        return totalActivities;
    }

    public void setTotalActivities(int totalActivities) {
        this.totalActivities = totalActivities;
    }

    public int getActivitiesStarted() {
        return activitiesStarted;
    }

    public void setActivitiesStarted(int activitiesStarted) {
        this.activitiesStarted = activitiesStarted;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getAttemptedTasks() {
        return attemptedTasks;
    }

    public void setAttemptedTasks(int attemptedTasks) {
        this.attemptedTasks = attemptedTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public List<Map<String, Object>> getActivitySnapshots() {
        return activitySnapshots;
    }

    public void setActivitySnapshots(List<Map<String, Object>> activitySnapshots) {
        this.activitySnapshots = activitySnapshots != null ? activitySnapshots : new ArrayList<>();
    }

    public Map<String, ModuleProgress> getModuleProgress() {
        return moduleProgress;
    }

    public void setModuleProgress(Map<String, ModuleProgress> moduleProgress) {
        this.moduleProgress = moduleProgress != null ? moduleProgress : new HashMap<>();
    }

    public int getEarnedXp() {
        return earnedXp;
    }

    public void setEarnedXp(int earnedXp) {
        this.earnedXp = Math.max(0, earnedXp);
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = Math.max(0, totalXp);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("totalActivities", totalActivities);
        map.put("activitiesStarted", activitiesStarted);
        map.put("totalTasks", totalTasks);
        map.put("attemptedTasks", attemptedTasks);
        map.put("completedTasks", completedTasks);
        map.put("completionPercentage", completionPercentage);
        map.put("activitySnapshots", new ArrayList<>(activitySnapshots));
        map.put("earnedXp", earnedXp);
        map.put("totalXp", totalXp);
        if (moduleProgress != null) {
            Map<String, Object> moduleProgressMap = new HashMap<>();
            for (Map.Entry<String, ModuleProgress> entry : moduleProgress.entrySet()) {
                ModuleProgress progress = entry.getValue();
                if (progress != null) {
                    Map<String, Object> progressMap = new HashMap<>();
                    progressMap.put("completedTasks", progress.getCompletedTasks());
                    progressMap.put("totalTasks", progress.getTotalTasks());
                    progressMap.put("completionPercentage", progress.getCompletionPercentage());
                    moduleProgressMap.put(entry.getKey(), progressMap);
                }
            }
            map.put("moduleProgress", moduleProgressMap);
        } else {
            map.put("moduleProgress", new HashMap<>());
        }
        return map;
    }
}
