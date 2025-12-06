package com.choicecrafter.studentapp.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.databinding.NewsCourseCardBinding;
import com.choicecrafter.studentapp.models.Course;
import com.choicecrafter.studentapp.models.Module;
import com.google.android.material.button.MaterialButton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewsCourseAdapter extends RecyclerView.Adapter<NewsCourseAdapter.CourseViewHolder> {

    private final List<Course> courses;
    private final Set<String> enrolledCourseIds;
    private final OnEnrollClickListener enrollClickListener;
    private final OnCourseDetailClickListener detailClickListener;

    public NewsCourseAdapter(List<Course> courses,
                             Set<String> enrolledCourseIds,
                             OnEnrollClickListener enrollClickListener,
                             OnCourseDetailClickListener detailClickListener) {
        this.courses = courses;
        this.enrolledCourseIds = enrolledCourseIds != null ? new HashSet<>(enrolledCourseIds) : new HashSet<>();
        this.enrollClickListener = enrollClickListener;
        this.detailClickListener = detailClickListener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        NewsCourseCardBinding binding = NewsCourseCardBinding.inflate(inflater, parent, false);
        return new CourseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.binding.courseTitleTextView.setText(!TextUtils.isEmpty(course.getTitle())
                ? course.getTitle()
                : holder.itemView.getContext().getString(R.string.loading_placeholder));

        if (!TextUtils.isEmpty(course.getDescription())) {
            holder.binding.courseDescriptionTextView.setText(course.getDescription());
            holder.binding.courseDescriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.binding.courseDescriptionTextView.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(course.getTeacher())) {
            holder.binding.courseTeacherTextView.setText(
                    holder.itemView.getContext().getString(R.string.course_teacher_label, course.getTeacher()));
            holder.binding.courseTeacherTextView.setVisibility(View.VISIBLE);
        } else {
            holder.binding.courseTeacherTextView.setVisibility(View.GONE);
        }

        int moduleCount = 0;
        int activityCount = 0;
        if (course.getModules() != null && !course.getModules().isEmpty()) {
            moduleCount = course.getModules().size();
            for (Module module : course.getModules()) {
                if (module != null && module.getActivities() != null) {
                    activityCount += module.getActivities().size();
                }
            }
        } else if (course.getActivities() != null) {
            activityCount = course.getActivities().size();
        }

        if (moduleCount > 0) {
            holder.binding.courseModulesTextView.setText(
                    holder.itemView.getContext().getString(R.string.course_summary_modules_format, moduleCount));
            holder.binding.courseModulesTextView.setVisibility(View.VISIBLE);
        } else {
            holder.binding.courseModulesTextView.setVisibility(View.GONE);
        }

        if (activityCount > 0) {
            holder.binding.courseActivitiesTextView.setText(
                    holder.itemView.getContext().getString(R.string.course_summary_activities_format, activityCount));
            holder.binding.courseActivitiesTextView.setVisibility(View.VISIBLE);
        } else {
            holder.binding.courseActivitiesTextView.setVisibility(View.GONE);
        }

        boolean isEnrolled = course.getId() != null && enrolledCourseIds.contains(course.getId());
        Button enrollButton = holder.binding.enrollButton;
        if (isEnrolled) {
            enrollButton.setText(R.string.course_enrolled_label);
            enrollButton.setEnabled(false);
            enrollButton.setOnClickListener(null);
        } else {
            enrollButton.setText(R.string.course_enroll_button);
            boolean canEnroll = enrollClickListener != null;
            enrollButton.setEnabled(canEnroll);
            enrollButton.setOnClickListener(canEnroll ? v -> enrollClickListener.onEnrollClicked(course) : null);
        }

        MaterialButton viewDetailsButton = holder.binding.viewDetailsButton;
        boolean canShowDetails = detailClickListener != null;
        viewDetailsButton.setEnabled(canShowDetails);
        viewDetailsButton.setOnClickListener(canShowDetails ? v -> detailClickListener.onCourseDetailsClicked(course) : null);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        private final NewsCourseCardBinding binding;

        CourseViewHolder(NewsCourseCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnEnrollClickListener {
        void onEnrollClicked(Course course);
    }

    public interface OnCourseDetailClickListener {
        void onCourseDetailsClicked(Course course);
    }
}
