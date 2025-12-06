package com.choicecrafter.studentapp.repositories;

import com.choicecrafter.studentapp.models.Course;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple in-memory cache for course data so that it can be preloaded during
 * the loading screen and reused across the app without additional network calls.
 */
public class CourseCache {

    private static final CourseCache INSTANCE = new CourseCache();

    private final Object lock = new Object();
    private List<Course> cachedCourses = new ArrayList<>();
    private boolean isLoaded = false;

    private CourseCache() {
    }

    public static CourseCache getInstance() {
        return INSTANCE;
    }

    public void setCourses(List<Course> courses) {
        synchronized (lock) {
            if (courses == null) {
                this.cachedCourses = new ArrayList<>();
            } else {
                this.cachedCourses = new ArrayList<>(courses);
            }
            isLoaded = true;
        }
    }

    public List<Course> getCourses() {
        synchronized (lock) {
            return new ArrayList<>(cachedCourses);
        }
    }

    public boolean isLoaded() {
        synchronized (lock) {
            return isLoaded;
        }
    }

    public void clear() {
        synchronized (lock) {
            cachedCourses.clear();
            isLoaded = false;
        }
    }
}
