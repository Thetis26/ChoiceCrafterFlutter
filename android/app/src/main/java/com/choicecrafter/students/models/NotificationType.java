package com.choicecrafter.studentapp.models;

public enum NotificationType {
    ACTIVITY_STARTED("Activity Started"),
    COLLEAGUE_ACTIVITY_STARTED("Colleague Activity Started"),
    POINTS_THRESHOLD_REACHED("Points Milestone"),
    COMMENT_ADDED("Comment Added"),
    CHAT_MESSAGE("Chat Message"),
    REMINDER("Reminder");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
