package com.choicecrafter.studentapp.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.choicecrafter.studentapp.BuildConfig;
import com.choicecrafter.studentapp.models.TaskStats;
import com.choicecrafter.studentapp.models.tasks.CodingChallengeTask;
import com.choicecrafter.studentapp.models.tasks.FillInTheBlank;
import com.choicecrafter.studentapp.models.tasks.InfoCardTask;
import com.choicecrafter.studentapp.models.tasks.MatchingPairTask;
import com.choicecrafter.studentapp.models.tasks.MultipleChoiceQuestion;
import com.choicecrafter.studentapp.models.tasks.OrderingTask;
import com.choicecrafter.studentapp.models.tasks.SpotTheErrorTask;
import com.choicecrafter.studentapp.models.tasks.Task;
import com.choicecrafter.studentapp.models.tasks.TrueFalseTask;

import static com.choicecrafter.studentapp.utils.TaskStatsKeyUtils.findStatsForTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class responsible for transforming task completion data into activity scores.
 * The calculation is based on fixed XP values associated with every supported task type
 * and enhanced with IRT smart scoring plus effort-aware adjustments for multiple choice items.
 */
public final class ActivityScoreCalculator {

    private static final int DEFAULT_TASK_XP = 10;
    private static final int MULTIPLE_CHOICE_XP = 20;
    private static final int TRUE_FALSE_XP = 10;
    private static final int FILL_IN_THE_BLANK_XP = 15;
    private static final int MATCHING_PAIR_XP = 25;
    private static final int ORDERING_XP = 25;
    private static final int SPOT_THE_ERROR_XP = 30;
    private static final int CODING_CHALLENGE_XP = 40;
    private static final int INFO_CARD_XP = 5;

    private ActivityScoreCalculator() {
        // Utility class
    }

    /**
     * Builds a human readable summary that includes the XP-based score details.
     */
    @NonNull
    public static String buildScoreSummary(@Nullable List<Task> tasks,
                                           @Nullable Map<String, TaskStats> taskStatsMap,
                                           @NonNull ScoreSummaryFormatter formatter) {
        int totalXp = calculateTotalXp(tasks, taskStatsMap);
        int earnedXp = calculateEarnedXp(tasks, taskStatsMap);
        return formatter.format(earnedXp, earnedXp, totalXp);
    }

    public static int calculateEarnedXp(@Nullable List<Task> tasks,
                                        @Nullable Map<String, TaskStats> taskStatsMap) {
        return computeScoreBreakdown(tasks, taskStatsMap).earnedXp;
    }

    public static int calculateTotalXp(@Nullable List<Task> tasks) {
        return calculateTotalXp(tasks, null);
    }

