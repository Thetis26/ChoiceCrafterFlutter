package com.choicecrafter.students.models;

public class PersonalActivity {
    private String activityName;
    private String activityDescription;
    private String activityTime;

    public PersonalActivity(String activityName, String activityTime, String activityDescription) {
        this.activityName = activityName;
        this.activityTime = activityTime;
        this.activityDescription = activityDescription;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getActivityTime() {
        return activityTime;
    }

    public String getActivityDescription() {
        return activityDescription;
    }
}
