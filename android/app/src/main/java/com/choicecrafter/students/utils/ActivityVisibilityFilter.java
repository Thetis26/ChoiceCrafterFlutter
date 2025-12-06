package com.choicecrafter.studentapp.utils;

import com.choicecrafter.studentapp.models.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility helpers that determine whether an activity should be visible for students.
 */
public final class ActivityVisibilityFilter {

    private ActivityVisibilityFilter() {
        // Utility class
    }

    public static boolean isVisible(Activity activity) {
        if (activity == null) {
            return false;
        }
        Activity.Status status = activity.getStatus();
        return status == Activity.Status.STARTED || status == Activity.Status.ENDED;
    }

    public static List<Activity> filterVisible(List<Activity> activities) {
        List<Activity> result = new ArrayList<>();
        if (activities == null) {
            return result;
        }
        for (Activity activity : activities) {
            if (isVisible(activity)) {
                result.add(activity);
            }
        }
        return result;
    }
}
