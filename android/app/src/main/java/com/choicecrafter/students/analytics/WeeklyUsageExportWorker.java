package com.choicecrafter.studentapp.analytics;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Worker responsible for collecting weekly usage insights and exporting them as JSON files to
 * Firebase Storage.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class WeeklyUsageExportWorker extends Worker {

    public static final String WORK_NAME = "weekly_usage_export";
    private static final String TAG = "WeeklyUsageWorker";
    private static final String EXPERIMENT_ASSIGNMENTS_COLLECTION = "EXPERIMENT_ASSIGNMENTS";
    private static final String EXPERIMENT_METADATA_COLLECTION = "EXPERIMENT_METADATA";
    private static final String EXPERIMENT_METADATA_DOCUMENT = "config";
    private static final String SUMMARY_VERSION = "2.0";
    private static final String[] PHASE_SEQUENCE = new String[]{"phase1", "phase2", "phase3", "phase4"};
    private static final String[] EXPERIMENT_GROUPS = new String[]{"A", "B"};

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter FLEXIBLE_TIMESTAMP_PARSER = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalStart().appendOffsetId().optionalEnd()
            .toFormatter(Locale.US);

    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private final Gson gson;

    public WeeklyUsageExportWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Enqueues a weekly periodic work that will run this worker once every 7 days while keeping a
     * single scheduled instance at all times.
     */
    public static void schedule(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                WeeklyUsageExportWorker.class,
                7,
                TimeUnit.DAYS)
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    @NonNull
    @Override
    public Result doWork() {
        LocalDate referenceDate = LocalDate.now(ZONE).minusDays(1); // previous full day
        LocalDate weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        Instant weekStartInstant = weekStart.atStartOfDay(ZONE).toInstant();
        Instant weekEndInstant = weekEnd.plusDays(1).atStartOfDay(ZONE).minusNanos(1).toInstant();

        try {
            QuerySnapshot snapshot = Tasks.await(firestore.collection("COURSE_ENROLLMENTS").get());
            Map<String, UserUsageAccumulator> usageByUser = new HashMap<>();

            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String userId = doc.getString("userId");
                if (userId == null || userId.trim().isEmpty()) {
                    continue;
                }
                Object progressSummaryObj = doc.get("progressSummary");
                if (!(progressSummaryObj instanceof Map<?, ?> progressSummary)) {
                    continue;
                }
                Object activitySnapshotsObj = progressSummary.get("activitySnapshots");
                if (!(activitySnapshotsObj instanceof List<?> snapshots)) {
                    continue;
                }

                UserUsageAccumulator accumulator = usageByUser.computeIfAbsent(userId, UserUsageAccumulator::new);

                for (Object entry : snapshots) {
                    if (!(entry instanceof Map<?, ?> snapshotMapRaw)) {
                        continue;
                    }
                    Map<String, Object> snapshotMap = new HashMap<>();
                    for (Map.Entry<?, ?> mapEntry : snapshotMapRaw.entrySet()) {
                        snapshotMap.put(String.valueOf(mapEntry.getKey()), mapEntry.getValue());
                    }
                    String courseId = asString(snapshotMap.get("courseId"));
                    String activityId = asString(snapshotMap.get("activityId"));
                    Object taskStatsObj = snapshotMap.get("taskStats");
                    if (!(taskStatsObj instanceof Map<?, ?> taskStatsMap)) {
                        continue;
                    }

                    ActivityUsageAccumulator activityAccumulator = null;
                    Instant earliestAttempt = null;

                    for (Object value : taskStatsMap.values()) {
                        if (!(value instanceof Map<?, ?> attemptMap)) {
                            continue;
                        }

                        @SuppressWarnings("unchecked")
                        Map<String, Object> attempt = (Map<String, Object>) attemptMap;

                        Instant attemptInstant = parseAttemptInstant(asString(attempt.get("attemptDateTime")));
                        if (attemptInstant == null) {
                            continue;
                        }

                        if (earliestAttempt == null || attemptInstant.isBefore(earliestAttempt)) {
                            earliestAttempt = attemptInstant;
                        }

                        if (attemptInstant.isBefore(weekStartInstant) || attemptInstant.isAfter(weekEndInstant)) {
                            continue;
                        }

                        if (activityAccumulator == null) {
                            activityAccumulator = accumulator.ensureActivityAccumulator(courseId, activityId);
                            accumulator.registerActivityParticipation(courseId);
                        }

                        accumulator.registerAttempt(attemptInstant, attempt);
                        activityAccumulator.registerAttempt(attemptInstant, attempt);
                    }

                    if (activityAccumulator != null) {
                        boolean isNewContent = isWithinWeek(earliestAttempt, weekStartInstant, weekEndInstant);
                        activityAccumulator.setNewContent(isNewContent);
                    }
                }
            }

            Map<String, String> userGroupAssignments = fetchUserGroupAssignments();
            LocalDate experimentStartDate = resolveExperimentStartDate(weekStart);

            List<UserWeeklyUsage> users = new ArrayList<>();
            for (UserUsageAccumulator accumulator : usageByUser.values()) {
                if (accumulator.hasData()) {
                    UserWeekMetadata metadata = buildMetadata(
                            accumulator.getUserId(),
                            weekStart,
                            experimentStartDate,
                            userGroupAssignments
                    );
                    users.add(accumulator.toWeeklyUsage(metadata));
                }
            }

            users.sort(Comparator.comparing(UserWeeklyUsage::getUserId));

            WeeklyUsageSummary summary = buildSummary(weekStart, weekEnd, users);

            String jsonPayload = gson.toJson(summary);
            uploadJson(jsonPayload, weekStart, weekEnd, users.size());

            return Result.success();
        } catch (ExecutionException e) {
            Log.e(TAG, "Failed to gather weekly usage", e);
            return Result.retry();
        } catch (InterruptedException e) {
            Log.e(TAG, "Weekly usage export interrupted", e);
            Thread.currentThread().interrupt();
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected failure while exporting weekly usage", e);
            return Result.failure();
        }
    }

    private void uploadJson(String jsonPayload, LocalDate weekStart, LocalDate weekEnd, int userCount)
            throws ExecutionException, InterruptedException {
        String fileName = DATE_FORMAT.format(weekStart) + "_" + DATE_FORMAT.format(weekEnd) + "_" + userCount + ".json";
        StorageReference reference = storage.getReference()
                .child("weekly-usage")
                .child(fileName);

        UploadTask uploadTask = reference.putBytes(jsonPayload.getBytes(StandardCharsets.UTF_8));
        Tasks.await(uploadTask);
    }

    private Map<String, String> fetchUserGroupAssignments() {
        Map<String, String> assignments = new HashMap<>();
        try {
            QuerySnapshot snapshot = Tasks.await(firestore.collection(EXPERIMENT_ASSIGNMENTS_COLLECTION).get());
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                String userId = document.getString("userId");
                if (userId == null || userId.trim().isEmpty()) {
                    userId = document.getId();
                }
                String normalizedGroup = normalizeGroup(document.getString("group"));
                if (userId != null && normalizedGroup != null) {
                    assignments.put(userId, normalizedGroup);
                }
            }
        } catch (ExecutionException e) {
            Log.w(TAG, "Failed to fetch experiment assignments", e);
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted while fetching experiment assignments", e);
            Thread.currentThread().interrupt();
        }
        return assignments;
    }

    private LocalDate resolveExperimentStartDate(LocalDate fallback) {
        try {
            DocumentSnapshot document = Tasks.await(
                    firestore.collection(EXPERIMENT_METADATA_COLLECTION)
                            .document(EXPERIMENT_METADATA_DOCUMENT)
                            .get()
            );
            if (document.exists()) {
                String startDate = document.getString("startDate");
                if (startDate != null && !startDate.trim().isEmpty()) {
                    try {
                        return LocalDate.parse(startDate);
                    } catch (Exception parseError) {
                        Log.w(TAG, "Unable to parse experiment start date: " + startDate, parseError);
                    }
                }
            }
        } catch (ExecutionException e) {
            Log.w(TAG, "Failed to fetch experiment metadata", e);
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted while fetching experiment metadata", e);
            Thread.currentThread().interrupt();
        }
        return fallback;
    }

    static WeeklyUsageSummary buildSummary(LocalDate weekStart, LocalDate weekEnd, List<UserWeeklyUsage> users) {
        return buildSummary(weekStart, weekEnd, users, Instant.now());
    }

    static WeeklyUsageSummary buildSummary(LocalDate weekStart, LocalDate weekEnd, List<UserWeeklyUsage> users, Instant generatedAt) {
        AggregateCollector collector = new AggregateCollector();
        for (UserWeeklyUsage usage : users) {
            collector.include(usage);
        }

        String generatedAtUtc = generatedAt == null ? Instant.now().toString() : generatedAt.toString();

        return new WeeklyUsageSummary(
                SUMMARY_VERSION,
                generatedAtUtc,
                DATE_FORMAT.format(weekStart),
                DATE_FORMAT.format(weekEnd),
                users.size(),
                collector.buildCountsByCondition(),
                collector.buildCountsByPhaseAndGroup(),
                users
        );
    }

    static UserWeekMetadata buildMetadata(
            String userId,
            LocalDate weekStart,
            LocalDate experimentStartDate,
            Map<String, String> assignments
    ) {
        String phase = determineExperimentPhase(experimentStartDate, weekStart);
        String group = assignments == null ? null : normalizeGroup(assignments.get(userId));
        boolean groupMissing = false;
        if (group == null) {
            group = "A";
            if (requiresGroupAssignment(phase)) {
                groupMissing = true;
            }
        }
        boolean nudged = computeNudged(phase, group);
        return new UserWeekMetadata(phase, group, nudged, groupMissing);
    }

    static String determineExperimentPhase(LocalDate experimentStartDate, LocalDate weekStart) {
        if (experimentStartDate == null || weekStart == null) {
            return PHASE_SEQUENCE[0];
        }
        long weeks = ChronoUnit.WEEKS.between(experimentStartDate, weekStart);
        if (weeks < 0) {
            weeks = 0;
        }
        if (weeks < 2) {
            return "phase1";
        }
        if (weeks < 4) {
            return "phase2";
        }
        if (weeks < 6) {
            return "phase3";
        }
        return "phase4";
    }

    static boolean computeNudged(String phase, String group) {
        if ("phase2".equals(phase)) {
            return "B".equals(group);
        }
        if ("phase3".equals(phase)) {
            return "A".equals(group);
        }
        return true;
    }

    private static String normalizeGroup(String group) {
        if (group == null) {
            return null;
        }
        String normalized = group.trim().toUpperCase(Locale.US);
        if ("A".equals(normalized) || "B".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private static boolean requiresGroupAssignment(String phase) {
        return "phase2".equals(phase) || "phase3".equals(phase);
    }

    private static boolean isWithinWeek(Instant value, Instant start, Instant end) {
        return value != null && !value.isBefore(start) && !value.isAfter(end);
    }

    private static Instant parseAttemptInstant(String attemptDateTime) {
        if (attemptDateTime == null || attemptDateTime.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(attemptDateTime, FLEXIBLE_TIMESTAMP_PARSER).atZone(ZONE).toInstant();
        } catch (Exception firstError) {
            try {
                return Instant.parse(attemptDateTime);
            } catch (Exception ignored) {
                Log.w(TAG, "Unable to parse attempt timestamp: " + attemptDateTime, firstError);
                return null;
            }
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return null;
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static long parseTimeSpentSeconds(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0L;
        }
        String[] parts = raw.split(":");
        int[] units = new int[]{0, 0, 0}; // hours, minutes, seconds
        int partIndex = parts.length - 1;
        for (int i = 2; i >= 0 && partIndex >= 0; i--, partIndex--) {
            try {
                units[i] = Integer.parseInt(parts[partIndex]);
            } catch (NumberFormatException ignored) {
                units[i] = 0;
            }
        }
        return units[0] * 3600L + units[1] * 60L + units[2];
    }

    private static double resolveScoreRatio(Map<String, Object> attempt) {
        if (attempt == null) {
            return 0.0;
        }
        Object scoreObj = attempt.get("scoreRatio");
        Double ratio = null;
        if (scoreObj instanceof Number number) {
            ratio = number.doubleValue();
        } else if (scoreObj instanceof String ratioString) {
            try {
                ratio = Double.parseDouble(ratioString);
            } catch (NumberFormatException ignored) {
                ratio = null;
            }
        }
        if (ratio == null) {
            Boolean success = asBoolean(attempt.get("success"));
            ratio = Boolean.TRUE.equals(success) ? 1.0 : 0.0;
        }
        return Math.max(0.0, Math.min(1.0, ratio));
    }

    static final class UserWeekMetadata {
        private final String experimentPhase;
        private final String group;
        private final boolean nudged;
        private final boolean groupAssignmentMissing;

        UserWeekMetadata(String experimentPhase, String group, boolean nudged, boolean groupAssignmentMissing) {
            this.experimentPhase = experimentPhase;
            this.group = group;
            this.nudged = nudged;
            this.groupAssignmentMissing = groupAssignmentMissing;
        }

        String getExperimentPhase() {
            return experimentPhase;
        }

        String getGroup() {
            return group;
        }

        boolean isNudged() {
            return nudged;
        }

        boolean isGroupAssignmentMissing() {
            return groupAssignmentMissing;
        }
    }

    private static class UserUsageAccumulator {
        private final String userId;
        private final Set<LocalDate> activeDays = new HashSet<>();
        private final Set<String> courseIds = new HashSet<>();
        private final Map<String, ActivityUsageAccumulator> activities = new HashMap<>();
        private int tasksAttempted;
        private int successfulTasks;
        private long totalTimeSeconds;
        private long totalRetries;
        private int retrySamples;
        private int hintsUsed;
        private double scoreAccumulator;

        private UserUsageAccumulator(String userId) {
            this.userId = userId;
        }

        String getUserId() {
            return userId;
        }

        ActivityUsageAccumulator ensureActivityAccumulator(String courseId, String activityId) {
            String key = courseId + "|" + activityId;
            ActivityUsageAccumulator accumulator = activities.get(key);
            if (accumulator == null) {
                accumulator = new ActivityUsageAccumulator(courseId, activityId);
                activities.put(key, accumulator);
            }
            return accumulator;
        }

        void registerActivityParticipation(String courseId) {
            if (courseId != null) {
                courseIds.add(courseId);
            }
        }

        void registerAttempt(Instant attemptInstant, Map<String, Object> attempt) {
            activeDays.add(LocalDateTime.ofInstant(attemptInstant, ZONE).toLocalDate());
            tasksAttempted++;

            double scoreRatio = resolveScoreRatio(attempt);
            scoreAccumulator += scoreRatio;
            if (scoreRatio >= 0.999) {
                successfulTasks++;
            }

            Integer retries = asInteger(attempt.get("retries"));
            if (retries != null) {
                totalRetries += retries;
                retrySamples++;
            }

            Boolean hints = asBoolean(attempt.get("hintsUsed"));
            if (Boolean.TRUE.equals(hints)) {
                hintsUsed++;
            }

            totalTimeSeconds += parseTimeSpentSeconds(asString(attempt.get("timeSpent")));
        }

        boolean hasData() {
            return !activities.isEmpty() && tasksAttempted > 0;
        }

        UserWeeklyUsage toWeeklyUsage(UserWeekMetadata metadata) {
            List<ActivityUsage> activitySummaries = new ArrayList<>();
            for (ActivityUsageAccumulator accumulator : activities.values()) {
                activitySummaries.add(accumulator.toSummary());
            }
            activitySummaries.sort(Comparator
                    .comparing(ActivityUsage::getCourseId, Comparator.nullsLast(String::compareTo))
                    .thenComparing(ActivityUsage::getActivityId, Comparator.nullsLast(String::compareTo)));

            double successRate = tasksAttempted == 0 ? 0.0 : scoreAccumulator / tasksAttempted;
            double averageRetries = retrySamples == 0 ? 0.0 : (double) totalRetries / retrySamples;
            int newActivities = 0;
            for (ActivityUsage summary : activitySummaries) {
                if (summary.isNewContent()) {
                    newActivities++;
                }
            }

            List<String> activeDayStrings = new ArrayList<>();
            List<LocalDate> sortedDays = new ArrayList<>(activeDays);
            Collections.sort(sortedDays);
            for (LocalDate day : sortedDays) {
                activeDayStrings.add(DATE_FORMAT.format(day));
            }

            String experimentPhase = metadata == null ? PHASE_SEQUENCE[0] : metadata.getExperimentPhase();
            String group = metadata == null ? "A" : metadata.getGroup();
            boolean nudged = metadata != null && metadata.isNudged();
            Boolean groupMissing = metadata != null && metadata.isGroupAssignmentMissing() ? Boolean.TRUE : null;

            return new UserWeeklyUsage(
                    userId,
                    experimentPhase,
                    group,
                    nudged,
                    groupMissing,
                    activeDayStrings,
                    courseIds.size(),
                    activitySummaries,
                    tasksAttempted,
                    successfulTasks,
                    successRate,
                    averageRetries,
                    totalTimeSeconds,
                    hintsUsed,
                    newActivities,
                    totalRetries,
                    retrySamples
            );
        }
    }

    private static class ActivityUsageAccumulator {
        private final String courseId;
        private final String activityId;
        private int tasksAttempted;
        private int successfulTasks;
        private long totalTimeSeconds;
        private long totalRetries;
        private int retrySamples;
        private int hintsUsed;
        private boolean newContent;
        private final Set<LocalDate> activeDays = new HashSet<>();
        private double scoreAccumulator;

        ActivityUsageAccumulator(String courseId, String activityId) {
            this.courseId = courseId;
            this.activityId = activityId;
        }

        void registerAttempt(Instant attemptInstant, Map<String, Object> attempt) {
            tasksAttempted++;
            activeDays.add(LocalDateTime.ofInstant(attemptInstant, ZONE).toLocalDate());

            double scoreRatio = resolveScoreRatio(attempt);
            scoreAccumulator += scoreRatio;
            if (scoreRatio >= 0.999) {
                successfulTasks++;
            }

            Integer retries = asInteger(attempt.get("retries"));
            if (retries != null) {
                totalRetries += retries;
                retrySamples++;
            }

            Boolean hints = asBoolean(attempt.get("hintsUsed"));
            if (Boolean.TRUE.equals(hints)) {
                hintsUsed++;
            }

            totalTimeSeconds += parseTimeSpentSeconds(asString(attempt.get("timeSpent")));
        }

        void setNewContent(boolean newContent) {
            this.newContent = this.newContent || newContent;
        }

        ActivityUsage toSummary() {
            double successRate = tasksAttempted == 0 ? 0.0 : scoreAccumulator / tasksAttempted;
            double averageRetries = retrySamples == 0 ? 0.0 : (double) totalRetries / retrySamples;

            List<String> activeDayStrings = new ArrayList<>();
            List<LocalDate> sortedDays = new ArrayList<>(activeDays);
            Collections.sort(sortedDays);
            for (LocalDate day : sortedDays) {
                activeDayStrings.add(DATE_FORMAT.format(day));
            }

            return new ActivityUsage(
                    courseId,
                    activityId,
                    tasksAttempted,
                    successfulTasks,
                    successRate,
                    averageRetries,
                    totalTimeSeconds,
                    hintsUsed,
                    newContent,
                    activeDayStrings
            );
        }
    }

    public static class WeeklyUsageSummary {
        private final String version;
        private final String generatedAtUtc;
        private final String weekStart;
        private final String weekEnd;
        private final int activeUsers;
        private final Map<String, AggregateMetrics> countsByCondition;
        private final Map<String, Map<String, AggregateMetrics>> countsByPhaseAndGroup;
        private final List<UserWeeklyUsage> users;

        WeeklyUsageSummary(
                String version,
                String generatedAtUtc,
                String weekStart,
                String weekEnd,
                int activeUsers,
                Map<String, AggregateMetrics> countsByCondition,
                Map<String, Map<String, AggregateMetrics>> countsByPhaseAndGroup,
                List<UserWeeklyUsage> users) {
            this.version = version;
            this.generatedAtUtc = generatedAtUtc;
            this.weekStart = weekStart;
            this.weekEnd = weekEnd;
            this.activeUsers = activeUsers;
            this.countsByCondition = countsByCondition;
            this.countsByPhaseAndGroup = countsByPhaseAndGroup;
            this.users = users;
        }

        public String getVersion() {
            return version;
        }

        public String getGeneratedAtUtc() {
            return generatedAtUtc;
        }

        public String getWeekStart() {
            return weekStart;
        }

        public String getWeekEnd() {
            return weekEnd;
        }

        public int getActiveUsers() {
            return activeUsers;
        }

        public Map<String, AggregateMetrics> getCountsByCondition() {
            return countsByCondition;
        }

        public Map<String, Map<String, AggregateMetrics>> getCountsByPhaseAndGroup() {
            return countsByPhaseAndGroup;
        }

        public List<UserWeeklyUsage> getUsers() {
            return users;
        }
    }

    public static class UserWeeklyUsage {
        private final String userId;
        private final String experimentPhase;
        private final String group;
        private final boolean nudged;
        private final Boolean groupAssignmentMissing;
        private final List<String> activeDays;
        private final int courseCount;
        private final List<ActivityUsage> areas;
        private final int tasksAttempted;
        private final int successfulTasks;
        private final double successRate;
        private final double averageRetries;
        private final long totalTimeSpentSeconds;
        private final int hintsUsed;
        private final int newActivitiesExplored;
        private final transient long totalRetries;
        private final transient int retrySamples;

        UserWeeklyUsage(
                String userId,
                String experimentPhase,
                String group,
                boolean nudged,
                Boolean groupAssignmentMissing,
                List<String> activeDays,
                int courseCount,
                List<ActivityUsage> areas,
                int tasksAttempted,
                int successfulTasks,
                double successRate,
                double averageRetries,
                long totalTimeSpentSeconds,
                int hintsUsed,
                int newActivitiesExplored,
                long totalRetries,
                int retrySamples) {
            this.userId = userId;
            this.experimentPhase = experimentPhase;
            this.group = group;
            this.nudged = nudged;
            this.groupAssignmentMissing = groupAssignmentMissing;
            this.activeDays = activeDays;
            this.courseCount = courseCount;
            this.areas = areas;
            this.tasksAttempted = tasksAttempted;
            this.successfulTasks = successfulTasks;
            this.successRate = successRate;
            this.averageRetries = averageRetries;
            this.totalTimeSpentSeconds = totalTimeSpentSeconds;
            this.hintsUsed = hintsUsed;
            this.newActivitiesExplored = newActivitiesExplored;
            this.totalRetries = totalRetries;
            this.retrySamples = retrySamples;
        }

        public String getUserId() {
            return userId;
        }

        public String getExperimentPhase() {
            return experimentPhase;
        }

        public String getGroup() {
            return group;
        }

        public boolean isNudged() {
            return nudged;
        }

        public Boolean getGroupAssignmentMissing() {
            return groupAssignmentMissing;
        }

        public List<String> getActiveDays() {
            return activeDays;
        }

        public int getCourseCount() {
            return courseCount;
        }

        public List<ActivityUsage> getAreas() {
            return areas;
        }

        public int getTasksAttempted() {
            return tasksAttempted;
        }

        public int getSuccessfulTasks() {
            return successfulTasks;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public double getAverageRetries() {
            return averageRetries;
        }

        public long getTotalTimeSpentSeconds() {
            return totalTimeSpentSeconds;
        }

        public int getHintsUsed() {
            return hintsUsed;
        }

        public int getNewActivitiesExplored() {
            return newActivitiesExplored;
        }

        long getTotalRetries() {
            return totalRetries;
        }

        int getRetrySamples() {
            return retrySamples;
        }
    }

    private static class AggregateCollector {
        private final Map<Boolean, RunningTotals> conditionTotals = new HashMap<>();
        private final Map<String, Map<String, RunningTotals>> phaseGroupTotals = new HashMap<>();

        void include(UserWeeklyUsage usage) {
            if (usage == null) {
                return;
            }
            boolean nudged = usage.isNudged();
            RunningTotals condition = conditionTotals.computeIfAbsent(nudged, ignored -> new RunningTotals());
            condition.include(usage);

            String phase = usage.getExperimentPhase() == null ? PHASE_SEQUENCE[0] : usage.getExperimentPhase();
            String group = usage.getGroup() == null ? "A" : usage.getGroup();
            Map<String, RunningTotals> groupTotals = phaseGroupTotals.computeIfAbsent(phase, ignored -> new HashMap<>());
            RunningTotals totals = groupTotals.computeIfAbsent(group, ignored -> new RunningTotals());
            totals.include(usage);
        }

        Map<String, AggregateMetrics> buildCountsByCondition() {
            Map<String, AggregateMetrics> result = new LinkedHashMap<>();
            result.put("nudged", AggregateMetrics.fromTotals(conditionTotals.get(Boolean.TRUE), null));
            result.put("nonNudged", AggregateMetrics.fromTotals(conditionTotals.get(Boolean.FALSE), null));
            return result;
        }

        Map<String, Map<String, AggregateMetrics>> buildCountsByPhaseAndGroup() {
            Map<String, Map<String, AggregateMetrics>> result = new LinkedHashMap<>();
            for (String phase : PHASE_SEQUENCE) {
                Map<String, AggregateMetrics> groups = new LinkedHashMap<>();
                Map<String, RunningTotals> totalsByGroup = phaseGroupTotals.get(phase);
                for (String group : EXPERIMENT_GROUPS) {
                    RunningTotals totals = totalsByGroup == null ? null : totalsByGroup.get(group);
                    boolean nudged = computeNudged(phase, group);
                    groups.put(group, AggregateMetrics.fromTotals(totals, nudged));
                }
                result.put(phase, groups);
            }
            return result;
        }
    }

    public static class AggregateMetrics {
        private final int activeUsers;
        private final int totalTasks;
        private final long totalTimeSpentSeconds;
        private final double avgRetries;
        private final int hintsUsed;
        private final double successRate;
        private final Boolean nudged;

        AggregateMetrics(int activeUsers,
                         int totalTasks,
                         long totalTimeSpentSeconds,
                         double avgRetries,
                         int hintsUsed,
                         double successRate,
                         Boolean nudged) {
            this.activeUsers = activeUsers;
            this.totalTasks = totalTasks;
            this.totalTimeSpentSeconds = totalTimeSpentSeconds;
            this.avgRetries = avgRetries;
            this.hintsUsed = hintsUsed;
            this.successRate = successRate;
            this.nudged = nudged;
        }

        static AggregateMetrics fromTotals(RunningTotals totals, Boolean nudged) {
            if (totals == null) {
                totals = new RunningTotals();
            }
            double avgRetries = totals.retrySamples == 0 ? 0.0 : (double) totals.totalRetries / totals.retrySamples;
            double successRate = totals.totalTasks == 0 ? 0.0 : (double) totals.successfulTasks / totals.totalTasks;
            return new AggregateMetrics(
                    totals.activeUsers,
                    totals.totalTasks,
                    totals.totalTimeSeconds,
                    avgRetries,
                    totals.hintsUsed,
                    successRate,
                    nudged
            );
        }

        public int getActiveUsers() {
            return activeUsers;
        }

        public int getTotalTasks() {
            return totalTasks;
        }

        public long getTotalTimeSpentSeconds() {
            return totalTimeSpentSeconds;
        }

        public double getAvgRetries() {
            return avgRetries;
        }

        public int getHintsUsed() {
            return hintsUsed;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public Boolean getNudged() {
            return nudged;
        }
    }

    private static class RunningTotals {
        private int activeUsers;
        private int totalTasks;
        private int successfulTasks;
        private long totalTimeSeconds;
        private long totalRetries;
        private int retrySamples;
        private int hintsUsed;

        void include(UserWeeklyUsage usage) {
            activeUsers++;
            totalTasks += usage.getTasksAttempted();
            successfulTasks += usage.getSuccessfulTasks();
            totalTimeSeconds += usage.getTotalTimeSpentSeconds();
            totalRetries += usage.getTotalRetries();
            retrySamples += usage.getRetrySamples();
            hintsUsed += usage.getHintsUsed();
        }
    }

    public static class ActivityUsage {
        private final String courseId;
        private final String activityId;
        private final int tasksAttempted;
        private final int successfulTasks;
        private final double successRate;
        private final double averageRetries;
        private final long totalTimeSpentSeconds;
        private final int hintsUsed;
        private final boolean newContent;
        private final List<String> activeDays;

        ActivityUsage(
                String courseId,
                String activityId,
                int tasksAttempted,
                int successfulTasks,
                double successRate,
                double averageRetries,
                long totalTimeSpentSeconds,
                int hintsUsed,
                boolean newContent,
                List<String> activeDays) {
            this.courseId = courseId;
            this.activityId = activityId;
            this.tasksAttempted = tasksAttempted;
            this.successfulTasks = successfulTasks;
            this.successRate = successRate;
            this.averageRetries = averageRetries;
            this.totalTimeSpentSeconds = totalTimeSpentSeconds;
            this.hintsUsed = hintsUsed;
            this.newContent = newContent;
            this.activeDays = activeDays;
        }

        public String getCourseId() {
            return courseId;
        }

        public String getActivityId() {
            return activityId;
        }

        public int getTasksAttempted() {
            return tasksAttempted;
        }

        public int getSuccessfulTasks() {
            return successfulTasks;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public double getAverageRetries() {
            return averageRetries;
        }

        public long getTotalTimeSpentSeconds() {
            return totalTimeSpentSeconds;
        }

        public int getHintsUsed() {
            return hintsUsed;
        }

        public boolean isNewContent() {
            return newContent;
        }

        public List<String> getActiveDays() {
            return activeDays;
        }
    }
}
