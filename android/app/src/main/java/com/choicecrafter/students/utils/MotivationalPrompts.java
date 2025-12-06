package com.choicecrafter.studentapp.utils;

import android.content.Context;

import com.choicecrafter.studentapp.R;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MotivationalPrompts {

    private static final Map<MotivationalPromptType, Integer> PROMPT_RESOURCES =
            new EnumMap<>(MotivationalPromptType.class);
    private static final Random RANDOM = new Random();

    static {
        PROMPT_RESOURCES.put(
                MotivationalPromptType.REMINDER_ACTIVITY,
                R.array.motivational_prompts_reminder_activity);
        PROMPT_RESOURCES.put(
                MotivationalPromptType.COMPLETED_ACTIVITY,
                R.array.motivational_prompts_completed_activity);
        PROMPT_RESOURCES.put(
                MotivationalPromptType.PERSONAL_PERFORMANCE,
                R.array.motivational_prompts_personal_performance);
        PROMPT_RESOURCES.put(
                MotivationalPromptType.COLLEAGUE_PERFORMANCE,
                R.array.motivational_prompts_colleague_performance);
        PROMPT_RESOURCES.put(
                MotivationalPromptType.GENERAL_MOTIVATION,
                R.array.motivational_prompts_general_motivation);
    }

    private MotivationalPrompts() {
        // Utility class
    }

    public static String getRandomPrompt(Context context, MotivationalPromptType category) {
        Integer resourceId = PROMPT_RESOURCES.get(category);
        if (resourceId != null) {
            String[] prompts = context.getResources().getStringArray(resourceId);
            if (prompts.length > 0) {
                return prompts[RANDOM.nextInt(prompts.length)];
            }
        }
        return context.getString(R.string.motivational_prompt_fallback);
    }

    public static String getPersonalizedPrompt(Context context,
                                               MotivationalPromptType category,
                                               PersonalizationData data) {
        if (context == null) {
            return "";
        }
        if (data == null || !data.hasMetrics()) {
            return getRandomPrompt(context, category);
        }

        return switch (category) {
            case PERSONAL_PERFORMANCE -> buildPersonalPerformancePrompt(context, data);
            case COMPLETED_ACTIVITY -> buildCompletedActivityPrompt(context, data);
            case COLLEAGUE_PERFORMANCE -> buildColleaguePerformancePrompt(context, data);
            default -> getRandomPrompt(context, category);
        };
    }

    private static String buildPersonalPerformancePrompt(Context context, PersonalizationData data) {
        List<String> sentences = new ArrayList<>();

        Integer streak = data.getStreak();
        if (streak != null) {
            if (streak >= 5) {
                sentences.add(context.getString(R.string.motivational_ai_streak, streak));
            } else if (streak >= 2) {
                sentences.add(context.getString(R.string.motivational_ai_small_streak, streak));
            }
        }

        Long millisSinceLast = data.getMillisSinceLastActivity();
        if (millisSinceLast != null && millisSinceLast > 0) {
            long daysSince = TimeUnit.MILLISECONDS.toDays(millisSinceLast);
            if (daysSince >= 3) {
                sentences.add(context.getString(
                        R.string.motivational_ai_reengage,
                        formatDayCount(context, daysSince)));
            } else if (daysSince >= 1) {
                sentences.add(context.getString(
                        R.string.motivational_ai_keep_momentum,
                        formatDayCount(context, daysSince)));
            }
        }

        Integer completionsToday = data.getCompletionsToday();
        if (completionsToday != null && completionsToday > 0) {
            sentences.add(context.getString(
                    R.string.motivational_ai_recent_today,
                    formatActivityCount(context, completionsToday)));
        }

        Integer recentActivityCount = data.getRecentActivityCount();
        if (recentActivityCount != null && recentActivityCount >= 3) {
            sentences.add(context.getString(
                    R.string.motivational_ai_recent_activities,
                    formatActivityCount(context, recentActivityCount)));
        }

        Double scoreTrend = data.getScoreTrend();
        if (scoreTrend != null) {
            if (Double.isInfinite(scoreTrend) && scoreTrend > 0) {
                sentences.add(context.getString(R.string.motivational_ai_trend_first_scores));
            } else if (Double.isFinite(scoreTrend)) {
                long rounded = Math.round(Math.abs(scoreTrend));
                if (scoreTrend > 5) {
                    sentences.add(context.getString(R.string.motivational_ai_trend_up, rounded));
                } else if (scoreTrend < -5) {
                    sentences.add(context.getString(R.string.motivational_ai_trend_down, rounded));
                }
            }
        }

        if (sentences.isEmpty()) {
            Integer totalPoints = data.getTotalPoints();
            if (totalPoints != null && totalPoints > 0) {
                sentences.add(context.getString(R.string.motivational_ai_total_points, totalPoints));
            }
        }

        String message = joinSentences(sentences);
        if (message.isEmpty()) {
            return getRandomPrompt(context, MotivationalPromptType.PERSONAL_PERFORMANCE);
        }
        return message;
    }

    private static String buildCompletedActivityPrompt(Context context, PersonalizationData data) {
        List<String> sentences = new ArrayList<>();

        Integer lastScore = data.getLastScore();
        Integer totalPoints = data.getTotalPoints();
        if (lastScore != null) {
            double ratio = Double.NaN;
            if (totalPoints != null && totalPoints > 0) {
                ratio = lastScore / (double) totalPoints;
            }
            if (!Double.isNaN(ratio)) {
                if (ratio >= 0.9) {
                    sentences.add(context.getString(R.string.motivational_ai_completed_high_score, lastScore));
                } else if (ratio >= 0.7) {
                    sentences.add(context.getString(R.string.motivational_ai_completed_mid_score, lastScore));
                } else {
                    sentences.add(context.getString(R.string.motivational_ai_completed_low_score, lastScore));
                }
            } else {
                if (lastScore >= 80) {
                    sentences.add(context.getString(R.string.motivational_ai_completed_high_score, lastScore));
                } else if (lastScore >= 40) {
                    sentences.add(context.getString(R.string.motivational_ai_completed_mid_score, lastScore));
                } else {
                    sentences.add(context.getString(R.string.motivational_ai_completed_low_score, lastScore));
                }
            }
        }

        Boolean hintsUsed = data.getHintsUsed();
        if (Boolean.TRUE.equals(hintsUsed)) {
            sentences.add(context.getString(R.string.motivational_ai_completed_hints_used));
        } else if (Boolean.FALSE.equals(hintsUsed)) {
            sentences.add(context.getString(R.string.motivational_ai_completed_no_hints));
        }

        Integer timeSpentSeconds = data.getTimeSpentSeconds();
        String formattedTimeSpent = data.getFormattedTimeSpent();
        if (timeSpentSeconds != null && timeSpentSeconds > 0 && formattedTimeSpent != null) {
            if (timeSpentSeconds <= 120) {
                sentences.add(context.getString(
                        R.string.motivational_ai_completed_fast_finish,
                        formattedTimeSpent));
            } else if (timeSpentSeconds >= 600) {
                sentences.add(context.getString(
                        R.string.motivational_ai_completed_persistent_finish,
                        formattedTimeSpent));
            }
        }

        String message = joinSentences(sentences);
        if (message.isEmpty()) {
            return getRandomPrompt(context, MotivationalPromptType.COMPLETED_ACTIVITY);
        }
        return message;
    }

    private static String buildColleaguePerformancePrompt(Context context, PersonalizationData data) {
        List<String> sentences = new ArrayList<>();

        Integer rank = data.getPeerRank();
        Integer peerCount = data.getPeerCount();
        Integer deltaAhead = data.getPeerScoreDeltaAhead();
        Integer deltaBehind = data.getPeerScoreDeltaBehind();

        if (rank != null && rank == 1) {
            if (deltaBehind != null && deltaBehind > 0) {
                sentences.add(context.getString(R.string.motivational_ai_peer_leader, deltaBehind));
            } else {
                sentences.add(context.getString(R.string.motivational_ai_peer_leader_generic));
            }
        } else {
            if (deltaAhead != null && deltaAhead > 0) {
                if (deltaAhead <= 20) {
                    sentences.add(context.getString(R.string.motivational_ai_peer_close_gap, deltaAhead));
                } else {
                    sentences.add(context.getString(R.string.motivational_ai_peer_chasing, deltaAhead));
                }
            }
            if (deltaBehind != null && deltaBehind > 0 && deltaBehind <= 40) {
                sentences.add(context.getString(R.string.motivational_ai_peer_guard, deltaBehind));
            }
        }

        if (sentences.isEmpty() && rank != null && peerCount != null && peerCount > 0) {
            sentences.add(context.getString(R.string.motivational_ai_peer_rank, rank, peerCount));
        }

        if (sentences.isEmpty()) {
            sentences.add(context.getString(R.string.motivational_ai_peer_newcomer));
        }

        return joinSentences(sentences);
    }

    private static String joinSentences(List<String> sentences) {
        StringBuilder builder = new StringBuilder();
        for (String sentence : sentences) {
            if (sentence == null || sentence.trim().isEmpty()) {
                continue;
            }
            String trimmed = sentence.trim();
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(trimmed);
            if (!trimmed.isEmpty()) {
                char lastChar = trimmed.charAt(trimmed.length() - 1);
                if (lastChar != '.' && lastChar != '!' && lastChar != '?') {
                    builder.append('.');
                }
            }
        }
        return builder.toString().trim();
    }

    private static String formatDayCount(Context context, long days) {
        int quantity = (int) Math.max(days, 0);
        return context.getResources().getQuantityString(
                R.plurals.motivational_ai_day_count,
                quantity,
                quantity);
    }

    private static String formatActivityCount(Context context, int activities) {
        int quantity = Math.max(activities, 0);
        return context.getResources().getQuantityString(
                R.plurals.motivational_ai_activity_count,
                quantity,
                quantity);
    }

    public static final class PersonalizationData {
        private Integer streak;
        private Integer totalPoints;
        private Integer recentActivityCount;
        private Integer completionsToday;
        private Long millisSinceLastActivity;
        private Double scoreTrend;
        private Integer lastScore;
        private Boolean hintsUsed;
        private Integer timeSpentSeconds;
        private String formattedTimeSpent;
        private Integer peerRank;
        private Integer peerCount;
        private Integer peerScoreDeltaAhead;
        private Integer peerScoreDeltaBehind;

        private PersonalizationData() {
        }

        public Integer getStreak() {
            return streak;
        }

        public Integer getTotalPoints() {
            return totalPoints;
        }

        public Integer getRecentActivityCount() {
            return recentActivityCount;
        }

        public Integer getCompletionsToday() {
            return completionsToday;
        }

        public Long getMillisSinceLastActivity() {
            return millisSinceLastActivity;
        }

        public Double getScoreTrend() {
            return scoreTrend;
        }

        public Integer getLastScore() {
            return lastScore;
        }

        public Boolean getHintsUsed() {
            return hintsUsed;
        }

        public Integer getTimeSpentSeconds() {
            return timeSpentSeconds;
        }

        public String getFormattedTimeSpent() {
            return formattedTimeSpent;
        }

        public Integer getPeerRank() {
            return peerRank;
        }

        public Integer getPeerCount() {
            return peerCount;
        }

        public Integer getPeerScoreDeltaAhead() {
            return peerScoreDeltaAhead;
        }

        public Integer getPeerScoreDeltaBehind() {
            return peerScoreDeltaBehind;
        }

        private boolean hasMetrics() {
            return streak != null
                    || totalPoints != null
                    || recentActivityCount != null
                    || completionsToday != null
                    || millisSinceLastActivity != null
                    || scoreTrend != null
                    || lastScore != null
                    || hintsUsed != null
                    || timeSpentSeconds != null
                    || formattedTimeSpent != null
                    || peerRank != null
                    || peerCount != null
                    || peerScoreDeltaAhead != null
                    || peerScoreDeltaBehind != null;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final PersonalizationData data = new PersonalizationData();

            public Builder setStreak(Integer streak) {
                if (streak != null && streak > 0) {
                    data.streak = streak;
                }
                return this;
            }

            public Builder setTotalPoints(Integer totalPoints) {
                if (totalPoints != null && totalPoints >= 0) {
                    data.totalPoints = totalPoints;
                }
                return this;
            }

            public Builder setRecentActivityCount(Integer recentActivityCount) {
                if (recentActivityCount != null && recentActivityCount > 0) {
                    data.recentActivityCount = recentActivityCount;
                }
                return this;
            }

            public Builder setCompletionsToday(Integer completionsToday) {
                if (completionsToday != null && completionsToday > 0) {
                    data.completionsToday = completionsToday;
                }
                return this;
            }

            public Builder setMillisSinceLastActivity(Long millisSinceLastActivity) {
                if (millisSinceLastActivity != null && millisSinceLastActivity > 0) {
                    data.millisSinceLastActivity = millisSinceLastActivity;
                }
                return this;
            }

            public Builder setScoreTrend(Double scoreTrend) {
                if (scoreTrend != null) {
                    data.scoreTrend = scoreTrend;
                }
                return this;
            }

            public Builder setLastScore(Integer lastScore) {
                if (lastScore != null && lastScore >= 0) {
                    data.lastScore = lastScore;
                }
                return this;
            }

            public Builder setHintsUsed(Boolean hintsUsed) {
                if (hintsUsed != null) {
                    data.hintsUsed = hintsUsed;
                }
                return this;
            }

            public Builder setTimeSpentSeconds(Integer timeSpentSeconds) {
                if (timeSpentSeconds != null && timeSpentSeconds > 0) {
                    data.timeSpentSeconds = timeSpentSeconds;
                }
                return this;
            }

            public Builder setFormattedTimeSpent(String formattedTimeSpent) {
                if (formattedTimeSpent != null && !formattedTimeSpent.trim().isEmpty()) {
                    data.formattedTimeSpent = formattedTimeSpent;
                }
                return this;
            }

            public Builder setPeerRank(Integer peerRank) {
                if (peerRank != null && peerRank > 0) {
                    data.peerRank = peerRank;
                }
                return this;
            }

            public Builder setPeerCount(Integer peerCount) {
                if (peerCount != null && peerCount > 0) {
                    data.peerCount = peerCount;
                }
                return this;
            }

            public Builder setPeerScoreDeltaAhead(Integer peerScoreDeltaAhead) {
                if (peerScoreDeltaAhead != null) {
                    data.peerScoreDeltaAhead = Math.max(0, peerScoreDeltaAhead);
                }
                return this;
            }

            public Builder setPeerScoreDeltaBehind(Integer peerScoreDeltaBehind) {
                if (peerScoreDeltaBehind != null) {
                    data.peerScoreDeltaBehind = Math.max(0, peerScoreDeltaBehind);
                }
                return this;
            }

            public PersonalizationData build() {
                return data;
            }
        }
    }
}
