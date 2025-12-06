package com.choicecrafter.studentapp.badges;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.choicecrafter.studentapp.MainViewModel;
import com.choicecrafter.studentapp.models.User;
import com.choicecrafter.studentapp.models.badges.BadgeStatus;
import com.choicecrafter.studentapp.repositories.UserRepository;
import com.choicecrafter.studentapp.utils.BadgePreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Coordinates badge evaluation, persistence, and bookkeeping.
 */
public final class BadgeUpdateManager {

    private static final String TAG = "BadgeUpdateManager";

    private BadgeUpdateManager() {
    }

    public static BadgeUpdateOutcome evaluateAndPersist(@NonNull Context context,
                                                        @Nullable User user,
                                                        boolean hasCompletedActivity,
                                                        boolean hasCompletedModule,
                                                        @NonNull UserRepository userRepository,
                                                        @Nullable MainViewModel mainViewModel) {
        if (user == null) {
            return BadgeUpdateOutcome.empty();
        }
        BadgeEvaluator.StoredBadges storedBadges = BadgeEvaluator.prepareStoredBadges(user.getBadges());
        Set<String> normalizedBadges = new HashSet<>(storedBadges.getNormalizedIds());

        String email = user.getEmail();
        boolean hasCommented = (!TextUtils.isEmpty(email) && BadgePreferences.hasCommented(context, email))
                || normalizedBadges.contains(BadgeCatalog.ID_FIRST_COMMENT);
        boolean hasChatbotDiscussion = (!TextUtils.isEmpty(email) && BadgePreferences.hasChatbotDiscussion(context, email))
                || normalizedBadges.contains(BadgeCatalog.ID_CHATBOT_DISCUSSION);

        BadgeProgress progress = BadgeProgress.builder()
                .setHasCompletedActivity(hasCompletedActivity)
                .setHasCompletedModule(hasCompletedModule)
                .setStreak(user.computeStreak())
                .setHasCommentedOnFeed(hasCommented)
                .setHasChatbotDiscussion(hasChatbotDiscussion)
                .setExistingBadgeIds(normalizedBadges)
                .build();

        BadgeEvaluator.BadgeEvaluationResult result = BadgeEvaluator.evaluate(progress, storedBadges);
        List<String> newlyUnlockedBadges = new ArrayList<>();
        for (BadgeStatus status : result.getStatuses()) {
            if (status.isEarned() && !normalizedBadges.contains(status.getDefinition().getId())) {
                newlyUnlockedBadges.add(status.getDefinition().getTitle());
            }
        }

        if (mainViewModel != null) {
            mainViewModel.setBadgeStatuses(result.getStatuses());
        }

        if (result.isChanged()) {
            user.setBadges(result.getUpdatedBadgeValues());
            userRepository.updateUserBadges(user,
                    () -> Log.i(TAG, "Updated badges for user"),
                    e -> Log.w(TAG, "Failed to update badges", e));
            if (mainViewModel != null) {
                mainViewModel.setUser(user);
            }
        }

        return new BadgeUpdateOutcome(result.getStatuses(), newlyUnlockedBadges, result.isChanged());
    }

    public static final class BadgeUpdateOutcome {
        private final List<BadgeStatus> statuses;
        private final List<String> newlyUnlockedBadgeTitles;
        private final boolean badgesChanged;

        private BadgeUpdateOutcome(List<BadgeStatus> statuses,
                                   List<String> newlyUnlockedBadgeTitles,
                                   boolean badgesChanged) {
            this.statuses = statuses;
            this.newlyUnlockedBadgeTitles = newlyUnlockedBadgeTitles;
            this.badgesChanged = badgesChanged;
        }

        private static BadgeUpdateOutcome empty() {
            return new BadgeUpdateOutcome(new ArrayList<>(), new ArrayList<>(), false);
        }

        public List<BadgeStatus> getStatuses() {
            return statuses;
        }

        public List<String> getNewlyUnlockedBadgeTitles() {
            return newlyUnlockedBadgeTitles;
        }

        public boolean haveBadgesChanged() {
            return badgesChanged;
        }
    }
}
