package com.choicecrafter.students.models.badges;

import androidx.annotation.NonNull;

/**
 * Represents the earning status of a badge for the current user.
 */
public class BadgeStatus {

    private final BadgeDefinition definition;
    private final boolean earned;

    public BadgeStatus(@NonNull BadgeDefinition definition, boolean earned) {
        this.definition = definition;
        this.earned = earned;
    }

    @NonNull
    public BadgeDefinition getDefinition() {
        return definition;
    }

    public boolean isEarned() {
        return earned;
    }
}
