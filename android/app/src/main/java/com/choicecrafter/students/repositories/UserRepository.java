package com.choicecrafter.students.repositories;

import android.util.Log;

import com.choicecrafter.students.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class UserRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final ZoneId ZONE = ZoneId.of("Europe/Bucharest");
    private static final DateTimeFormatter FLEX_PARSER =
            new DateTimeFormatterBuilder()
                    .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .optionalStart().appendOffsetId().optionalEnd()
                    .toFormatter();

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("uuuu-MM-dd");

    public interface DailyScoresCallback {
        void onComplete(Map<String, Long> dailyScores);
    }

    // In UserRepository.java
    public static void buildDailyScores(
            FirebaseFirestore db,
            String userEmail,
            DailyScoresCallback callback) {
        Map<String, Long> daily = new TreeMap<>();
        Log.i("UserRepository", "Building daily scores for user: " + userEmail);
        db.collection("COURSE_ENROLLMENTS")
                .whereEqualTo("userId", userEmail)
                .get()
                .addOnSuccessListener(task -> {
                    for (DocumentSnapshot doc : task.getDocuments()) {
                        Log.i("UserRepository", "Fetched enrollment document: " + doc.getId());
                        Object progressSummaryObj = doc.get("progressSummary");
                        if (!(progressSummaryObj instanceof Map<?, ?> progressSummary)) {
                            continue;
                        }
                        Object activitySnapshotsObj = progressSummary.get("activitySnapshots");
                        if (!(activitySnapshotsObj instanceof List<?> snapshots)) {
                            continue;
                        }
                        for (Object entry : snapshots) {
                            if (!(entry instanceof Map<?, ?> snapshotMapRaw)) {
                                continue;
                            }
                            Map<String, Object> snapshotMap = new HashMap<>();
                            for (Map.Entry<?, ?> mapEntry : snapshotMapRaw.entrySet()) {
                                snapshotMap.put(String.valueOf(mapEntry.getKey()), mapEntry.getValue());
                            }
                            Object taskStatsObj = snapshotMap.get("taskStats");
                            if (!(taskStatsObj instanceof Map<?, ?> taskStatsMap)) {
                                continue;
                            }
                            for (Object value : taskStatsMap.values()) {
                                if (!(value instanceof Map<?, ?> attemptMap)) {
                                    continue;
                                }
                                @SuppressWarnings("unchecked")
                                Map<String, Object> attempt = (Map<String, Object>) attemptMap;

                                String attemptDateTime = asString(attempt.get("attemptDateTime"));
                                if (attemptDateTime == null || attemptDateTime.isEmpty()) {
                                    continue;
                                }

                                Instant instant = parseToInstant(attemptDateTime);
                                String dayKey = DAY_FMT.withZone(ZONE).format(instant);

                                long points = attemptScore(attempt);
                                daily.merge(dayKey, points, Long::sum);
                            }
                        }
                    }
                    Log.i("UserRepository", "Daily scores: " + daily);
                    callback.onComplete(daily);
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Failed to fetch daily scores", e);
                    callback.onComplete(daily); // return empty map on failure
                });
    }

    private static int attemptScore(Map<String, Object> attempt) {
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
            Object s = attempt.get("success");
            boolean success = (s instanceof Boolean) ? (Boolean) s : false;
            ratio = success ? 1.0 : 0.0;
        }
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        // Each task used to award up to 100 points. Reduce the value by ~60% so a
        // perfect attempt now grants 40 points.
        return (int) Math.round(clamped * 40.0);
    }

    private static Instant parseToInstant(String s) {
        TemporalAccessor ta = FLEX_PARSER.parse(s);
        return ta.isSupported(ChronoField.OFFSET_SECONDS)
                ? OffsetDateTime.from(ta).toInstant()
                : LocalDateTime.from(ta).atZone(ZONE).toInstant();
    }

    private static String asString(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }

    public void updateUserScores(User user, Runnable onSuccess, java.util.function.Consumer<Exception> onFailure) {
        db.collection("users")
                .whereEqualTo("email", user.getEmail())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        db.collection("users")
                                .document(docId)
                                .update("scores", user.getScores())
                                .addOnSuccessListener(aVoid -> {
                                    Log.i("UserRepository", "Scores updated for user: " + user.getEmail());
                                    if (onSuccess != null) onSuccess.run();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("UserRepository", "Failed to update scores", e);
                                    if (onFailure != null) onFailure.accept(e);
                                });
                    } else {
                        Log.e("UserRepository", "No user found with email: " + user.getEmail());
                        if (onFailure != null) onFailure.accept(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Failed to query user by email", e);
                    if (onFailure != null) onFailure.accept(e);
                });
    }

    public void updateUserBadges(User user, Runnable onSuccess, Consumer<Exception> onFailure) {
        if (user == null || user.getEmail() == null) {
            if (onFailure != null) {
                onFailure.accept(new IllegalArgumentException("User or email is null"));
            }
            return;
        }
        db.collection("users")
                .whereEqualTo("email", user.getEmail())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        db.collection("users")
                                .document(docId)
                                .update("badges", user.getBadges())
                                .addOnSuccessListener(unused -> {
                                    if (onSuccess != null) {
                                        onSuccess.run();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (onFailure != null) {
                                        onFailure.accept(e);
                                    }
                                });
                    } else if (onFailure != null) {
                        onFailure.accept(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) {
                        onFailure.accept(e);
                    }
                });
    }
}
