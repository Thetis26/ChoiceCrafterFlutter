package com.choicecrafter.studentapp.ui.activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.choicecrafter.studentapp.models.Activity;

import java.util.HashMap;
import java.util.Map;

public class ActivityViewModel extends ViewModel {

    private final MutableLiveData<Activity> activity = new MutableLiveData<>();
    private final Map<String, TaskSessionState> taskSessionStates = new HashMap<>();

    public void updateActivity(Activity activity) {
        this.activity.setValue(activity);
    }

    public LiveData<Activity> getActivity() {
        return activity;
    }

    public LiveData<String> getActivityDescription() {
        return new MutableLiveData<>(activity.getValue() != null ? activity.getValue().getDescription() : "");
    }

    public LiveData<String> getActivityType() {
        return new MutableLiveData<>(activity.getValue() != null ? activity.getValue().getType() : "");
    }

    public LiveData<String> getActivityDate() {
        return new MutableLiveData<>(activity.getValue() != null ? activity.getValue().getDate() : "");
    }

    public LiveData<String> getActivityTime() {
        return new MutableLiveData<>(activity.getValue() != null ? activity.getValue().getTime() : "");
    }

    public LiveData<String> getActivityTitle() {
        return new MutableLiveData<>(activity.getValue() != null ? activity.getValue().getTitle() : "");
    }

    public TaskSessionState getOrCreateSessionState(String activityId) {
        if (activityId == null) {
            return new TaskSessionState();
        }
        TaskSessionState existing = taskSessionStates.get(activityId);
        if (existing == null) {
            existing = new TaskSessionState();
            taskSessionStates.put(activityId, existing);
        }
        return existing;
    }

    public void clearSessionState(String activityId) {
        if (activityId != null) {
            taskSessionStates.remove(activityId);
        }
    }
}