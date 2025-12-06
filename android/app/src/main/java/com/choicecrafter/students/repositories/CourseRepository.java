package com.choicecrafter.studentapp.repositories;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.choicecrafter.studentapp.models.Comment;
import com.choicecrafter.studentapp.models.Course;
import com.choicecrafter.studentapp.models.tasks.CodingChallengeTask;
import com.choicecrafter.studentapp.models.tasks.FillInTheBlank;
import com.choicecrafter.studentapp.models.tasks.InfoCardTask;
import com.choicecrafter.studentapp.models.tasks.MatchingPairTask;
import com.choicecrafter.studentapp.models.tasks.MultipleChoiceQuestion;
import com.choicecrafter.studentapp.models.Recommendation;
import com.choicecrafter.studentapp.models.tasks.OrderingTask;
import com.choicecrafter.studentapp.models.tasks.SpotTheErrorTask;
import com.choicecrafter.studentapp.models.tasks.Task;
import com.choicecrafter.studentapp.models.tasks.SupportingContent;
import com.choicecrafter.studentapp.models.Teacher;
import com.choicecrafter.studentapp.models.tasks.TrueFalseTask;
import com.choicecrafter.studentapp.models.Activity;
import com.choicecrafter.studentapp.models.Module;
import com.choicecrafter.studentapp.utils.ActivityVisibilityFilter;
import com.choicecrafter.studentapp.utils.DefaultRecommendationsProvider;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;


public class CourseRepository {

    private static final String TAG = "CourseRepository";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Gson gson = new Gson();
    private final CourseCache courseCache = CourseCache.getInstance();
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void fetchCourses(final Callback<List<Course>> callback) {
        fetchCourses(false, callback);
    }

    public void fetchCourses(boolean forceRefresh, final Callback<List<Course>> callback) {
        if (!forceRefresh && courseCache.isLoaded()) {
            postSuccess(callback, courseCache.getCourses());
            return;
        }
        db.collection("COURSES").get().addOnCompleteListener(backgroundExecutor, task -> {
            if (!task.isSuccessful()) {
                Exception exception = task.getException();
                if (exception == null) {
                    exception = new IllegalStateException("Failed to fetch courses");
                }
                postFailure(callback, exception);
                return;
            }

            if (task.getResult() == null) {
                postFailure(callback, new IllegalStateException("No course data available"));
                return;
            }

            try {
                List<Course> courses = new ArrayList<>();

                for (QueryDocumentSnapshot courseDoc : task.getResult()) {
                    Course course = mapCourseDocument(courseDoc);
                    courses.add(course);
                }

                courseCache.setCourses(courses);
                postSuccess(callback, courses);
            } catch (Exception e) {
                postFailure(callback, e);
            }
        });
    }

