package com.choicecrafter.studentapp.ui.news;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.navigation.fragment.NavHostFragment;

import com.choicecrafter.studentapp.MainViewModel;
import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.adapters.NewsCourseAdapter;
import com.choicecrafter.studentapp.models.Course;
import com.choicecrafter.studentapp.models.CourseEnrollment;
import com.choicecrafter.studentapp.models.User;
import com.choicecrafter.studentapp.repositories.CourseEnrollmentRepository;
import com.choicecrafter.studentapp.repositories.CourseRepository;
import com.choicecrafter.studentapp.repositories.UserCourseAvailabilityRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewsFragment extends Fragment {

    private MainViewModel mainViewModel;
    private User user;
    private final CourseRepository courseRepository = new CourseRepository();
    private final CourseEnrollmentRepository enrollmentRepository = new CourseEnrollmentRepository();
    private final UserCourseAvailabilityRepository userCourseAvailabilityRepository = new UserCourseAvailabilityRepository();
    private RecyclerView coursesRecyclerView;

    public NewsFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);
        coursesRecyclerView = view.findViewById(R.id.coursesRecyclerView);
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requireActivity().setTitle("News");
        if (mainViewModel != null) {
            mainViewModel.getUser().observe(getViewLifecycleOwner(), updatedUser -> {
                user = updatedUser;
                loadCourses();
            });
        }

        Log.i("NewsFragment", "onCreateView called");
        loadCourses();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        coursesRecyclerView = null;
    }

    private void loadCourses() {
        if (coursesRecyclerView == null) {
            return;
        }
        courseRepository.fetchCourses(new CourseRepository.Callback<>() {
            @Override
            public void onSuccess(List<Course> courses) {
                Log.i("NewsFragment", "Courses retrieved: " + courses);
                if (!isAdded() || courses == null) {
                    return;
                }
                final List<Course> safeCourses = new ArrayList<>(courses);
                String userId = resolveUserId();
                if (TextUtils.isEmpty(userId)) {
                    if (coursesRecyclerView != null) {
                        bindCoursesToAdapter(safeCourses, Collections.emptySet());
                    }
                    return;
                }
                userCourseAvailabilityRepository.fetchAvailableCourseIds(userId, new UserCourseAvailabilityRepository.Callback<>() {
                    @Override
                    public void onSuccess(Set<String> availableCourseIds) {
                        if (!isAdded() || coursesRecyclerView == null) {
                            return;
                        }
                        List<Course> filteredCourses = filterCoursesByAvailability(safeCourses, availableCourseIds);
                        if (filteredCourses.isEmpty()) {
                            bindCoursesToAdapter(filteredCourses, Collections.emptySet());
                            return;
                        }
                        fetchEnrollmentsAndBind(filteredCourses, userId);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.w("NewsFragment", "Failed to fetch user course availability", e);
                        if (!isAdded() || coursesRecyclerView == null) {
                            return;
                        }
                        Set<String> fallback = Collections.singleton(UserCourseAvailabilityRepository.DEFAULT_COURSE_ID);
                        List<Course> filteredCourses = filterCoursesByAvailability(safeCourses, fallback);
                        if (filteredCourses.isEmpty()) {
                            bindCoursesToAdapter(filteredCourses, Collections.emptySet());
                            return;
                        }
                        fetchEnrollmentsAndBind(filteredCourses, userId);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.w("NewsFragment", "Failed to load courses", e);
            }
        });
    }

    private void fetchEnrollmentsAndBind(List<Course> courses, String userId) {
        enrollmentRepository.fetchEnrollmentsForUser(userId, new CourseEnrollmentRepository.Callback<>() {
            @Override
            public void onSuccess(List<CourseEnrollment> result) {
                if (!isAdded() || coursesRecyclerView == null) {
                    return;
                }
                Set<String> enrolledIds = new HashSet<>();
                if (result != null) {
                    for (CourseEnrollment enrollment : result) {
                        if (enrollment != null && enrollment.getCourseId() != null) {
                            enrolledIds.add(enrollment.getCourseId());
                        }
                    }
                }
                bindCoursesToAdapter(courses, enrolledIds);
            }

            @Override
            public void onFailure(Exception e) {
                Log.w("NewsFragment", "Failed to fetch enrollments", e);
                if (!isAdded() || coursesRecyclerView == null) {
                    return;
                }
                bindCoursesToAdapter(courses, Collections.emptySet());
            }
        });
    }

    private void bindCoursesToAdapter(List<Course> courses, Set<String> enrolledIds) {
        if (coursesRecyclerView == null) {
            return;
        }
        List<Course> safeCourses = courses != null ? new ArrayList<>(courses) : Collections.emptyList();
        coursesRecyclerView.setAdapter(new NewsCourseAdapter(safeCourses,
                enrolledIds,
                NewsFragment.this::handleEnrollClick,
                NewsFragment.this::openCourseDetails));
    }

    private List<Course> filterCoursesByAvailability(List<Course> courses, Set<String> availableCourseIds) {
        if (courses == null || courses.isEmpty()) {
            return Collections.emptyList();
        }
        if (availableCourseIds == null || availableCourseIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Course> filtered = new ArrayList<>();
        for (Course course : courses) {
            if (course == null) {
                continue;
            }
            String courseId = course.getId();
            if (!TextUtils.isEmpty(courseId) && availableCourseIds.contains(courseId)) {
                filtered.add(course);
            }
        }
        return filtered;
    }

    private String resolveUserId() {
        if (user != null && !TextUtils.isEmpty(user.getEmail())) {
            return user.getEmail();
        }
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        return firebaseUser != null ? firebaseUser.getEmail() : null;
    }

    private void handleEnrollClick(Course course) {
        if (!isAdded()) {
            return;
        }
        if (course == null) {
            return;
        }

        String userId = user != null ? user.getEmail() : null;
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(requireContext(), R.string.course_enroll_login_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String courseId = course.getId();
        if (courseId == null || courseId.isEmpty()) {
            Toast.makeText(requireContext(), R.string.course_enroll_failure, Toast.LENGTH_SHORT).show();
            return;
        }

        enrollmentRepository.enrollUserInCourse(userId, courseId, new CourseEnrollmentRepository.Callback<>() {
            @Override
            public void onSuccess(Void unused) {
                if (!isAdded()) {
                    return;
                }
                if (mainViewModel != null) {
                    mainViewModel.setNewlyEnrolledCourseId(courseId);
                }
                Toast.makeText(requireContext(), R.string.course_enroll_success, Toast.LENGTH_SHORT).show();
                navigateToHome();
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) {
                    return;
                }
                Log.w("NewsFragment", "Failed to enroll in course", e);
                Toast.makeText(requireContext(), R.string.course_enroll_failure, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHome() {
        if (!isAdded()) {
            return;
        }
        try {
            NavHostFragment.findNavController(this).navigate(R.id.home);
        } catch (IllegalStateException e) {
            Log.w("NewsFragment", "Unable to navigate to home", e);
        }
    }

    private void openCourseDetails(Course course) {
        if (!isAdded() || course == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putParcelable("course", course);
        if (user != null && user.getEmail() != null) {
            bundle.putString("userId", user.getEmail());
        }
        try {
            NavHostFragment.findNavController(this).navigate(R.id.learningPathDetailFragment, bundle);
        } catch (IllegalStateException e) {
            Log.w("NewsFragment", "Unable to open course details", e);
        }
    }
}

