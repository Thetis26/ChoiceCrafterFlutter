package com.choicecrafter.students.ui.colleagues_activity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.choicecrafter.students.R;
import com.choicecrafter.students.MainViewModel;
import com.choicecrafter.students.adapters.ColleaguesActivityAdapter;
import com.choicecrafter.students.models.ColleagueActivity;
import com.choicecrafter.students.models.TaskStats;
import com.choicecrafter.students.models.User;
import com.choicecrafter.students.models.EnrollmentActivityProgress;
import com.choicecrafter.students.models.NudgePreferences;
import com.choicecrafter.students.utils.MotivationalPromptType;
import com.choicecrafter.students.utils.MotivationalPrompts;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.time.temporal.TemporalAccessor;

public class ColleaguesActivityFragment extends Fragment {

    private static final String TAG = "ColleaguesActivity";
    private static final int MAX_DISPLAYED_ACTIVITIES = 20;
    private static final ZoneId APP_ZONE = ZoneId.of("Europe/Bucharest");
    private static final DateTimeFormatter ISO_MILLIS = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS");
    private static final DateTimeFormatter FLEXIBLE_TIMESTAMP = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
            .appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalStart().appendOffsetId().optionalEnd()
            .toFormatter();

    private RecyclerView colleagueActivityList;
    private ColleaguesActivityAdapter activityAdapter;
    private final List<ColleagueActivity> activityList = new ArrayList<>();
    private final Map<String, User> usersByEmail = new HashMap<>();
    private final List<User> leaderboardUsers = new ArrayList<>();
    private TextView motivationalPromptView;
    private View currentUserScoreCard;
    private TextView currentUserScoreView;
    private TextView currentUserRankView;
    private TextView currentUserInitialView;
    private FirebaseUser currentFirebaseUser;
    private ListenerRegistration usersRegistration;
    private ListenerRegistration enrollmentsRegistration;
    private MainViewModel mainViewModel;
    private View motivationalPromptCard;
    private TextView motivationalPromptTextView;
    private NudgePreferences currentNudgePreferences;

