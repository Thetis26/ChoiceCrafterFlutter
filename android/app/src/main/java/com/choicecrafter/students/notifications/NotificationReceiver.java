package com.choicecrafter.students.notifications;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class NotificationReceiver extends BroadcastReceiver {
    private static final int SNOOZE_TIME_MILLIS = 10 * 1000; // 10 seconds
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String courseId = intent.getStringExtra("COURSE_ID");
        String activityId = intent.getStringExtra("ACTIVITY_ID");
        int notificationId = intent.getIntExtra(NotificationHelper.NOTIFICATION_ID_EXTRA, NotificationHelper.NOTIFICATION_ID);
        Log.d(TAG, "Snooze action received for course: " + courseId + ", activity: " + activityId + ", notificationId: " + notificationId);

        // Create input data
        Data inputData = new Data.Builder()
                .putString("COURSE_ID", courseId)
                .putString("ACTIVITY_ID", activityId)
                .build();

        // Create a one-time work request
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInputData(inputData)
                .build();

        // Enqueue the work request
        WorkManager.getInstance(context).enqueue(workRequest);

        // Schedule the reminder notification after 10 seconds
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent newIntent = new Intent(context, NotificationReceiver.class);
        newIntent.putExtra("COURSE_ID", courseId);
        newIntent.putExtra("ACTIVITY_ID", activityId);
        newIntent.putExtra(NotificationHelper.NOTIFICATION_ID_EXTRA, notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, notificationId, newIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + SNOOZE_TIME_MILLIS,
                    pendingIntent
            );
        }

        // Cancel the current notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }
}
