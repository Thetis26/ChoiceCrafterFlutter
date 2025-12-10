package com.choicecrafter.students.badges;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.choicecrafter.students.models.badges.BadgeDefinition;
import com.choicecrafter.students.models.badges.BadgeStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides badge evaluation and storage helpers.
 */
public final class BadgeEvaluator {

    private BadgeEvaluator() {
    }

    public static StoredBadges prepareStoredBadges(@Nullable List<String> storedValues) {
        LinkedHashSet<String> normalizedIds = new LinkedHashSet<>();
        List<String> unknownValues = new ArrayList<>();
        if (storedValues != null) {
            for (String value : storedValues) {
                String normalized = BadgeCatalog.normalizeBadgeValue(value);
                if (normalized != null) {
                    normalizedIds.add(normalized);
                } else if (value != null) {
                    unknownValues.add(value);
                }
            }
        }
        return new StoredBadges(normalizedIds, unknownValues);
    }

    public static BadgeEvaluationResult evaluate(@NonNull BadgeProgress progress,
                                                 @NonNull StoredBadges storedBadges) {
        List<BadgeStatus> statuses = new ArrayList<>();
        LinkedHashSet<String> earnedIds = new LinkedHashSet<>(storedBadges.getNormalizedIds());

        for (BadgeDefinition definition : BadgeCatalog.getAll()) {
            boolean earned = isEarned(definition.getId(), progress);
            statuses.add(new BadgeStatus(definition, earned));
            if (earned) {
                earnedIds.add(definition.getId());
            }
        }

        List<String> updatedValues = new ArrayList<>();
        for (BadgeDefinition definition : BadgeCatalog.getAll()) {
            if (earnedIds.contains(definition.getId())) {
                updatedValues.add(definition.getId());
            }
        }
        updatedValues.addAll(storedBadges.getUnknownValues());

        boolean changed = !earnedIds.equals(storedBadges.getNormalizedIds());
        return new BadgeEvaluationResult(statuses, updatedValues, changed);
    }

    public static List<BadgeStatus> buildDefaultStatuses() {
        List<BadgeStatus> defaults = new ArrayList<>();
        for (BadgeDefinition definition : BadgeCatalog.getAll()) {
            defaults.add(new BadgeStatus(definition, false));
        }
        return defaults;
    }

    private static boolean isEarned(String badgeId, BadgeProgress progress) {
        boolean earned;
        switch (badgeId) {
            case BadgeCatalog.ID_FIRST_ACTIVITY:
                earned = progress.hasCompletedActivity();
                break;
            case BadgeCatalog.ID_FIRST_MODULE:
                earned = progress.hasCompletedModule();
                break;
            case BadgeCatalog.ID_STREAK_5:
                earned = progress.getStreak() >= 5;
                break;
            case BadgeCatalog.ID_STREAK_10:
                earned = progress.getStreak() >= 10;
                break;
            case BadgeCatalog.ID_STREAK_30:
                earned = progress.getStreak() >= 30;
                break;
            case BadgeCatalog.ID_STREAK_50:
                earned = progress.getStreak() >= 50;
                break;
            case BadgeCatalog.ID_FIRST_COMMENT:
                earned = progress.hasCommentedOnFeed();
                break;
            case BadgeCatalog.ID_CHATBOT_DISCUSSION:
                earned = progress.hasChatbotDiscussion();
                break;
            default:
                earned = false;
                break;
        }
        if (!earned) {
            earned = progress.getExistingBadgeIds().contains(badgeId);
        }
        return earned;
    }

    public static final class StoredBadges {
        private final LinkedHashSet<String> normalizedIds;
        private final List<String> unknownValues;

        private StoredBadges(LinkedHashSet<String> normalizedIds, List<String> unknownValues) {
            this.normalizedIds = normalizedIds;
            this.unknownValues = unknownValues;
        }

        public Set<String> getNormalizedIds() {
            return Collections.unmodifiableSet(normalizedIds);
        }

        public List<String> getUnknownValues() {
            return Collections.unmodifiableList(unknownValues);
        }
    }

    public static final class BadgeEvaluationResult {
        private final List<BadgeStatus> statuses;
        private final List<String> updatedBadgeValues;
        private final boolean changed;

        private BadgeEvaluationResult(List<BadgeStatus> statuses,
                                      List<String> updatedBadgeValues,
                                      boolean changed) {
            this.statuses = statuses;
            this.updatedBadgeValues = updatedBadgeValues;
            this.changed = changed;
        }

        public List<BadgeStatus> getStatuses() {
            return statuses;
        }

        public List<String> getUpdatedBadgeValues() {
            return updatedBadgeValues;
        }

        public boolean isChanged() {
            return changed;
        }
    }
}
