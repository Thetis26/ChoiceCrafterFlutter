package com.choicecrafter.studentapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import com.choicecrafter.studentapp.models.NudgePreferences;
import com.choicecrafter.studentapp.models.User;
import com.choicecrafter.studentapp.models.badges.BadgeStatus;
import com.choicecrafter.studentapp.utils.AppLogger;

public class MainViewModel extends ViewModel {

    private static final String TAG = "MainViewModel";
    private final MutableLiveData<User> user = new MutableLiveData<>();
    private final MutableLiveData<String> newlyEnrolledCourseId = new MutableLiveData<>();
    private final MutableLiveData<NudgePreferences> nudgePreferences = new MutableLiveData<>();
    private final MutableLiveData<List<BadgeStatus>> badgeStatuses = new MutableLiveData<>();

    public LiveData<User> getUser() {
        AppLogger.d(TAG, "getUser invoked");
        return user;
    }

    public void setUser(User user) {
        AppLogger.d(TAG, "Updating logged in user",
                "hasUser", user != null,
                "email", user != null ? user.getEmail() : null);
        this.user.setValue(user);
    }

    public LiveData<String> getNewlyEnrolledCourseId() {
        return newlyEnrolledCourseId;
    }

    public void setNewlyEnrolledCourseId(String courseId) {
        newlyEnrolledCourseId.setValue(courseId);
    }

    public void clearNewlyEnrolledCourseId() {
        newlyEnrolledCourseId.setValue(null);
    }

    public LiveData<NudgePreferences> getNudgePreferences() {
        return nudgePreferences;
    }

    public void setNudgePreferences(NudgePreferences preferences) {
        AppLogger.d(TAG, "Updating nudge preferences",
                "preferences", preferences);
        nudgePreferences.setValue(preferences);
    }

    public LiveData<List<BadgeStatus>> getBadgeStatuses() {
        return badgeStatuses;
    }

    public void setBadgeStatuses(List<BadgeStatus> statuses) {
        badgeStatuses.setValue(statuses);
    }
}