    public static int calculateTotalXp(@Nullable List<Task> tasks,
                                       @Nullable Map<String, TaskStats> taskStatsMap) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        if (taskStatsMap == null || taskStatsMap.isEmpty()) {
            int totalXp = 0;
            for (Task task : tasks) {
                if (task == null) {
                    continue;
                }
                totalXp += resolveTaskXp(task);
            }
            return totalXp;
        }
        return computeScoreBreakdown(tasks, taskStatsMap).totalXp;
    }

    private static int resolveTaskXp(@NonNull Task task) {
        if (task instanceof MultipleChoiceQuestion) {
            return MULTIPLE_CHOICE_XP;
        }
        if (task instanceof TrueFalseTask) {
            return TRUE_FALSE_XP;
        }
        if (task instanceof FillInTheBlank) {
            return FILL_IN_THE_BLANK_XP;
        }
        if (task instanceof MatchingPairTask) {
            return MATCHING_PAIR_XP;
        }
        if (task instanceof OrderingTask) {
            return ORDERING_XP;
        }
        if (task instanceof SpotTheErrorTask) {
            return SPOT_THE_ERROR_XP;
        }
        if (task instanceof CodingChallengeTask) {
            return CODING_CHALLENGE_XP;
        }
        if (task instanceof InfoCardTask) {
            return INFO_CARD_XP;
        }
        return DEFAULT_TASK_XP;
    }

    @NonNull
    private static ScoreBreakdown computeScoreBreakdown(@Nullable List<Task> tasks,
                                                        @Nullable Map<String, TaskStats> taskStatsMap) {
        ScoreBreakdown breakdown = new ScoreBreakdown();
        if (tasks == null || tasks.isEmpty()) {
            return breakdown;
        }

        if (taskStatsMap == null || taskStatsMap.isEmpty()) {
            for (Task task : tasks) {
                if (task == null) {
                    continue;
                }
                breakdown.totalXp += resolveTaskXp(task);
            }
            return breakdown;
        }

        boolean irtScoringEnabled = BuildConfig.ENABLE_IRT_SCORING;
        double abilityEstimate = irtScoringEnabled
                ? IrtSmartScoreAdjuster.estimateAbility(tasks, taskStatsMap)
                : 0.0;

        for (Task task : tasks) {
            if (task == null) {
                continue;
            }

            TaskStats stats = findStatsForTask(taskStatsMap, task);
            if (stats == null) {
                breakdown.totalXp += resolveTaskXp(task);
                continue;
            }

            if (RapidGuessingDetector.isRapidGuess(stats)) {
                // Remove rapid guesses from both earned and available XP to avoid rewarding lucky clicks.
                continue;
            }

            int taskXp = resolveTaskXp(task);
            breakdown.totalXp += taskXp;

            double ratio = Math.min(1.0, Math.max(0.0, stats.resolveScoreRatio()));
            if (irtScoringEnabled && task instanceof MultipleChoiceQuestion) {
                ratio = IrtSmartScoreAdjuster.adjustScore((MultipleChoiceQuestion) task, stats, abilityEstimate);
            }
            ratio = applyEffortModifiers(ratio, stats);

            breakdown.earnedXp += (int) Math.round(taskXp * ratio);
        }

        return breakdown;
    }

    /**
     * Builds a breakdown per task including the XP earned, XP available, and reason for any losses.
     */
    @NonNull
    public static List<TaskScoreBreakdown> buildTaskScoreBreakdown(@Nullable List<Task> tasks,
                                                                   @Nullable Map<String, TaskStats> taskStatsMap) {
        List<TaskScoreBreakdown> breakdowns = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) {
            return breakdowns;
        }

        boolean irtScoringEnabled = BuildConfig.ENABLE_IRT_SCORING;
        boolean hasStatsMap = taskStatsMap != null && !taskStatsMap.isEmpty();
        double abilityEstimate = irtScoringEnabled && hasStatsMap
                ? IrtSmartScoreAdjuster.estimateAbility(tasks, taskStatsMap)
                : 0.0;

        for (Task task : tasks) {
            if (task == null) {
                continue;
            }

            int taskXp = resolveTaskXp(task);
            TaskStats stats = hasStatsMap ? findStatsForTask(taskStatsMap, task) : null;
            boolean rapidGuess = RapidGuessingDetector.isRapidGuess(stats);
            boolean countedTowardsScore = !rapidGuess;

            double ratio = 0.0;
            if (stats == null) {
                countedTowardsScore = true;
            } else if (!rapidGuess) {
                ratio = Math.min(1.0, Math.max(0.0, stats.resolveScoreRatio()));
                if (irtScoringEnabled && task instanceof MultipleChoiceQuestion) {
                    ratio = IrtSmartScoreAdjuster.adjustScore((MultipleChoiceQuestion) task, stats, abilityEstimate);
                }
                ratio = applyEffortModifiers(ratio, stats);
                ratio = Math.max(0.0, Math.min(1.0, ratio));
            }

            int totalXp = countedTowardsScore ? taskXp : 0;
            int earnedXp = countedTowardsScore ? (int) Math.round(taskXp * ratio) : 0;
            int lostXp = Math.max(0, totalXp - earnedXp);
            LossReason lossReason = resolveLossReason(stats, rapidGuess, lostXp, ratio);

            breakdowns.add(new TaskScoreBreakdown(task, stats, totalXp, earnedXp, lostXp, lossReason, rapidGuess,
                    countedTowardsScore ? ratio : 0.0));
        }

        return breakdowns;
    }

    @NonNull
    private static LossReason resolveLossReason(@Nullable TaskStats stats,
                                                boolean rapidGuess,
                                                int lostXp,
                                                double ratio) {
        if (stats == null) {
            return LossReason.NOT_ATTEMPTED;
        }
        if (rapidGuess) {
            return LossReason.RAPID_GUESS;
        }
        if (lostXp <= 0) {
            return LossReason.NONE;
        }
        if (ratio <= 0.0) {
            return LossReason.INCORRECT;
        }
        if (ratio < 1.0) {
            return LossReason.PARTIALLY_CORRECT;
        }
        return LossReason.NONE;
    }

    private static double applyEffortModifiers(double ratio, @Nullable TaskStats stats) {
        if (stats == null) {
            return ratio;
        }

        double adjusted = ratio;
        if (Boolean.TRUE.equals(stats.getHintsUsed())) {
            adjusted *= 0.75;
        }

        Integer retries = stats.getRetries();
        if (retries != null && retries > 0) {
            double penalty = 0.15 * Math.min(retries, 4);
            adjusted *= Math.max(0.0, 1.0 - penalty);
        }

        if (adjusted < 0.0) {
            return 0.0;
        }
        if (adjusted > 1.0) {
            return 1.0;
        }
        return adjusted;
    }

    /**
     * Contract used to format score summaries based on XP calculations.
     *
     * @param scoreXp  the XP value representing the activity score (e.g. the best run or the latest attempt)
     * @param earnedXp the XP earned for the current attempt
     * @param totalXp  the total XP available in the activity
     */
    public interface ScoreSummaryFormatter {
        String format(int scoreXp, int earnedXp, int totalXp);
    }

    /** Reasons why XP might have been lost for a task. */
    public enum LossReason {
        NONE,
        NOT_ATTEMPTED,
        INCORRECT,
        PARTIALLY_CORRECT,
        RAPID_GUESS
    }

    /** Represents the XP breakdown for a single task. */
    public static final class TaskScoreBreakdown {
        private final Task task;
        private final TaskStats taskStats;
        private final int totalXp;
        private final int earnedXp;
        private final int lostXp;
        private final LossReason lossReason;
        private final boolean rapidGuess;
        private final double scoreRatio;

        private TaskScoreBreakdown(Task task,
                                    TaskStats taskStats,
                                    int totalXp,
                                    int earnedXp,
                                    int lostXp,
                                    LossReason lossReason,
                                    boolean rapidGuess,
                                    double scoreRatio) {
            this.task = task;
            this.taskStats = taskStats;
            this.totalXp = totalXp;
            this.earnedXp = earnedXp;
            this.lostXp = lostXp;
            this.lossReason = lossReason;
            this.rapidGuess = rapidGuess;
            this.scoreRatio = scoreRatio;
        }

        @Nullable
        public Task getTask() {
            return task;
        }

        @Nullable
        public TaskStats getTaskStats() {
            return taskStats;
        }

        public int getTotalXp() {
            return totalXp;
        }

        public int getEarnedXp() {
            return earnedXp;
        }

        public int getLostXp() {
            return lostXp;
        }

        @NonNull
        public LossReason getLossReason() {
            return lossReason;
        }

        public boolean isRapidGuess() {
            return rapidGuess;
        }

        public double getScoreRatio() {
            return scoreRatio;
        }
    }

    private static final class ScoreBreakdown {
        private int earnedXp;
        private int totalXp;
    }
}
