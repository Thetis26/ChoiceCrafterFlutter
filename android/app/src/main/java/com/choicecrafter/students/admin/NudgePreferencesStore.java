package com.choicecrafter.students.admin;

import androidx.annotation.NonNull;

import com.choicecrafter.students.models.NudgePreferences;

/**
 * Abstraction over the persistence layer that stores {@link NudgePreferences} documents.
 */
public interface NudgePreferencesStore {

    /**
     * Persist the provided {@link NudgePreferences} snapshot for the associated user.
     *
     * @param preferences complete set of preferences to persist.
     */
    void save(@NonNull NudgePreferences preferences);
}
