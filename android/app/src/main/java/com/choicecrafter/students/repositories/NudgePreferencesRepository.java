package com.choicecrafter.students.repositories;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.choicecrafter.students.models.NudgePreferences;
import com.choicecrafter.students.utils.AppLogger;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.concurrent.ExecutionException;

public class NudgePreferencesRepository {

    private static final String TAG = "NudgePreferencesRepo";

    private final FirebaseFirestore firestore;

    public interface PreferencesListener {
        void onPreferencesChanged(@NonNull NudgePreferences preferences);
    }

    public NudgePreferencesRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public NudgePreferencesRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Task<Void> createDefaultPreferences(@NonNull String userEmail) {
        NudgePreferences defaults = NudgePreferences.createDefault(userEmail);
        return firestore.collection(NudgePreferences.COLLECTION_NAME)
                .document(userEmail)
                .set(defaults);
    }

    public ListenerRegistration listenToPreferences(@NonNull String userEmail,
                                                    @NonNull PreferencesListener listener) {
        DocumentReference documentReference = firestore.collection(NudgePreferences.COLLECTION_NAME)
                .document(userEmail);
        return documentReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    AppLogger.e(TAG, "Failed to listen for nudge preferences", error,
                            "userEmail", userEmail);
                    listener.onPreferencesChanged(NudgePreferences.createDefault(userEmail));
                    return;
                }
                listener.onPreferencesChanged(parseSnapshot(userEmail, snapshot));
            }
        });
    }

    public NudgePreferences blockingFetchPreferences(@NonNull String userEmail)
            throws ExecutionException, InterruptedException {
        DocumentReference documentReference = firestore.collection(NudgePreferences.COLLECTION_NAME)
                .document(userEmail);
        DocumentSnapshot snapshot = Tasks.await(documentReference.get());
        return parseSnapshot(userEmail, snapshot);
    }

    private NudgePreferences parseSnapshot(@NonNull String userEmail, @Nullable DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return NudgePreferences.createDefault(userEmail);
        }
        NudgePreferences preferences = snapshot.toObject(NudgePreferences.class);
        if (preferences == null) {
            return NudgePreferences.createDefault(userEmail);
        }
        if (preferences.getUserEmail() == null) {
            preferences.setUserEmail(userEmail);
        }
        return preferences;
    }
}
