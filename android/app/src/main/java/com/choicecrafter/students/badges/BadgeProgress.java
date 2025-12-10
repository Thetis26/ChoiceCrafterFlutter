package com.choicecrafter.students.badges;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Snapshot of the learner's progress used for badge evaluation.
 */
public class BadgeProgress {

    private final boolean hasCompletedActivity;
    private final boolean hasCompletedModule;
    private final int streak;
    private final boolean hasCommentedOnFeed;
    private final boolean hasChatbotDiscussion;
    private final Set<String> existingBadgeIds;

    private BadgeProgress(Builder builder) {
        this.hasCompletedActivity = builder.hasCompletedActivity;
        this.hasCompletedModule = builder.hasCompletedModule;
        this.streak = builder.streak;
        this.hasCommentedOnFeed = builder.hasCommentedOnFeed;
        this.hasChatbotDiscussion = builder.hasChatbotDiscussion;
        this.existingBadgeIds = Collections.unmodifiableSet(new HashSet<>(builder.existingBadgeIds));
    }

    public boolean hasCompletedActivity() {
        return hasCompletedActivity;
    }

    public boolean hasCompletedModule() {
        return hasCompletedModule;
    }

    public int getStreak() {
        return streak;
    }

    public boolean hasCommentedOnFeed() {
        return hasCommentedOnFeed;
    }

    public boolean hasChatbotDiscussion() {
        return hasChatbotDiscussion;
    }

    public Set<String> getExistingBadgeIds() {
        return existingBadgeIds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean hasCompletedActivity;
        private boolean hasCompletedModule;
        private int streak;
        private boolean hasCommentedOnFeed;
        private boolean hasChatbotDiscussion;
        private Set<String> existingBadgeIds = new HashSet<>();

        private Builder() {
        }

        public Builder setHasCompletedActivity(boolean value) {
            this.hasCompletedActivity = value;
            return this;
        }

        public Builder setHasCompletedModule(boolean value) {
            this.hasCompletedModule = value;
            return this;
        }

        public Builder setStreak(int streak) {
            this.streak = streak;
            return this;
        }

        public Builder setHasCommentedOnFeed(boolean value) {
            this.hasCommentedOnFeed = value;
            return this;
        }

        public Builder setHasChatbotDiscussion(boolean value) {
            this.hasChatbotDiscussion = value;
            return this;
        }

        public Builder setExistingBadgeIds(Set<String> existingBadgeIds) {
            this.existingBadgeIds = existingBadgeIds != null ? new HashSet<>(existingBadgeIds) : new HashSet<>();
            return this;
        }

        public BadgeProgress build() {
            return new BadgeProgress(this);
        }
    }
}
