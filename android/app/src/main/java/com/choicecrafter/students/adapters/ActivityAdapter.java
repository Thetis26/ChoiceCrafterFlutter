package com.choicecrafter.students.adapters;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.students.R;
import com.choicecrafter.students.databinding.ActivityCardBinding;
import com.choicecrafter.students.models.Activity;
import com.choicecrafter.students.models.tasks.Task;
import com.choicecrafter.students.models.TaskStats;
import com.choicecrafter.students.ui.activity.ActivityFragment;
import com.choicecrafter.students.utils.ActivityScoreCalculator;
import com.choicecrafter.students.utils.ActivityVisibilityFilter;
import com.choicecrafter.students.utils.TaskStatsKeyUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private final List<Activity> activitiesList;
    private final String courseId;
    private final String userId;
    private final Map<String, Map<String, Object>> activityProgressSnapshots;
    private String highlightedActivityKey;

    public ActivityAdapter(List<Activity> activitiesList, String courseId, String userId) {
        this(activitiesList, courseId, userId, null);
    }

    public ActivityAdapter(List<Activity> activitiesList,
                           String courseId,
                           String userId,
                           List<Map<String, Object>> activitySnapshots) {
        this.activitiesList = new ArrayList<>();
        if (activitiesList != null) {
            this.activitiesList.addAll(ActivityVisibilityFilter.filterVisible(activitiesList));
        }
        this.courseId = courseId;
        this.userId = userId;
        this.activityProgressSnapshots = new HashMap<>();
        if (activitySnapshots != null) {
            for (Map<String, Object> snapshot : activitySnapshots) {
                if (snapshot == null) {
                    continue;
                }
                indexSnapshotByKeys(snapshot);
            }
        }
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ActivityCardBinding binding = ActivityCardBinding.inflate(inflater, parent, false);
        return new ActivityViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        Activity activityItem = activitiesList.get(position);
        holder.binding.activityTitleTextView.setText(activityItem.getTitle());
        holder.binding.activityDescriptionTextView.setText(activityItem.getDescription());
        int taskCount = activityItem.getTasks() != null ? activityItem.getTasks().size() : 0;
        holder.binding.activityTasksTextView.setText(String.valueOf(taskCount));
        holder.binding.activityReactionsTextView.setText(String.valueOf(activityItem.getReactions()));
        holder.binding.activityCommentsTextView.setText(String.valueOf(activityItem.getComments().size()));

        applyHighlightState(holder.binding.materialCard, activityItem);

        bindProgress(holder, activityItem);

        holder.binding.getRoot().setOnClickListener(v -> {
            if (isActivityCompleted(activityItem)) {
                showActivityCompletedDialog(holder.binding.getRoot(), activityItem);
            } else {
                Navigation.findNavController(v).navigate(R.id.activityFragment, createNavigationBundle(activityItem));
            }
        });
    }

    @Override
    public int getItemCount() {
        return activitiesList.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final ActivityCardBinding binding;

        public ActivityViewHolder(ActivityCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private void bindProgress(ActivityViewHolder holder, Activity activityItem) {
        if (activityItem.getTasks() == null || activityItem.getTasks().isEmpty()) {
            holder.binding.activityProgressContainer.setVisibility(View.GONE);
            holder.binding.activityProgressBar.setProgress(0);
            holder.binding.activityProgressTextView.setText(R.string.activity_progress_label_placeholder);
            holder.binding.activityProgressHintTextView.setText(R.string.activity_progress_hint_placeholder);
            return;
        }

        holder.binding.activityProgressContainer.setVisibility(View.VISIBLE);

        int totalTasks = activityItem.getTasks().size();
        Map<String, Object> progressSnapshot = findProgressSnapshot(activityItem);
        Map<String, TaskStats> statsMap = extractTaskStats(progressSnapshot);
        int completedTasks = 0;
        for (Task task : activityItem.getTasks()) {
            TaskStats stats = TaskStatsKeyUtils.findStatsForTask(statsMap, task);
            if (stats != null && stats.resolveCompletionRatio() > 0) {
                completedTasks++;
            }
        }

        int percent = totalTasks == 0 ? 0 : (int) Math.round((completedTasks * 100.0) / totalTasks);
        animateProgress(holder.binding.activityProgressBar, percent);
        Context context = holder.binding.getRoot().getContext();
        holder.binding.activityProgressPercentageTextView.setText(
                context.getString(R.string.percentage_format, percent));
        holder.binding.activityProgressTextView.setText(
                context.getString(
                        R.string.activity_progress_label_format,
                        completedTasks,
                        totalTasks));

        int tasksRemaining = Math.max(0, totalTasks - completedTasks);
        String hintText = tasksRemaining > 0
                ? context.getString(R.string.activity_progress_hint_format, tasksRemaining)
                : context.getString(R.string.activity_progress_completed_text);
        holder.binding.activityProgressHintTextView.setText(hintText);
    }

    private void animateProgress(ProgressBar progressBar, int target) {
        if (progressBar == null) {
            return;
        }
        int clampedTarget = Math.max(0, Math.min(target, progressBar.getMax()));
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), clampedTarget);
        animator.setDuration(600);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private void showActivityCompletedDialog(View anchorView, Activity activityItem) {
        Context context = anchorView.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_activity_status, null, false);

        AppCompatImageView iconView = dialogView.findViewById(R.id.dialog_icon);
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        MaterialButton primaryButton = dialogView.findViewById(R.id.dialog_primary_button);
        MaterialButton secondaryButton = dialogView.findViewById(R.id.dialog_secondary_button);
        MaterialButton tertiaryButton = dialogView.findViewById(R.id.dialog_tertiary_button);

        iconView.setImageResource(R.drawable.trophy_award);
        iconView.setContentDescription(context.getString(R.string.activity_retry_dialog_title));
        titleView.setText(R.string.activity_retry_dialog_title);
        messageView.setText(buildCompletionSummary(activityItem, context));
        primaryButton.setText(R.string.activity_retry_dialog_retry);
        secondaryButton.setText(R.string.activity_retry_dialog_cancel);
        tertiaryButton.setText(R.string.activity_retry_dialog_statistics);
        tertiaryButton.setVisibility(View.VISIBLE);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        primaryButton.setOnClickListener(v -> {
            dialog.dismiss();
            Bundle retryBundle = createNavigationBundle(activityItem);
            retryBundle.putBoolean("retry", true);
            Navigation.findNavController(anchorView).navigate(R.id.activityFragment, retryBundle);
        });

        secondaryButton.setOnClickListener(v -> dialog.dismiss());

        tertiaryButton.setOnClickListener(v -> {
            dialog.dismiss();
            Bundle viewBundle = createNavigationBundle(activityItem);
            viewBundle.putBoolean(ActivityFragment.ARG_SHOW_STATISTICS, true);
            Navigation.findNavController(anchorView).navigate(R.id.activityFragment, viewBundle);
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void applyHighlightState(MaterialCardView cardView, Activity activity) {
        if (cardView == null) {
            return;
        }
        boolean shouldHighlight = isHighlighted(activity);
        int highlightColor = ContextCompat.getColor(cardView.getContext(), R.color.course_highlight);
        int transparent = ContextCompat.getColor(cardView.getContext(), R.color.transparent);
        int strokeWidth = Math.round(cardView.getResources().getDimension(R.dimen.course_highlight_stroke_width));
        cardView.setStrokeColor(shouldHighlight ? highlightColor : transparent);
        cardView.setStrokeWidth(shouldHighlight ? strokeWidth : 0);
    }

    private Bundle createNavigationBundle(Activity activityItem) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("activity", activityItem);
        bundle.putString("courseId", courseId);
        bundle.putString("userId", userId);
        return bundle;
    }

    private boolean isActivityCompleted(Activity activityItem) {
        if (activityItem.getTasks() == null || activityItem.getTasks().isEmpty()) {
            return false;
        }
        Map<String, TaskStats> statsMap = extractTaskStats(findProgressSnapshot(activityItem));
        if (statsMap.isEmpty()) {
            return false;
        }
        for (Task task : activityItem.getTasks()) {
            TaskStats stats = TaskStatsKeyUtils.findStatsForTask(statsMap, task);
            if (stats == null || !stats.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> findProgressSnapshot(Activity activityItem) {
        if (activityItem == null) {
            return null;
        }
        String key = resolveActivityKey(activityItem);
        if (key != null) {
            Map<String, Object> snapshot = activityProgressSnapshots.get(key);
            if (snapshot != null) {
                return snapshot;
            }
        }
        String title = activityItem.getTitle();
        if (title != null && !title.isEmpty()) {
            return activityProgressSnapshots.get(title);
        }
        return null;
    }

    private void indexSnapshotByKeys(Map<String, Object> snapshot) {
        addSnapshotIndex(snapshot, "activityId");
        addSnapshotIndex(snapshot, "activityTitle");
        addSnapshotIndex(snapshot, "title");
    }

    private void addSnapshotIndex(Map<String, Object> snapshot, String keyName) {
        Object value = snapshot.get(keyName);
        if (value == null) {
            return;
        }
        String key = String.valueOf(value);
        if (!key.trim().isEmpty()) {
            activityProgressSnapshots.put(key, snapshot);
        }
    }

    public void updateActivitySnapshots(List<Map<String, Object>> snapshots) {
        activityProgressSnapshots.clear();
        if (snapshots != null) {
            for (Map<String, Object> snapshot : snapshots) {
                if (snapshot == null) {
                    continue;
                }
                Map<String, Object> normalized = new HashMap<>();
                for (Map.Entry<?, ?> entry : snapshot.entrySet()) {
                    normalized.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                indexSnapshotByKeys(normalized);
            }
        }
        notifyDataSetChanged();
    }

    public void updateActivities(List<Activity> activities) {
        activitiesList.clear();
        if (activities != null) {
            activitiesList.addAll(ActivityVisibilityFilter.filterVisible(activities));
        }
        notifyDataSetChanged();
    }

    public boolean highlightActivity(String activityId) {
        if (activityId == null || activityId.trim().isEmpty()) {
            return false;
        }
        String normalized = activityId.trim();
        for (Activity activity : activitiesList) {
            if (matchesActivityKey(activity, normalized)) {
                highlightedActivityKey = resolveActivityKey(activity);
                notifyDataSetChanged();
                return true;
            }
        }
        return false;
    }

    public void clearHighlight() {
        if (highlightedActivityKey != null) {
            highlightedActivityKey = null;
            notifyDataSetChanged();
        }
    }

    public int findPositionForActivity(String activityId) {
        if (activityId == null || activityId.trim().isEmpty()) {
            return -1;
        }
        String normalized = activityId.trim();
        for (int i = 0; i < activitiesList.size(); i++) {
            if (matchesActivityKey(activitiesList.get(i), normalized)) {
                return i;
            }
        }
        return -1;
    }

    private boolean matchesActivityKey(Activity activity, String candidateKey) {
        if (activity == null || candidateKey == null || candidateKey.isEmpty()) {
            return false;
        }
        String key = resolveActivityKey(activity);
        if (key != null && key.equals(candidateKey)) {
            return true;
        }
        String title = activity.getTitle();
        return title != null && title.equals(candidateKey);
    }

    private boolean isHighlighted(Activity activity) {
        if (highlightedActivityKey == null) {
            return false;
        }
        String key = resolveActivityKey(activity);
        return key != null && key.equals(highlightedActivityKey);
    }

    private String resolveActivityKey(Activity activity) {
        if (activity == null) {
            return null;
        }
        String id = activity.getId();
        if (id != null && !id.trim().isEmpty()) {
            return id;
        }
        String title = activity.getTitle();
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        return null;
    }

    private Map<String, TaskStats> extractTaskStats(Map<String, Object> progressSnapshot) {
        Map<String, TaskStats> result = new HashMap<>();
        if (progressSnapshot == null) {
            return result;
        }
        Object taskStatsObj = progressSnapshot.get("taskStats");
        if (taskStatsObj instanceof Map<?, ?> taskStatsMap) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) taskStatsMap).entrySet()) {
                TaskStats stats = mapToTaskStats(entry.getValue());
                if (stats != null) {
                    result.put(String.valueOf(entry.getKey()), stats);
                }
            }
        }
        return result;
    }

    private TaskStats mapToTaskStats(Object value) {
        if (value instanceof TaskStats taskStats) {
            return taskStats;
        }
        if (value instanceof Map<?, ?> statsMap) {
            TaskStats taskStats = new TaskStats();
            Object timeSpentObj = ((Map<?, ?>) statsMap).get("timeSpent");
            if (timeSpentObj != null) {
                taskStats.setTimeSpent(String.valueOf(timeSpentObj));
            }
            Object retriesObj = ((Map<?, ?>) statsMap).get("retries");
            if (retriesObj instanceof Number) {
                taskStats.setRetries(((Number) retriesObj).intValue());
            } else if (retriesObj instanceof String) {
                try {
                    taskStats.setRetries(Integer.parseInt((String) retriesObj));
                } catch (NumberFormatException ignored) {
                }
            }
            Object successObj = ((Map<?, ?>) statsMap).get("success");
            if (successObj != null) {
                taskStats.setSuccess(parseBoolean(successObj));
            }
            Object hintsObj = ((Map<?, ?>) statsMap).get("hintsUsed");
            if (hintsObj != null) {
                taskStats.setHintsUsed(parseBoolean(hintsObj));
            }
            Object dateTimeObj = ((Map<?, ?>) statsMap).get("attemptDateTime");
            if (dateTimeObj != null) {
                taskStats.setAttemptDateTime(String.valueOf(dateTimeObj));
            }
            Object completionRatioObj = ((Map<?, ?>) statsMap).get("completionRatio");
            if (completionRatioObj instanceof Number number) {
                taskStats.setCompletionRatio(number.doubleValue());
            } else if (completionRatioObj instanceof String ratioString) {
                try {
                    taskStats.setCompletionRatio(Double.parseDouble(ratioString));
                } catch (NumberFormatException ignored) {
                }
            }
            Object scoreRatioObj = ((Map<?, ?>) statsMap).get("scoreRatio");
            if (scoreRatioObj instanceof Number number) {
                taskStats.setScoreRatio(number.doubleValue());
            } else if (scoreRatioObj instanceof String ratioString) {
                try {
                    taskStats.setScoreRatio(Double.parseDouble(ratioString));
                } catch (NumberFormatException ignored) {
                }
            }
            return taskStats;
        }
        return null;
    }

    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private int extractHighestScore(Map<String, Object> progressSnapshot) {
        if (progressSnapshot == null) {
            return 0;
        }
        Object highestScoreObj = progressSnapshot.get("highestScore");
        if (highestScoreObj instanceof Number) {
            return ((Number) highestScoreObj).intValue();
        }
        if (highestScoreObj instanceof String) {
            try {
                return Integer.parseInt((String) highestScoreObj);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private String buildCompletionSummary(Activity activityItem, Context context) {
        Map<String, Object> progressSnapshot = activityProgressSnapshots.get(activityItem.getTitle());
        Map<String, TaskStats> statsMap = extractTaskStats(progressSnapshot);
        int totalSeconds = 0;
        boolean hintsUsed = false;

        if (activityItem.getTasks() != null) {
            for (Task task : activityItem.getTasks()) {
                TaskStats stats = TaskStatsKeyUtils.findStatsForTask(statsMap, task);
                if (stats == null) {
                    continue;
                }
                String timeSpent = stats.getTimeSpent();
                if (timeSpent != null && timeSpent.contains(":")) {
                    String[] parts = timeSpent.split(":");
                    try {
                        int minutes = Integer.parseInt(parts[0]);
                        int seconds = Integer.parseInt(parts[1]);
                        totalSeconds += minutes * 60 + seconds;
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (!hintsUsed && Boolean.TRUE.equals(stats.getHintsUsed())) {
                    hintsUsed = true;
                }
            }
        }

        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        List<Task> tasks = activityItem.getTasks();
        int earnedXp = ActivityScoreCalculator.calculateEarnedXp(tasks, statsMap);
        int totalXp = ActivityScoreCalculator.calculateTotalXp(tasks, statsMap);
        int highestScore = extractHighestScore(progressSnapshot);
        if (highestScore > earnedXp) {
            earnedXp = highestScore;
        }
        String hintsLabel = context.getString(hintsUsed ? R.string.statistics_card_hints_used_yes : R.string.statistics_card_hints_used_no);
        return context.getString(R.string.activity_retry_dialog_message, earnedXp, timeFormatted, hintsLabel, totalXp);
    }
}
