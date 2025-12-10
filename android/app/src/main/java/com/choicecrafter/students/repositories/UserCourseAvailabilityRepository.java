package com.choicecrafter.students.repositories;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Repository responsible for managing the list of courses that are visible to each user.
 */
public class UserCourseAvailabilityRepository {

    private static final String TAG = "UserCourseAvailability";
    private static final String COLLECTION_NAME = "USER_AVAILABLE_COURSES";
    private static final String FIELD_AVAILABLE_COURSES = "availableCourses";

    public static final String DEFAULT_COURSE_ID = "FfwkHveow9h8bpwjdEQl";

    private final FirebaseFirestore firestore;

    public UserCourseAvailabilityRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public UserCourseAvailabilityRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public interface Callback<T> {
        void onSuccess(@Nullable T result);

        void onFailure(@NonNull Exception e);
    }

    /**
     * Ensures that a user has a course availability document populated with at least the default course.
     */
    public void ensureDefaultCourseAccess(String userId, Callback<Void> callback) {
        List<String> defaultCourses = new ArrayList<>();
        defaultCourses.add(DEFAULT_COURSE_ID);
        ensureCourseAccess(userId, defaultCourses, callback);
    }

    /**
     * Creates a new availability document for the user if one does not already exist.
     */
    public void ensureCourseAccess(String userId, List<String> courseIds, Callback<Void> callback) {
        if (TextUtils.isEmpty(userId)) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("User id is required"));
            }
            return;
        }

        DocumentReference document = firestore.collection(COLLECTION_NAME).document(userId);
        document.get().addOnSuccessListener(snapshot -> {
            if (snapshot != null && snapshot.exists()) {
                if (callback != null) {
                    callback.onSuccess(null);
                }
                return;
            }

            writeCourseAccess(document, courseIds, callback, userId, true);
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Failed to check course availability for user " + userId, e);
            if (callback != null) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Overwrites the course availability document for the provided user with the supplied course identifiers.
     */
    public void setCourseAccess(String userId, List<String> courseIds, Callback<Void> callback) {
        if (TextUtils.isEmpty(userId)) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("User id is required"));
            }
            return;
        }

        DocumentReference document = firestore.collection(COLLECTION_NAME).document(userId);
        writeCourseAccess(document, courseIds, callback, userId, false);
    }

    /**
     * Retrieves the set of course identifiers the user is allowed to see.
     */
    public void fetchAvailableCourseIds(String userId, Callback<Set<String>> callback) {
        if (TextUtils.isEmpty(userId)) {
            if (callback != null) {
                callback.onSuccess(new HashSet<>(Collections.singleton(DEFAULT_COURSE_ID)));
            }
            return;
        }

        firestore.collection(COLLECTION_NAME)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> courseIds = extractCourseIds(snapshot);
                    if (callback != null) {
                        callback.onSuccess(courseIds);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to fetch available courses for user " + userId, e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    private List<String> buildUniqueCourseList(List<String> courseIds) {
        Set<String> unique = new LinkedHashSet<>();
        if (courseIds != null) {
            for (String courseId : courseIds) {
                if (!TextUtils.isEmpty(courseId)) {
                    unique.add(courseId);
                }
            }
        }
        if (unique.isEmpty()) {
            unique.add(DEFAULT_COURSE_ID);
        }
        return new ArrayList<>(unique);
    }

    private void writeCourseAccess(DocumentReference document,
                                   List<String> courseIds,
                                   Callback<Void> callback,
                                   String userId,
                                   boolean isCreate) {
        List<String> uniqueCourseIds = buildUniqueCourseList(courseIds);
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_AVAILABLE_COURSES, uniqueCourseIds);

        document.set(data)
                .addOnSuccessListener(unused -> {
                    Log.i(TAG, (isCreate ? "Created" : "Updated") + " course availability for user " + userId);
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to " + (isCreate ? "create" : "update") + " course availability for user " + userId, e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    private Set<String> extractCourseIds(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return new HashSet<>(Collections.singleton(DEFAULT_COURSE_ID));
        }
        Object rawList = snapshot.get(FIELD_AVAILABLE_COURSES);
        if (!(rawList instanceof List<?> list)) {
            return new HashSet<>(Collections.singleton(DEFAULT_COURSE_ID));
        }
        Set<String> result = new HashSet<>();
        for (Object value : list) {
            if (value instanceof String id && !TextUtils.isEmpty(id)) {
                result.add(id);
            }
        }
        if (result.isEmpty()) {
            result.add(DEFAULT_COURSE_ID);
        }
        return result;
    }
}