package com.choicecrafter.students.notifications;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.choicecrafter.students.R;
import com.choicecrafter.students.models.Notification;
import com.choicecrafter.students.models.NotificationType;
import com.choicecrafter.students.models.NudgePreferences;
import com.choicecrafter.students.repositories.NotificationRepository;
import com.choicecrafter.students.repositories.NudgePreferencesRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Periodic worker that nudges learners to keep their streaks alive with motivational reminders.
 */
public class MotivationalReminderWorker extends Worker {

    public static final String WORK_NAME = "motivational_reminder";
    private static final String TAG = "MotivationWorker";

    private final FirebaseFirestore firestore;
    private final NotificationRepository notificationRepository;
    private final NudgePreferencesRepository nudgePreferencesRepository;
    private final ZoneId zoneId;

    public MotivationalReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.firestore = FirebaseFirestore.getInstance();
        this.notificationRepository = new NotificationRepository();
        this.nudgePreferencesRepository = new NudgePreferencesRepository();
        this.zoneId = ZoneId.systemDefault();
    }

    /**
     * Ensures a single periodic reminder worker is scheduled for the current application instance.
     */
    public static void schedule(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                MotivationalReminderWorker.class,
                1,
                TimeUnit.DAYS)
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No authenticated user found. Skipping motivational reminders.");
            return Result.success();
        }

        String userEmail = currentUser.getEmail();
        if (TextUtils.isEmpty(userEmail)) {
            Log.w(TAG, "Authenticated user is missing an email. Cannot resolve user document.");
            return Result.success();
        }

        try {
            NudgePreferences preferences = nudgePreferencesRepository.blockingFetchPreferences(userEmail);
            if (preferences != null && !preferences.isReminderNotificationsEnabled()) {
                Log.d(TAG, "Reminder notifications disabled for user. Skipping work.");
                return Result.success();
            }

            QuerySnapshot querySnapshot = Tasks.await(firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .limit(1)
                    .get());

            if (querySnapshot == null || querySnapshot.isEmpty()) {
                Log.w(TAG, "Unable to find Firestore profile for email: " + userEmail);
                return Result.success();
            }

            DocumentSnapshot userDocument = querySnapshot.getDocuments().get(0);
            String userId = userDocument.getId();
            LocalDate lastActivityDate = resolveLastActivityDate(userDocument);
            long daysSinceActivity = calculateDaysSinceActivity(lastActivityDate);

            NotificationHelper notificationHelper = new NotificationHelper(getApplicationContext());

            sendDailyMotivation(notificationHelper, userId);
            maybeSendStreakLostReminder(notificationHelper, userId, daysSinceActivity);
            maybeSendExtendedInactivityReminder(notificationHelper, userId, daysSinceActivity);

            return Result.success();
        } catch (ExecutionException e) {
            Log.e(TAG, "Failed to fetch user data for motivational reminders", e);
            return Result.retry();
        } catch (InterruptedException e) {
            Log.w(TAG, "Motivational reminder worker interrupted", e);
            Thread.currentThread().interrupt();
            return Result.retry();
        }
    }

    private void sendDailyMotivation(@NonNull NotificationHelper notificationHelper, @NonNull String userId) {
        String title = getApplicationContext().getString(R.string.notification_motivational_reminder_title);
        String message = getApplicationContext().getString(R.string.notification_motivational_reminder_message);
        recordInboxEntry(userId, message);
        notificationHelper.sendMotivationalReminderNotification(title, message);
    }

    private void maybeSendStreakLostReminder(@NonNull NotificationHelper notificationHelper,
                                             @NonNull String userId,
                                             long daysSinceActivity) {
        if (daysSinceActivity != 2) {
            return;
        }
        String title = getApplicationContext().getString(R.string.notification_streak_lost_title);
        String message = getApplicationContext().getString(R.string.notification_streak_lost_message);
        recordInboxEntry(userId, message);
        notificationHelper.sendMotivationalReminderNotification(title, message);
    }

    private void maybeSendExtendedInactivityReminder(@NonNull NotificationHelper notificationHelper,
                                                     @NonNull String userId,
                                                     long daysSinceActivity) {
        boolean noRecordedActivity = daysSinceActivity < 0;
        if (!noRecordedActivity && daysSinceActivity <= 3) {
            return;
        }
        String title = getApplicationContext().getString(R.string.notification_inactivity_title);
        String message = noRecordedActivity
                ? getApplicationContext().getString(R.string.notification_inactivity_no_activity_message)
                : getApplicationContext().getString(R.string.notification_inactivity_message, daysSinceActivity);
        recordInboxEntry(userId, message);
        notificationHelper.sendMotivationalReminderNotification(title, message);
    }

    private void recordInboxEntry(@NonNull String userId, @NonNull String details) {
        Notification notification = new Notification(
                userId,
                NotificationType.REMINDER,
                null,
                null,
                null,
                Instant.now().toString(),
                details
        );
        notificationRepository.addNotification(notification);
    }

    private long calculateDaysSinceActivity(LocalDate lastActivityDate) {
        if (lastActivityDate == null) {
            return -1;
        }
        LocalDate today = LocalDate.now(zoneId);
        long days = ChronoUnit.DAYS.between(lastActivityDate, today);
        return Math.max(days, 0);
    }

    private LocalDate resolveLastActivityDate(DocumentSnapshot userDocument) {
        Object scoresObj = userDocument.get("scores");
        if (!(scoresObj instanceof Map<?, ?> scoresMap)) {
            return null;
        }

        LocalDate latest = null;
        for (Map.Entry<?, ?> entry : scoresMap.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String dayKey = entry.getKey().toString();
            try {
                LocalDate parsed = LocalDate.parse(dayKey);
                if (latest == null || parsed.isAfter(latest)) {
                    latest = parsed;
                }
            } catch (DateTimeParseException ex) {
                Log.w(TAG, String.format(Locale.US, "Ignoring unparsable score key '%s'", dayKey), ex);
            }
        }
        return latest;
    }
}
