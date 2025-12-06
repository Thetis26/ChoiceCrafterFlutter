package com.choicecrafter.studentapp.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SettingsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public SettingsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Personalize how Choice Crafter works for you.");
    }

    public LiveData<String> getText() {
        return mText;
    }
}