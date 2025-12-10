package com.choicecrafter.students.repositories;

import android.util.Log;

import com.choicecrafter.students.models.Activity;
import com.choicecrafter.students.models.Course;
import com.choicecrafter.students.models.CourseEnrollment;
import com.choicecrafter.students.models.CourseProgress;
import com.choicecrafter.students.models.Module;
import com.choicecrafter.students.models.ModuleProgress;
import com.choicecrafter.students.models.TaskStats;
import com.choicecrafter.students.models.tasks.Task;
import com.choicecrafter.students.utils.ActivityScoreCalculator;
import com.choicecrafter.students.utils.TaskStatsKeyUtils;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CourseEnrollmentRepository {

    private static final String TAG = "CourseEnrollmentRepo";
    private static final String ENROLLMENTS_COLLECTION = "COURSE_ENROLLMENTS";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CourseRepository courseRepository = new CourseRepository();

    public interface Callback<T> {
        void onSuccess(T result);

        void onFailure(Exception e);
    }

    public void fetchEnrollmentsForUser(String userId, Callback<List<CourseEnrollment>> callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        firestore.collection(ENROLLMENTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<com.google.android.gms.tasks.Task<CourseEnrollment>> enrollmentTasks = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                        enrollmentTasks.add(buildEnrollmentTask(documentSnapshot));
                    }

                    Tasks.whenAllSuccess(enrollmentTasks)
                            .addOnSuccessListener(results -> {
                                List<CourseEnrollment> enrollments = new ArrayList<>();
                                for (Object result : results) {
                                    if (result instanceof CourseEnrollment enrollment) {
                                        enrollments.add(enrollment);
                                    }
                                }
                                callback.onSuccess(enrollments);
                            })
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void fetchEnrollmentForCourse(String userId,
                                         String courseId,
                                         Callback<CourseEnrollment> callback) {
        if (callback == null) {
            return;
        }
        if (userId == null || userId.isEmpty() || courseId == null || courseId.isEmpty()) {
            callback.onSuccess(null);
            return;
        }

        firestore.collection(ENROLLMENTS_COLLECTION)
                .document(userId + "_" + courseId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        callback.onSuccess(null);
                        return;
                    }
                    com.google.android.gms.tasks.Task<CourseEnrollment> enrollmentTask = buildEnrollmentTask(snapshot);
                    enrollmentTask.addOnSuccessListener(callback::onSuccess)
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }

    private com.google.android.gms.tasks.Task<CourseEnrollment> buildEnrollmentTask(DocumentSnapshot documentSnapshot) {
        TaskCompletionSource<CourseEnrollment> taskCompletionSource = new TaskCompletionSource<>();
        CourseEnrollment enrollment = documentSnapshot.toObject(CourseEnrollment.class);
        if (enrollment == null) {
            taskCompletionSource.setException(new IllegalStateException("Failed to parse enrollment document"));
            return taskCompletionSource.getTask();
        }
        enrollment.setId(documentSnapshot.getId());

        courseRepository.getCourseById(enrollment.getCourseId(), new CourseRepository.Callback<>() {
            @Override
            public void onSuccess(Course course) {
                enrollment.setCourse(course);
                CourseProgress progress = buildProgress(enrollment, documentSnapshot);
                enrollment.setProgress(progress);
                taskCompletionSource.setResult(enrollment);
            }

            @Override
            public void onFailure(Exception e) {
                taskCompletionSource.setException(e);
            }
        });

        return taskCompletionSource.getTask();
    }

    public void enrollUserInCourse(String userId, String courseId, Callback<Void> callback) {
        if (userId == null || userId.isEmpty() || courseId == null || courseId.isEmpty()) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("User id and course id are required for enrollment"));
            }
            return;
        }

        String documentId = userId + "_" + courseId;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> enrollmentData = new HashMap<>();
        enrollmentData.put("userId", userId);
        enrollmentData.put("courseId", courseId);
        enrollmentData.put("enrollmentDate", today);
        enrollmentData.put("selfEnrolled", true);
        enrollmentData.put("enrolledBy", userId);
        enrollmentData.put("createdAt", Timestamp.now());

        firestore.collection(ENROLLMENTS_COLLECTION)
                .document(documentId)
                .set(enrollmentData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.i(TAG, "User " + userId + " enrolled in course " + courseId);
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to enroll user " + userId + " in course " + courseId, e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    private CourseProgress buildProgress(CourseEnrollment enrollment,
                                         DocumentSnapshot enrollmentSnapshot) {
        int totalActivities = 0;
        int totalTasks = 0;
        Map<String, String> activityToModule = new HashMap<>();
        Map<String, ModuleAccumulator> moduleAccumulators = new HashMap<>();
        Map<String, ModuleProgress> legacyModuleProgress = new HashMap<>();
        Map<String, Activity> activityLookup = new HashMap<>();
        int totalEarnedXp = 0;
        int totalAvailableXp = 0;

        if (enrollment.getCourse() != null) {
            if (enrollment.getCourse().getActivities() != null && !enrollment.getCourse().getActivities().isEmpty()) {
                totalActivities = enrollment.getCourse().getActivities().size();
                for (Activity activity : enrollment.getCourse().getActivities()) {
                    if (activity == null) {
                        continue;
                    }
                    indexActivity(activity, null, activityToModule, activityLookup);
                    if (activity.getTasks() != null) {
                        totalTasks += activity.getTasks().size();
                    }
                }
            } else if (enrollment.getCourse().getModules() != null && !enrollment.getCourse().getModules().isEmpty()) {
                List<Module> modules = enrollment.getCourse().getModules();
                for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
                    Module module = modules.get(moduleIndex);
                    if (module == null) {
                        continue;
                    }
                    String moduleKey = resolveModuleKey(module, moduleIndex);
                    ModuleAccumulator accumulator = moduleAccumulators.computeIfAbsent(moduleKey, key -> new ModuleAccumulator());
                    List<Activity> moduleActivities = module.getActivities();
                    if (moduleActivities != null) {
                        totalActivities += moduleActivities.size();
                        for (Activity activity : moduleActivities) {
                            if (activity == null) {
                                continue;
                            }
                            indexActivity(activity, moduleKey, activityToModule, activityLookup);
                            int taskCount = activity.getTasks() != null ? activity.getTasks().size() : 0;
                            totalTasks += taskCount;
                            accumulator.totalTasks += taskCount;
                        }
                    }
                }
            }
        }

        int activitiesStarted = 0;
        int attemptedTasks = 0;
        int completedTasks = 0;
        List<Map<String, Object>> activitySnapshots = new ArrayList<>();

        if (enrollmentSnapshot != null) {
            Object progressSummaryObj = enrollmentSnapshot.get("progressSummary");
            if (progressSummaryObj instanceof Map<?, ?> progressSummary) {
                Object activitySnapshotsObj = progressSummary.get("activitySnapshots");
                if (activitySnapshotsObj instanceof List<?> snapshotsList) {
                    for (Object entry : snapshotsList) {
                        if (entry instanceof Map<?, ?> snapshotMapRaw) {
                            Map<String, Object> snapshotMap = new HashMap<>();
                            for (Map.Entry<?, ?> snapshotEntry : snapshotMapRaw.entrySet()) {
                                snapshotMap.put(String.valueOf(snapshotEntry.getKey()), snapshotEntry.getValue());
                            }
                            activitySnapshots.add(snapshotMap);

                            String moduleKey = findModuleKeyForSnapshot(activityToModule, snapshotMap);
                            ModuleAccumulator moduleAccumulator = moduleKey != null
                                    ? moduleAccumulators.get(moduleKey)
                                    : null;

                            Object taskStatsObj = snapshotMap.get("taskStats");
                            if (taskStatsObj instanceof Map<?, ?> taskStatsMap) {
                                boolean hasAttempts = false;
                                Map<String, TaskStats> rawStats = new HashMap<>();
                                for (Map.Entry<?, ?> taskEntry : taskStatsMap.entrySet()) {
                                    TaskStats taskStats = mapToTaskStats(taskEntry.getValue());
                                    if (taskStats == null) {
                                        continue;
                                    }

                                    String key = String.valueOf(taskEntry.getKey());
                                    rawStats.put(key, taskStats);
                                    String normalizedKey = normalizeString(key);
                                    if (!normalizedKey.isEmpty()) {
                                        rawStats.putIfAbsent(normalizedKey, taskStats);
                                        rawStats.putIfAbsent(normalizedKey.toLowerCase(Locale.ROOT), taskStats);
                                    }

                                    boolean attempted = taskStats.resolveCompletionRatio() > 0.0
                                            || taskStats.getRetries() != null
                                            || taskStats.getTimeSpent() != null
                                            || taskStats.getAttemptDateTime() != null;
                                    if (attempted) {
                                        hasAttempts = true;
                                        attemptedTasks++;
                                    }

                                    if (taskStats.isCompleted()) {
                                        completedTasks++;
                                        if (moduleAccumulator != null) {
                                            moduleAccumulator.completedTasks++;
                                        }
                                    }
                                }
                                if (hasAttempts) {
                                    activitiesStarted++;
                                }

                                Activity activity = findActivityForSnapshot(activityLookup, snapshotMap);
                                if (activity != null) {
                                    Map<String, TaskStats> statsForCalculation = rawStats.isEmpty()
                                            ? rawStats
                                            : enrichStatsMap(activity, rawStats);
                                    totalEarnedXp += ActivityScoreCalculator.calculateEarnedXp(activity.getTasks(),
                                            statsForCalculation);
                                    totalAvailableXp += ActivityScoreCalculator.calculateTotalXp(activity.getTasks(),
                                            statsForCalculation);
                                }
                            }
                        }
                    }
                }

                Object moduleProgressObj = progressSummary.get("moduleProgress");
                if (moduleProgressObj instanceof Map<?, ?> moduleMap) {
                    for (Map.Entry<?, ?> moduleEntry : moduleMap.entrySet()) {
                        if (moduleEntry.getValue() instanceof Map<?, ?> moduleValues) {
                            ModuleProgress module = new ModuleProgress();
                            Object completed = moduleValues.get("completedTasks");
                            if (completed instanceof Number number) {
                                module.setCompletedTasks(number.intValue());
                            }
                            Object total = moduleValues.get("totalTasks");
                            if (total instanceof Number number) {
                                module.setTotalTasks(number.intValue());
                            }
                            legacyModuleProgress.put(String.valueOf(moduleEntry.getKey()), module);
                        }
                    }
                }
            }
        }

        Map<String, ModuleProgress> moduleProgressMap = new HashMap<>();
        for (Map.Entry<String, ModuleAccumulator> entry : moduleAccumulators.entrySet()) {
            ModuleAccumulator accumulator = entry.getValue();
            moduleProgressMap.put(entry.getKey(), new ModuleProgress(accumulator.completedTasks, accumulator.totalTasks));
        }
        for (Map.Entry<String, ModuleProgress> entry : legacyModuleProgress.entrySet()) {
            moduleProgressMap.putIfAbsent(entry.getKey(), entry.getValue());
        }

        double completionPercentage = totalTasks == 0 ? 0 : (completedTasks * 100.0) / totalTasks;
        CourseProgress progress = new CourseProgress(totalActivities, activitiesStarted, totalTasks, attemptedTasks, completedTasks, completionPercentage);
        progress.setActivitySnapshots(activitySnapshots);
        progress.setModuleProgress(moduleProgressMap);
        progress.setEarnedXp(totalEarnedXp);
        progress.setTotalXp(totalAvailableXp);

        return progress;
    }

    private String resolveModuleKey(Module module, int index) {
        String moduleId = normalizeString(module != null ? module.getId() : null);
        if (!moduleId.isEmpty()) {
            return moduleId;
        }
        String title = normalizeString(module != null ? module.getTitle() : null);
        if (!title.isEmpty()) {
            return title;
        }
        return "module_" + index;
    }

    private void indexActivity(Activity activity,
                               String moduleKey,
                               Map<String, String> activityToModule,
                               Map<String, Activity> activityLookup) {
        if (activity == null) {
            return;
        }

        if (activityLookup != null) {
            String activityId = normalizeString(activity.getId());
            if (!activityId.isEmpty()) {
                activityLookup.put(activityId, activity);
                activityLookup.putIfAbsent(activityId.toLowerCase(Locale.ROOT), activity);
            }
            String title = normalizeString(activity.getTitle());
            if (!title.isEmpty()) {
                activityLookup.putIfAbsent(title, activity);
                activityLookup.putIfAbsent(title.toLowerCase(Locale.ROOT), activity);
            }
        }

        if (moduleKey == null || activityToModule == null) {
            return;
        }

        String activityId = normalizeString(activity.getId());
        if (!activityId.isEmpty()) {
            activityToModule.put(activityId, moduleKey);
            activityToModule.putIfAbsent(activityId.toLowerCase(Locale.ROOT), moduleKey);
        }
        String title = normalizeString(activity.getTitle());
        if (!title.isEmpty()) {
            activityToModule.putIfAbsent(title, moduleKey);
            activityToModule.putIfAbsent(title.toLowerCase(Locale.ROOT), moduleKey);
        }
    }

    private String findModuleKeyForSnapshot(Map<String, String> activityToModule,
                                            Map<String, Object> snapshot) {
        if (snapshot == null || activityToModule == null || activityToModule.isEmpty()) {
            return null;
        }
        String[] keys = new String[]{"activityId", "activityTitle", "activityName"};
        for (String key : keys) {
            String identifier = normalize(snapshot.get(key));
            if (!identifier.isEmpty()) {
                String moduleKey = activityToModule.get(identifier);
                if (moduleKey == null) {
                    moduleKey = activityToModule.get(identifier.toLowerCase(Locale.ROOT));
                }
                if (moduleKey != null) {
                    return moduleKey;
                }
            }
        }
        return null;
    }

    private Activity findActivityForSnapshot(Map<String, Activity> activityLookup,
                                             Map<String, Object> snapshot) {
        if (snapshot == null || activityLookup == null || activityLookup.isEmpty()) {
            return null;
        }
        String[] keys = new String[]{"activityId", "activityTitle", "activityName"};
        for (String key : keys) {
            String identifier = normalize(snapshot.get(key));
            if (!identifier.isEmpty()) {
                Activity activity = activityLookup.get(identifier);
                if (activity == null) {
                    activity = activityLookup.get(identifier.toLowerCase(Locale.ROOT));
                }
                if (activity != null) {
                    return activity;
                }
            }
        }
        return null;
    }

    private Map<String, TaskStats> enrichStatsMap(Activity activity,
                                                  Map<String, TaskStats> rawStats) {
        if (activity == null || rawStats == null || rawStats.isEmpty()) {
            return rawStats;
        }
        Map<String, TaskStats> enriched = new HashMap<>(rawStats);
        List<Task> tasks = activity.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            return enriched;
        }
        for (Task task : tasks) {
            if (task == null) {
                continue;
            }
            String stableKey = TaskStatsKeyUtils.buildKey(task);
            if (enriched.containsKey(stableKey)) {
                continue;
            }
            TaskStats stats = tryResolveStatsForTask(task, rawStats);
            if (stats != null) {
                enriched.put(stableKey, stats);
            }
        }
        return enriched;
    }

    private TaskStats tryResolveStatsForTask(Task task,
                                             Map<String, TaskStats> rawStats) {
        if (task == null || rawStats == null || rawStats.isEmpty()) {
            return null;
        }
        String id = normalizeString(task.getId());
        if (!id.isEmpty()) {
            TaskStats stats = rawStats.get(id);
            if (stats == null) {
                stats = rawStats.get(id.toLowerCase(Locale.ROOT));
            }
            if (stats != null) {
                return stats;
            }
        }
        String title = normalizeString(task.getTitle());
        if (!title.isEmpty()) {
            TaskStats stats = rawStats.get(title);
            if (stats == null) {
                stats = rawStats.get(title.toLowerCase(Locale.ROOT));
            }
            if (stats != null) {
                return stats;
            }
        }
        for (Map.Entry<String, TaskStats> entry : rawStats.entrySet()) {
            String key = normalizeString(entry.getKey());
            if (!key.isEmpty() && !title.isEmpty() && key.equalsIgnoreCase(title)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalize(Object value) {
        return value != null ? value.toString().trim() : "";
    }

    private String normalizeString(String value) {
        return value != null ? value.trim() : "";
    }

    private TaskStats mapToTaskStats(Object value) {
        if (value instanceof TaskStats taskStats) {
            return taskStats;
        }
        if (value instanceof Map<?, ?> statsMap) {
            TaskStats taskStats = new TaskStats();
            Object attemptDateTime = ((Map<?, ?>) statsMap).get("attemptDateTime");
            if (attemptDateTime != null) {
                taskStats.setAttemptDateTime(String.valueOf(attemptDateTime));
            }
            Object timeSpent = ((Map<?, ?>) statsMap).get("timeSpent");
            if (timeSpent != null) {
                taskStats.setTimeSpent(String.valueOf(timeSpent));
            }
            Object retries = ((Map<?, ?>) statsMap).get("retries");
            if (retries instanceof Number number) {
                taskStats.setRetries(number.intValue());
            }
            Object success = ((Map<?, ?>) statsMap).get("success");
            if (success instanceof Boolean) {
                taskStats.setSuccess((Boolean) success);
            }
            Object hintsUsed = ((Map<?, ?>) statsMap).get("hintsUsed");
            if (hintsUsed instanceof Boolean) {
                taskStats.setHintsUsed((Boolean) hintsUsed);
            }
            Object completionRatio = ((Map<?, ?>) statsMap).get("completionRatio");
            if (completionRatio instanceof Number number) {
                taskStats.setCompletionRatio(number.doubleValue());
            } else if (completionRatio instanceof String ratioString) {
                try {
                    taskStats.setCompletionRatio(Double.parseDouble(ratioString));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed values but keep other stats
                }
            }
            Object scoreRatio = ((Map<?, ?>) statsMap).get("scoreRatio");
            if (scoreRatio instanceof Number number) {
                taskStats.setScoreRatio(number.doubleValue());
            } else if (scoreRatio instanceof String ratioString) {
                try {
                    taskStats.setScoreRatio(Double.parseDouble(ratioString));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed values but keep other stats
                }
            }
            return taskStats;
        }
        return null;
    }

    private static final class ModuleAccumulator {
        private int totalTasks;
        private int completedTasks;
    }

    public void seedDummyEnrollments(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot seed dummy enrollments without a user id");
            return;
        }
        courseRepository.fetchCourses(new CourseRepository.Callback<>() {
            @Override
            public void onSuccess(List<Course> courses) {
                if (courses == null || courses.isEmpty()) {
                    Log.w(TAG, "No courses available to seed dummy enrollments");
                    return;
                }

                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                Course firstCourse = courses.get(0);
                Map<String, Object> selfEnrollment = new HashMap<>();
                selfEnrollment.put("userId", userId);
                selfEnrollment.put("courseId", firstCourse.getId());
                selfEnrollment.put("enrollmentDate", today);
                selfEnrollment.put("selfEnrolled", true);
                selfEnrollment.put("enrolledBy", userId);
                selfEnrollment.put("createdAt", Timestamp.now());

                firestore.collection(ENROLLMENTS_COLLECTION)
                        .document(userId + "_" + firstCourse.getId())
                        .set(selfEnrollment, SetOptions.merge())
                        .addOnFailureListener(e -> Log.w(TAG, "Failed to seed self enrollment", e));

                if (courses.size() > 1) {
                    Course secondCourse = courses.get(1);
                    Map<String, Object> teacherEnrollment = new HashMap<>();
                    teacherEnrollment.put("userId", userId);
                    teacherEnrollment.put("courseId", secondCourse.getId());
                    teacherEnrollment.put("enrollmentDate", today);
                    teacherEnrollment.put("selfEnrolled", false);
                    teacherEnrollment.put("enrolledBy", "teacher@example.com");
                    teacherEnrollment.put("createdAt", Timestamp.now());

                    firestore.collection(ENROLLMENTS_COLLECTION)
                            .document(userId + "_" + secondCourse.getId())
                            .set(teacherEnrollment, SetOptions.merge())
                            .addOnFailureListener(e -> Log.w(TAG, "Failed to seed teacher enrollment", e));
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "Unable to seed dummy enrollments due to course fetch failure", e);
            }
        });
    }
}
