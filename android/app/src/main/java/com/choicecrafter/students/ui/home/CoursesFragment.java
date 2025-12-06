package com.choicecrafter.studentapp.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.MainViewModel;
import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.adapters.CourseAdapter;
import com.choicecrafter.studentapp.databinding.FragmentCoursesBinding;
import com.choicecrafter.studentapp.models.CourseEnrollment;
import com.choicecrafter.studentapp.repositories.CourseEnrollmentRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class CoursesFragment extends Fragment {

    private ProgressBar progressBar;
    private CourseAdapter adapter;
    private final CourseEnrollmentRepository enrollmentRepository = new CourseEnrollmentRepository();

    private final List<CourseEnrollment> enrollments = new ArrayList<>();
    private String userId = FirebaseAuth.getInstance().getCurrentUser().getEmail();

    private FragmentCoursesBinding binding;
    private MainViewModel mainViewModel;
    private final Handler highlightHandler = new Handler(Looper.getMainLooper());
    private Runnable clearHighlightRunnable;
    private String pendingHighlightCourseId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCoursesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        progressBar = binding.progressBar;

        Log.i("In Home Screen", "User ID: " + userId);
        adapter = new CourseAdapter(enrollments, userId, this.getContext());
        adapter.updateEnrollments(enrollments);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
        observeEnrollmentHighlights();
        fetchEnrollments();

        return root;
    }

    private void fetchEnrollments() {
        Log.i("In Home Screen", "Fetching enrollments...");
        progressBar.setVisibility(View.VISIBLE);
        enrollmentRepository.fetchEnrollmentsForUser(userId, new CourseEnrollmentRepository.Callback<>() {
            @Override
            public void onSuccess(List<CourseEnrollment> result) {
                enrollments.clear();
                enrollments.addAll(result);
                adapter.updateEnrollments(enrollments);
                progressBar.setVisibility(View.GONE);
                Log.i("In Home Screen", "Enrollments fetched: " + enrollments);
                maybeHighlightPendingCourse();
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Log.e("In Home Screen", "Error fetching enrollments: ", e);
            }
        });
    }

    private void observeEnrollmentHighlights() {
        if (mainViewModel == null) {
            return;
        }
        mainViewModel.getNewlyEnrolledCourseId().observe(getViewLifecycleOwner(), courseId -> {
            if (courseId == null || courseId.isEmpty()) {
                return;
            }
            pendingHighlightCourseId = courseId;
            maybeHighlightPendingCourse();
        });
    }

    private void maybeHighlightPendingCourse() {
        if (pendingHighlightCourseId == null || adapter == null || enrollments.isEmpty() || binding == null) {
            return;
        }
        int position = findEnrollmentPosition(pendingHighlightCourseId);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        adapter.highlightCourse(pendingHighlightCourseId);
        binding.recyclerView.post(() -> binding.recyclerView.smoothScrollToPosition(position));
        if (clearHighlightRunnable != null) {
            highlightHandler.removeCallbacks(clearHighlightRunnable);
        }
        clearHighlightRunnable = () -> {
            adapter.clearHighlight();
        };
        highlightHandler.postDelayed(clearHighlightRunnable, 5000);
        if (mainViewModel != null) {
            mainViewModel.clearNewlyEnrolledCourseId();
        }
        pendingHighlightCourseId = null;
    }

    private int findEnrollmentPosition(String courseId) {
        if (courseId == null) {
            return RecyclerView.NO_POSITION;
        }
        for (int i = 0; i < enrollments.size(); i++) {
            CourseEnrollment enrollment = enrollments.get(i);
            if (enrollment != null && courseId.equals(enrollment.getCourseId())) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        highlightHandler.removeCallbacksAndMessages(null);
        if (adapter != null) {
            adapter.clearHighlight();
        }
        clearHighlightRunnable = null;
        pendingHighlightCourseId = null;
        binding = null;
    }

}
