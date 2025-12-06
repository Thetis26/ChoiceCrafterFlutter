package com.choicecrafter.studentapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.choicecrafter.studentapp.models.tasks.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Activity implements Parcelable {
    public enum Status {
        CREATED, STARTED, ENDED
    }

    private String id;
    private String title;
    private String description;
    private String type;
    private String date;
    private String time;
    private List<Task> tasks;
    private List<Recommendation> recommendations;
    private Status status;
    private List<String> reminders; // List of date-time strings representing reminders
    private List<Comment> comments;
    private Map<String, Long> reactionCounts;

    public Activity(String title, String description, String type, String date, String time, List<Task> tasks,
                    List<Recommendation> recommendations, Status status, List<String> reminders,
                    List<Comment> comments, long reactions) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.date = date;
        this.time = time;
        this.tasks = tasks;
        this.recommendations = recommendations;
        this.status = status;
        this.reminders = reminders;
        this.comments = comments;
        this.reactionCounts = new HashMap<>();
        setReactions(reactions);
    }

    public Activity() {
        tasks = new ArrayList<>();
        recommendations = new ArrayList<>();
        reminders = new ArrayList<>();
        comments = new ArrayList<>();
        status = Status.CREATED;
        reactionCounts = new HashMap<>();
    }

    protected Activity(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        type = in.readString();
        date = in.readString();
        time = in.readString();
        tasks = new ArrayList<>();
        in.readList(tasks, Task.class.getClassLoader());
        recommendations = new ArrayList<>();
        in.readList(recommendations, Recommendation.class.getClassLoader());
        status = Status.valueOf(in.readString());
        reminders = in.createStringArrayList();
        comments = new ArrayList<>();
        in.readList(comments, Comment.class.getClassLoader());
        int reactionsSize = in.readInt();
        reactionCounts = new HashMap<>();
        for (int i = 0; i < reactionsSize; i++) {
            String key = in.readString();
            long value = in.readLong();
            reactionCounts.put(key, value);
        }
    }

    public static final Creator<Activity> CREATOR = new Creator<Activity>() {
        @Override
        public Activity createFromParcel(Parcel in) {
            return new Activity(in);
        }

        @Override
        public Activity[] newArray(int size) {
            return new Activity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(type);
        dest.writeString(date);
        dest.writeString(time);
        dest.writeList(tasks);
        dest.writeList(recommendations);
        dest.writeStringList(reminders);
        dest.writeString(status.name());
        dest.writeList(comments);
        if (reactionCounts != null) {
            dest.writeInt(reactionCounts.size());
            for (Map.Entry<String, Long> entry : reactionCounts.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeLong(entry.getValue() != null ? entry.getValue() : 0L);
            }
        } else {
            dest.writeInt(0);
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<String> getReminders() {
        return reminders;
    }

    public void setReminders(List<String> reminders) {
        this.reminders = reminders;
    }

    // Getters and Setters
    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public long getReactions() {
        if (reactionCounts == null || reactionCounts.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (Long count : reactionCounts.values()) {
            total += count != null ? count : 0;
        }
        return total;
    }

    public Map<String, Long> getReactionCounts() {
        if (reactionCounts == null) {
            reactionCounts = new HashMap<>();
        }
        return reactionCounts;
    }

    public long getReactionCount(String reactionType) {
        return getReactionCounts().getOrDefault(reactionType, 0L);
    }

    public void setReactions(long reactions) {
        reactionCounts = new HashMap<>();
        reactionCounts.put("likes", reactions);
    }

    public void setReactions(Map<String, Long> reactions) {
        reactionCounts = new HashMap<>();
        if (reactions != null) {
            for (Map.Entry<String, Long> entry : reactions.entrySet()) {
                reactionCounts.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void incrementReaction(String reactionType) {
        Map<String, Long> reactions = getReactionCounts();
        long newValue = reactions.getOrDefault(reactionType, 0L) + 1;
        reactions.put(reactionType, newValue);
    }

    @Override
    public String toString() {
        return "Activity{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", tasks=" + tasks +
                ", recommendations=" + recommendations +
                ", status=" + status +
                ", reminders=" + reminders +
                '}';
    }
}