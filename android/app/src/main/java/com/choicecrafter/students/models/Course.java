package com.choicecrafter.students.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Course implements Parcelable {

    private String id;
    private String title;
    private String description;
    private String teacher;
    private String imageUrl;
    private List<Activity> activities = new ArrayList<>();
    private List<Module> modules = new ArrayList<>();

    public Course(String title, String description, String teacher, List<Activity> activities) {
        this.title = title;
        this.description = description;
        this.teacher = teacher;
        this.activities = activities;
    }

    public Course() {
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

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public List<Activity> getActivities() {
        return activities;
    }

    public void setActivities(List<Activity> activities) {
        this.activities = activities;
    }

    public List<Module> getModules() {
        return modules;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules != null ? modules : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    protected Course(Parcel in) {
        title = in.readString();
        description = in.readString();
        teacher = in.readString();
        imageUrl = in.readString();
        activities = new ArrayList<>();
        in.readList(activities, Activity.class.getClassLoader());
        modules = new ArrayList<>();
        in.readList(modules, Module.class.getClassLoader());
    }

    public static final Creator<Course> CREATOR = new Creator<Course>() {
        @Override
        public Course createFromParcel(Parcel in) {
            return new Course(in);
        }

        @Override
        public Course[] newArray(int size) {
            return new Course[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(teacher);
        dest.writeString(imageUrl);
        dest.writeList(activities);
        dest.writeList(modules);
    }
}