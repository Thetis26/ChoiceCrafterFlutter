package com.choicecrafter.studentapp.models.badges;

import androidx.annotation.NonNull;

/**
 * Represents a badge that can be earned by the learner.
 */
public class BadgeDefinition {

    private final String id;
    private final String title;
    private final String description;
    private final int points;

    public BadgeDefinition(@NonNull String id,
                           @NonNull String title,
                           @NonNull String description,
                           int points) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.points = points;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public int getPoints() {
        return points;
    }
}
