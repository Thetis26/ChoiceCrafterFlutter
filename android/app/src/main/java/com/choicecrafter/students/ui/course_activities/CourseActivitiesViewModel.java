package com.choicecrafter.students.ui.course_activities;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.choicecrafter.students.models.Course;

public class CourseActivitiesViewModel extends ViewModel {

    private final MutableLiveData<Course> course = new MutableLiveData<>();

    public void updateCourse(Course course) {
        this.course.setValue(course);
    }

    public LiveData<Course> getCourse() {
        return course;
    }

    public LiveData<String> getCourseDescription() {
        return new MutableLiveData<>(course.getValue() != null ? course.getValue().getDescription() : "");
    }

    public LiveData<String> getCourseTeacher() {
        return new MutableLiveData<>(course.getValue() != null ? course.getValue().getTeacher() : "");
    }

    public LiveData<String> getCourseName() {
        return new MutableLiveData<>(course.getValue() != null ? course.getValue().getTitle() : "");
    }
}