package com.choicecrafter.studentapp.adapters;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.databinding.LearningPathCardBinding;
import com.choicecrafter.studentapp.models.Course;
import com.choicecrafter.studentapp.models.Module;
import com.choicecrafter.studentapp.models.ModuleProgress;
import com.choicecrafter.studentapp.ui.news.LearningPathDetailFragment;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LearningPathCardAdapter extends RecyclerView.Adapter<LearningPathCardAdapter.LearningPathViewHolder> {

    private final List<Course> learningPaths;
    private final String userId;
    private final Map<String, ModuleProgress> userModuleProgress;
    private final Set<String> enrolledLearningPathIds;
    private final OnEnrollClickListener enrollClickListener;

    public LearningPathCardAdapter(List<Course> learningPaths, String userId) {
        this(learningPaths, userId, Collections.emptyMap(), Collections.emptySet(), null);
    }

    public LearningPathCardAdapter(List<Course> learningPaths,
                                   String userId,
                                   Map<String, ModuleProgress> userModuleProgress) {
        this(learningPaths, userId, userModuleProgress, Collections.emptySet(), null);
    }

    public LearningPathCardAdapter(List<Course> learningPaths,
                                   String userId,
                                   Map<String, ModuleProgress> userModuleProgress,
                                   Set<String> enrolledLearningPathIds,
                                   OnEnrollClickListener enrollClickListener) {
        this.learningPaths = learningPaths;
        this.userId = userId;
        this.userModuleProgress = userModuleProgress != null ? userModuleProgress : Collections.emptyMap();
        this.enrolledLearningPathIds = enrolledLearningPathIds != null
                ? new HashSet<>(enrolledLearningPathIds)
                : new HashSet<>();
        this.enrollClickListener = enrollClickListener;
    }

    @NonNull
    @Override
    public LearningPathViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        LearningPathCardBinding binding = LearningPathCardBinding.inflate(inflater, parent, false);
        return new LearningPathViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LearningPathViewHolder holder, int position) {
        Course learningPath = learningPaths.get(position);
        holder.binding.lpTitleTextView.setText(learningPath.getTitle());
        holder.binding.lpDescriptionTextView.setText(learningPath.getDescription());
        int moduleCount = learningPath.getModules().size();
        int activityCount = 0;
        for (Module module : learningPath.getModules()) {
            activityCount += module.getActivities().size();
        }
        holder.binding.lpModulesTextView.setText(String.valueOf(moduleCount));
        holder.binding.lpActivitiesTextView.setText(String.valueOf(activityCount));

        boolean isEnrolled = learningPath.getId() != null && enrolledLearningPathIds.contains(learningPath.getId());

        if (isEnrolled) {
            holder.binding.lpEnrollButton.setOnClickListener(null);
            holder.binding.lpEnrollButton.setVisibility(View.GONE);
            bindProgress(holder, learningPath);
        } else {
            holder.binding.lpProgressContainer.setVisibility(View.GONE);
            holder.binding.lpEnrollButton.setVisibility(View.VISIBLE);
            boolean canEnroll = enrollClickListener != null;
            holder.binding.lpEnrollButton.setEnabled(canEnroll);
            holder.binding.lpEnrollButton.setOnClickListener(canEnroll
                    ? v -> enrollClickListener.onEnrollClicked(learningPath)
                    : null);
        }

        holder.binding.getRoot().setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putParcelable("course", learningPath);
            bundle.putString("userId", userId);
            Navigation.findNavController(v).navigate(R.id.learningPathDetailFragment, bundle);
        });
    }

    @Override
    public int getItemCount() {
        return learningPaths.size();
    }

    static class LearningPathViewHolder extends RecyclerView.ViewHolder {
        private final LearningPathCardBinding binding;

        public LearningPathViewHolder(LearningPathCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private void bindProgress(LearningPathViewHolder holder, Course learningPath) {
        holder.binding.lpProgressContainer.setVisibility(View.VISIBLE);

        int totalTasks = 0;
        int completedTasks = 0;
        if (learningPath.getModules() != null) {
            for (Module module : learningPath.getModules()) {
                int moduleTaskCount = 0;
                if (module.getActivities() != null) {
                    for (com.choicecrafter.studentapp.models.Activity activity : module.getActivities()) {
                        if (activity.getTasks() != null) {
                            moduleTaskCount += activity.getTasks().size();
                        }
                    }
                }
                ModuleProgress progress = userModuleProgress.get(module.getId());
                if (moduleTaskCount == 0 && progress != null) {
                    moduleTaskCount = progress.getTotalTasks();
                }
                totalTasks += moduleTaskCount;
                if (progress != null) {
                    completedTasks += Math.min(progress.getCompletedTasks(),
                            moduleTaskCount > 0 ? moduleTaskCount : progress.getCompletedTasks());
                }
            }
        }

        if (totalTasks == 0 && !userModuleProgress.isEmpty()) {
            for (ModuleProgress progress : userModuleProgress.values()) {
                totalTasks += progress.getTotalTasks();
                completedTasks += progress.getCompletedTasks();
            }
        }

        int percent = totalTasks == 0 ? 0 : (int) Math.round((completedTasks * 100.0) / totalTasks);
        animateProgress(holder.binding.lpProgressBar, percent);
        View view = holder.binding.getRoot();
        holder.binding.lpProgressPercentageTextView.setText(
                view.getContext().getString(R.string.percentage_format, percent));
        holder.binding.lpProgressTextView.setText(
                view.getContext().getString(
                        R.string.learning_path_progress_label_format,
                        completedTasks,
                        totalTasks));

        int remainingChallenges = Math.max(0, totalTasks - completedTasks);
        String hint = remainingChallenges > 0
                ? view.getContext().getString(
                R.string.learning_path_progress_hint_format, remainingChallenges)
                : view.getContext().getString(R.string.learning_path_progress_completed_message);
        holder.binding.lpProgressHintTextView.setText(hint);
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

    public interface OnEnrollClickListener {
        void onEnrollClicked(Course learningPath);
    }
}
