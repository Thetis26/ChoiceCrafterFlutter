package com.choicecrafter.studentapp.ui.statistics;

import static com.choicecrafter.studentapp.repositories.UserRepository.buildDailyScores;
import static com.choicecrafter.studentapp.utils.TimeAgoUtil.toIsoMillis;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.navigation.fragment.NavHostFragment;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.MainViewModel;
import com.choicecrafter.studentapp.adapters.PersonalActivityAdapter;
import com.choicecrafter.studentapp.models.PersonalActivity;
import com.choicecrafter.studentapp.models.User;
import com.choicecrafter.studentapp.models.EnrollmentActivityProgress;
import com.choicecrafter.studentapp.models.TaskStats;
import com.choicecrafter.studentapp.models.NudgePreferences;
import com.choicecrafter.studentapp.models.badges.BadgeStatus;
import com.choicecrafter.studentapp.badges.BadgeUpdateManager;
import com.choicecrafter.studentapp.badges.ModuleCompletionAggregator;
import com.choicecrafter.studentapp.repositories.UserRepository;
import com.choicecrafter.studentapp.utils.MotivationalPromptType;
import com.choicecrafter.studentapp.utils.MotivationalPrompts;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.model.GradientColor;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


@RequiresApi(api = Build.VERSION_CODES.O)
public class StatisticsFragment extends Fragment {

    private static final ZoneId APP_ZONE = ZoneId.of("Europe/Bucharest");
    private static final DateTimeFormatter ISO_MILLIS = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS");

    private final UserRepository userRepository = new UserRepository();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private BarChart timeSpentBarChart;
    private RecyclerView latestActivitiesRecyclerView;
    private PersonalActivityAdapter activityAdapter;
    private ArrayList<PersonalActivity> activitiesList;
    private View rootView;
    private View motivationalPromptCard;
    private TextView motivationalPromptTextView;
    private TextView motivationalPromptView;
    private BadgeSummaryAdapter badgeSummaryAdapter;
    private View badgesEmptyState;
    private Button viewAllBadgesButton;
    private final ModuleCompletionAggregator moduleCompletionAggregator = new ModuleCompletionAggregator();
    private int completedModulesCount;

    private MainViewModel mainViewModel;
    private User loggedInUser;
    private ListenerRegistration enrollmentRegistration;
    private NudgePreferences currentNudgePreferences;

