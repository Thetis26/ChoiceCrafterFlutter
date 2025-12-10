package com.choicecrafter.students.ui.news;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.choicecrafter.students.R;
import com.choicecrafter.students.adapters.ActivityAdapter;
import com.choicecrafter.students.databinding.FragmentModuleBinding;
import com.choicecrafter.students.models.Activity;
import com.choicecrafter.students.models.Course;
import com.choicecrafter.students.models.CourseEnrollment;
import com.choicecrafter.students.models.CourseProgress;
import com.choicecrafter.students.models.Module;
import com.choicecrafter.students.repositories.CourseEnrollmentRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModuleFragment extends Fragment {

    private FragmentModuleBinding binding;
    private ActivityAdapter activityAdapter;
    private Module module;
    private String userId;
    private String courseId;
    private final CourseEnrollmentRepository enrollmentRepository = new CourseEnrollmentRepository();
    private final Handler highlightHandler = new Handler(Looper.getMainLooper());
    private final Runnable clearHighlightRunnable = () -> {
        if (activityAdapter != null) {
            activityAdapter.clearHighlight();
        }
    };
    private String pendingHighlightActivityId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentModuleBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            module = getArguments().getParcelable("module");
            userId = getArguments().getString("userId");
            courseId = getArguments().getString("courseId");
            String highlightActivityId = getArguments().getString("highlightActivityId");
            if (!TextUtils.isEmpty(highlightActivityId)) {
                pendingHighlightActivityId = highlightActivityId;
            }
            if (module != null) {
                binding.moduleTitle.setText(module.getTitle());
                binding.moduleDescription.setText(module.getDescription());
                binding.activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                String targetCourseId = !TextUtils.isEmpty(courseId)
                        ? courseId
                        : module.getCourseId();
                activityAdapter = new ActivityAdapter(module.getActivities(), targetCourseId, userId);
                binding.activitiesRecyclerView.setAdapter(activityAdapter);
                refreshActivityProgress();
                applyPendingHighlightIfReady();
            }
        }
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        highlightHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshActivityProgress();
    }

    private void refreshActivityProgress() {
        if (activityAdapter == null || TextUtils.isEmpty(userId) || TextUtils.isEmpty(courseId)) {
            return;
        }
        enrollmentRepository.fetchEnrollmentForCourse(userId, courseId, new CourseEnrollmentRepository.Callback<>() {
            @Override
            public void onSuccess(CourseEnrollment enrollment) {
                if (!isAdded() || enrollment == null) {
                    return;
                }
                CourseProgress progress = enrollment.getProgress();
                if (enrollment.getCourse() != null) {
                    Module updatedModule = findUpdatedModule(enrollment.getCourse());
                    if (updatedModule != null) {
                        module = updatedModule;
                        activityAdapter.updateActivities(module.getActivities());
                    }
                }
                if (progress == null) {
                    return;
                }
                List<Map<String, Object>> snapshots = filterSnapshotsForModule(progress.getActivitySnapshots());
                activityAdapter.updateActivitySnapshots(snapshots);
                applyPendingHighlightIfReady();
            }

            @Override
            public void onFailure(Exception e) {
                // No-op: we simply keep the existing state when progress fails to load.
            }
        });
    }

    private List<Map<String, Object>> filterSnapshotsForModule(List<Map<String, Object>> snapshots) {
        if (snapshots == null || module == null || module.getActivities() == null || module.getActivities().isEmpty()) {
            return snapshots;
        }
        Set<String> allowedIdentifiers = new HashSet<>();
        for (Activity activity : module.getActivities()) {
            if (activity == null) {
                continue;
            }
            if (!TextUtils.isEmpty(activity.getId())) {
                allowedIdentifiers.add(activity.getId());
            }
            if (!TextUtils.isEmpty(activity.getTitle())) {
                allowedIdentifiers.add(activity.getTitle());
            }
        }
        if (allowedIdentifiers.isEmpty()) {
            return snapshots;
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> snapshot : snapshots) {
            if (snapshot == null) {
                continue;
            }
            if (matchesIdentifier(allowedIdentifiers, snapshot.get("activityId"))
                    || matchesIdentifier(allowedIdentifiers, snapshot.get("activityTitle"))
                    || matchesIdentifier(allowedIdentifiers, snapshot.get("title"))) {
                filtered.add(snapshot);
            }
        }
        return filtered;
    }

    private boolean matchesIdentifier(Set<String> allowedIdentifiers, Object candidate) {
        if (candidate == null) {
            return false;
        }
        String value = String.valueOf(candidate).trim();
        return !value.isEmpty() && allowedIdentifiers.contains(value);
    }

    private void applyPendingHighlightIfReady() {
        if (activityAdapter == null || TextUtils.isEmpty(pendingHighlightActivityId) || binding == null) {
            return;
        }
        boolean applied = activityAdapter.highlightActivity(pendingHighlightActivityId);
        if (!applied) {
            return;
        }
        int position = activityAdapter.findPositionForActivity(pendingHighlightActivityId);
        if (position >= 0) {
            binding.activitiesRecyclerView.post(() -> binding.activitiesRecyclerView.smoothScrollToPosition(position));
        }
        highlightHandler.removeCallbacks(clearHighlightRunnable);
        highlightHandler.postDelayed(clearHighlightRunnable, 3000);
        pendingHighlightActivityId = null;
    }

    private Module findUpdatedModule(Course course) {
        if (course == null || module == null) {
            return null;
        }
        List<Module> modules = course.getModules();
        if (modules == null || modules.isEmpty()) {
            return null;
        }
        String targetId = module.getId();
        String targetTitle = module.getTitle();
        for (Module candidate : modules) {
            if (candidate == null) {
                continue;
            }
            if (!TextUtils.isEmpty(targetId) && targetId.equals(candidate.getId())) {
                return candidate;
            }
            if (!TextUtils.isEmpty(targetTitle) && targetTitle.equals(candidate.getTitle())) {
                return candidate;
            }
        }
        return null;
    }
}
