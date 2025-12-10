package com.choicecrafter.students.repositories;

import android.content.Context;

import com.choicecrafter.students.models.Activity;
import com.choicecrafter.students.models.Comment;
import com.choicecrafter.students.models.tasks.CodingChallengeExample;
import com.choicecrafter.students.models.tasks.CodingChallengeTask;
import com.choicecrafter.students.models.tasks.FillInTheBlank;
import com.choicecrafter.students.models.tasks.InfoCardTask;
import com.choicecrafter.students.models.tasks.MatchingPairTask;
import com.choicecrafter.students.models.tasks.MultipleChoiceQuestion;
import com.choicecrafter.students.models.Notification;
import com.choicecrafter.students.models.NotificationType;
import com.choicecrafter.students.models.tasks.OrderingTask;
import com.choicecrafter.students.models.Recommendation;
import com.choicecrafter.students.models.tasks.SpotTheErrorTask;
import com.choicecrafter.students.models.tasks.Task;
import com.choicecrafter.students.models.tasks.TrueFalseTask;
import com.choicecrafter.students.notifications.NotificationHelper;
import com.choicecrafter.students.utils.AppLogger;
import com.choicecrafter.students.utils.DefaultRecommendationsProvider;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FirestoreListener {
    private static final String TAG = "FirestoreAllCourses";
    private static final String ENROLLMENTS_COLLECTION = "COURSE_ENROLLMENTS";

    private static final long DEFAULT_POINTS_THRESHOLD = 100L;

    private FirebaseFirestore db;
    private ListenerRegistration coursesListenerRegistration;
    private ListenerRegistration enrollmentListenerRegistration;
    private final SimpleDateFormat notificationTimestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());

    private final Map<String, Activity.Status> activityStatusCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> activityCommentCountCache = new ConcurrentHashMap<>();
    private final Map<String, Long> activityPointsCache = new ConcurrentHashMap<>();

    private NotificationHelper notificationHelper;
    private final NotificationRepository notificationRepository;
    private final String userId;
    private final Set<String> enrolledCourseIds = Collections.synchronizedSet(new HashSet<>());
    private volatile boolean activityStartedNotificationsEnabled = true;
    private volatile boolean discussionForumEnabled = true;

    public FirestoreListener(Context context, String userId) {
        db = FirebaseFirestore.getInstance();
        notificationHelper = new NotificationHelper(context);
        notificationRepository = new NotificationRepository();
        this.userId = userId;
    }

    public void setActivityStartedNotificationsEnabled(boolean enabled) {
        this.activityStartedNotificationsEnabled = enabled;
    }

    public void setDiscussionForumEnabled(boolean enabled) {
        this.discussionForumEnabled = enabled;
    }

    public void startListeningForActivityStatusChanges() {
        if (userId == null || userId.isEmpty()) {
            AppLogger.w(TAG, "Cannot start Firestore listener without a user id");
            return;
        }

        AppLogger.d(TAG, "Starting Firestore listeners", "userId", userId);

        fetchInitialEnrollments(() -> {
            startEnrollmentListener();
            startCoursesListener();
        });
    }

    private void startCoursesListener() {
        if (coursesListenerRegistration != null) {
            coursesListenerRegistration.remove();
        }

        coursesListenerRegistration = db.collection("COURSES")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            AppLogger.e(TAG, "Firestore course listener error", e);
                            return;
                        }

                        if (snapshots != null) {
                            AppLogger.d(TAG, "Firestore course snapshot received",
                                    "changeCount", snapshots.getDocumentChanges().size());

                            for (DocumentChange change : snapshots.getDocumentChanges()) {
                                String courseDocumentId = change.getDocument().getId();

                                switch (change.getType()) {
                                    case ADDED:
                                        AppLogger.d(TAG, "Course change detected",
                                                "changeType", "added",
                                                "courseId", courseDocumentId);
                                        break;

                                    case MODIFIED:
                                        AppLogger.d(TAG, "Course change detected",
                                                "changeType", "modified",
                                                "courseId", courseDocumentId);
                                        processCourseDocument(change.getDocument());
                                        break;

                                    case REMOVED:
                                        AppLogger.d(TAG, "Course change detected",
                                                "changeType", "removed",
                                                "courseId", courseDocumentId);
                                        break;
                                }
                            }
                        }
                    }
                });
    }

    private void startEnrollmentListener() {
        if (enrollmentListenerRegistration != null) {
            enrollmentListenerRegistration.remove();
        }

        enrollmentListenerRegistration = db.collection(ENROLLMENTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        AppLogger.e(TAG, "Enrollment listener error", e);
                        return;
                    }

                    updateEnrolledCourses(snapshots);
                });
    }

    private void fetchInitialEnrollments(Runnable onComplete) {
        db.collection(ENROLLMENTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    updateEnrolledCourses(snapshots);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    AppLogger.e(TAG, "Failed to fetch initial enrollments", e);
                    synchronized (enrolledCourseIds) {
                        enrolledCourseIds.clear();
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    private void updateEnrolledCourses(QuerySnapshot snapshots) {
        if (snapshots == null) {
            return;
        }

        Set<String> updatedCourseIds = new HashSet<>();
        for (DocumentSnapshot document : snapshots.getDocuments()) {
            String courseId = document.getString("courseId");
            if (courseId != null && !courseId.isEmpty()) {
                updatedCourseIds.add(courseId);
            }
        }

        synchronized (enrolledCourseIds) {
            enrolledCourseIds.clear();
            enrolledCourseIds.addAll(updatedCourseIds);
        }

        AppLogger.d(TAG, "Enrolled courses updated",
                "courseCount", enrolledCourseIds.size(),
                "courses", enrolledCourseIds);
    }

    private boolean isUserEnrolledInCourse(String courseId) {
        synchronized (enrolledCourseIds) {
            return enrolledCourseIds.contains(courseId);
        }
    }

    private void processCourseDocument(DocumentSnapshot snapshot) {
        String courseId = snapshot.getId();
        String courseTitle = snapshot.getString("title");
        AppLogger.d(TAG, "Processing course snapshot",
                "courseId", courseId,
                "courseTitle", courseTitle);

        if (!isUserEnrolledInCourse(courseId)) {
            AppLogger.d(TAG, "Skipping course because user is not enrolled", "courseId", courseId);
            return;
        }

        List<Map<String, Object>> activities = (List<Map<String, Object>>) snapshot.get("activities");

        if (activities != null) {
            for (Map<String, Object> activity : activities) {
                String activityId = activity.get("title") != null ? activity.get("title").toString() : "UNKNOWN";
                String status = activity.get("status") != null ? activity.get("status").toString() : "UNKNOWN";

                AppLogger.d(TAG, "Checking course activity",
                        "courseId", courseId,
                        "activityId", activityId,
                        "status", status);

                handleActivityUpdates(courseId, courseTitle, activityId, activity);
            }
        } else {
            AppLogger.w(TAG, "Course does not have activity definitions", "courseId", courseId);
        }
    }

    private void handleActivityUpdates(String courseId, String courseTitle, String activityId, Map<String, Object> activityData) {
        Activity activityMapped = mapToActivity(activityData);
        if (activityMapped == null) {
            AppLogger.w(TAG, "Unable to map activity for notifications",
                    "courseId", courseId,
                    "activityId", activityId);
            return;
        }

        String displayCourseName = courseTitle != null ? courseTitle : courseId;
        String cacheKey = buildCacheKey(courseId, activityId);

        handleStatusChange(cacheKey, courseId, displayCourseName, activityId, activityData, activityMapped);
        handleCommentChanges(cacheKey, courseId, displayCourseName, activityId, activityData, activityMapped);
        handlePointsMilestones(cacheKey, courseId, displayCourseName, activityId, activityData, activityMapped);
    }

    private Activity mapToActivity(Map<String, Object> activityMap) {
        Activity activity = new Activity();
        activity.setId(resolveActivityId(activityMap));
        activity.setTitle(activityMap.get("title") != null ? activityMap.get("title").toString() : null);
        activity.setDescription(activityMap.get("description") != null ? activityMap.get("description").toString() : null);
        activity.setType(activityMap.get("type") != null ? activityMap.get("type").toString() : null);
        activity.setDate(activityMap.get("date") != null ? activityMap.get("date").toString() : null);
        activity.setTime(activityMap.get("time") != null ? activityMap.get("time").toString() : null);
        if (activityMap.get("status") != null) {
            activity.setStatus(Activity.Status.valueOf(activityMap.get("status").toString()));
        }
        if (activityMap.get("reminders") instanceof List) {
            activity.setReminders(getStringList(activityMap, "reminders"));
        }
        if (activityMap.get("tasks") instanceof List) {
            List<Map<String, Object>> taskMaps = (List<Map<String, Object>>) activityMap.get("tasks");
            List<Task> tasks = new ArrayList<>();
            for (Map<String, Object> taskMap : taskMaps) {
                tasks.add(mapToTask(taskMap));
            }
            activity.setTasks(tasks);
        }
        if (activityMap.get("recommendations") instanceof List) {
            List<Map<String, Object>> recMaps = (List<Map<String, Object>>) activityMap.get("recommendations");
            List<Recommendation> recommendations = new ArrayList<>();
            for (Map<String, Object> recMap : recMaps) {
                recommendations.add(mapToRecommendation(recMap));
            }
            activity.setRecommendations(recommendations);
        }
        DefaultRecommendationsProvider.ensureDefaults(activity);
        if (activityMap.get("comments") instanceof List) {
            List<Map<String, Object>> commentMaps = (List<Map<String, Object>>) activityMap.get("comments");
            List<Comment> comments = new ArrayList<>();
            for (Map<String, Object> commentMap : commentMaps) {
                String user = getString(commentMap, "userId");
                String text = getString(commentMap, "text");
                String timestamp = getString(commentMap, "timestamp");
                comments.add(new Comment(user, text, timestamp));
            }
            activity.setComments(comments);
        }
        if (activityMap.get("reactions") instanceof Map) {
            Map<String, Object> reactionsRaw = (Map<String, Object>) activityMap.get("reactions");
            Map<String, Long> normalized = new HashMap<>();
            for (Map.Entry<String, Object> entry : reactionsRaw.entrySet()) {
                Long value = toLong(entry.getValue());
                if (value != null) {
                    normalized.put(entry.getKey(), value);
                }
            }
            activity.setReactions(normalized);
        }
        return activity;
    }

    private String resolveActivityId(Map<String, Object> activityMap) {
        if (activityMap == null) {
            return null;
        }
        Object idValue = activityMap.get("id");
        if (idValue == null) {
            idValue = activityMap.get("activityId");
        }
        if (idValue == null) {
            idValue = activityMap.get("title");
        }
        return idValue != null ? String.valueOf(idValue) : null;
    }

    private Task mapToTask(Map<String, Object> map) {
        String type = map.get("type") != null ? map.get("type").toString() : "";
        Task task = null;
        switch (type) {
            case "MultipleChoice":
                MultipleChoiceQuestion mcTask = new MultipleChoiceQuestion();
                mcTask.setTitle(getString(map, "title"));
                mcTask.setDescription(getString(map, "description"));
                mcTask.setOptions(getStringList(map, "options"));
                if (map.get("correctAnswer") != null) {
                    mcTask.setCorrectAnswer(Integer.valueOf(map.get("correctAnswer").toString()));
                }
                mcTask.setStatus(getString(map, "status"));
                task = mcTask;
                break;
            case "FillInTheBlank":
                FillInTheBlank fibTask = new FillInTheBlank();
                fibTask.setTitle(getString(map, "title"));
                fibTask.setDescription(getString(map, "description"));
                fibTask.setText(getString(map, "text"));
                fibTask.setSegmentPositions(getIntList(map, "segmentPositions"));
                fibTask.setMissingSegments(getStringList(map, "missingSegments"));
                fibTask.setStatus(getString(map, "status"));
                task = fibTask;
                break;
            case "Ordering":
                OrderingTask orderingTask = new OrderingTask();
                orderingTask.setTitle(getString(map, "title"));
                orderingTask.setDescription(getString(map, "description"));
                orderingTask.setItems(getStringList(map, "items"));
                orderingTask.setCorrectOrder(getIntList(map, "correctOrder"));
                orderingTask.setStatus(getString(map, "status"));
                task = orderingTask;
                break;
            case "MatchingPair":
                MatchingPairTask matchingTask = new MatchingPairTask();
                matchingTask.setTitle(getString(map, "title"));
                matchingTask.setDescription(getString(map, "description"));
                matchingTask.setLeftItems(getStringList(map, "leftItems"));
                matchingTask.setRightItems(getStringList(map, "rightItems"));
                if (map.get("correctMatches") instanceof Map) {
                    matchingTask.setCorrectMatches((Map<String, String>) map.get("correctMatches"));
                }
                matchingTask.setStatus(getString(map, "status"));
                task = matchingTask;
                break;
            case "TrueFalse":
                TrueFalseTask trueFalseTask = new TrueFalseTask();
                trueFalseTask.setTitle(getString(map, "title"));
                trueFalseTask.setDescription(getString(map, "description"));
                trueFalseTask.setStatement(getString(map, "statement"));
                Object boolValue = map.get("correctAnswer");
                if (boolValue instanceof Boolean b) {
                    trueFalseTask.setCorrectAnswer(b);
                } else if (boolValue instanceof Number number) {
                    trueFalseTask.setCorrectAnswer(number.intValue() != 0);
                } else if (boolValue != null) {
                    trueFalseTask.setCorrectAnswer(Boolean.parseBoolean(boolValue.toString()));
                }
                trueFalseTask.setStatus(getString(map, "status"));
                task = trueFalseTask;
                break;
            case "SpotError":
                SpotTheErrorTask spotErrorTask = new SpotTheErrorTask();
                spotErrorTask.setTitle(getString(map, "title"));
                spotErrorTask.setDescription(getString(map, "description"));
                spotErrorTask.setPrompt(getString(map, "prompt"));
                spotErrorTask.setSnippet(getString(map, "codeSnippet"));
                spotErrorTask.setOptions(getStringList(map, "options"));
                Object correct = map.get("correctAnswer");
                if (correct != null) {
                    spotErrorTask.setCorrectOptionIndex(Integer.parseInt(correct.toString()));
                }
                spotErrorTask.setStatus(getString(map, "status"));
                task = spotErrorTask;
                break;
            case "CodingChallenge":
                CodingChallengeTask codingTask = new CodingChallengeTask();
                codingTask.setTitle(getString(map, "title"));
                codingTask.setDescription(getString(map, "description"));
                codingTask.setProblemDescription(getString(map, "problemDescription"));
                codingTask.setExpectedOutputDescription(getString(map, "expectedOutput"));
                codingTask.setStatus(getString(map, "status"));
                codingTask.setDefaultLanguage(getString(map, "defaultLanguage"));
                codingTask.setExamples(parseCodingExamples(map.get("examples")));
                Map<String, String> starterCode = getStringMap(map, "starterCode");
                if (starterCode.isEmpty()) {
                    starterCode = getStringMap(map, "starterTemplates");
                }
                codingTask.setStarterCodeByLanguage(starterCode);
                Map<String, String> solutionCode = getStringMap(map, "solutionCode");
                if (solutionCode.isEmpty()) {
                    solutionCode = getStringMap(map, "solutions");
                }
                codingTask.setSolutionCodeByLanguage(solutionCode);
                codingTask.setSolutionInput(getString(map, "solutionInput"));
                task = codingTask;
                break;
            case "InfoCard":
                InfoCardTask infoCardTask = new InfoCardTask();
                infoCardTask.setTitle(getString(map, "title"));
                infoCardTask.setDescription(getString(map, "description"));
                infoCardTask.setContentType(getString(map, "contentType"));
                infoCardTask.setContentText(getString(map, "contentText"));
                infoCardTask.setMediaUrl(getString(map, "mediaUrl"));
                infoCardTask.setInteractiveUrl(getString(map, "interactiveUrl"));
                infoCardTask.setActionText(getString(map, "actionText"));
                infoCardTask.setStatus(getString(map, "status"));
                task = infoCardTask;
                break;
            default:
                AppLogger.w(TAG, "Unknown task type encountered", "type", type);
                return null;
        }
        if (task != null) {
            task.setExplanation(getString(map, "explanation"));
        }
        return task;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<?> rawList = (List<?>) value;
            List<String> result = new ArrayList<>();
            for (Object item : rawList) {
                result.add(item != null ? item.toString() : null);
            }
            return result;
        }
        return new ArrayList<>();
    }

    private List<Integer> getIntList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<?> rawList = (List<?>) value;
            List<Integer> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Number) {
                    result.add(((Number) item).intValue());
                } else if (item != null) {
                    try {
                        result.add(Integer.parseInt(item.toString()));
                    } catch (NumberFormatException ignored) {}
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private List<CodingChallengeExample> parseCodingExamples(Object value) {
        List<CodingChallengeExample> examples = new ArrayList<>();
        if (!(value instanceof List<?> rawList)) {
            return examples;
        }
        for (Object exampleObject : rawList) {
            if (exampleObject instanceof Map<?, ?> map) {
                CodingChallengeExample example = new CodingChallengeExample();
                example.setInput(getString((Map<String, Object>) map, "input"));
                example.setOutput(getString((Map<String, Object>) map, "output"));
                example.setExplanation(getString((Map<String, Object>) map, "explanation"));
                examples.add(example);
            }
        }
        return examples;
    }

    private Map<String, String> getStringMap(Map<String, Object> map, String key) {
        Map<String, String> result = new HashMap<>();
        Object value = map.get(key);
        if (value instanceof Map<?, ?> rawMap) {
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                Object entryKey = entry.getKey();
                Object entryValue = entry.getValue();
                if (entryKey != null && entryValue != null) {
                    result.put(String.valueOf(entryKey), String.valueOf(entryValue));
                }
            }
        }
        return result;
    }

    private Recommendation mapToRecommendation(Map<String, Object> map) {
        Recommendation rec = new Recommendation();
        rec.setType(getString(map, "type"));
        rec.setUrl(getString(map, "url"));
        return rec;
    }

    private void handleStatusChange(String cacheKey, String courseId, String courseTitle, String activityId,
                                    Map<String, Object> activityData, Activity activityMapped) {
        Activity.Status status = activityMapped.getStatus();
        if (status == null) {
            return;
        }

        Activity.Status previousStatus = activityStatusCache.put(cacheKey, status);
        if (status != Activity.Status.STARTED) {
            return;
        }
        if (previousStatus == Activity.Status.STARTED) {
            return;
        }

        if (!activityStartedNotificationsEnabled) {
            AppLogger.d(TAG, "Activity started notifications disabled by preferences",
                    "courseId", courseId,
                    "activityId", activityId);
            return;
        }

        String details = activityMapped.getTitle() != null
                ? activityMapped.getTitle() + " is now available."
                : "A new activity is available.";
        recordNotification(NotificationType.ACTIVITY_STARTED, courseId, activityId, null, details);
        notificationHelper.sendActivityStartedNotification(courseId, courseTitle, activityMapped);
    }

    private void handleCommentChanges(String cacheKey, String courseId, String courseTitle, String activityId,
                                      Map<String, Object> activityData, Activity activityMapped) {
        List<Map<String, Object>> comments = extractComments(activityData);
        int currentCount = comments != null ? comments.size() : 0;

        Integer previousCount = activityCommentCountCache.put(cacheKey, currentCount);
        if (previousCount == null || currentCount <= previousCount || comments == null || comments.isEmpty()) {
            return;
        }

        if (!discussionForumEnabled) {
            AppLogger.d(TAG, "Discussion forum nudges disabled; skipping comment notification",
                    "courseId", courseId,
                    "activityId", activityId);
            return;
        }

        Map<String, Object> latestComment = comments.get(comments.size() - 1);
        if (latestComment == null) {
            return;
        }

        String commenterId = getString(latestComment, "userId");
        if (commenterId != null && commenterId.equals(userId)) {
            AppLogger.d(TAG, "Comment authored by current user. Skipping notification",
                    "courseId", courseId,
                    "activityId", activityId);
            return;
        }

        String commentText = getString(latestComment, "text");
        recordNotification(NotificationType.COMMENT_ADDED, courseId, activityId, commenterId, commentText);
        notificationHelper.sendCommentAddedNotification(courseId, courseTitle, activityMapped, commenterId, commentText);
    }

    private void handlePointsMilestones(String cacheKey, String courseId, String courseTitle, String activityId,
                                        Map<String, Object> activityData, Activity activityMapped) {
        Long points = extractPointsValue(activityData);
        if (points == null) {
            return;
        }

        Long threshold = extractPointsThreshold(activityData);
        if (threshold == null) {
            threshold = DEFAULT_POINTS_THRESHOLD;
        }

        Long previousPoints = activityPointsCache.put(cacheKey, points);
        if (previousPoints == null || previousPoints >= threshold || points < threshold) {
            return;
        }

        String contributorId = extractActor(activityData);
        if (contributorId != null && contributorId.equals(userId)) {
            AppLogger.d(TAG, "Points milestone achieved by current user. Skipping notification",
                    "courseId", courseId,
                    "activityId", activityId,
                    "points", points);
            return;
        }

        String details = "Reached " + points + " points" + (contributorId != null ? " thanks to " + contributorId : "");
        recordNotification(NotificationType.POINTS_THRESHOLD_REACHED, courseId, activityId, contributorId, details);
        notificationHelper.sendPointsMilestoneNotification(courseId, courseTitle, activityMapped, points, threshold, contributorId, details);
    }

    private List<Map<String, Object>> extractComments(Map<String, Object> activityData) {
        Object commentsObj = activityData.get("comments");
        if (commentsObj instanceof List) {
            return (List<Map<String, Object>>) commentsObj;
        }
        return null;
    }

    private Long extractPointsValue(Map<String, Object> activityData) {
        Object[] potentialKeys = {activityData.get("points"), activityData.get("currentPoints"), activityData.get("score"), activityData.get("earnedPoints")};
        for (Object candidate : potentialKeys) {
            Long parsed = toLong(candidate);
            if (parsed != null) {
                return parsed;
            }
        }

        Object reactionsObj = activityData.get("reactions");
        if (reactionsObj instanceof Map<?, ?> reactionsMap) {
            long total = 0L;
            for (Object value : reactionsMap.values()) {
                Long parsed = toLong(value);
                if (parsed != null) {
                    total += parsed;
                }
            }
            return total;
        }

        return null;
    }

    private Long extractPointsThreshold(Map<String, Object> activityData) {
        Object[] potentialKeys = {activityData.get("pointsThreshold"), activityData.get("targetPoints"), activityData.get("goalPoints")};
        for (Object candidate : potentialKeys) {
            Long parsed = toLong(candidate);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private void recordNotification(NotificationType type, String courseId, String activityId, String relatedUserId, String details) {
        String timestamp = getNotificationTimestamp();
        Notification notification = new Notification(userId, type, courseId, activityId, relatedUserId, timestamp, details);
        AppLogger.i(TAG, "Recording notification event",
                "type", type,
                "courseId", courseId,
                "activityId", activityId,
                "relatedUserId", relatedUserId,
                "details", details);
        notificationRepository.addNotification(notification);
    }

    private String extractActor(Map<String, Object> activityData) {
        String[] keys = {"startedBy", "lastStartedBy", "lastUpdatedBy", "updatedBy", "actorId", "triggeredBy", "ownerId"};
        for (String key : keys) {
            Object value = activityData.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private String buildCacheKey(String courseId, String activityId) {
        return courseId + "::" + activityId;
    }

    public void stopListening() {
        if (coursesListenerRegistration != null) {
            coursesListenerRegistration.remove();
            coursesListenerRegistration = null;
            AppLogger.d(TAG, "Firestore course listener stopped");
        }
        if (enrollmentListenerRegistration != null) {
            enrollmentListenerRegistration.remove();
            enrollmentListenerRegistration = null;
            AppLogger.d(TAG, "Enrollment listener stopped");
        }
    }

    private String getNotificationTimestamp() {
        synchronized (notificationTimestampFormat) {
            return notificationTimestampFormat.format(new Date());
        }
    }
}
