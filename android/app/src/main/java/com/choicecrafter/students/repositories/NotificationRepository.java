package com.choicecrafter.studentapp.repositories;
import static com.choicecrafter.studentapp.models.NotificationType.ACTIVITY_STARTED;
import static com.choicecrafter.studentapp.models.NotificationType.COLLEAGUE_ACTIVITY_STARTED;
import static com.choicecrafter.studentapp.models.NotificationType.COMMENT_ADDED;
import static com.choicecrafter.studentapp.models.NotificationType.POINTS_THRESHOLD_REACHED;

import android.util.Log;

import com.choicecrafter.studentapp.models.Notification;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {
    private static final String TAG = "NotificationRepository";
    private static final String COLLECTION_NAME = "NOTIFICATIONS";
    private FirebaseFirestore db;

    public NotificationRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Add a notification to Firestore
    public void addNotification(Notification notification) {
        Log.d(TAG, "Adding notification: " + notification.getType() + " for userId: " + notification.getUserId());
        db.collection(COLLECTION_NAME)
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding notification", e);
                });
    }

    public void getNotificationsForUser(String userId, NotificationCallback callback) {
        Log.d(TAG, "Retrieving notifications for userId: " + userId);
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    queryDocumentSnapshots.forEach(document -> {
                        Notification notification = document.toObject(Notification.class);
                        notification.setId(document.getId());
                        Log.d(TAG, "Fetched notification: " + notification.getType() + ", ID: " + notification.getId());
                        notifications.add(notification);
                    });
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving notifications", e);
                    callback.onFailure(e);
                });
    }

    public static void insertDummyNotifications() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Emails of the users
        String[] emails = {"andreipass11@gmail.com", "madapass12@gmail.com"};

        for (String email : emails) {
            db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            String userId = queryDocumentSnapshots.getDocuments().get(0).getId();

                            // Create dummy notifications
                            Notification notification1 = new Notification(
                                    userId,
                                    ACTIVITY_STARTED,
                                    "AYakQeP0XVSaSoIHdRDj",
                                    "Activity 1",
                                    null,
                                    "2023-10-01T12:00:00.000",
                                    "Teacher added a new activity"
                            );

                            Notification notification2 = new Notification(
                                    userId,
                                    COMMENT_ADDED,
                                    "AYakQeP0XVSaSoIHdRDj",
                                    "Activity 1",
                                    "user987", // Example related user ID
                                    "2023-10-02T12:00:00.000",
                                    "Looking forward to this lesson!"
                            );

                            Notification notification3 = new Notification(
                                    userId,
                                    COLLEAGUE_ACTIVITY_STARTED,
                                    "AYakQeP0XVSaSoIHdRDj",
                                    "Activity 1",
                                    "user123",
                                    "2023-10-03T12:00:00.000",
                                    "user123 kicked things off"
                            );

                            Notification notification4 = new Notification(
                                    userId,
                                    POINTS_THRESHOLD_REACHED,
                                    "AYakQeP0XVSaSoIHdRDj",
                                    "Activity 1",
                                    "user123",
                                    "2023-10-04T12:00:00.000",
                                    "Reached 100 points"
                            );

                            // Add notifications to Firestore
                            NotificationRepository repository = new NotificationRepository();
                            repository.addNotification(notification1);
                            repository.addNotification(notification2);
                            repository.addNotification(notification3);
                            repository.addNotification(notification4);

                            Log.d("InsertDummyNotifications", "Dummy notifications added for user: " + email);
                        } else {
                            Log.e("InsertDummyNotifications", "No user found with email: " + email);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("InsertDummyNotifications", "Error retrieving user by email: " + email, e));
        }
    }
    // Callback interface for retrieving notifications
    public interface NotificationCallback {
        void onSuccess(List<Notification> notifications);
        void onFailure(Exception e);
    }
}