    public void getCourseById(String courseId, final Callback<Course> callback) {
        db.collection("COURSES")
                .document(courseId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Course course = mapCourseDocument(documentSnapshot);
                        callback.onSuccess(course);
                    } else {
                        callback.onFailure(new IllegalArgumentException("Course not found for id " + courseId));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    private Course mapCourseDocument(DocumentSnapshot courseDoc) {
        Course course = new Course();
        course.setId(courseDoc.getId());
        course.setDescription(courseDoc.getString("description"));
        course.setTitle(courseDoc.getString("title"));
        course.setTeacher(courseDoc.getString("teacher"));
        course.setImageUrl(courseDoc.getString("imageUrl"));

        List<Map<String, Object>> activitiesList = extractMapList(courseDoc.get("activities"));
        List<Activity> activities = new ArrayList<>();

        if (activitiesList != null) {
            for (Map<String, Object> activityMap : activitiesList) {
                Activity activity = mapActivity(activityMap);
                if (ActivityVisibilityFilter.isVisible(activity)) {
                    activities.add(activity);
                }
            }
        }

        course.setActivities(activities);

        List<Map<String, Object>> modulesList = extractMapList(courseDoc.get("modules"));
        if (modulesList != null) {
            List<Module> modules = new ArrayList<>();
            for (Map<String, Object> moduleMap : modulesList) {
                Module module = mapModule(moduleMap, course.getId());
                if (module != null) {
                    modules.add(module);
                }
            }
            course.setModules(modules);
        }
        return course;
    }

    private Activity mapActivity(Map<String, Object> activityMap) {
        Activity activity = new Activity();
        activity.setId(extractActivityId(activityMap));
        activity.setDescription((String) activityMap.get("description"));
        activity.setType((String) activityMap.get("type"));
        activity.setDate((String) activityMap.get("date"));
        activity.setTime((String) activityMap.get("time"));
        activity.setTitle((String) activityMap.get("title"));

        Activity.Status status = parseStatus(activityMap != null ? activityMap.get("status") : null);
        if (status != null) {
            activity.setStatus(status);
        }

        List<Map<String, Object>> tasksList = extractMapList(activityMap.get("tasks"));
        List<Task> tasks = new ArrayList<>();
        if (tasksList != null) {
            for (Map<String, Object> taskMap : tasksList) {
                JsonObject taskJson = JsonParser.parseString(gson.toJson(taskMap)).getAsJsonObject();
                Task taskParsed = parseTask(taskJson);
                if (taskParsed != null) {
                    Object idValue = taskMap.get("id");
                    if (idValue == null) {
                        idValue = taskMap.get("taskId");
                    }
                    if (idValue == null) {
                        idValue = taskMap.get("title");
                    }
                    if (idValue != null) {
                        taskParsed.setId(String.valueOf(idValue));
                    }
                    tasks.add(taskParsed);
                }
            }
        }
        activity.setTasks(tasks);

        List<Map<String, Object>> recommendationsList = extractMapList(activityMap.get("recommendations"));
        List<Recommendation> recommendations = new ArrayList<>();
        if (recommendationsList != null) {
            for (Map<String, Object> recommendationMap : recommendationsList) {
                String type = (String) recommendationMap.get("type");
                String link = (String) recommendationMap.get("url");
                recommendations.add(new Recommendation(type, link));
            }
        }
        activity.setRecommendations(recommendations);
        DefaultRecommendationsProvider.ensureDefaults(activity);

        List<Map<String, Object>> commentsList = extractMapList(activityMap.get("comments"));
        List<Comment> comments = new ArrayList<>();
        if (commentsList != null) {
            for (Map<String, Object> commentMap : commentsList) {
                String userId = (String) commentMap.get("userId");
                String text = (String) commentMap.get("text");
                String timestamp = (String) commentMap.get("timestamp");
                comments.add(new Comment(userId, text, timestamp));
            }
        }
        activity.setComments(comments);

        Map<String, Long> reactions = parseReactions(activityMap.get("reactions"));
        activity.setReactions(reactions);
        return activity;
    }

    private String extractActivityId(Map<String, Object> activityMap) {
        Object idValue = activityMap != null ? activityMap.get("id") : null;
        if (idValue == null && activityMap != null) {
            idValue = activityMap.get("activityId");
        }
        if (idValue == null && activityMap != null) {
            idValue = activityMap.get("title");
        }
        return idValue != null ? String.valueOf(idValue) : null;
    }

    private Module mapModule(Map<String, Object> moduleMap, String courseId) {
        if (moduleMap == null) {
            return null;
        }

        Module module = new Module();
        module.setId((String) moduleMap.get("id"));
        module.setTitle((String) moduleMap.get("title"));
        module.setDescription((String) moduleMap.get("description"));
        module.setCourseId(courseId);

        List<Map<String, Object>> activitiesList = extractMapList(moduleMap.get("activities"));
        List<Activity> activities = new ArrayList<>();
        if (activitiesList != null) {
            for (Map<String, Object> activityMap : activitiesList) {
                Activity activity = mapActivity(activityMap);
                if (ActivityVisibilityFilter.isVisible(activity)) {
                    activities.add(activity);
                }
            }
        }
        module.setActivities(activities);

        Object completionObj = moduleMap.get("completedPercentage");
        if (completionObj instanceof Number number) {
            module.setCompletedPercentage(number.intValue());
        }

        return module;
    }

    private Activity.Status parseStatus(Object statusValue) {
        if (statusValue == null) {
            return Activity.Status.CREATED;
        }
        String normalized = statusValue.toString().trim();
        if (normalized.isEmpty()) {
            return Activity.Status.CREATED;
        }
        try {
            return Activity.Status.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return Activity.Status.CREATED;
        }
    }

    public void addComment(String courseId, String activityIdOrTitle, Comment comment) {
        if (courseId == null || courseId.trim().isEmpty() || comment == null
                || activityIdOrTitle == null || activityIdOrTitle.trim().isEmpty()) {
            Log.w(TAG, "Cannot add comment. Missing courseId, activity identifier, or comment data.");
            return;
        }
        DocumentReference courseRef = db.collection("COURSES").document(courseId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(courseRef);
            if (!snapshot.exists()) {
                throw new IllegalStateException("Course document not found for id " + courseId);
            }

            List<Map<String, Object>> activities = extractMapList(snapshot.get("activities"));
            List<Map<String, Object>> modules = extractMapList(snapshot.get("modules"));

            boolean activitiesUpdated = updateCommentsInActivities(activities, activityIdOrTitle, comment);
            boolean modulesUpdated = updateCommentsInModules(modules, activityIdOrTitle, comment);

            if (!activitiesUpdated && !modulesUpdated) {
                throw new IllegalStateException("Activity not found for comment update");
            }

            if (activitiesUpdated) {
                transaction.update(courseRef, "activities", activities);
            }
            if (modulesUpdated) {
                transaction.update(courseRef, "modules", modules);
            }
            return null;
        }).addOnSuccessListener(unused -> courseCache.clear())
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add comment to activity", e));
    }

    public void addReaction(String courseId, String activityIdOrTitle, String reactionType) {
        addReaction(courseId, activityIdOrTitle, reactionType, null);
    }

    public void addReaction(String courseId, String activityIdOrTitle, String reactionType,
                            ReactionUpdateCallback callback) {
        if (courseId == null || courseId.trim().isEmpty()
                || activityIdOrTitle == null || activityIdOrTitle.trim().isEmpty()) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Course id and activity identifier are required for reactions"));
            }
            return;
        }

