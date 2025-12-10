package com.choicecrafter.students.ui.activity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.choicecrafter.students.R;
import com.choicecrafter.students.adapters.TaskAdapter;
import com.choicecrafter.students.adapters.TaskAdapter.ActivityActionListener;
import com.choicecrafter.students.databinding.FragmentActivityBinding;
import com.choicecrafter.students.MainViewModel;
import com.choicecrafter.students.badges.BadgeUpdateManager;
import com.choicecrafter.students.badges.ModuleCompletionAggregator;
import com.choicecrafter.students.models.Activity;
import com.choicecrafter.students.models.tasks.Task;
import com.choicecrafter.students.models.TaskStats;
import com.choicecrafter.students.models.EnrollmentActivityProgress;
import com.choicecrafter.students.models.NudgePreferences;
import com.choicecrafter.students.models.User;
import com.choicecrafter.students.repositories.ActivityRepository;
import com.choicecrafter.students.repositories.UserRepository;
import com.choicecrafter.students.utils.ActivityScoreCalculator;
import com.choicecrafter.students.utils.TaskStatsKeyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActivityFragment extends Fragment {

    public static final String ARG_SHOW_STATISTICS = "showStatistics";

    private ActivityViewModel mViewModel;
    private TaskAdapter taskAdapter;
    private FragmentActivityBinding binding;
    private Activity currentActivity;
    private EnrollmentActivityProgress currentEnrollmentActivityProgress;
    private boolean completionDialogShown = false;
    private boolean retryRequested = false;
    private LinearLayoutManager taskLayoutManager;
    private boolean showStatisticsOnLoad = false;

    private final ActivityRepository activityRepository = new ActivityRepository();
    private final UserRepository userRepository = new UserRepository();
    private MainViewModel sharedMainViewModel;
    private NudgePreferences currentNudgePreferences;
    private boolean badgeNotificationTriggered = false;
    private TaskSessionState taskSessionState;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_activity, container, false);
        mViewModel = new ViewModelProvider(this).get(ActivityViewModel.class);
        sharedMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        mViewModel.getActivity().observe(getViewLifecycleOwner(), activity -> {
            if (activity != null) {
                Log.d("ActivityFragment", "Setting title: " + activity.getTitle());
                requireActivity().setTitle(activity.getTitle());
                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                transaction.setReorderingAllowed(true);
                transaction.commit();
            } else {
                Log.d("ActivityFragment", "Activity is null in observer");
            }
        });

        ProgressBar progressBar = binding.progressBar;
        RecyclerView recyclerView = binding.tasksRecyclerView;
        taskLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(taskLayoutManager);
        setupTaskRecyclerViewInteractions(recyclerView);

        if (sharedMainViewModel != null) {
            sharedMainViewModel.getNudgePreferences().observe(getViewLifecycleOwner(), preferences -> {
                currentNudgePreferences = preferences;
                if (taskAdapter != null) {
                    taskAdapter.updateNudgePreferences(preferences);
                }
            });
        }

        Activity activity = null;
        String course = null;
        String userId = null;
        Bundle arguments = getArguments();
        if (arguments != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity = arguments.getParcelable("activity", Activity.class);
            } else {
                @SuppressWarnings("deprecation")
                Activity legacyActivity = arguments.getParcelable("activity");
                activity = legacyActivity;
            }
            course = arguments.getString("courseId");
            userId = arguments.getString("userId");
            Log.i("ActivityFragment", "Received activity: " + activity.getTitle() + ", courseId: " + course + ", userId: " + userId);
            retryRequested = arguments.getBoolean("retry", false);
            showStatisticsOnLoad = arguments.getBoolean(ARG_SHOW_STATISTICS, false);
        } else {
            Log.d("ActivityFragment", "Arguments are null");
        }

        if (activity != null && userId != null && course != null) {
            Activity finalActivity = activity;
            String finalCourse = course;
            String activityId = !TextUtils.isEmpty(activity.getId()) ? activity.getId() : activity.getTitle();
            taskSessionState = mViewModel.getOrCreateSessionState(activityId);

            activityRepository.startActivity(userId, course, activityId, new ActivityRepository.StartActivityCallback() {
                @Override
                public void onSuccess(EnrollmentActivityProgress userActivity) {
                    Log.d("ActivityFragment", "Activity received: " + finalActivity.getTitle());
                    currentActivity = finalActivity;
                    currentEnrollmentActivityProgress = ensureTaskStats(userActivity);
                    mViewModel.updateActivity(finalActivity);
                    User loggedInUser = sharedMainViewModel != null ? sharedMainViewModel.getUser().getValue() : null;
                    taskAdapter = new TaskAdapter(finalActivity.getTasks(), finalActivity.getRecommendations(), currentEnrollmentActivityProgress,
                            progressBar, recyclerView, finalActivity, finalCourse, createActivityActionListener(), loggedInUser, taskSessionState);
                    recyclerView.setAdapter(taskAdapter);
                    if (currentNudgePreferences != null) {
                        taskAdapter.updateNudgePreferences(currentNudgePreferences);
                    }
                    int maxTasks = finalActivity.getTasks() != null ? finalActivity.getTasks().size() : 0;
                    progressBar.setMax(maxTasks);
                    progressBar.setProgress(taskAdapter.getCurrentProgress());
                    if (showStatisticsOnLoad) {
                        scrollToStatisticsCard();
                        completionDialogShown = true;
                        showStatisticsOnLoad = false;
                    } else {
                        scrollToInitialTaskPosition();
                    }

                    badgeNotificationTriggered = false;

                    if (retryRequested) {
                        retryRequested = false;
                        triggerRetry(false);
                    } else if (isActivityCompleted(currentEnrollmentActivityProgress, currentActivity) && !completionDialogShown) {
                        showCompletionDialog(currentEnrollmentActivityProgress, currentActivity, true);
                        taskAdapter.refreshCompletionUi();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("ActivityFragment", "Error starting activity", e);
                }
            });
        } else {
            Log.d("ActivityFragment", "Activity or user information is missing");
        }

        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (dismissKeyboardIfVisible()) {
                    return;
                }
                setEnabled(false);
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        mViewModel.getActivity().observe(getViewLifecycleOwner(), activity -> {
            if (activity != null) {
                Log.d("ActivityFragment", "Setting title: " + activity.getTitle());
                requireActivity().setTitle(activity.getTitle());
                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                transaction.setReorderingAllowed(true);
                transaction.commit();
            } else {
                Log.d("ActivityFragment", "Activity is null in observer");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        mViewModel.getActivity().observe(getViewLifecycleOwner(), activity -> {
            if (activity != null) {
                Log.d("ActivityFragment", "Setting title: " + activity.getTitle());
                requireActivity().setTitle(activity.getTitle());

                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                transaction.setReorderingAllowed(true);
                transaction.commit();
            } else {
                Log.d("ActivityFragment", "Activity is null in observer");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private boolean dismissKeyboardIfVisible() {
        if (binding == null) {
            return false;
        }

        View rootView = binding.getRoot();
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(rootView);
        if (insets == null || !insets.isVisible(WindowInsetsCompat.Type.ime())) {
            return false;
        }

        View currentFocus = requireActivity().getCurrentFocus();
        if (currentFocus == null) {
            currentFocus = rootView;
        }

        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }

        currentFocus.clearFocus();
        return true;
    }

    private ActivityActionListener createActivityActionListener() {
        return new ActivityActionListener() {
            @Override
            public void onReturnToActivities() {
                NavHostFragment.findNavController(ActivityFragment.this).popBackStack();
            }

            @Override
            public void onRetryActivity() {
                triggerRetry(true);
            }

            @Override
            public void onActivityCompleted() {
                if (!completionDialogShown && currentEnrollmentActivityProgress != null && currentActivity != null) {
                    showCompletionDialog(currentEnrollmentActivityProgress, currentActivity, false);
                }
                if (!badgeNotificationTriggered) {
                    evaluateBadgesAfterActivityCompletion();
                }
            }
        };
    }

    private void triggerRetry(boolean showConfirmationDialog) {
        if (currentEnrollmentActivityProgress == null || currentActivity == null) {
            return;
        }

        Runnable resetAction = () -> {
            activityRepository.resetTaskStats(currentEnrollmentActivityProgress.getUserId(),
                    currentEnrollmentActivityProgress.getCourseId(),
                    currentEnrollmentActivityProgress.getActivityId());
            if (currentEnrollmentActivityProgress.getTaskStats() != null) {
                currentEnrollmentActivityProgress.getTaskStats().clear();
            }
            currentEnrollmentActivityProgress.setHighestScore(null);
            completionDialogShown = false;
            badgeNotificationTriggered = false;
            if (taskSessionState != null) {
                taskSessionState.clear();
            }
            if (taskAdapter != null) {
                taskAdapter.resetForRetry();
            }
            if (taskLayoutManager != null) {
                taskLayoutManager.scrollToPositionWithOffset(0, 0);
            }
            if (binding != null) {
                binding.progressBar.setProgress(0);
            }
        };

        if (showConfirmationDialog) {
            showActivityDialog(
                    R.drawable.ic_retry,
                    getString(R.string.activity_retry_confirmation_title),
                    getString(R.string.activity_retry_confirmation),
                    R.string.activity_retry_dialog_retry,
                    resetAction,
                    R.string.activity_retry_dialog_cancel,
                    null,
                    null,
                    null
            );
        } else {
            resetAction.run();
        }
    }

    private EnrollmentActivityProgress ensureTaskStats(EnrollmentActivityProgress userActivity) {
        if (userActivity == null) {
            return null;
        }
        if (userActivity.getTaskStats() == null) {
            userActivity.setTaskStats(new HashMap<>());
        }
        return userActivity;
    }

    private void showCompletionDialog(EnrollmentActivityProgress userActivity, Activity activity, boolean offerRetryOption) {
        completionDialogShown = true;
        String message = buildCompletionSummary(userActivity, activity);
        Runnable secondaryAction = offerRetryOption
                ? () -> NavHostFragment.findNavController(ActivityFragment.this).popBackStack()
                : null;

        showActivityDialog(
                R.drawable.trophy_award,
                getString(R.string.activity_retry_dialog_title),
                message,
                R.string.activity_retry_dialog_retry,
                () -> triggerRetry(false),
                R.string.activity_retry_dialog_cancel,
                secondaryAction,
                null,
                null
        );
    }

    private void showActivityDialog(int iconRes,
                                    CharSequence title,
                                    CharSequence message,
                                    int primaryTextRes,
                                    Runnable primaryAction,
                                    Integer secondaryTextRes,
                                    Runnable secondaryAction,
                                    Integer tertiaryTextRes,
                                    Runnable tertiaryAction) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_activity_status, null, false);

        AppCompatImageView iconView = dialogView.findViewById(R.id.dialog_icon);
        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        TextView messageView = dialogView.findViewById(R.id.dialog_message);
        MaterialButton primaryButton = dialogView.findViewById(R.id.dialog_primary_button);
        MaterialButton secondaryButton = dialogView.findViewById(R.id.dialog_secondary_button);
        MaterialButton tertiaryButton = dialogView.findViewById(R.id.dialog_tertiary_button);

        iconView.setImageResource(iconRes);
        iconView.setContentDescription(title);
        titleView.setText(title);
        messageView.setText(message);

        primaryButton.setText(primaryTextRes);

        boolean hasSecondary = secondaryTextRes != null;
        if (hasSecondary) {
            secondaryButton.setText(secondaryTextRes);
            secondaryButton.setVisibility(View.VISIBLE);
        } else {
            secondaryButton.setVisibility(View.GONE);
        }

        boolean hasTertiary = tertiaryTextRes != null;
        if (hasTertiary) {
            tertiaryButton.setText(tertiaryTextRes);
            tertiaryButton.setVisibility(View.VISIBLE);
        } else {
            tertiaryButton.setVisibility(View.GONE);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        primaryButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (primaryAction != null) {
                primaryAction.run();
            }
        });

        secondaryButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (secondaryAction != null) {
                secondaryAction.run();
            }
        });

        tertiaryButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (tertiaryAction != null) {
                tertiaryAction.run();
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void evaluateBadgesAfterActivityCompletion() {
        if (!isAdded() || sharedMainViewModel == null) {
            return;
        }
        User user = sharedMainViewModel.getUser().getValue();
        if (user == null || TextUtils.isEmpty(user.getEmail())) {
            return;
        }
        badgeNotificationTriggered = true;
        FirebaseFirestore.getInstance().collection("COURSE_ENROLLMENTS")
                .whereEqualTo("userId", user.getEmail())
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded()) {
                        badgeNotificationTriggered = false;
                        return;
                    }
                    ModuleCompletionAggregator aggregator = new ModuleCompletionAggregator();
                    if (snapshots != null) {
                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            Object progressSummaryObj = document.get("progressSummary");
                            if (progressSummaryObj instanceof Map<?, ?> progressSummary) {
                                aggregator.collectFromProgressSummary(progressSummary);
                            }
                        }
                    }
                    BadgeUpdateManager.BadgeUpdateOutcome outcome = BadgeUpdateManager.evaluateAndPersist(
                            requireContext(),
                            user,
                            true,
                            aggregator.getCompletedModulesCount() > 0,
                            userRepository,
                            sharedMainViewModel);
                    if (!outcome.getNewlyUnlockedBadgeTitles().isEmpty()) {
                        showBadgeUnlockedDialog(outcome.getNewlyUnlockedBadgeTitles());
                    }
                })
                .addOnFailureListener(e -> badgeNotificationTriggered = false);
    }

    private void showBadgeUnlockedDialog(List<String> badgeTitles) {
        if (!isAdded() || badgeTitles == null || badgeTitles.isEmpty()) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_badge_unlocked, null, false);

        AppCompatImageView iconView = dialogView.findViewById(R.id.badgeDialogIcon);
        TextView titleView = dialogView.findViewById(R.id.badgeDialogTitle);
        TextView messageView = dialogView.findViewById(R.id.badgeDialogMessage);
        MaterialButton primaryButton = dialogView.findViewById(R.id.badgeDialogPrimaryButton);
        MaterialButton secondaryButton = dialogView.findViewById(R.id.badgeDialogSecondaryButton);

        iconView.setImageResource(R.drawable.trophy_award);
        iconView.setContentDescription(getString(R.string.badge_unlocked_dialog_title));
        titleView.setText(R.string.badge_unlocked_dialog_title);

        String badgeList = TextUtils.join("\n", badgeTitles);
        if (badgeTitles.size() == 1) {
            messageView.setText(getString(R.string.badge_unlocked_dialog_message_single, badgeList));
        } else {
            messageView.setText(getString(R.string.badge_unlocked_dialog_message_multiple, badgeList));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        primaryButton.setOnClickListener(v -> {
            dialog.dismiss();
            NavHostFragment.findNavController(ActivityFragment.this).navigate(R.id.badgesInfoFragment);
        });

        secondaryButton.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private String buildCompletionSummary(EnrollmentActivityProgress userActivity, Activity activity) {
        Map<String, TaskStats> statsMap = userActivity != null ? userActivity.getTaskStats() : null;
        int totalSeconds = 0;
        boolean hintsUsed = false;

        if (statsMap != null && activity != null && activity.getTasks() != null) {
            for (Task task : activity.getTasks()) {
                TaskStats stats = TaskStatsKeyUtils.findStatsForTask(statsMap, task);
                if (stats == null) {
                    continue;
                }
                String timeSpent = stats.getTimeSpent();
                if (!TextUtils.isEmpty(timeSpent) && timeSpent.contains(":")) {
                    String[] parts = timeSpent.split(":");
                    try {
                        int minutes = Integer.parseInt(parts[0]);
                        int seconds = Integer.parseInt(parts[1]);
                        totalSeconds += minutes * 60 + seconds;
                    } catch (NumberFormatException ignored) {
                        // ignore malformed values
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
        List<Task> tasks = activity != null ? activity.getTasks() : null;
        int earnedXp = ActivityScoreCalculator.calculateEarnedXp(tasks, statsMap);
        int totalXp = ActivityScoreCalculator.calculateTotalXp(tasks, statsMap);
        Integer highestScore = userActivity != null ? userActivity.getHighestScore() : null;
        if (highestScore != null && highestScore > earnedXp) {
            earnedXp = highestScore;
        }

        String hintsLabel = getString(hintsUsed ? R.string.statistics_card_hints_used_yes : R.string.statistics_card_hints_used_no);
        return getString(R.string.activity_retry_dialog_message, earnedXp, timeFormatted, hintsLabel, totalXp);
    }

    private boolean isActivityCompleted(EnrollmentActivityProgress userActivity, Activity activity) {
        if (userActivity == null || activity == null || activity.getTasks() == null) {
            return false;
        }
        Map<String, TaskStats> statsMap = userActivity.getTaskStats();
        if (statsMap == null) {
            return false;
        }
        for (Task task : activity.getTasks()) {
            TaskStats stats = TaskStatsKeyUtils.findStatsForTask(statsMap, task);
            if (stats == null || !stats.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    private void setupTaskRecyclerViewInteractions(RecyclerView recyclerView) {
        final float[] lastTouchX = new float[1];
        recyclerView.setOnTouchListener((v, event) -> {
            if (taskAdapter == null || taskLayoutManager == null) {
                return false;
            }
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                lastTouchX[0] = event.getX();
                return false;
            } else if (action == MotionEvent.ACTION_MOVE) {
                float dx = event.getX() - lastTouchX[0];
                lastTouchX[0] = event.getX();
                if (dx < 0) {
                    int maxPosition = taskAdapter.getMaxAccessiblePosition();
                    int lastVisible = Math.max(taskLayoutManager.findLastCompletelyVisibleItemPosition(),
                            taskLayoutManager.findLastVisibleItemPosition());
                    if (lastVisible == RecyclerView.NO_POSITION) {
                        lastVisible = 0;
                    }
                    if (lastVisible >= maxPosition && maxPosition >= 0) {
                        recyclerView.stopScroll();
                        taskLayoutManager.scrollToPositionWithOffset(maxPosition, 0);
                        return true;
                    }
                }
            }
            return false;
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && taskAdapter != null && taskLayoutManager != null) {
                    int maxPosition = taskAdapter.getMaxAccessiblePosition();
                    int lastVisible = Math.max(taskLayoutManager.findLastCompletelyVisibleItemPosition(),
                            taskLayoutManager.findLastVisibleItemPosition());
                    if (lastVisible > maxPosition && maxPosition >= 0) {
                        taskLayoutManager.scrollToPositionWithOffset(maxPosition, 0);
                    }
                }
            }
        });
    }

    private void scrollToInitialTaskPosition() {
        if (taskLayoutManager == null || taskAdapter == null || binding == null) {
            return;
        }
        int targetPosition = Math.min(taskAdapter.getMaxAccessiblePosition(),
                Math.max(taskAdapter.getCurrentProgress(), 0));
        binding.tasksRecyclerView.post(() -> taskLayoutManager.scrollToPositionWithOffset(targetPosition, 0));
    }

    private void scrollToStatisticsCard() {
        if (binding == null || taskAdapter == null) {
            return;
        }
        int statisticsPosition = taskAdapter.getStatisticsCardPosition();
        if (statisticsPosition < 0) {
            return;
        }
        binding.tasksRecyclerView.post(() -> {
            if (taskLayoutManager != null) {
                taskLayoutManager.scrollToPositionWithOffset(statisticsPosition, 0);
            } else {
                binding.tasksRecyclerView.scrollToPosition(statisticsPosition);
            }
        });
    }
}
