package com.choicecrafter.studentapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Notification implements Parcelable {
    private String id; // Firestore document ID
    private String userId; // User who receives the notification
    private NotificationType type; // Type of notification (e.g., "ACTIVITY_STARTED", "COMMENT_ADDED")
    private String courseId; // Course ID related to the notification
    private String activityId; // Activity ID related to the notification
    private String relatedUserId; // (Optional) User who triggered the notification (e.g., commenter)
    private String timestamp; // Timestamp of when the notification was created
    private String details; // Additional context (e.g., comment text or milestone description)

    public Notification() {
        // Default constructor required for Firestore
    }

    public Notification(String userId, NotificationType type, String courseId, String activityId,
                        String relatedUserId, String timestamp, String details) {
        this.userId = userId;
        this.type = type;
        this.courseId = courseId;
        this.activityId = activityId;
        this.relatedUserId = relatedUserId;
        this.timestamp = timestamp;
        this.details = details;
    }

    protected Notification(Parcel in) {
        id = in.readString();
        userId = in.readString();
        type = NotificationType.valueOf(in.readString());
        courseId = in.readString();
        activityId = in.readString();
        relatedUserId = in.readString();
        timestamp = in.readString();
        details = in.readString();
    }

    public static final Creator<Notification> CREATOR = new Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel in) {
            return new Notification(in);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getActivityId() {
        return activityId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getRelatedUserId() {
        return relatedUserId;
    }

    public void setRelatedUserId(String relatedUserId) {
        this.relatedUserId = relatedUserId;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(userId);
        parcel.writeString(String.valueOf(type));
        parcel.writeString(courseId);
        parcel.writeString(activityId);
        parcel.writeString(relatedUserId);
        parcel.writeString(timestamp);
        parcel.writeString(details);
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
