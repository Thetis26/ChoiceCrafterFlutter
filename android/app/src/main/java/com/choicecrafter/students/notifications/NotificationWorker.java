package com.choicecrafter.studentapp.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.util.Log;

public class NotificationWorker extends Worker {
    private static final String TAG = "NotificationWorker";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String courseId = getInputData().getString("COURSE_ID");
        String activityId = getInputData().getString("ACTIVITY_ID");
        Log.d(TAG, "Snooze action received for course: " + courseId + ", activity: " + activityId);

        // Handle the snooze action (e.g., reschedule the notification)
        // For example, you can use AlarmManager to reschedule the notification

        return Result.success();
    }
}