    public StatisticsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_personal_statistics, container, false);

        // Set up user stats header
        updateStatisticsViews(rootView);

        // Set up the Bar Chart for Time Spent
        timeSpentBarChart = rootView.findViewById(R.id.timeSpentBarChart);
        timeSpentBarChart.getDescription().setEnabled(false);

        setupBarChart();

        // Set up RecyclerView for Latest Activities
        latestActivitiesRecyclerView = rootView.findViewById(R.id.latestActivitiesRecyclerView);
        latestActivitiesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        RecyclerView badgesRecyclerView = rootView.findViewById(R.id.badgesRecyclerView);
        badgesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        badgeSummaryAdapter = new BadgeSummaryAdapter();
        badgesRecyclerView.setAdapter(badgeSummaryAdapter);
        badgesEmptyState = rootView.findViewById(R.id.badgesEmptyState);
        viewAllBadgesButton = rootView.findViewById(R.id.viewAllBadgesButton);
        if (viewAllBadgesButton != null) {
            viewAllBadgesButton.setOnClickListener(v -> NavHostFragment.findNavController(StatisticsFragment.this)
                    .navigate(R.id.action_library_to_badgesInfoFragment));
        }

        motivationalPromptCard = rootView.findViewById(R.id.motivationalPromptCard);
        motivationalPromptTextView = rootView.findViewById(R.id.motivationalPrompt);
        if (motivationalPromptTextView != null) {
            motivationalPromptTextView.setText(MotivationalPrompts.getRandomPrompt(
                    requireContext(),
                    MotivationalPromptType.PERSONAL_PERFORMANCE));
        }
        applyNudgePreferences();
        motivationalPromptView = rootView.findViewById(R.id.motivationalPrompt);
        updatePersonalMotivationPrompt();

        // Sample data for activity cards
        activitiesList = new ArrayList<>();
        activityAdapter = new PersonalActivityAdapter(activitiesList);
        latestActivitiesRecyclerView.setAdapter(activityAdapter);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            if (enrollmentRegistration != null) {
                enrollmentRegistration.remove();
            }
            enrollmentRegistration = db.collection("COURSE_ENROLLMENTS")
                    .whereEqualTo("userId", currentUser.getEmail())
                    .addSnapshotListener((value, error) -> {
                        Log.i("StatisticsFragment", "Received enrollment updates: " + value);
                        if (value == null) {
                            return;
                        }
                        moduleCompletionAggregator.reset();
                        activitiesList.clear();
                        for (DocumentSnapshot snapshot : value.getDocuments()) {
                            Object progressSummaryObj = snapshot.get("progressSummary");
                            if (!(progressSummaryObj instanceof Map<?, ?> progressSummary)) {
                                continue;
                            }
                            moduleCompletionAggregator.collectFromProgressSummary(progressSummary);
                            Object activitySnapshotsObj = progressSummary.get("activitySnapshots");
                            if (!(activitySnapshotsObj instanceof List<?> snapshotsList)) {
                                continue;
                            }
                            for (Object entry : snapshotsList) {
                                if (!(entry instanceof Map<?, ?> snapshotMapRaw)) {
                                    continue;
                                }
                                Map<String, Object> snapshotMap = new HashMap<>();
                                for (Map.Entry<?, ?> mapEntry : snapshotMapRaw.entrySet()) {
                                    snapshotMap.put(String.valueOf(mapEntry.getKey()), mapEntry.getValue());
                                }
                                EnrollmentActivityProgress activityProgress = EnrollmentActivityProgress.fromMap(snapshotMap);
                                if (activityProgress.getTaskStats().isEmpty()) {
                                    continue;
                                }
                                TaskStats firstAttempt = activityProgress.getTaskStats().values().stream()
                                        .filter(taskStats -> taskStats != null && taskStats.getAttemptDateTime() != null)
                                        .findFirst()
                                        .orElse(null);
                                if (firstAttempt == null) {
                                    continue;
                                }
                                String timestampIso = toIsoMillis(firstAttempt.getAttemptDateTime(), APP_ZONE);
                                activitiesList.add(new PersonalActivity(
                                        "Completed " + activityProgress.getActivityId(),
                                        timestampIso,
                                        "Completed activity"));
                            }
                        }
                        sortPersonalActivitiesByNewest();
                        if (activityAdapter != null) {
                            activityAdapter.notifyDataSetChanged();
                        }
                        updatePersonalMotivationPrompt();
                        completedModulesCount = moduleCompletionAggregator.getCompletedModulesCount();
                        maybeEvaluateBadges();
                    });
        }

        requireActivity().setTitle(R.string.statistics);

        if (mainViewModel != null) {
            mainViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
                loggedInUser = user;
                loadDailyScores();
                if (rootView != null) {
                    updateStatisticsViews(rootView);
                    setupBarChart();
                    updatePersonalMotivationPrompt();
                    maybeEvaluateBadges();
                }
            });
            mainViewModel.getNudgePreferences().observe(getViewLifecycleOwner(), preferences -> {
                currentNudgePreferences = preferences;
                applyNudgePreferences();
            });
        }
        return rootView;
    }

    @Override
    public void onDestroyView() {
        if (enrollmentRegistration != null) {
            enrollmentRegistration.remove();
            enrollmentRegistration = null;
        }
        timeSpentBarChart = null;
        latestActivitiesRecyclerView = null;
        activityAdapter = null;
        motivationalPromptCard = null;
        motivationalPromptTextView = null;
        rootView = null;
        motivationalPromptView = null;
        badgeSummaryAdapter = null;
        badgesEmptyState = null;
        viewAllBadgesButton = null;
        moduleCompletionAggregator.reset();
        completedModulesCount = 0;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        maybeEvaluateBadges();
    }

    private void applyNudgePreferences() {
        if (motivationalPromptCard == null) {
            return;
        }
        boolean showPrompt = currentNudgePreferences == null
                || currentNudgePreferences.isPersonalStatisticsPromptEnabled();
        motivationalPromptCard.setVisibility(showPrompt ? View.VISIBLE : View.GONE);
        if (showPrompt && motivationalPromptTextView != null) {
            motivationalPromptTextView.setText(MotivationalPrompts.getRandomPrompt(
                    requireContext(),
                    MotivationalPromptType.PERSONAL_PERFORMANCE));
        }
    }

    private void sortPersonalActivitiesByNewest() {
        if (activitiesList == null || activitiesList.isEmpty()) {
            return;
        }
        activitiesList.sort((first, second) -> {
            Instant firstInstant = parseActivityInstant(first != null ? first.getActivityTime() : null);
            Instant secondInstant = parseActivityInstant(second != null ? second.getActivityTime() : null);
            return secondInstant.compareTo(firstInstant);
        });
    }

    private void updateBadgeSummary(List<BadgeStatus> statuses) {
        if (badgeSummaryAdapter == null) {
            return;
        }
        badgeSummaryAdapter.submitList(statuses);
        if (badgesEmptyState != null) {
            badgesEmptyState.setVisibility(badgeSummaryAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
        }
    }

    private void maybeEvaluateBadges() {
        if (!isAdded() || badgeSummaryAdapter == null || loggedInUser == null) {
            return;
        }
        boolean hasCompletedActivity = activitiesList != null && !activitiesList.isEmpty();
        BadgeUpdateManager.BadgeUpdateOutcome outcome = BadgeUpdateManager.evaluateAndPersist(
                requireContext(),
                loggedInUser,
                hasCompletedActivity,
                completedModulesCount > 0,
                userRepository,
                mainViewModel);

        updateBadgeSummary(outcome.getStatuses());

        List<String> newlyUnlockedBadges = outcome.getNewlyUnlockedBadgeTitles();
        if (!newlyUnlockedBadges.isEmpty() && rootView != null) {
            String badgeList = TextUtils.join(", ", newlyUnlockedBadges);
            String message;
            if (newlyUnlockedBadges.size() == 1) {
                message = getString(R.string.badge_unlocked_notification_single, badgeList);
            } else {
                message = getString(R.string.badge_unlocked_notification_multiple, badgeList);
            }
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
        }
    }

    private Instant parseActivityInstant(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return Instant.EPOCH;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(isoTimestamp, ISO_MILLIS);
            return dateTime.atZone(APP_ZONE).toInstant();
        } catch (DateTimeParseException exception) {
            Log.w("StatisticsFragment", "Unable to parse personal activity timestamp: " + isoTimestamp, exception);
            return Instant.EPOCH;
        }
    }

    private void updatePersonalMotivationPrompt() {
        if (!isAdded() || motivationalPromptView == null) {
            return;
        }

        MotivationalPrompts.PersonalizationData.Builder builder = MotivationalPrompts.PersonalizationData.builder();

        if (loggedInUser != null) {
            builder.setStreak(loggedInUser.computeStreak());
            builder.setTotalPoints(loggedInUser.getTotalScore());
            Double trend = computeWeeklyScoreTrend(loggedInUser.getScores());
            if (trend != null) {
                builder.setScoreTrend(trend);
            }
        }

        if (activitiesList != null && !activitiesList.isEmpty()) {
            Instant now = Instant.now();
            Instant latest = null;
            int recentActivities = 0;
            int todayCompletions = 0;

            for (PersonalActivity activity : activitiesList) {
                if (activity == null) {
                    continue;
                }
                Instant instant = parseActivityInstant(activity.getActivityTime());
                if (Instant.EPOCH.equals(instant)) {
                    continue;
                }
                if (instant.isAfter(now)) {
                    instant = now;
                }
                if (latest == null || instant.isAfter(latest)) {
                    latest = instant;
                }
                Duration since = Duration.between(instant, now);
                if (since.isNegative()) {
                    continue;
                }
                if (since.toDays() < 7) {
                    recentActivities++;
                }
                if (since.toHours() < 24) {
                    todayCompletions++;
                }
            }

            if (latest != null) {
                builder.setMillisSinceLastActivity(Duration.between(latest, now).toMillis());
            }
            builder.setRecentActivityCount(recentActivities);
            builder.setCompletionsToday(todayCompletions);
        }

        MotivationalPrompts.PersonalizationData data = builder.build();
        String prompt = MotivationalPrompts.getPersonalizedPrompt(
                requireContext(),
                MotivationalPromptType.PERSONAL_PERFORMANCE,
                data);
        motivationalPromptView.setText(prompt);
    }

    private Double computeWeeklyScoreTrend(Map<String, Long> scores) {
        if (scores == null || scores.isEmpty()) {
            return null;
        }
        LocalDate today = LocalDate.now(APP_ZONE);
        double recentSum = 0;
        double previousSum = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(i);
            recentSum += resolveDailyScore(scores.get(day.toString()));
        }
        for (int i = 7; i < 14; i++) {
            LocalDate day = today.minusDays(i);
            previousSum += resolveDailyScore(scores.get(day.toString()));
        }

        if (previousSum == 0) {
            if (recentSum > 0) {
                return Double.POSITIVE_INFINITY;
            }
            return null;
        }

        double recentAvg = recentSum / 7.0;
        double previousAvg = previousSum / 7.0;
        double diff = ((recentAvg - previousAvg) / previousAvg) * 100.0;
        if (Double.isNaN(diff)) {
            return null;
        }
        return diff;
    }

    private long resolveDailyScore(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private void updateStatisticsViews(View rootView) {
        if (loggedInUser == null) {
            return;
        }
        TextView streakText = rootView.findViewById(R.id.streakText);
        TextView pointsText = rootView.findViewById(R.id.pointsText);

        streakText.setText(MessageFormat.format("{0} Day Streak", loggedInUser.computeStreak()));
        pointsText.setText(MessageFormat.format("{0} points", loggedInUser.getTotalScore()));
    }

    private void setupBarChart() {
        if (loggedInUser == null || timeSpentBarChart == null) {
            return;
        }
        Map<String, Long> scores = loggedInUser.getScores();
        if (scores == null) {
            scores = new HashMap<>();
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> xAxisLabels = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", java.util.Locale.getDefault());
        long currentTime = System.currentTimeMillis();

        for (int i = 4; i >= 0; i--) {
            long dayTime = currentTime - (i * 24L * 60L * 60L * 1000L);
            String dateLabel = sdf.format(new java.util.Date(dayTime));
            xAxisLabels.add(dateLabel);

            String key = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(new java.util.Date(dayTime));
            long value = 0L;
            if (scores.containsKey(key)) {
                Object scoreValue = scores.get(key);
                if (scoreValue instanceof Integer) value = ((Integer) scoreValue).longValue();
                else if (scoreValue instanceof Long) value = (Long) scoreValue;
                else if (scoreValue instanceof String) value = Long.parseLong((String) scoreValue);
            }
            entries.add(new BarEntry(4 - i, value));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setHighlightEnabled(false);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.parseColor("#374151")); // slate‑700

        // Pastel gradient per bar (playful look)
        ArrayList<GradientColor> gradients = new ArrayList<>();
        int[][] palette = new int[][]{
                {Color.parseColor("#A8E6CF"), Color.parseColor("#DCEDC1")}, // mint → light mint
                {Color.parseColor("#FFD3B6"), Color.parseColor("#FFAAA5")}, // peach → coral
                {Color.parseColor("#D4A5EA"), Color.parseColor("#A0C4FF")}, // lavender → baby blue
                {Color.parseColor("#BDB2FF"), Color.parseColor("#FFC6FF")}, // periwinkle → pink
                {Color.parseColor("#FDE68A"), Color.parseColor("#FCA5A5")}  // soft yellow → rose
        };
        for (int i = 0; i < entries.size(); i++) {
            int[] p = palette[i % palette.length];
            gradients.add(new GradientColor(p[0], p[1]));
        }
        dataSet.setGradientColors(gradients);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barData.setDrawValues(true);
        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        timeSpentBarChart.setData(barData);

        // X axis styling
        XAxis xAxis = timeSpentBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#6B7280")); // gray‑500
        xAxis.setTextSize(11f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = Math.round(value);
                return index >= 0 && index < xAxisLabels.size() ? xAxisLabels.get(index) : "";
            }
        });

        // Y axis styling
        YAxis left = timeSpentBarChart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setTextColor(Color.parseColor("#6B7280"));
        left.setGridColor(Color.parseColor("#F3F4F6")); // light grid
        timeSpentBarChart.getAxisRight().setEnabled(false);

        // Layout, legend, and description
        timeSpentBarChart.setDrawGridBackground(false);
        timeSpentBarChart.getLegend().setEnabled(false);
        timeSpentBarChart.getDescription().setEnabled(false);
        timeSpentBarChart.setExtraTopOffset(8f);
        timeSpentBarChart.setExtraBottomOffset(12f);
        timeSpentBarChart.setExtraLeftOffset(8f);
        timeSpentBarChart.setExtraRightOffset(8f);

        // Rounded corners renderer (radius based on dp)
        float radiusPx = getResources().getDisplayMetrics().density * 8f;
        timeSpentBarChart.setRenderer(
                new RoundedBarChartRenderer(
                        timeSpentBarChart,
                        timeSpentBarChart.getAnimator(),
                        timeSpentBarChart.getViewPortHandler(),
                        radiusPx
                )
        );

        // Playful bounce‑in animation
        timeSpentBarChart.animateY(1000, Easing.EaseOutBack);
        timeSpentBarChart.invalidate();
    }

    private void loadDailyScores() {
        if (loggedInUser == null || loggedInUser.getEmail() == null) {
            return;
        }
        buildDailyScores(db, loggedInUser.getEmail(), dailyScores -> {
            loggedInUser.setScores(dailyScores);
            userRepository.updateUserScores(loggedInUser, null, null);
            Log.i("StatisticsFragment", "Updated User: " + loggedInUser);
            if (isAdded() && rootView != null) {
                updateStatisticsViews(rootView);
                setupBarChart();
                updatePersonalMotivationPrompt();
                maybeEvaluateBadges();
            }
        });
    }
}
