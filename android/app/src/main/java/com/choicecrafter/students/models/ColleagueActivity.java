package com.choicecrafter.studentapp.models;

import androidx.annotation.Nullable;

import com.choicecrafter.studentapp.utils.Avatar;

public class ColleagueActivity {
    private final String colleagueName;
    private final String activityName;
    private final String activityDescription;
    private final Avatar anonymousAvatar; // Anonymous avatar metadata for display
    private final String timestamp; // Timestamp of the activity update

    public ColleagueActivity(String colleagueName,
                             String activityName,
                             @Nullable Avatar anonymousAvatar,
                             String timestamp) {
        this.colleagueName = colleagueName;
        this.activityName = activityName;
        this.activityDescription = "Completed: " + activityName;
        this.anonymousAvatar = anonymousAvatar;
        this.timestamp = timestamp;
    }

    public String getColleagueName() {
        return colleagueName;
    }
    public String getActivityName() {
        return activityName;
    }
    public String getActivityDescription() {
        return activityDescription;
    }

    public String getImageUrl() {
        if (anonymousAvatar == null) {
            return null;
        }
        String url = anonymousAvatar.getImageUrl();
        return url != null && !url.trim().isEmpty() ? url : null;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getAnonymousName() {
        if (anonymousAvatar == null) {
            return null;
        }
        String name = anonymousAvatar.getName();
        return name != null && !name.trim().isEmpty() ? name : null;
    }

    @Nullable
    public Avatar getAnonymousAvatar() {
        return anonymousAvatar;
    }
}