    public ColleaguesActivityFragment() {}

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_colleagues_activity, container, false);
        requireActivity().setTitle("Peers' Activity");

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        TextView[] leaderNames = {
                rootView.findViewById(R.id.rank1StudentName),
                rootView.findViewById(R.id.rank2StudentName),
                rootView.findViewById(R.id.rank3StudentName)
        };
        TextView[] leaderScores = {
                rootView.findViewById(R.id.rank1Score),
                rootView.findViewById(R.id.rank2Score),
                rootView.findViewById(R.id.rank3Score)
        };
        currentUserScoreCard = rootView.findViewById(R.id.currentUserScoreCard);
        currentUserScoreView = rootView.findViewById(R.id.currentUserScoreValue);
        currentUserRankView = rootView.findViewById(R.id.currentUserRank);
        currentUserInitialView = rootView.findViewById(R.id.currentUserInitial);
        setCurrentUserScorePlaceholder();
        int[] imageViews = {
                R.id.rank1StudentImage,
                R.id.rank2StudentImage,
                R.id.rank3StudentImage
        };

        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseUser currentUser = currentFirebaseUser;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (usersRegistration != null) {
            usersRegistration.remove();
        }
        usersRegistration = db.collection("users")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<User> users = new ArrayList<>();
                        usersByEmail.clear();
                        for (DocumentSnapshot snapshot : value.getDocuments()) {
                            try {
                                User user = snapshot.toObject(User.class);
                                if (user != null) {
                                    user.computeTotalScore();
                                    if (user.getEmail() != null) {
                                        usersByEmail.put(user.getEmail(), user);
                                    }
                                    users.add(user);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error getting user data", e);
                            }
                        }

                        users.sort((u1, u2) -> Integer.compare(u2.getTotalScore(), u1.getTotalScore()));

                        leaderboardUsers.clear();
                        leaderboardUsers.addAll(users);
                        updateColleagueMotivationalPrompt();

                        for (int i = 0; i < Math.min(3, users.size()); i++) {
                            setLeaderInfo(users.get(i), currentUser, leaderNames[i], leaderScores[i],
                                    rootView.findViewById(imageViews[i]));
                        }

                        updateCurrentUserScore(users, currentUser);
                    } else {
                        setCurrentUserScorePlaceholder();
                    }
                });

        motivationalPromptCard = rootView.findViewById(R.id.motivationalPromptCard);
        motivationalPromptTextView = rootView.findViewById(R.id.motivationalPrompt);
        if (motivationalPromptTextView != null) {
            motivationalPromptTextView.setText(com.choicecrafter.students.utils.MotivationalPrompts.getRandomPrompt(
                    requireContext(),
                    com.choicecrafter.students.utils.MotivationalPromptType.COLLEAGUE_PERFORMANCE));
        }
        applyNudgePreferences();
        motivationalPromptView = rootView.findViewById(R.id.motivationalPrompt);
        updateColleagueMotivationalPrompt();

        colleagueActivityList = rootView.findViewById(R.id.colleagueActivityList);
        colleagueActivityList.setLayoutManager(new LinearLayoutManager(getContext()));
        // Initialize adapter before listeners to avoid null updates
        activityAdapter = new ColleaguesActivityAdapter(activityList);
        colleagueActivityList.setAdapter(activityAdapter);

        if (mainViewModel != null) {
            mainViewModel.getNudgePreferences().observe(getViewLifecycleOwner(), preferences -> {
                currentNudgePreferences = preferences;
                applyNudgePreferences();
            });
        }

        if (enrollmentsRegistration != null) {
            enrollmentsRegistration.remove();
        }
        enrollmentsRegistration = db.collection("COURSE_ENROLLMENTS")
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;

                    activityList.clear();

                    for (DocumentSnapshot snapshot : value.getDocuments()) {
                        String enrollmentUserId = snapshot.getString("userId");
                        if (enrollmentUserId == null || (currentUser != null && Objects.equals(enrollmentUserId, currentUser.getEmail()))) {
                            continue;
                        }

                        Object progressSummaryObj = snapshot.get("progressSummary");
                        if (!(progressSummaryObj instanceof Map<?, ?> progressSummary)) {
                            continue;
                        }
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

                            User user = usersByEmail.get(enrollmentUserId);
                            if (user == null || user.getAnonymousAvatar() == null) {
                                Log.w(TAG, "Skipping activity without cached user: " + enrollmentUserId);
                                continue;
                            }

                            String timestampIso = resolveActivityTimestamp(activityProgress);
                            activityList.add(new ColleagueActivity(
                                    user.getName(),
                                    activityProgress.getActivityId(),
                                    user.getAnonymousAvatar(),
                                    timestampIso
                            ));
                        }
                    }
                    sortColleagueActivitiesByNewest();
                    if (activityAdapter != null) {
                        activityAdapter.notifyDataSetChanged();
                    }
                });

        return rootView;
    }

    private void updateCurrentUserScore(List<User> sortedUsers, FirebaseUser firebaseUser) {
        if (currentUserScoreView == null) {
            return;
        }

        if (firebaseUser == null) {
            setCurrentUserScorePlaceholder();
            return;
        }

        String email = firebaseUser.getEmail();
        if (email == null) {
            setCurrentUserScorePlaceholder();
            return;
        }

        User currentUser = null;
        int position = -1;
        if (sortedUsers != null) {
            for (int i = 0; i < sortedUsers.size(); i++) {
                User candidate = sortedUsers.get(i);
                if (candidate == null || candidate.getEmail() == null) {
                    continue;
                }
                if (email.equalsIgnoreCase(candidate.getEmail())) {
                    currentUser = candidate;
                    position = i;
                    break;
                }
            }
        }
        if (currentUser == null) {
            setCurrentUserScorePlaceholder();
            return;
        }

        bindCurrentUserScoreCard(currentUser, position, sortedUsers != null ? sortedUsers.size() : 0);
    }

    private void setCurrentUserScorePlaceholder() {
        if (currentUserScoreView != null) {
            currentUserScoreView.setText(R.string.colleagues_activity_current_user_score_placeholder);
        }
        if (currentUserRankView != null) {
            currentUserRankView.setText(R.string.colleagues_activity_current_user_rank_placeholder);
        }
        if (currentUserInitialView != null) {
            currentUserInitialView.setText(R.string.colleagues_activity_current_user_initial_placeholder);
        }
        if (currentUserScoreCard != null) {
            currentUserScoreCard.setAlpha(0.6f);
        }
    }

    private void bindCurrentUserScoreCard(User currentUser, int zeroBasedPosition, int totalPeers) {
        if (currentUserScoreView == null || currentUserRankView == null || currentUserInitialView == null) {
            return;
        }

        currentUserScoreView.setText(
                getString(R.string.colleagues_activity_current_user_score_value, currentUser.getTotalScore()));

        if (totalPeers > 0 && zeroBasedPosition >= 0) {
            currentUserRankView.setText(getString(
                    R.string.colleagues_activity_current_user_rank,
                    zeroBasedPosition + 1,
                    totalPeers));
        } else {
            currentUserRankView.setText(R.string.colleagues_activity_current_user_rank_placeholder);
        }

        currentUserInitialView.setText(extractInitial(currentUser));

        if (currentUserScoreCard != null) {
            currentUserScoreCard.setAlpha(1f);
        }
    }

    private String extractInitial(User user) {
        if (user == null) {
            return getString(R.string.colleagues_activity_current_user_initial_placeholder);
        }
        String initial = firstCharacter(user.getName());
        if (initial == null) {
            initial = firstCharacter(user.getEmail());
        }
        if (initial == null) {
            return getString(R.string.colleagues_activity_current_user_initial_placeholder);
        }
        return initial;
    }

    private String firstCharacter(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int firstCodePoint = trimmed.codePointAt(0);
        String initial = new String(Character.toChars(firstCodePoint));
        return initial.toUpperCase(Locale.getDefault());
    }

    private void applyNudgePreferences() {
        if (motivationalPromptCard == null) {
            return;
        }
        boolean showPrompt = currentNudgePreferences == null
                || currentNudgePreferences.isColleaguesPromptEnabled();
        motivationalPromptCard.setVisibility(showPrompt ? View.VISIBLE : View.GONE);
        if (showPrompt && motivationalPromptTextView != null) {
            motivationalPromptTextView.setText(com.choicecrafter.students.utils.MotivationalPrompts.getRandomPrompt(
                    requireContext(),
                    com.choicecrafter.students.utils.MotivationalPromptType.COLLEAGUE_PERFORMANCE));
        }
    }

    @Override
    public void onDestroyView() {
        if (usersRegistration != null) {
            usersRegistration.remove();
            usersRegistration = null;
        }
        if (enrollmentsRegistration != null) {
            enrollmentsRegistration.remove();
            enrollmentsRegistration = null;
        }
        usersByEmail.clear();
        motivationalPromptCard = null;
        motivationalPromptTextView = null;
        currentNudgePreferences = null;
        leaderboardUsers.clear();
        motivationalPromptView = null;
        currentUserScoreCard = null;
        currentUserScoreView = null;
        currentUserRankView = null;
        currentUserInitialView = null;
        currentFirebaseUser = null;
        super.onDestroyView();
    }

    private void updateColleagueMotivationalPrompt() {
        if (!isAdded() || motivationalPromptView == null) {
            return;
        }

        MotivationalPrompts.PersonalizationData.Builder builder = MotivationalPrompts.PersonalizationData.builder();

        if (currentFirebaseUser != null && !leaderboardUsers.isEmpty()) {
            int peerCount = leaderboardUsers.size();
            builder.setPeerCount(peerCount);

            String email = currentFirebaseUser.getEmail();
            int rank = -1;
            Integer myScore = null;
            for (int index = 0; index < leaderboardUsers.size(); index++) {
                User candidate = leaderboardUsers.get(index);
                if (candidate == null) {
                    continue;
                }
                if (email != null && email.equalsIgnoreCase(candidate.getEmail())) {
                    rank = index + 1;
                    myScore = candidate.getTotalScore();
                    break;
                }
            }

            if (rank > 0 && myScore != null) {
                builder.setPeerRank(rank);
                builder.setTotalPoints(myScore);

                if (rank > 1) {
                    User ahead = leaderboardUsers.get(rank - 2);
                    if (ahead != null) {
                        builder.setPeerScoreDeltaAhead(ahead.getTotalScore() - myScore);
                    }
                }
                if (rank < peerCount) {
                    User behind = leaderboardUsers.get(rank);
                    if (behind != null) {
                        builder.setPeerScoreDeltaBehind(myScore - behind.getTotalScore());
                    }
                }
            }
        }

        MotivationalPrompts.PersonalizationData data = builder.build();
        String prompt = MotivationalPrompts.getPersonalizedPrompt(
                requireContext(),
                MotivationalPromptType.COLLEAGUE_PERFORMANCE,
                data);
        motivationalPromptView.setText(prompt);
    }

    private void sortColleagueActivitiesByNewest() {
        if (activityList.isEmpty()) {
            return;
        }
        activityList.sort((first, second) -> {
            Instant firstInstant = parseIsoMillisTimestamp(first != null ? first.getTimestamp() : null);
            Instant secondInstant = parseIsoMillisTimestamp(second != null ? second.getTimestamp() : null);
            return secondInstant.compareTo(firstInstant);
        });
        if (activityList.size() > MAX_DISPLAYED_ACTIVITIES) {
            activityList.subList(MAX_DISPLAYED_ACTIVITIES, activityList.size()).clear();
        }
    }

    private void setLeaderInfo(User user, FirebaseUser currentUser,
                               TextView nameView, TextView scoreView, ImageView imageView) {
        if (!isAdded() || getContext() == null || imageView == null || user.getAnonymousAvatar() == null) {
            return;
        }

        String displayName = user.getAnonymousAvatar().getName();
        if (currentUser != null && Objects.equals(currentUser.getEmail(), user.getEmail())) {
            displayName += " (You)";
        }

        nameView.setText(displayName);
        scoreView.setText(String.valueOf(user.getTotalScore()));
        Glide.with(imageView)
                .load(user.getAnonymousAvatar().getImageUrl())
                .placeholder(R.drawable.avatar_andrei)
                .error(R.drawable.avatar_andrei)
                .into(imageView);
    }

    // Safely compute an ISO timestamp for an activity.
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String resolveActivityTimestamp(EnrollmentActivityProgress ua) {
        Map<String, TaskStats> stats = ua.getTaskStats();
        Instant latestInstant = null;
        if (stats != null) {
            for (TaskStats value : stats.values()) {
                if (value == null) {
                    continue;
                }
                String attempt = value.getAttemptDateTime();
                if (attempt == null || attempt.isEmpty()) {
                    continue;
                }
                Instant parsed = parseAttemptInstant(attempt);
                if (parsed != null && (latestInstant == null || parsed.isAfter(latestInstant))) {
                    latestInstant = parsed;
                }
            }
        }

        Instant effectiveInstant = latestInstant != null ? latestInstant : Instant.now();
        return ISO_MILLIS.withZone(APP_ZONE).format(effectiveInstant);
    }

    private Instant parseIsoMillisTimestamp(String value) {
        if (value == null || value.isEmpty()) {
            return Instant.EPOCH;
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(value, ISO_MILLIS);
            return localDateTime.atZone(APP_ZONE).toInstant();
        } catch (DateTimeParseException exception) {
            Log.w(TAG, "Unable to parse colleague activity timestamp: " + value, exception);
            return Instant.EPOCH;
        }
    }

    private Instant parseAttemptInstant(String value) {
        try {
            TemporalAccessor accessor = FLEXIBLE_TIMESTAMP.parse(value);
            if (accessor.isSupported(java.time.temporal.ChronoField.OFFSET_SECONDS)) {
                return OffsetDateTime.from(accessor).toInstant();
            }
            return LocalDateTime.from(accessor).atZone(APP_ZONE).toInstant();
        } catch (DateTimeParseException | IllegalArgumentException exception) {
            Log.w(TAG, "Unable to parse activity attempt timestamp: " + value, exception);
            return null;
        }
    }
}
