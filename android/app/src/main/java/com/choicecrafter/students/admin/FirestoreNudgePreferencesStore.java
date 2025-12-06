package com.choicecrafter.studentapp.admin;

import androidx.annotation.NonNull;

import com.choicecrafter.studentapp.models.NudgePreferences;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * {@link NudgePreferencesStore} implementation backed by Firebase Firestore.
 */
public class FirestoreNudgePreferencesStore implements NudgePreferencesStore {

    private final FirebaseFirestore firestore;

    public FirestoreNudgePreferencesStore() {
        this(FirebaseFirestore.getInstance());
    }

    public FirestoreNudgePreferencesStore(@NonNull FirebaseFirestore firestore) {
        this.firestore = Objects.requireNonNull(firestore, "firestore == null");
    }

    @Override
    public void save(@NonNull NudgePreferences preferences) {
        Objects.requireNonNull(preferences, "preferences == null");
        String userEmail = Objects.requireNonNull(preferences.getUserEmail(),
                "preferences.userEmail == null");
        try {
            Tasks.await(firestore.collection(NudgePreferences.COLLECTION_NAME)
                    .document(userEmail)
                    .set(preferences));
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to persist nudge preferences", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while persisting nudge preferences", e);
        }
    }
}