        DocumentReference courseRef = db.collection("COURSES").document(courseId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(courseRef);
            if (!snapshot.exists()) {
                throw new IllegalStateException("Course document not found for id " + courseId);
            }

            List<Map<String, Object>> activities = extractMapList(snapshot.get("activities"));
            List<Map<String, Object>> modules = extractMapList(snapshot.get("modules"));

            Map<String, Long> updatedCounts = updateReactionsInActivities(activities, activityIdOrTitle, reactionType);
            boolean activitiesUpdated = updatedCounts != null;

            AtomicReference<Map<String, Long>> reactionReference = new AtomicReference<>(updatedCounts);
            boolean modulesUpdated = updateReactionsInModules(modules, activityIdOrTitle, reactionType, reactionReference);

            Map<String, Long> finalCounts = reactionReference.get();
            if (!activitiesUpdated && !modulesUpdated) {
                throw new IllegalStateException("Activity not found for reaction update");
            }

            if (activitiesUpdated) {
                transaction.update(courseRef, "activities", activities);
            }
            if (modulesUpdated) {
                transaction.update(courseRef, "modules", modules);
            }
            return finalCounts != null ? new HashMap<>(finalCounts) : null;
        }).addOnSuccessListener(reactionCounts -> {
            courseCache.clear();
            if (callback != null) {
                callback.onSuccess(reactionCounts);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to add reaction to activity", e);
            if (callback != null) {
                callback.onFailure(e);
            }
        });
    }

    // Factory method to parse Task
    private Task parseTask(JsonObject taskJson) {
        if (taskJson == null) {
            Log.w(TAG, "Skipping null task definition");
            return null;
        }

        if (!taskJson.has("type") || taskJson.get("type").isJsonNull()) {
            Log.w(TAG, "Task entry is missing a type field. Skipping task: " + taskJson);
            return null;
        }

        String type = taskJson.get("type").getAsString();
        if (type == null) {
            Log.w(TAG, "Task type is null. Skipping task: " + taskJson);
            return null;
        }

        String normalizedType = type.trim();
        if (normalizedType.isEmpty()) {
            Log.w(TAG, "Task type is empty. Skipping task: " + taskJson);
            return null;
        }

        try {
            Task parsedTask = switch (normalizedType) {
                case "MultipleChoice" -> gson.fromJson(taskJson, MultipleChoiceQuestion.class);
                case "FillInTheBlank" -> gson.fromJson(taskJson, FillInTheBlank.class);
                case "MatchingPair" -> gson.fromJson(taskJson, MatchingPairTask.class);
                case "Ordering" -> gson.fromJson(taskJson, OrderingTask.class);
                case "InfoCard" -> gson.fromJson(taskJson, InfoCardTask.class);
                case "CodingChallenge" -> gson.fromJson(taskJson, CodingChallengeTask.class);
                case "TrueFalse" -> gson.fromJson(taskJson, TrueFalseTask.class);
                case "SpotError" -> gson.fromJson(taskJson, SpotTheErrorTask.class);
                default -> {
                    Log.w(TAG, "Unknown task type '" + normalizedType + "'. Skipping task: " + taskJson);
                    yield null;
                }
            };

            if (parsedTask != null) {
                applySupportingContent(taskJson, parsedTask);
            }

            return parsedTask;
        } catch (RuntimeException ex) {
            Log.e(TAG, "Failed to parse task of type " + normalizedType + ": " + taskJson, ex);
            return null;
        }
    }

    private void applySupportingContent(JsonObject taskJson, Task parsedTask) {
        if (!(parsedTask instanceof MultipleChoiceQuestion || parsedTask instanceof FillInTheBlank)) {
            return;
        }

        SupportingContent supportingContent = null;

        if (taskJson.has("supportingContent") && taskJson.get("supportingContent").isJsonObject()) {
            supportingContent = gson.fromJson(taskJson.getAsJsonObject("supportingContent"), SupportingContent.class);
        } else {
            String supportingText = normalizeString(getAsString(taskJson, "supportingText"));
            String supportingImageUrl = normalizeString(getAsString(taskJson, "supportingImageUrl"));
            if (supportingImageUrl == null) {
                supportingImageUrl = normalizeString(getAsString(taskJson, "supportingImage"));
            }

            if (supportingText != null || supportingImageUrl != null) {
                supportingContent = new SupportingContent(supportingText, supportingImageUrl);
            }
        }

        if (supportingContent == null) {
            return;
        }

        if (parsedTask instanceof MultipleChoiceQuestion multipleChoiceQuestion) {
            multipleChoiceQuestion.setSupportingContent(supportingContent);
        } else if (parsedTask instanceof FillInTheBlank fillInTheBlank) {
            fillInTheBlank.setSupportingContent(supportingContent);
        }
    }

    private String getAsString(JsonObject jsonObject, String memberName) {
        if (jsonObject == null || memberName == null || !jsonObject.has(memberName)) {
            return null;
        }
        if (jsonObject.get(memberName).isJsonNull()) {
            return null;
        }
        try {
            return jsonObject.get(memberName).getAsString();
        } catch (ClassCastException | IllegalStateException ex) {
            Log.w(TAG, "Failed to read string value for " + memberName + " in " + jsonObject, ex);
            return null;
        }
    }

    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Long> parseReactions(Object reactionsObject) {
        Map<String, Long> reactionCounts = new HashMap<>();
        if (reactionsObject instanceof Map<?, ?> reactionsMap) {
            for (Map.Entry<?, ?> entry : reactionsMap.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key != null && value instanceof Number) {
                    reactionCounts.put(String.valueOf(key), ((Number) value).longValue());
                }
            }
        } else if (reactionsObject instanceof Number number) {
            reactionCounts.put("likes", number.longValue());
        }
        return reactionCounts;
    }

    private boolean matchesActivity(Map<String, Object> activity, String activityIdOrTitle) {
        if (activity == null || activityIdOrTitle == null) {
            return false;
        }
        Object idValue = activity.get("id");
        if (idValue != null && activityIdOrTitle.equals(String.valueOf(idValue))) {
            return true;
        }
        Object legacyId = activity.get("activityId");
        if (legacyId != null && activityIdOrTitle.equals(String.valueOf(legacyId))) {
            return true;
        }
        Object titleValue = activity.get("title");
        return titleValue != null && activityIdOrTitle.equals(String.valueOf(titleValue));
    }

    private String normalizeReactionKey(String reactionType) {
        if (reactionType == null) {
            return "likes";
        }
        return switch (reactionType.toLowerCase()) {
            case "heart", "hearts" -> "hearts";
            case "like", "likes" -> "likes";
            default -> reactionType.toLowerCase();
        };
    }

    private List<Map<String, Object>> extractMapList(Object source) {
        if (!(source instanceof List<?> list)) {
            return null;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> converted = convertToStringObjectMap(item);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
    }

    private boolean updateCommentsInActivities(List<Map<String, Object>> activities,
                                               String activityIdOrTitle,
                                               Comment comment) {
        if (activities == null) {
            return false;
        }

        for (Map<String, Object> activity : activities) {
            if (!matchesActivity(activity, activityIdOrTitle)) {
                continue;
            }

            List<Map<String, Object>> comments = extractMapList(activity.get("comments"));
            if (comments == null) {
                comments = new ArrayList<>();
            }

            Map<String, Object> newComment = new HashMap<>();
            newComment.put("userId", comment.getUserId());
            newComment.put("text", comment.getText());
            newComment.put("timestamp", comment.getTimestamp());
            comments.add(newComment);
            activity.put("comments", comments);
            return true;
        }
        return false;
    }

    private boolean updateCommentsInModules(List<Map<String, Object>> modules,
                                            String activityIdOrTitle,
                                            Comment comment) {
        if (modules == null) {
            return false;
        }

        boolean updated = false;
        for (Map<String, Object> module : modules) {
            List<Map<String, Object>> moduleActivities = extractMapList(module.get("activities"));
            if (updateCommentsInActivities(moduleActivities, activityIdOrTitle, comment)) {
                module.put("activities", moduleActivities);
                updated = true;
            }

            List<Map<String, Object>> nestedModules = extractMapList(module.get("modules"));
            if (updateCommentsInModules(nestedModules, activityIdOrTitle, comment)) {
                module.put("modules", nestedModules);
                updated = true;
            }

            List<Map<String, Object>> compositionModules = extractMapList(module.get("composition"));
            if (updateCommentsInModules(compositionModules, activityIdOrTitle, comment)) {
                module.put("composition", compositionModules);
                updated = true;
            }
        }
        return updated;
    }

    private Map<String, Long> updateReactionsInActivities(List<Map<String, Object>> activities,
                                                          String activityIdOrTitle,
                                                          String reactionType) {
        if (activities == null) {
            return null;
        }

        for (Map<String, Object> activity : activities) {
            if (!matchesActivity(activity, activityIdOrTitle)) {
                continue;
            }

            Map<String, Long> reactionCounts = parseReactions(activity.get("reactions"));
            String reactionKey = normalizeReactionKey(reactionType);
            long updatedValue = reactionCounts.getOrDefault(reactionKey, 0L) + 1;
            reactionCounts.put(reactionKey, updatedValue);
            activity.put("reactions", new HashMap<>(reactionCounts));
            return new HashMap<>(reactionCounts);
        }
        return null;
    }

    private boolean updateReactionsInModules(List<Map<String, Object>> modules,
                                             String activityIdOrTitle,
                                             String reactionType,
                                             AtomicReference<Map<String, Long>> reactionReference) {
        if (modules == null) {
            return false;
        }

        boolean updated = false;
        for (Map<String, Object> module : modules) {
            List<Map<String, Object>> moduleActivities = extractMapList(module.get("activities"));
            Map<String, Long> activityReactions = updateReactionsInActivities(moduleActivities, activityIdOrTitle, reactionType);
            if (activityReactions != null) {
                module.put("activities", moduleActivities);
                reactionReference.compareAndSet(null, activityReactions);
                updated = true;
            }

            List<Map<String, Object>> nestedModules = extractMapList(module.get("modules"));
            if (updateReactionsInModules(nestedModules, activityIdOrTitle, reactionType, reactionReference)) {
                module.put("modules", nestedModules);
                updated = true;
            }

            List<Map<String, Object>> compositionModules = extractMapList(module.get("composition"));
            if (updateReactionsInModules(compositionModules, activityIdOrTitle, reactionType, reactionReference)) {
                module.put("composition", compositionModules);
                updated = true;
            }
        }
        return updated;
    }

    private Map<String, Object> convertToStringObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    public interface Callback<T> {
        void onSuccess(T result);

        void onFailure(Exception e);
    }

    public interface ReactionUpdateCallback {
        void onSuccess(Map<String, Long> reactionCounts);

        void onFailure(Exception e);
    }

    private <T> void postSuccess(Callback<T> callback, T result) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void postFailure(Callback<T> callback, Exception exception) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onFailure(exception));
    }

    public void addCourse(Course course) {
        db.collection("COURSES")
                .add(course)
                .addOnSuccessListener(documentReference -> Log.i("CourseRepository", "Course added with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e("CourseRepository", "Error adding course", e));
    }

//    private List<Activity> generateActivities() {
//        List<Activity> activities = new ArrayList<>();
//        int activityCount = 3 + (int) (Math.random() * 3); // 3 to 5 activities
//
//        for (int i = 0; i < activityCount; i++) {
//            List<Task> tasks = generateTasks();
//            List<Recommendation> recommendations = generateRecommendations();
//            activities.add(new Activity("Activity " + (i + 1), "Description for activity " + (i + 1), "Type", "2024-01-10", "10:00 AM", tasks, recommendations, Activity.Status.CREATED, null));
//        }
//
//        return activities;
//    }

    private List<Recommendation> generateRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();
        recommendations.add(new Recommendation("youtube", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        recommendations.add(new Recommendation("webpage", "https://www.example.com"));
        recommendations.add(new Recommendation("firestore", "firestore_document_reference"));
        return recommendations;
    }

    private List<Task> generateTasks() {
        List<Task> tasks = new ArrayList<>();
        int taskCount = 3 + (int) (Math.random() * 3); // 3 to 5 tasks

        for (int i = 0; i < taskCount; i++) {
            if (Math.random() < 0.5) {
                MultipleChoiceQuestion question = new MultipleChoiceQuestion(
                        "Geography Quiz",
                        "Select the correct answer for each question.",
                        "MultipleChoice",
                        "Incomplete",
                        "What is the capital of France?",
                        Arrays.asList("Paris", "London", "Berlin", "Madrid"),
                        0
                );
                question.setExplanation("Paris is the capital and most populous city of France, making it the correct choice.");
                tasks.add(question);
            } else {
                FillInTheBlank fillTask = new FillInTheBlank(
                        "History Quiz",
                        "Fill in the missing dates in the historical events.",
                        "FillInTheBlank",
                        "Incomplete",
                        "The Declaration of Independence was signed in ____. The Civil War started in ____.",
                        Arrays.asList("1776", "1861"),
                        Arrays.asList(39, 67)
                );
                fillTask.setExplanation("1776 marks the Declaration of Independence and 1861 marks the start of the U.S. Civil War.");
                tasks.add(fillTask);
            }
        }

        return tasks;
    }

//    public void addDummyCourses() {
//        List<Course> courses = new ArrayList<>();
//        courses.add(new Course("Mathematics 101", "Basic Mathematics Course", "John Doe", generateActivities()));
//        courses.add(new Course("Physics 101", "Basic Physics Course", "Jane Smith", generateActivities()));
//
//        for (Course course : courses) {
//            addCourse(course);
//        }
//    }


    public void addDummyTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        teachers.add(new Teacher("John Doe", "Mathematics"));
        teachers.add(new Teacher("Jane Smith", "Physics"));
        teachers.add(new Teacher("Emily Johnson", "Chemistry"));

        for (Teacher teacher : teachers) {
            db.collection("TEACHERS")
                    .add(teacher)
                    .addOnSuccessListener(documentReference -> Log.i("CourseRepository", "Teacher added with ID: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.e("CourseRepository", "Error adding teacher", e));
        }
    }

    public void getCourseNameById(String courseId, final Callback<String> callback) {
        db.collection("COURSES").document(courseId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String courseName = documentSnapshot.getString("title");
                        Log.i("CourseRepository", "Course name fetched: " + courseName);
                        callback.onSuccess(courseName);
                    } else {
                        Log.e("CourseRepository", "No course found with ID: " + courseId);
                        callback.onFailure(new Exception("Course not found"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CourseRepository", "Error fetching course name", e);
                    callback.onFailure(e);
                });
    }

    public void uploadCourseFromAsset(Context context, String assetPath, final Callback<String> callback) {
        try (InputStream inputStream = context.getAssets().open(assetPath);
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonObject courseJson = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, Object> courseMap = gson.fromJson(courseJson, Map.class);
            String courseId = null;
            if (courseJson.has("id")) {
                courseId = courseJson.get("id").getAsString();
            }

            if (courseId == null || courseId.isEmpty()) {
                db.collection("COURSES")
                        .add(courseMap)
                        .addOnSuccessListener(documentReference -> {
                            Log.i("CourseRepository", "Course uploaded with generated ID: " + documentReference.getId());
                            if (callback != null) {
                                callback.onSuccess(documentReference.getId());
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("CourseRepository", "Error uploading course", e);
                            if (callback != null) {
                                callback.onFailure(e);
                            }
                        });
            } else {
                final String finalCourseId = courseId;
                db.collection("COURSES")
                        .document(courseId)
                        .set(courseMap)
                        .addOnSuccessListener(unused -> {
                            Log.i("CourseRepository", "Course uploaded with ID: " + finalCourseId);
                            if (callback != null) {
                                callback.onSuccess(finalCourseId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("CourseRepository", "Error uploading course", e);
                            if (callback != null) {
                                callback.onFailure(e);
                            }
                        });
            }
        } catch (IOException e) {
            Log.e("CourseRepository", "Error reading course asset", e);
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }
}
