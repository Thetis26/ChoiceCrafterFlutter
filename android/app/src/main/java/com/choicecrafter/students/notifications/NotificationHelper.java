package com.choicecrafter.studentapp.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.Activity;
import com.choicecrafter.studentapp.models.NotificationType;
import com.choicecrafter.studentapp.ui.messages.ChatActivity;
import com.choicecrafter.studentapp.MainActivity;
import com.choicecrafter.studentapp.utils.AppLogger;

public class NotificationHelper {
    private static final String CHANNEL_ACTIVITY_UPDATES = "activity_updates";
    private static final String CHANNEL_CHAT_MESSAGES = "chat_messages";
    public static final int NOTIFICATION_ID = 1001;
    static final String NOTIFICATION_ID_EXTRA = "NOTIFICATION_ID";
    private static final String TAG = "NotificationHelper";
    private static final String PREFERENCES_NAME = "settings";
    private static final String NOTIFICATION_PREF_PREFIX = "notification_";
    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannels();
    }

    public NotificationHelper(){
        this.context = null;
    }

    private void createNotificationChannels() {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        CharSequence activityName = "Activity Updates";
        String activityDescription = "Notifications for activity status changes";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel activityChannel = new NotificationChannel(CHANNEL_ACTIVITY_UPDATES, activityName, importance);
        activityChannel.setDescription(activityDescription);
        notificationManager.createNotificationChannel(activityChannel);

        CharSequence chatName = context.getString(R.string.notification_chat_channel_name);
        String chatDescription = context.getString(R.string.notification_chat_channel_description);
        NotificationChannel chatChannel = new NotificationChannel(CHANNEL_CHAT_MESSAGES, chatName, importance);
        chatChannel.setDescription(chatDescription);
        notificationManager.createNotificationChannel(chatChannel);
    }

    public void sendActivityStartedNotification(String courseId, String courseTitle, Activity activity) {
        if (!isNotificationEnabled(NotificationType.ACTIVITY_STARTED)) {
            AppLogger.d(TAG, "Notifications disabled. Skipping dispatch",
                    "type", NotificationType.ACTIVITY_STARTED);
            return;
        }
        if (!canPostNotifications()) {
            AppLogger.w(TAG, "Notification permission not granted. Unable to show activity started notification.");
            return;
        }

        String courseLabel = courseTitle != null ? courseTitle : courseId;
        String activityTitle = activity != null && activity.getTitle() != null
                ? activity.getTitle()
                : context.getString(R.string.notification_activity_default_title);
        String message = context.getString(R.string.notification_activity_started_message, activityTitle);

        AppLogger.i(TAG, "Dispatching activity started notification",
                "courseId", courseId,
                "courseTitle", courseTitle,
                "activityTitle", activityTitle);

        int notificationId = buildNotificationId("activity-started", courseId, activityTitle);
        PendingIntent openCoursePendingIntent = createOpenCourseActivitiesPendingIntent(
                courseId,
                activity != null ? activity.getId() : null,
                activityTitle,
                notificationId);

        NotificationCompat.Builder builder = createBaseBuilder(CHANNEL_ACTIVITY_UPDATES, courseLabel, message, courseLabel, openCoursePendingIntent);
        dispatchNotification(notificationId, builder);
    }

    public void sendColleagueActivityStartedNotification(String courseId, String courseTitle, Activity activity, String colleagueId) {
        NotificationType notificationType = colleagueId == null
                ? NotificationType.ACTIVITY_STARTED
                : NotificationType.COLLEAGUE_ACTIVITY_STARTED;
        if (!isNotificationEnabled(notificationType)) {
            AppLogger.d(TAG, "Notifications disabled. Skipping dispatch",
                    "type", notificationType);
            return;
        }
        if (!canPostNotifications()) {
            AppLogger.w(TAG, "Notification permission not granted. Unable to show colleague activity notification.");
            return;
        }

        String courseLabel = courseTitle != null ? courseTitle : courseId;
        String activityTitle = activity != null && activity.getTitle() != null ? activity.getTitle() : context.getString(R.string.app_name);
        String colleagueLabel = colleagueId != null ? colleagueId : context.getString(R.string.notification_colleague_placeholder);
        String message = colleagueLabel + " started \"" + activityTitle + "\".";

        AppLogger.i(TAG, "Dispatching colleague activity notification",
                "courseId", courseId,
                "activityTitle", activityTitle,
                "colleagueId", colleagueId);

        int notificationId = buildNotificationId("colleague-start", courseId, activityTitle, colleagueId);
        PendingIntent openActivityPendingIntent = createOpenActivityPendingIntent(courseId, activity, notificationId);
        PendingIntent snoozePendingIntent = createSnoozePendingIntent(courseId, activityTitle, notificationId);

        NotificationCompat.Builder builder = createBaseBuilder(CHANNEL_ACTIVITY_UPDATES, courseLabel, message, courseLabel, openActivityPendingIntent)
                .addAction(R.drawable.ic_snooze, context.getString(R.string.notification_action_snooze), snoozePendingIntent)
                .addAction(R.drawable.ic_open, context.getString(R.string.notification_action_open), openActivityPendingIntent);

        dispatchNotification(notificationId, builder);
    }

    public void sendPointsMilestoneNotification(String courseId, String courseTitle, Activity activity, long points, long threshold,
                                                String colleagueId, String details) {
        if (!isNotificationEnabled(NotificationType.POINTS_THRESHOLD_REACHED)) {
            AppLogger.d(TAG, "Notifications disabled. Skipping dispatch",
                    "type", NotificationType.POINTS_THRESHOLD_REACHED);
            return;
        }
        if (!canPostNotifications()) {
            AppLogger.w(TAG, "Notification permission not granted. Unable to show milestone notification.");
            return;
        }

        String courseLabel = courseTitle != null ? courseTitle : courseId;
        String activityTitle = activity != null && activity.getTitle() != null ? activity.getTitle() : context.getString(R.string.app_name);
        String contributorLabel = colleagueId != null ? colleagueId : context.getString(R.string.notification_colleague_placeholder);
        String milestoneMessage = details != null ? details : "Reached " + points + " points";
        if (colleagueId != null && !milestoneMessage.contains(colleagueId)) {
            milestoneMessage += " thanks to " + contributorLabel;
        }
        milestoneMessage += ". Target: " + threshold;

        AppLogger.i(TAG, "Dispatching points milestone notification",
                "courseId", courseId,
                "activityTitle", activityTitle,
                "points", points,
                "threshold", threshold,
                "colleagueId", colleagueId);

        int notificationId = buildNotificationId("points", courseId, activityTitle, String.valueOf(points));
        PendingIntent openActivityPendingIntent = createOpenActivityPendingIntent(courseId, activity, notificationId);

        NotificationCompat.Builder builder = createBaseBuilder(CHANNEL_ACTIVITY_UPDATES, courseLabel, milestoneMessage, courseLabel, openActivityPendingIntent);
        dispatchNotification(notificationId, builder);
    }

    public void sendCommentAddedNotification(String courseId, String courseTitle, Activity activity, String commenterId, String commentText) {
        if (!isNotificationEnabled(NotificationType.COMMENT_ADDED)) {
            AppLogger.d(TAG, "Notifications disabled. Skipping dispatch",
                    "type", NotificationType.COMMENT_ADDED);
            return;
        }
        if (!canPostNotifications()) {
            AppLogger.w(TAG, "Notification permission not granted. Unable to show comment notification.");
            return;
        }

        String courseLabel = courseTitle != null ? courseTitle : courseId;
        String activityTitle = activity != null && activity.getTitle() != null ? activity.getTitle() : context.getString(R.string.app_name);
        String commenterLabel = commenterId != null ? commenterId : context.getString(R.string.notification_colleague_placeholder);
        String commentSnippet = commentText != null ? commentText : context.getString(R.string.notification_comment_placeholder);
        String message = commenterLabel + " commented on \"" + activityTitle + "\": " + commentSnippet;

        AppLogger.i(TAG, "Dispatching comment notification",
                "courseId", courseId,
                "activityTitle", activityTitle,
                "commenterId", commenterId);

        int notificationId = buildNotificationId("comment", courseId, activityTitle, commenterLabel, commentSnippet);
        PendingIntent openActivityPendingIntent = createOpenActivityPendingIntent(courseId, activity, notificationId);

        NotificationCompat.Builder builder = createBaseBuilder(CHANNEL_ACTIVITY_UPDATES, courseLabel, message, courseLabel, openActivityPendingIntent);
        dispatchNotification(notificationId, builder);
    }

    public void sendChatMessageNotification(String conversationId, String conversationTitle, String messageText) {
        if (!isNotificationEnabled(NotificationType.CHAT_MESSAGE)) {
            AppLogger.d(TAG, "Notifications disabled. Skipping dispatch",
                    "type", NotificationType.CHAT_MESSAGE);
            return;
        }
        if (!canPostNotifications()) {
            AppLogger.w(TAG, "Notification permission not granted. Unable to show chat message notification.");
            return;
        }

        String title = !TextUtils.isEmpty(conversationTitle) ? conversationTitle : context.getString(R.string.app_name);
        String body = !TextUtils.isEmpty(messageText) ? messageText : context.getString(R.string.notification_chat_message_fallback);

        int notificationId = buildNotificationId("chat", conversationId, String.valueOf(body.hashCode()));
        PendingIntent openChatPendingIntent = createOpenChatPendingIntent(conversationId, title, notificationId);

        NotificationCompat.Builder builder = createBaseBuilder(CHANNEL_CHAT_MESSAGES, title, body, title, openChatPendingIntent);
        dispatchNotification(notificationId, builder);
    }

    public void sendMotivationalReminderNotification(String title, String message) {
        if (!isNotificationEnabled(NotificationType.REMINDER)) {
            AppLogger.d(TAG, "Notifications disabled. Skipping dispatch",
                    "type", NotificationType.REMINDER);
            return;
        }
        if (!canPostNotifications()) {
            AppLogger.w(TAG, "Notification permission not granted. Unable to show motivational reminder.");
            return;
        }

        String safeTitle = !TextUtils.isEmpty(title) ? title : context.getString(R.string.app_name);
        String safeMessage = !TextUtils.isEmpty(message)
                ? message
                : context.getString(R.string.notification_motivational_reminder_message);
        int notificationId = buildNotificationId("reminder", safeTitle, safeMessage);
        PendingIntent openHomeIntent = createOpenHomePendingIntent(notificationId);

        NotificationCompat.Builder builder = createBaseBuilder(
                CHANNEL_ACTIVITY_UPDATES,
                safeTitle,
                safeMessage,
                safeTitle,
                openHomeIntent
        );
        dispatchNotification(notificationId, builder);
    }

    public void sendGenericNotification(String title, String message) {
        if (!canPostNotifications()) {
            AppLogger.w(TAG, "Notification permission not granted. Unable to show generic notification.");
            return;
        }
        String safeTitle = !TextUtils.isEmpty(title) ? title : context.getString(R.string.app_name);
        String safeMessage = !TextUtils.isEmpty(message)
                ? message
                : context.getString(R.string.notification_generic_message);
        int notificationId = buildNotificationId("generic", safeTitle, safeMessage);
        PendingIntent openHomeIntent = createOpenHomePendingIntent(notificationId);
        NotificationCompat.Builder builder = createBaseBuilder(
                CHANNEL_ACTIVITY_UPDATES,
                safeTitle,
                safeMessage,
                safeTitle,
                openHomeIntent
        );
        dispatchNotification(notificationId, builder);
    }

    private NotificationCompat.Builder createBaseBuilder(String channelId, String contentTitle, String message, String subText, PendingIntent openActivityPendingIntent) {
        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(contentTitle)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSubText(subText)
                .setContentIntent(openActivityPendingIntent);
    }

    private PendingIntent createOpenCourseActivitiesPendingIntent(String courseId,
                                                                  String activityId,
                                                                  String activityTitle,
                                                                  int requestCode) {
        Intent openCourseIntent = new Intent(context, com.choicecrafter.studentapp.MainActivity.class);
        openCourseIntent.putExtra("openCourseActivities", true);
        openCourseIntent.putExtra("courseId", courseId);
        if (activityId != null && !activityId.trim().isEmpty()) {
            openCourseIntent.putExtra("highlightActivityId", activityId);
        } else if (activityTitle != null && !activityTitle.trim().isEmpty()) {
            openCourseIntent.putExtra("highlightActivityId", activityTitle);
        }
        openCourseIntent.putExtra("highlightActivityTitle", activityTitle);
        openCourseIntent.putExtra(NOTIFICATION_ID_EXTRA, requestCode);
        return PendingIntent.getActivity(
                context,
                requestCode,
                openCourseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent createOpenActivityPendingIntent(String courseId, Activity activity, int requestCode) {
        Intent openActivityIntent = new Intent(context, com.choicecrafter.studentapp.MainActivity.class);
        openActivityIntent.putExtra("openActivityFragment", true);
        openActivityIntent.putExtra("activity", activity);
        openActivityIntent.putExtra("courseId", courseId);
        openActivityIntent.putExtra("activityName", activity != null ? activity.getTitle() : null);
        openActivityIntent.putExtra(NOTIFICATION_ID_EXTRA, requestCode);
        return PendingIntent.getActivity(
                context,
                requestCode,
                openActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent createOpenChatPendingIntent(String conversationId, String conversationTitle, int requestCode) {
        Intent openChatIntent = new Intent(context, ChatActivity.class);
        openChatIntent.putExtra("conversationId", conversationId);
        openChatIntent.putExtra("title", conversationTitle);
        openChatIntent.putExtra(NOTIFICATION_ID_EXTRA, requestCode);
        openChatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                requestCode,
                openChatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent createSnoozePendingIntent(String courseId, String activityTitle, int notificationId) {
        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.putExtra("COURSE_ID", courseId);
        snoozeIntent.putExtra("ACTIVITY_ID", activityTitle);
        snoozeIntent.putExtra(NOTIFICATION_ID_EXTRA, notificationId);
        return PendingIntent.getBroadcast(
                context,
                notificationId,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent createOpenHomePendingIntent(int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(NOTIFICATION_ID_EXTRA, requestCode);
        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void dispatchNotification(int notificationId, NotificationCompat.Builder builder) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        AppLogger.d(TAG, "Delivering notification",
                "notificationId", notificationId);
        notificationManager.notify(notificationId, builder.build());
    }

    private boolean canPostNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isNotificationEnabled(NotificationType type) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(NOTIFICATION_PREF_PREFIX + type.name(), true);
    }

    private int buildNotificationId(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part != null ? part : "").append("|");
        }
        int hash = builder.toString().hashCode();
        if (hash == Integer.MIN_VALUE) {
            hash = NOTIFICATION_ID;
        }
        return Math.abs(hash);
    }
}

