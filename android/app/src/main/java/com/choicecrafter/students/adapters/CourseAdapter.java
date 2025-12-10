package com.choicecrafter.students.adapters;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.choicecrafter.students.R;
import com.choicecrafter.students.databinding.CourseCardBinding;
import com.choicecrafter.students.models.Course;
import com.choicecrafter.students.models.CourseEnrollment;
import com.choicecrafter.students.models.CourseProgress;
import com.choicecrafter.students.models.Module;
import com.choicecrafter.students.models.ModuleProgress;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private List<CourseEnrollment> enrollmentList;
    private final Context context;
    private final String userId;
    private String highlightedCourseId;

    public CourseAdapter(List<CourseEnrollment> enrollmentList, String userId, Context context) {
        this.enrollmentList = enrollmentList;
        this.userId = userId;
        this.context = context;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        CourseCardBinding binding = CourseCardBinding.inflate(inflater, parent, false);
        return new CourseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        CourseEnrollment enrollment = enrollmentList.get(position);
        Course course = enrollment.getCourse();
        String imageUrl = course != null ? course.getImageUrl() : null;
        Log.i("CourseAdapter", "Loading course image URL: " + imageUrl);
        ImageView imageView = holder.binding.courseImageView;

        if (imageUrl != null && imageUrl.startsWith("gs://")) {
            try {
                com.google.firebase.storage.StorageReference ref =
                        com.google.firebase.storage.FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> Glide.with(imageView.getContext())
                                .load(uri)
                                .placeholder(R.drawable.course6)
                                .error(R.drawable.course6)
                                .into(imageView))
                        .addOnFailureListener(e -> {
                            Log.w("CourseAdapter", "Failed to get downloadUrl for " + imageUrl, e);
                            imageView.setImageResource(R.drawable.course6);
                        });
            } catch (Exception e) {
                Log.e("CourseAdapter", "Invalid gs:// URL: " + imageUrl, e);
                imageView.setImageResource(R.drawable.course6);
            }
        } else {
            Glide.with(imageView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.course6)
                    .error(R.drawable.course6)
                    .into(imageView);
        }

        if (course != null) {
            holder.binding.courseTitleTextView.setText(course.getTitle());
            holder.binding.teacherNameTextView.setText(course.getTeacher());
        } else {
            holder.binding.courseTitleTextView.setText(R.string.loading_placeholder);
            holder.binding.teacherNameTextView.setText("");
        }

        String enrollmentDate = enrollment.getEnrollmentDate();
        if (enrollmentDate == null || enrollmentDate.isEmpty()) {
            holder.binding.courseEnrollmentDateTextView.setText(R.string.course_enrollment_date_unknown);
        } else {
            holder.binding.courseEnrollmentDateTextView.setText(
                    context.getString(R.string.course_enrollment_date_format, enrollmentDate));
        }

        String enrollmentSource = enrollment.isSelfEnrolled()
                ? context.getString(R.string.course_enrollment_source_self)
                : context.getString(R.string.course_enrollment_source_teacher,
                enrollment.getEnrolledBy() == null || enrollment.getEnrolledBy().isEmpty()
                        ? context.getString(R.string.course_enrollment_unknown_teacher)
                        : enrollment.getEnrolledBy());
        holder.binding.courseEnrollmentSourceTextView.setText(enrollmentSource);

        CourseProgress progress = enrollment.getProgress();
        List<Module> modules = course != null ? course.getModules() : new ArrayList<>();
        boolean hasModules = modules != null && !modules.isEmpty();

        if (progress != null) {
            holder.binding.courseProgressTasksTextView.setText(
                    context.getString(R.string.course_progress_tasks_format,
                            progress.getCompletedTasks(),
                            progress.getTotalTasks(),
                            Math.round(progress.getCompletionPercentage())));
            if (hasModules) {
                Map<String, ModuleProgress> moduleProgressMap =
                        progress.getModuleProgress() != null ? progress.getModuleProgress() : new HashMap<>();
                int totalModules = modules.size();
                int startedModules = 0;
                for (Module module : modules) {
                    if (module == null) {
                        continue;
                    }
                    ModuleProgress moduleProgress = moduleProgressMap.get(module.getId());
                    if (moduleProgress != null) {
                        startedModules++;
                    }
                }
                holder.binding.courseProgressActivitiesTextView.setText(
                        context.getString(R.string.course_progress_modules_format,
                                startedModules,
                                totalModules));
            } else {
                holder.binding.courseProgressActivitiesTextView.setText(
                        context.getString(R.string.course_progress_activities_format,
                                progress.getActivitiesStarted(),
                                progress.getTotalActivities()));
            }
            holder.binding.courseProgressContainer.setVisibility(View.VISIBLE);

            int completionPercent = (int) Math.round(progress.getCompletionPercentage());
            animateProgress(holder.binding.courseProgressBar, completionPercent);
            holder.binding.courseProgressPercentageTextView.setText(
                    context.getString(R.string.percentage_format, completionPercent));

            int tasksRemaining = Math.max(0, progress.getTotalTasks() - progress.getCompletedTasks());
            int earnedXp = Math.max(0, progress.getEarnedXp());
            int totalXp = Math.max(0, progress.getTotalXp());
            String xpSummary = totalXp > 0
                    ? context.getString(R.string.course_progress_xp_summary_format, earnedXp, totalXp)
                    : context.getString(R.string.course_progress_xp_format, earnedXp);
            if (tasksRemaining > 0) {
                String hint = context.getString(R.string.course_progress_hint_format, tasksRemaining);
                xpSummary = context.getString(R.string.course_progress_xp_with_hint_format, xpSummary, hint);
            }
            holder.binding.courseProgressXpTextView.setText(xpSummary);
        } else {
            holder.binding.courseProgressTasksTextView.setText(R.string.course_progress_unavailable);
            if (hasModules) {
                holder.binding.courseProgressActivitiesTextView.setText(
                        context.getString(R.string.course_progress_modules_format, 0, modules.size()));
            } else {
                holder.binding.courseProgressActivitiesTextView.setText("");
            }
            holder.binding.courseProgressContainer.setVisibility(View.GONE);
            holder.binding.courseProgressBar.setProgress(0);
            holder.binding.courseProgressPercentageTextView.setText(R.string.percentage_placeholder);
            holder.binding.courseProgressXpTextView.setText(R.string.course_progress_xp_placeholder);
        }

        ArrayList<HashMap<String, Object>> activitySnapshots = toSerializableSnapshots(progress);
        HashMap<String, HashMap<String, Integer>> moduleProgressMap = toSerializableModuleProgress(progress);

        holder.binding.viewButton.setText(context.getString(R.string.action_view));

        holder.binding.viewButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putParcelable("course", course);
            bundle.putString("userId", userId);
            if (!activitySnapshots.isEmpty()) {
                bundle.putSerializable("activitySnapshots", activitySnapshots);
            }
            if (!moduleProgressMap.isEmpty()) {
                bundle.putSerializable("moduleProgress", moduleProgressMap);
            }
            Navigation.findNavController(v).navigate(R.id.courseActivities, bundle);
        });

        MaterialCardView cardView = holder.binding.materialCard;
        boolean shouldHighlight = highlightedCourseId != null
                && highlightedCourseId.equals(enrollment.getCourseId());
        int highlightColor = ContextCompat.getColor(context, R.color.course_highlight);
        int transparent = ContextCompat.getColor(context, R.color.transparent);
        int highlightStrokeWidth = Math.round(context.getResources().getDimension(R.dimen.course_highlight_stroke_width));
        cardView.setStrokeColor(shouldHighlight ? highlightColor : transparent);
        cardView.setStrokeWidth(shouldHighlight ? highlightStrokeWidth : 0);
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

    private ArrayList<HashMap<String, Object>> toSerializableSnapshots(CourseProgress progress) {
        ArrayList<HashMap<String, Object>> snapshots = new ArrayList<>();
        if (progress == null || progress.getActivitySnapshots() == null) {
            return snapshots;
        }
        for (Map<String, Object> snapshot : progress.getActivitySnapshots()) {
            if (snapshot != null) {
                snapshots.add(new HashMap<>(snapshot));
            }
        }
        return snapshots;
    }

    private HashMap<String, HashMap<String, Integer>> toSerializableModuleProgress(CourseProgress progress) {
        HashMap<String, HashMap<String, Integer>> serialized = new HashMap<>();
        if (progress == null || progress.getModuleProgress() == null) {
            return serialized;
        }
        for (Map.Entry<String, ModuleProgress> entry : progress.getModuleProgress().entrySet()) {
            String key = entry.getKey();
            ModuleProgress moduleProgress = entry.getValue();
            if (key == null || moduleProgress == null) {
                continue;
            }
            HashMap<String, Integer> values = new HashMap<>();
            values.put("completedTasks", moduleProgress.getCompletedTasks());
            values.put("totalTasks", moduleProgress.getTotalTasks());
            serialized.put(key, values);
        }
        return serialized;
    }

    public void updateEnrollments(List<CourseEnrollment> newEnrollments) {
        enrollmentList = newEnrollments;
        notifyDataSetChanged();
    }

    public void highlightCourse(String courseId) {
        highlightedCourseId = courseId;
        notifyDataSetChanged();
    }

    public void clearHighlight() {
        if (highlightedCourseId != null) {
            highlightedCourseId = null;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return enrollmentList.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        private final CourseCardBinding binding;

        public CourseViewHolder(CourseCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
