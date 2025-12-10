package com.choicecrafter.students.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NudgePreferences {

    public static final String COLLECTION_NAME = "NUDGE_PREFERENCES";

    private String userEmail;
    private Boolean personalStatisticsPromptEnabled;
    private Boolean completedActivityPromptEnabled;
    private Boolean colleaguesPromptEnabled;
    private Boolean activityStartedNotificationsEnabled;
    private Boolean reminderNotificationsEnabled;
    private Boolean colleaguesActivityPageEnabled;
    private Boolean discussionForumEnabled;

    public NudgePreferences() {
        // Required for Firestore serialization
    }

    private NudgePreferences(@Nullable String userEmail) {
        this.userEmail = userEmail;
        this.personalStatisticsPromptEnabled = true;
        this.completedActivityPromptEnabled = true;
        this.colleaguesPromptEnabled = true;
        this.activityStartedNotificationsEnabled = true;
        this.reminderNotificationsEnabled = true;
        this.colleaguesActivityPageEnabled = true;
        this.discussionForumEnabled = true;
    }

    public static NudgePreferences createDefault(@Nullable String userEmail) {
        return new NudgePreferences(userEmail);
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public boolean isPersonalStatisticsPromptEnabled() {
        return personalStatisticsPromptEnabled == null || personalStatisticsPromptEnabled;
    }

    public void setPersonalStatisticsPromptEnabled(Boolean personalStatisticsPromptEnabled) {
        this.personalStatisticsPromptEnabled = personalStatisticsPromptEnabled;
    }

    public boolean isCompletedActivityPromptEnabled() {
        return completedActivityPromptEnabled == null || completedActivityPromptEnabled;
    }

    public void setCompletedActivityPromptEnabled(Boolean completedActivityPromptEnabled) {
        this.completedActivityPromptEnabled = completedActivityPromptEnabled;
    }

    public boolean isColleaguesPromptEnabled() {
        return colleaguesPromptEnabled == null || colleaguesPromptEnabled;
    }

    public void setColleaguesPromptEnabled(Boolean colleaguesPromptEnabled) {
        this.colleaguesPromptEnabled = colleaguesPromptEnabled;
    }

    public boolean isActivityStartedNotificationsEnabled() {
        return activityStartedNotificationsEnabled == null || activityStartedNotificationsEnabled;
    }

    public void setActivityStartedNotificationsEnabled(Boolean activityStartedNotificationsEnabled) {
        this.activityStartedNotificationsEnabled = activityStartedNotificationsEnabled;
    }

    public boolean isReminderNotificationsEnabled() {
        return reminderNotificationsEnabled == null || reminderNotificationsEnabled;
    }

    public void setReminderNotificationsEnabled(Boolean reminderNotificationsEnabled) {
        this.reminderNotificationsEnabled = reminderNotificationsEnabled;
    }

    public boolean isColleaguesActivityPageEnabled() {
        return colleaguesActivityPageEnabled == null || colleaguesActivityPageEnabled;
    }

    public void setColleaguesActivityPageEnabled(Boolean colleaguesActivityPageEnabled) {
        this.colleaguesActivityPageEnabled = colleaguesActivityPageEnabled;
    }

    public boolean isDiscussionForumEnabled() {
        return discussionForumEnabled == null || discussionForumEnabled;
    }

    public void setDiscussionForumEnabled(Boolean discussionForumEnabled) {
        this.discussionForumEnabled = discussionForumEnabled;
    }

    @NonNull
    @Override
    public String toString() {
        return "NudgePreferences{" +
                "userEmail='" + userEmail + '\'' +
                ", personalStatisticsPromptEnabled=" + isPersonalStatisticsPromptEnabled() +
                ", completedActivityPromptEnabled=" + isCompletedActivityPromptEnabled() +
                ", colleaguesPromptEnabled=" + isColleaguesPromptEnabled() +
                ", activityStartedNotificationsEnabled=" + isActivityStartedNotificationsEnabled() +
                ", reminderNotificationsEnabled=" + isReminderNotificationsEnabled() +
                ", colleaguesActivityPageEnabled=" + isColleaguesActivityPageEnabled() +
                ", discussionForumEnabled=" + isDiscussionForumEnabled() +
                '}';
    }
}
