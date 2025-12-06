package com.choicecrafter.studentapp.notifications;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.choicecrafter.studentapp.models.Activity;
import com.choicecrafter.studentapp.models.NotificationType;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class StudentMessagingService extends FirebaseMessagingService {
    private static final String TAG = "StudentMsgService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Received new FCM token");
        MessagingTokenManager.storeToken(this, token);
        String email = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : null;
        MessagingTokenManager.syncTokenIfNecessary(this, email);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext());
        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            handleDataPayload(notificationHelper, data);
        } else if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            notificationHelper.sendGenericNotification(title, body);
        }
    }

    private void handleDataPayload(@NonNull NotificationHelper helper, @NonNull Map<String, String> data) {
        String typeValue = data.get("type");
        if (TextUtils.isEmpty(typeValue)) {
            helper.sendGenericNotification(data.get("title"), data.get("body"));
            return;
        }
        NotificationType notificationType;
        try {
            notificationType = NotificationType.valueOf(typeValue);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unknown notification type received: " + typeValue, ex);
            helper.sendGenericNotification(data.get("title"), data.get("body"));
            return;
        }
        String courseId = data.get("courseId");
        String courseTitle = data.get("courseTitle");
        Activity activity = buildActivity(data.get("activityId"), data.get("activityTitle"));
        switch (notificationType) {
            case ACTIVITY_STARTED -> helper.sendActivityStartedNotification(courseId, courseTitle, activity);
            case COLLEAGUE_ACTIVITY_STARTED -> helper.sendColleagueActivityStartedNotification(
                    courseId,
                    courseTitle,
                    activity,
                    data.get("colleagueId"));
            case POINTS_THRESHOLD_REACHED -> {
                long points = parseLong(data.get("points"));
                long threshold = parseLong(data.get("threshold"));
                helper.sendPointsMilestoneNotification(
                        courseId,
                        courseTitle,
                        activity,
                        points,
                        threshold,
                        data.get("colleagueId"),
                        data.get("details"));
            }
            case COMMENT_ADDED -> helper.sendCommentAddedNotification(
                    courseId,
                    courseTitle,
                    activity,
                    data.get("commenterId"),
                    data.get("comment"));
            case CHAT_MESSAGE -> helper.sendChatMessageNotification(
                    data.get("conversationId"),
                    data.get("conversationTitle"),
                    data.get("message"));
            case REMINDER -> helper.sendMotivationalReminderNotification(data.get("title"), data.get("body"));
        }
    }

    private Activity buildActivity(String activityId, String activityTitle) {
        if (TextUtils.isEmpty(activityId) && TextUtils.isEmpty(activityTitle)) {
            return null;
        }
        Activity activity = new Activity();
        activity.setId(activityId);
        activity.setTitle(activityTitle);
        return activity;
    }

    private long parseLong(String value) {
        if (TextUtils.isEmpty(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            Log.w(TAG, "Unable to parse long from value: " + value, ex);
            return 0L;
        }
    }
}
