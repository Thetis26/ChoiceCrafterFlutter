package com.choicecrafter.students.adapters;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.students.R;
import com.choicecrafter.students.models.Notification;
import com.choicecrafter.students.repositories.CourseRepository;
import com.choicecrafter.students.utils.TimeAgoUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<Notification> notifications;
    private final CourseRepository courseRepository;
    private final Map<String, String> courseNameCache = new HashMap<>();
    private final Set<String> pendingCourseRequests = new HashSet<>();

    public NotificationAdapter(List<Notification> notifications) {
        this(notifications, new CourseRepository());
    }

    public NotificationAdapter(List<Notification> notifications, CourseRepository courseRepository) {
        this.notifications = notifications;
        this.courseRepository = courseRepository != null ? courseRepository : new CourseRepository();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_card, parent, false);
        return new NotificationViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        Log.d("NotificationAdapter", "Binding notification at position " + position + ": " + notification.getType());

        holder.typeTextView.setText(notification.getType().getDisplayName());

        String courseId = notification.getCourseId();
        String cachedCourseName = TextUtils.isEmpty(courseId) ? null : courseNameCache.get(courseId);
        holder.detailsTextView.setText(buildDetailsMessage(notification, cachedCourseName));

        if (!TextUtils.isEmpty(courseId) && cachedCourseName == null) {
            maybeFetchCourseName(courseId);
        }

        String timestamp = notification.getTimestamp();
        if (timestamp != null) {
            holder.timeTextView.setText(TimeAgoUtil.getTimeAgo(timestamp));
        } else {
            holder.timeTextView.setText("-");
        }
    }

    @Override
    public int getItemCount() {
        Log.d("NotificationAdapter", "Total notifications: " + notifications.size());
        return notifications.size();
    }

    private String buildDetailsMessage(Notification notification, String courseName) {
        String courseSegment = courseName != null ? " in course: " + courseName : "";
        String activityName = notification.getActivityId() != null ? notification.getActivityId() : "this activity";
        String colleague = notification.getRelatedUserId() != null ? notification.getRelatedUserId() : "A colleague";
        String details = notification.getDetails();

        return switch (notification.getType()) {
            case ACTIVITY_STARTED -> {
                String baseMessage = !TextUtils.isEmpty(details)
                        ? details
                        : "Activity " + activityName + " is now available";
                yield baseMessage + courseSegment;
            }
            case COLLEAGUE_ACTIVITY_STARTED ->
                    colleague + " started " + activityName + courseSegment;
            case POINTS_THRESHOLD_REACHED -> {
                String milestone = details != null ? details : "Points milestone reached";
                yield milestone + " for " + activityName + courseSegment;
            }
            case COMMENT_ADDED -> {
                String commentSnippet = details != null ? details : "Left a comment";
                yield colleague + " commented: \"" + commentSnippet + "\" on " + activityName + courseSegment;
            }
            case CHAT_MESSAGE -> {
                String sender = notification.getRelatedUserId() != null ? notification.getRelatedUserId() : "Someone";
                String messageSnippet = !TextUtils.isEmpty(details) ? details : "sent a message";
                int maxLen = 120;
                if (messageSnippet.length() > maxLen) {
                    messageSnippet = messageSnippet.substring(0, maxLen - 1) + "…";
                }
                yield sender + " sent: \"" + messageSnippet + "\"" + courseSegment;
            }
            case REMINDER -> {
                if (!TextUtils.isEmpty(details)) {
                    yield details;
                }
                yield "Don't forget to complete " + activityName + courseSegment;
            }
        };
    }

    private void maybeFetchCourseName(String courseId) {
        if (pendingCourseRequests.contains(courseId)) {
            return;
        }
        pendingCourseRequests.add(courseId);
        courseRepository.getCourseNameById(courseId, new CourseRepository.Callback<>() {
            @Override
            public void onSuccess(String courseName) {
                courseNameCache.put(courseId, courseName);
                pendingCourseRequests.remove(courseId);
                notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                pendingCourseRequests.remove(courseId);
                Log.e("NotificationAdapter", "Failed to fetch course name", e);
            }
        });
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView typeTextView, detailsTextView, timeTextView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            typeTextView = itemView.findViewById(R.id.notificationTitle);
            detailsTextView = itemView.findViewById(R.id.notificationDescription);
            timeTextView = itemView.findViewById(R.id.notificationTimestamp);
        }
    }
}