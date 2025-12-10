package com.choicecrafter.students.ui.course_activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.students.R;
import com.choicecrafter.students.adapters.ActivityAdapter;
import com.choicecrafter.students.adapters.ModuleAdapter;
import com.choicecrafter.students.databinding.FragmentCourseActivitiesBinding;
import com.choicecrafter.students.models.Activity;
import com.choicecrafter.students.models.Course;
import com.choicecrafter.students.models.CourseEnrollment;
import com.choicecrafter.students.models.CourseProgress;
import com.choicecrafter.students.models.Module;
import com.choicecrafter.students.models.ModuleProgress;
import com.choicecrafter.students.repositories.CourseEnrollmentRepository;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseActivities extends Fragment {

    private CourseActivitiesViewModel mViewModel;
    private ActivityAdapter activityAdapter;
    private ModuleAdapter moduleAdapter;
    private FragmentCourseActivitiesBinding binding;
    private final CourseEnrollmentRepository enrollmentRepository = new CourseEnrollmentRepository();
    private final Handler highlightHandler = new Handler(Looper.getMainLooper());
    private final Runnable clearHighlightRunnable = () -> {
        if (activityAdapter != null) {
            activityAdapter.clearHighlight();
        }
    };
    private String pendingHighlightActivityId;
    private boolean highlightNavigationTriggered;
    private String courseId;
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_course_activities, container, false);

        mViewModel = new ViewModelProvider(this).get(CourseActivitiesViewModel.class);
        RecyclerView.Adapter<?> adapterForRecyclerView = null;

        if (getArguments() != null) {
            Course course = getArguments().getParcelable("course");
            userId = getArguments().getString("userId");
            courseId = course != null ? course.getId() : null;
            List<Map<String, Object>> activitySnapshots = extractActivitySnapshots(getArguments().getSerializable("activitySnapshots"));
            Map<String, ModuleProgress> moduleProgressMap = extractModuleProgress(getArguments().getSerializable("moduleProgress"));
            String highlightActivityId = getArguments().getString("highlightActivityId");
            if (!TextUtils.isEmpty(highlightActivityId)) {
                pendingHighlightActivityId = highlightActivityId;
                highlightNavigationTriggered = false;
            }
            if (course != null) {
                Log.d("CourseActivities", "Course received: " + course.getTitle());
                Log.d("CourseActivities", "Activities received: " + course.getActivities());
                if (course.getModules() != null && !course.getModules().isEmpty()) {
                    moduleAdapter = new ModuleAdapter(course.getModules(), userId, course.getId(), moduleProgressMap, true);
                    adapterForRecyclerView = moduleAdapter;
                    maybeNavigateToHighlightedModule(course);
                } else {
                    activityAdapter = new ActivityAdapter(course.getActivities(), course.getId(), userId, activitySnapshots);
                    adapterForRecyclerView = activityAdapter;
                    applyPendingHighlightIfReady();
                }
                mViewModel.updateCourse(course);
            } else {
                Log.d("CourseActivities", "Course is null");
            }
        } else {
            Log.d("CourseActivities", "Arguments are null");
        }

        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapterForRecyclerView);

        refreshProgressFromRepository();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel.getCourse().observe(getViewLifecycleOwner(), course -> {
            if (course != null) {
                Log.d("CourseActivities", "Setting title: " + course.getTitle());
                requireActivity().setTitle(course.getTitle());

                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                transaction.setReorderingAllowed(true);
                transaction.commit();
            } else {
                Log.d("CourseActivities", "Course is null in observer");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshProgressFromRepository();

        mViewModel.getCourse().observe(getViewLifecycleOwner(), course -> {
            if (course != null) {
                Log.d("CourseActivities", "Setting title: " + course.getTitle());
                requireActivity().setTitle(course.getTitle());

                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                transaction.setReorderingAllowed(true);
                transaction.commit();
            } else {
                Log.d("CourseActivities", "Course is null in observer");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        highlightHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    private List<Map<String, Object>> extractActivitySnapshots(Serializable serializable) {
        List<Map<String, Object>> snapshots = new ArrayList<>();
        if (serializable instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    snapshots.add((Map<String, Object>) map);
                }
            }
        }
        return snapshots;
    }

    private Map<String, ModuleProgress> extractModuleProgress(Serializable serializable) {
        Map<String, ModuleProgress> moduleProgressMap = new HashMap<>();
        if (serializable instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                Object value = entry.getValue();
                if (value instanceof Map<?, ?> progressValues) {
                    ModuleProgress progress = new ModuleProgress();
                    Object completed = progressValues.get("completedTasks");
                    if (completed instanceof Number number) {
                        progress.setCompletedTasks(number.intValue());
                    }
                    Object total = progressValues.get("totalTasks");
                    if (total instanceof Number number) {
                        progress.setTotalTasks(number.intValue());
                    }
                    moduleProgressMap.put(key, progress);
                }
            }
        }
        return moduleProgressMap;
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
            binding.recyclerView.post(() -> binding.recyclerView.smoothScrollToPosition(position));
        }
        highlightHandler.removeCallbacks(clearHighlightRunnable);
        highlightHandler.postDelayed(clearHighlightRunnable, 3000);
        pendingHighlightActivityId = null;
    }

    private void maybeNavigateToHighlightedModule(Course course) {
        if (course == null || TextUtils.isEmpty(pendingHighlightActivityId) || highlightNavigationTriggered || binding == null) {
            return;
        }
        List<Module> modules = course.getModules();
        if (modules == null || modules.isEmpty()) {
            return;
        }
        for (Module module : modules) {
            if (module == null || module.getActivities() == null) {
                continue;
            }
            for (Activity activity : module.getActivities()) {
                if (matchesActivityIdentifier(activity, pendingHighlightActivityId)) {
                    highlightNavigationTriggered = true;
                    Module moduleToOpen = module;
                    binding.recyclerView.post(() -> {
                        if (!isAdded()) {
                            return;
                        }
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("module", moduleToOpen);
                        bundle.putString("userId", userId);
                        bundle.putString("courseId", courseId);
                        bundle.putString("highlightActivityId", pendingHighlightActivityId);
                        Navigation.findNavController(binding.recyclerView).navigate(R.id.moduleFragment, bundle);
                    });
                    return;
                }
            }
        }
    }

    private boolean matchesActivityIdentifier(Activity activity, String identifier) {
        if (activity == null || identifier == null) {
            return false;
        }
        String trimmed = identifier.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String id = activity.getId();
        if (id != null && id.equals(trimmed)) {
            return true;
        }
        String title = activity.getTitle();
        return title != null && title.equals(trimmed);
    }

    private void refreshProgressFromRepository() {
        if (userId == null || userId.isEmpty() || courseId == null || courseId.isEmpty()) {
            return;
        }
        enrollmentRepository.fetchEnrollmentForCourse(userId, courseId, new CourseEnrollmentRepository.Callback<>() {
            @Override
            public void onSuccess(CourseEnrollment enrollment) {
                if (!isAdded() || enrollment == null) {
                    return;
                }
                CourseProgress progress = enrollment.getProgress();
                Course course = enrollment.getCourse();
                if (course != null) {
                    mViewModel.updateCourse(course);
                    if (moduleAdapter != null && course.getModules() != null) {
                        moduleAdapter.updateModules(course.getModules());
                        maybeNavigateToHighlightedModule(course);
                    }
                    if (activityAdapter != null) {
                        activityAdapter.updateActivities(course.getActivities());
                        applyPendingHighlightIfReady();
                    }
                }
                if (moduleAdapter != null) {
                    Map<String, ModuleProgress> moduleProgress = progress != null ? progress.getModuleProgress() : null;
                    moduleAdapter.updateModuleProgress(moduleProgress);
                }
                if (activityAdapter != null) {
                    List<Map<String, Object>> snapshots = progress != null ? progress.getActivitySnapshots() : null;
                    activityAdapter.updateActivitySnapshots(snapshots);
                    applyPendingHighlightIfReady();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("CourseActivities", "Failed to refresh enrollment progress", e);
            }
        });
    }
}
