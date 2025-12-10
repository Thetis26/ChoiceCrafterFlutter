package com.choicecrafter.students.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Module implements Parcelable {
    private String id;
    private String title;
    private String description;
    private List<Activity> activities;
    private int completedPercentage;
    private String courseId;

    public Module() {
        activities = new ArrayList<>();
    }

    public Module(String id, String title, String description,
                   List<Activity> activities,
                   int completedPercentage) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.activities = activities != null ? activities : new ArrayList<>();
        this.completedPercentage = completedPercentage;
        this.courseId = null;
    }

    protected Module(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        courseId = in.readString();
        activities = new ArrayList<>();
        in.readList(activities, Activity.class.getClassLoader());
        completedPercentage = in.readInt();
    }

    public static final Creator<Module> CREATOR = new Creator<Module>() {
        @Override
        public Module createFromParcel(Parcel in) {
            return new Module(in);
        }

        @Override
        public Module[] newArray(int size) {
            return new Module[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(courseId);
        dest.writeList(activities);
        dest.writeInt(completedPercentage);
    }

    @Override
    public int describeContents() {
        return 0;
    }

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

    public List<Activity> getActivities() {
        return activities;
    }

    public void setActivities(List<Activity> activities) {
        this.activities = activities;
    }

    public int getCompletedPercentage() {
        return completedPercentage;
    }

    public void setCompletedPercentage(int completedPercentage) {
        this.completedPercentage = completedPercentage;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    @Override
    public String toString() {
        return "Module{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", courseId='" + courseId + '\'' +
                ", activities=" + activities +
                ", completedPercentage=" + completedPercentage +
                '}';
    }
}
