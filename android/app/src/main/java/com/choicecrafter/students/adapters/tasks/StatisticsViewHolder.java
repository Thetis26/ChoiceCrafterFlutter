package com.choicecrafter.studentapp.adapters.tasks;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.TaskStats;
import com.choicecrafter.studentapp.models.EnrollmentActivityProgress;
import com.choicecrafter.studentapp.models.tasks.FillInTheBlank;
import com.choicecrafter.studentapp.models.tasks.MatchingPairTask;
import com.choicecrafter.studentapp.models.tasks.MultipleChoiceQuestion;
import com.choicecrafter.studentapp.models.tasks.OrderingTask;
import com.choicecrafter.studentapp.models.tasks.SpotTheErrorTask;
import com.choicecrafter.studentapp.models.tasks.Task;
import com.choicecrafter.studentapp.models.tasks.TrueFalseTask;
import com.choicecrafter.studentapp.utils.ActivityScoreCalculator;
import com.choicecrafter.studentapp.utils.ActivityScoreCalculator.LossReason;
import com.choicecrafter.studentapp.utils.ActivityScoreCalculator.TaskScoreBreakdown;
import com.choicecrafter.studentapp.utils.MotivationalPromptType;
import com.choicecrafter.studentapp.utils.MotivationalPrompts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatisticsViewHolder extends RecyclerView.ViewHolder {

    private final TextView timeTaken;
    private final TextView score;
    private final TextView completion;
    private final TextView hintsUsed;

    private final TextView colleaguesPerformance;
    private final TextView taskBreakdownHeader;
    private final LinearLayout taskBreakdownContainer;
    private final Button toDiscussionButton;
    private final View completionActionsContainer;
    private final Button returnButton;
    private final Button retryButton;

    public StatisticsViewHolder(@NonNull View itemView) {
        super(itemView);
        this.timeTaken = itemView.findViewById(R.id.time_taken);
        this.score = itemView.findViewById(R.id.score);
        this.completion = itemView.findViewById(R.id.completion);
        this.hintsUsed = itemView.findViewById(R.id.hints_used);
        this.colleaguesPerformance = itemView.findViewById(R.id.colleague_performance);
        this.taskBreakdownHeader = itemView.findViewById(R.id.task_breakdown_header);
        this.taskBreakdownContainer = itemView.findViewById(R.id.task_breakdown_container);
        this.toDiscussionButton = itemView.findViewById(R.id.to_discussion);
        this.completionActionsContainer = itemView.findViewById(R.id.completion_actions_container);
        this.returnButton = itemView.findViewById(R.id.return_to_activities_button);
        this.retryButton = itemView.findViewById(R.id.retry_activity_button);
    }

    public void bind(EnrollmentActivityProgress userActivity) {
        bind(userActivity, null, false, null, null, null, true, true);
    }

    public void bind(EnrollmentActivityProgress userActivity, OnClickListener toDiscussionClickListener) {
        bind(userActivity, null, false, toDiscussionClickListener, null, null, true, true);
    }

    public void bind(EnrollmentActivityProgress userActivity,
                     List<Task> tasks,
                     boolean isActivityCompleted,
                     OnClickListener toDiscussionClickListener,
                     OnClickListener returnClickListener,
                     OnClickListener retryClickListener,
                     boolean showMotivationalPrompt,
                     boolean showDiscussionButton) {
        if (toDiscussionButton != null) {
            if (showDiscussionButton) {
                toDiscussionButton.setVisibility(View.VISIBLE);
                toDiscussionButton.setOnClickListener(toDiscussionClickListener);
            } else {
                toDiscussionButton.setVisibility(View.GONE);
                toDiscussionButton.setOnClickListener(null);
            }
        }

        Map<String, TaskStats> taskStatsMap = userActivity != null ? userActivity.getTaskStats() : null;
        Context context = itemView.getContext();
        String motivationalPrompt = showMotivationalPrompt
                ? MotivationalPrompts.getRandomPrompt(context, MotivationalPromptType.COMPLETED_ACTIVITY)
                : null;

        if (taskStatsMap != null && !taskStatsMap.isEmpty()) {
            int totalSeconds = 0;
            double completionAccumulator = 0.0;
            int completionSamples = 0;
            boolean anyHintsUsed = false;
            for (TaskStats taskStats : taskStatsMap.values()) {
                String timeSpent = taskStats.getTimeSpent();
                if (timeSpent != null && timeSpent.contains(":")) {
                    String[] parts = timeSpent.split(":");
                    try {
                        int minutes = Integer.parseInt(parts[0]);
                        int seconds = Integer.parseInt(parts[1]);
                        totalSeconds += minutes * 60 + seconds;
                    } catch (NumberFormatException e) {
                        Log.w("StatisticsViewHolder", "Invalid time format: " + timeSpent, e);
                    }
                }
                completionAccumulator += clampRatio(taskStats.resolveCompletionRatio());
                completionSamples++;
                if (!anyHintsUsed && Boolean.TRUE.equals(taskStats.getHintsUsed())) {
                    anyHintsUsed = true;
                }
            }
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            String formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
            timeTaken.setText(context.getString(R.string.statistics_card_time_value, formattedTime));

            int totalXp = tasks != null ? ActivityScoreCalculator.calculateTotalXp(tasks, taskStatsMap) : 0;
            int earnedXp = tasks != null ? ActivityScoreCalculator.calculateEarnedXp(tasks, taskStatsMap) : 0;
            int scoreValue = 0;
            if (userActivity != null && userActivity.getHighestScore() != null) {
                scoreValue = userActivity.getHighestScore();
            } else {
                scoreValue = earnedXp;
            }
            if (totalXp > 0 && scoreValue > totalXp) {
                scoreValue = totalXp;
            }
            if (scoreValue < 0) {
                scoreValue = 0;
            }
            score.setText(context.getString(R.string.statistics_card_score_value, scoreValue));
            if (completion != null) {
                int completionValue = completionSamples > 0
                        ? (int) Math.round((completionAccumulator * 100.0) / completionSamples)
                        : 0;
                completion.setText(context.getString(R.string.statistics_card_completion_value, completionValue));
            }
            hintsUsed.setText(context.getString(anyHintsUsed ? R.string.statistics_card_hints_used_yes : R.string.statistics_card_hints_used_no));

            MotivationalPrompts.PersonalizationData data = MotivationalPrompts.PersonalizationData.builder()
                    .setLastScore(scoreValue)
                    .setTotalPoints(totalXp > 0 ? totalXp : null)
                    .setHintsUsed(anyHintsUsed)
                    .setTimeSpentSeconds(totalSeconds)
                    .setFormattedTimeSpent(formattedTime)
                    .build();
            colleaguesPerformance.setText(MotivationalPrompts.getPersonalizedPrompt(
                    context,
                    MotivationalPromptType.COMPLETED_ACTIVITY,
                    data));
        } else {
            Log.d("StatisticsViewHolder", "No task stats available for user activity: " + (userActivity != null ? userActivity.getActivityId() : "unknown"));
            timeTaken.setText(context.getString(R.string.statistics_card_time_value, "00:00"));
            score.setText(context.getString(R.string.statistics_card_score_value, 0));
            if (completion != null) {
                completion.setText(context.getString(R.string.statistics_card_completion_value, 0));
            }
            hintsUsed.setText(context.getString(R.string.statistics_card_hints_used_no));
        }

        if (colleaguesPerformance != null) {
            if (showMotivationalPrompt) {
                colleaguesPerformance.setVisibility(View.VISIBLE);
                if (motivationalPrompt == null) {
                    motivationalPrompt = MotivationalPrompts.getRandomPrompt(
                            context,
                            MotivationalPromptType.COMPLETED_ACTIVITY);
                }
                colleaguesPerformance.setText(motivationalPrompt);
            } else {
                colleaguesPerformance.setVisibility(View.GONE);
            }
        }

        renderTaskBreakdown(context, tasks, taskStatsMap);

        if (completionActionsContainer != null) {
            completionActionsContainer.setVisibility(isActivityCompleted ? View.VISIBLE : View.GONE);
            if (isActivityCompleted) {
                if (returnButton != null) {
                    returnButton.setOnClickListener(returnClickListener);
                }
                if (retryButton != null) {
                    retryButton.setOnClickListener(retryClickListener);
                }
            } else {
                if (returnButton != null) {
                    returnButton.setOnClickListener(null);
                }
                if (retryButton != null) {
                    retryButton.setOnClickListener(null);
                }
            }
        }
    }

    private double clampRatio(double ratio) {
        return Math.max(0.0, Math.min(1.0, ratio));
    }

    private void renderTaskBreakdown(Context context,
                                     List<Task> tasks,
                                     Map<String, TaskStats> taskStatsMap) {
        if (taskBreakdownContainer == null || taskBreakdownHeader == null) {
            return;
        }

        taskBreakdownContainer.removeAllViews();

        List<TaskScoreBreakdown> breakdowns = ActivityScoreCalculator.buildTaskScoreBreakdown(tasks, taskStatsMap);
        if (breakdowns.isEmpty()) {
            taskBreakdownContainer.setVisibility(View.GONE);
            taskBreakdownHeader.setVisibility(View.GONE);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        for (int index = 0; index < breakdowns.size(); index++) {
            TaskScoreBreakdown breakdown = breakdowns.get(index);
            View breakdownView = inflater.inflate(R.layout.statistics_task_breakdown_item, taskBreakdownContainer, false);

            TextView titleView = breakdownView.findViewById(R.id.task_breakdown_title);
            TextView pointsView = breakdownView.findViewById(R.id.task_breakdown_points);
            TextView hintsView = breakdownView.findViewById(R.id.task_breakdown_hints);
            TextView retriesView = breakdownView.findViewById(R.id.task_breakdown_retries);
            TextView reasonView = breakdownView.findViewById(R.id.task_breakdown_reason);

            String rawTitle = breakdown.getTask() != null ? breakdown.getTask().getTitle() : null;
            String displayTitle;
            if (TextUtils.isEmpty(rawTitle)) {
                displayTitle = context.getString(R.string.statistics_task_breakdown_unknown_task, index + 1);
            } else {
                displayTitle = context.getString(R.string.statistics_task_breakdown_title_format, index + 1, rawTitle);
            }
            titleView.setText(displayTitle);

            if (breakdown.getTotalXp() > 0) {
                pointsView.setText(context.getString(R.string.statistics_task_breakdown_points_lost,
                        breakdown.getLostXp(), breakdown.getTotalXp()));
            } else {
                pointsView.setText(R.string.statistics_task_breakdown_points_lost_none);
            }

            TaskStats stats = breakdown.getTaskStats();
            String hintsValue;
            if (stats == null || stats.getHintsUsed() == null) {
                hintsValue = context.getString(R.string.statistics_task_breakdown_not_available);
            } else {
                hintsValue = context.getString(Boolean.TRUE.equals(stats.getHintsUsed())
                        ? R.string.statistics_card_hints_used_yes
                        : R.string.statistics_card_hints_used_no);
            }
            hintsView.setText(context.getString(R.string.statistics_task_breakdown_hints, hintsValue));

            String retriesValue;
            if (stats == null || stats.getRetries() == null) {
                retriesValue = context.getString(R.string.statistics_task_breakdown_not_available);
            } else {
                retriesValue = String.valueOf(Math.max(stats.getRetries(), 0));
            }
            retriesView.setText(context.getString(R.string.statistics_task_breakdown_retries, retriesValue));

            String reasonText = context.getString(R.string.statistics_task_breakdown_reason,
                    mapLossReason(context, breakdown.getLossReason()),
                    resolveCorrectAnswer(context, breakdown));
            reasonView.setText(reasonText);

            taskBreakdownContainer.addView(breakdownView);
        }

        taskBreakdownContainer.setVisibility(View.VISIBLE);
        taskBreakdownHeader.setVisibility(View.VISIBLE);
    }

    @NonNull
    private String resolveCorrectAnswer(Context context, TaskScoreBreakdown breakdown) {
        if (breakdown == null) {
            return context.getString(R.string.statistics_task_breakdown_not_available);
        }

        Task task = breakdown.getTask();
        if (task instanceof MultipleChoiceQuestion multipleChoiceQuestion) {
            return resolveMultipleChoiceAnswer(context, multipleChoiceQuestion);
        }
        if (task instanceof SpotTheErrorTask spotTheErrorTask) {
            return resolveSpotTheErrorAnswer(context, spotTheErrorTask);
        }
        if (task instanceof TrueFalseTask trueFalseTask) {
            return context.getString(trueFalseTask.isCorrectAnswer()
                    ? R.string.task_true_option_label
                    : R.string.task_false_option_label);
        }
        if (task instanceof FillInTheBlank fillInTheBlank) {
            return resolveFillInTheBlankAnswers(context, fillInTheBlank);
        }
        if (task instanceof MatchingPairTask matchingPairTask) {
            return resolveMatchingPairsAnswer(context, matchingPairTask);
        }
        if (task instanceof OrderingTask orderingTask) {
            return resolveOrderingAnswer(context, orderingTask);
        }

        return context.getString(R.string.statistics_task_breakdown_not_available);
    }

    @NonNull
    private String resolveMultipleChoiceAnswer(Context context, MultipleChoiceQuestion question) {
        List<String> options = question != null ? question.getOptions() : null;
        int correctIndex = question != null ? question.getCorrectAnswer() : -1;
        if (options != null && correctIndex >= 0 && correctIndex < options.size()) {
            String option = options.get(correctIndex);
            if (!TextUtils.isEmpty(option)) {
                return option;
            }
        }
        return context.getString(R.string.statistics_task_breakdown_not_available);
    }

    @NonNull
    private String resolveSpotTheErrorAnswer(Context context, SpotTheErrorTask task) {
        List<String> options = task != null ? task.getOptions() : null;
        int correctIndex = task != null ? task.getCorrectOptionIndex() : -1;
        if (options != null && correctIndex >= 0 && correctIndex < options.size()) {
            String option = options.get(correctIndex);
            if (!TextUtils.isEmpty(option)) {
                return option;
            }
        }
        return context.getString(R.string.statistics_task_breakdown_not_available);
    }

    @NonNull
    private String resolveFillInTheBlankAnswers(Context context, FillInTheBlank task) {
        List<String> segments = task != null ? task.getMissingSegments() : null;
        if (segments == null || segments.isEmpty()) {
            return context.getString(R.string.statistics_task_breakdown_not_available);
        }

        List<String> cleanedSegments = new ArrayList<>();
        for (String segment : segments) {
            if (!TextUtils.isEmpty(segment)) {
                cleanedSegments.add(segment.trim());
            }
        }
        if (cleanedSegments.isEmpty()) {
            return context.getString(R.string.statistics_task_breakdown_not_available);
        }
        return TextUtils.join(", ", cleanedSegments);
    }

    @NonNull
    private String resolveMatchingPairsAnswer(Context context, MatchingPairTask task) {
        Map<String, String> matches = task != null ? task.getCorrectMatches() : null;
        if (matches == null || matches.isEmpty()) {
            return context.getString(R.string.statistics_task_breakdown_not_available);
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : matches.entrySet()) {
            String left = entry.getKey();
            String right = entry.getValue();
            if (TextUtils.isEmpty(left) || TextUtils.isEmpty(right)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(left.trim()).append(" → ").append(right.trim());
        }

        if (builder.length() == 0) {
            return context.getString(R.string.statistics_task_breakdown_not_available);
        }
        return builder.toString();
    }

    @NonNull
    private String resolveOrderingAnswer(Context context, OrderingTask task) {
        List<Integer> correctOrder = task != null ? task.getCorrectOrder() : null;
        List<String> items = task != null ? task.getItems() : null;
        if (correctOrder == null || correctOrder.isEmpty() || items == null || items.isEmpty()) {
            return context.getString(R.string.statistics_task_breakdown_not_available);
        }

        List<String> orderedItems = new ArrayList<>();
        int itemCount = items.size();
        for (Integer index : correctOrder) {
            if (index == null) {
                continue;
            }
            int position = index;
            if (position >= 0 && position < itemCount) {
                String value = items.get(position);
                if (!TextUtils.isEmpty(value)) {
                    orderedItems.add(value.trim());
                }
            }
        }

        if (orderedItems.isEmpty()) {
            return context.getString(R.string.statistics_task_breakdown_not_available);
        }
        return TextUtils.join(" → ", orderedItems);
    }

    @NonNull
    private String mapLossReason(Context context, LossReason lossReason) {
        if (lossReason == null) {
            return context.getString(R.string.statistics_task_breakdown_reason_full);
        }
        return switch (lossReason) {
            case NONE -> context.getString(R.string.statistics_task_breakdown_reason_full);
            case NOT_ATTEMPTED -> context.getString(R.string.statistics_task_breakdown_reason_not_attempted);
            case INCORRECT -> context.getString(R.string.statistics_task_breakdown_reason_incorrect);
            case PARTIALLY_CORRECT -> context.getString(R.string.statistics_task_breakdown_reason_partial);
            case RAPID_GUESS -> context.getString(R.string.statistics_task_breakdown_reason_rapid_guess);
        };
    }
}