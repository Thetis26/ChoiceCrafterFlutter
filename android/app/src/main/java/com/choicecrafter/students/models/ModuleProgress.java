package com.choicecrafter.studentapp.models;

public class ModuleProgress {
    private int completedTasks;
    private int totalTasks;

    public ModuleProgress() {
        // Default constructor required for Firestore serialization
    }

    public ModuleProgress(int completedTasks, int totalTasks) {
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public float getCompletionPercentage() {
        return totalTasks == 0 ? 0 : (completedTasks * 100f) / totalTasks;
    }

    public boolean isCompleted() {
        return totalTasks > 0 && completedTasks >= totalTasks;
    }
}
