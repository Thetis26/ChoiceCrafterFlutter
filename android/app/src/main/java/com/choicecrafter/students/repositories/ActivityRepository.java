package com.choicecrafter.students.repositories;

import com.choicecrafter.students.models.EnrollmentActivityProgress;
import com.choicecrafter.students.models.TaskStats;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityRepository {

    private static final String ENROLLMENTS_COLLECTION = "COURSE_ENROLLMENTS";
    private final FirebaseFirestore firestore;

    public ActivityRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public interface StartActivityCallback {
        void onSuccess(EnrollmentActivityProgress activityProgress);
        void onFailure(Exception e);
    }

    private String buildEnrollmentId(String userId, String courseId) {
        return userId + "_" + courseId;
    }

    public void startActivity(String userId,
                              String courseId,
                              String activityId,
                              StartActivityCallback callback) {
        DocumentReference documentReference = firestore.collection(ENROLLMENTS_COLLECTION)
                .document(buildEnrollmentId(userId, courseId));

        documentReference.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                callback.onFailure(new IllegalStateException("Enrollment document not found for user: " + userId + ", course: " + courseId));
                return;
            }

            Map<String, Object> progressSummary = getProgressSummary(documentSnapshot);
            List<Map<String, Object>> activitySnapshots = getActivitySnapshots(progressSummary);
            Map<String, Object> snapshot = getOrCreateActivitySnapshot(activitySnapshots, userId, courseId, activityId);
            EnrollmentActivityProgress progress = EnrollmentActivityProgress.fromMap(snapshot);

            saveProgressSummary(documentReference, progressSummary)
                    .addOnSuccessListener(unused -> callback.onSuccess(progress))
                    .addOnFailureListener(callback::onFailure);
        }).addOnFailureListener(callback::onFailure);
    }

    public void addTaskStats(String userId,
                             String courseId,
                             String activityId,
                             String taskId,
                             TaskStats taskStats) {
        DocumentReference documentReference = firestore.collection(ENROLLMENTS_COLLECTION)
                .document(buildEnrollmentId(userId, courseId));

        firestore.runTransaction(transaction -> {
            DocumentSnapshot documentSnapshot = transaction.get(documentReference);
            if (!documentSnapshot.exists()) {
                return null;
            }

            Map<String, Object> progressSummary = getProgressSummary(documentSnapshot);
            List<Map<String, Object>> activitySnapshots = getActivitySnapshots(progressSummary);
            Map<String, Object> snapshot = getOrCreateActivitySnapshot(activitySnapshots, userId, courseId, activityId);

            Map<String, Object> taskStatsMap = ensureTaskStatsMap(snapshot);
            taskStatsMap.put(taskId, convertTaskStats(taskStats));
            snapshot.put("taskStats", taskStatsMap);

            transaction.set(documentReference, buildProgressUpdate(progressSummary), SetOptions.merge());
            return null;
        });
    }

    public void resetTaskStats(String userId, String courseId, String activityId) {
        DocumentReference documentReference = firestore.collection(ENROLLMENTS_COLLECTION)
                .document(buildEnrollmentId(userId, courseId));

        documentReference.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                return;
            }

            Map<String, Object> progressSummary = getProgressSummary(documentSnapshot);
            List<Map<String, Object>> activitySnapshots = getActivitySnapshots(progressSummary);
            Map<String, Object> snapshot = getOrCreateActivitySnapshot(activitySnapshots, userId, courseId, activityId);

            snapshot.put("taskStats", new HashMap<>());
            snapshot.remove("highestScore");

            saveProgressSummary(documentReference, progressSummary);
        });
    }

    public void updateHighestScoreIfGreater(String userId,
                                            String courseId,
                                            String activityId,
                                            int score) {
        DocumentReference documentReference = firestore.collection(ENROLLMENTS_COLLECTION)
                .document(buildEnrollmentId(userId, courseId));

        documentReference.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                return;
            }

            Map<String, Object> progressSummary = getProgressSummary(snapshot);
            List<Map<String, Object>> activitySnapshots = getActivitySnapshots(progressSummary);
            Map<String, Object> activitySnapshot = getOrCreateActivitySnapshot(activitySnapshots, userId, courseId, activityId);

            Object highestScoreObj = activitySnapshot.get("highestScore");
            if (!(highestScoreObj instanceof Number) || score > ((Number) highestScoreObj).intValue()) {
                activitySnapshot.put("highestScore", score);
                saveProgressSummary(documentReference, progressSummary);
            }
        });
    }

    private Map<String, Object> getProgressSummary(DocumentSnapshot documentSnapshot) {
        Object summary = documentSnapshot.get("progressSummary");
        if (summary instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getActivitySnapshots(Map<String, Object> progressSummary) {
        Object snapshots = progressSummary.get("activitySnapshots");
        if (snapshots instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> snapshotMap = new HashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        snapshotMap.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    result.add(snapshotMap);
                }
            }
            progressSummary.put("activitySnapshots", result);
            return result;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        progressSummary.put("activitySnapshots", result);
        return result;
    }

    private Map<String, Object> getOrCreateActivitySnapshot(List<Map<String, Object>> activitySnapshots,
                                                            String userId,
                                                            String courseId,
                                                            String activityId) {
        for (Map<String, Object> snapshot : activitySnapshots) {
            Object activityIdObj = snapshot.get("activityId");
            if (activityIdObj != null && activityIdObj.toString().equals(activityId)) {
                ensureSnapshotIdentifiers(snapshot, userId, courseId, activityId);
                return snapshot;
            }
        }
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("activityId", activityId);
        snapshot.put("courseId", courseId);
        snapshot.put("userId", userId);
        snapshot.put("taskStats", new HashMap<>());
        activitySnapshots.add(snapshot);
        return snapshot;
    }

    private void ensureSnapshotIdentifiers(Map<String, Object> snapshot,
                                           String userId,
                                           String courseId,
                                           String activityId) {
        snapshot.put("userId", userId);
        snapshot.put("courseId", courseId);
        snapshot.put("activityId", activityId);
        if (!(snapshot.get("taskStats") instanceof Map)) {
            snapshot.put("taskStats", new HashMap<>());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> ensureTaskStatsMap(Map<String, Object> snapshot) {
        Object stats = snapshot.get("taskStats");
        if (stats instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            snapshot.put("taskStats", result);
            return result;
        }
        Map<String, Object> result = new HashMap<>();
        snapshot.put("taskStats", result);
        return result;
    }

    private Map<String, Object> convertTaskStats(TaskStats taskStats) {
        Map<String, Object> map = new HashMap<>();
        if (taskStats.getAttemptDateTime() != null) {
            map.put("attemptDateTime", taskStats.getAttemptDateTime());
        }
        if (taskStats.getTimeSpent() != null) {
            map.put("timeSpent", taskStats.getTimeSpent());
        }
        if (taskStats.getRetries() != null) {
            map.put("retries", taskStats.getRetries());
        }
        if (taskStats.getSuccess() != null) {
            map.put("success", taskStats.getSuccess());
        }
        if (taskStats.getHintsUsed() != null) {
            map.put("hintsUsed", taskStats.getHintsUsed());
        }
        Double completionRatio = taskStats.getCompletionRatio();
        if (completionRatio != null) {
            map.put("completionRatio", completionRatio);
        }
        Double scoreRatio = taskStats.getScoreRatio();
        if (scoreRatio != null) {
            map.put("scoreRatio", scoreRatio);
        }
        return map;
    }

    private Task<Void> saveProgressSummary(DocumentReference documentReference,
                                           Map<String, Object> progressSummary) {
        return documentReference.set(buildProgressUpdate(progressSummary), SetOptions.merge());
    }

    private Map<String, Object> buildProgressUpdate(Map<String, Object> progressSummary) {
        Map<String, Object> update = new HashMap<>();
        update.put("progressSummary", progressSummary);
        return update;
    }
}
