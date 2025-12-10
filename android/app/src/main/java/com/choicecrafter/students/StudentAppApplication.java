package com.choicecrafter.students;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.choicecrafter.students.notifications.MessagingTokenManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StudentAppApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "StudentAppApp";
    private static final long BACKGROUND_PRESENCE_GRACE_PERIOD_MS = 30_000;
    private int activityReferences = 0;
    private DocumentReference cachedUserDocument;
    private Boolean lastPresenceState = null;
    private final Handler presenceHandler = new Handler(Looper.getMainLooper());
    private final Runnable offlineRunnable = () -> updateUserPresence(false);

    @Override
    public void onCreate() {
        super.onCreate();
        applySavedThemePreference();
        applySavedFontScale();
        registerActivityLifecycleCallbacks(this);
        initializeMessagingToken();
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        // no-op
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!activity.isChangingConfigurations()) {
            cancelPendingOfflineUpdate();
            updateUserPresence(true);
        }
        activityReferences++;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        // no-op
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        // no-op
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (activity.isChangingConfigurations()) {
            activityReferences = Math.max(0, activityReferences - 1);
            return;
        }
        activityReferences = Math.max(0, activityReferences - 1);
        if (activityReferences == 0) {
            scheduleOfflineUpdate();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // no-op
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        // no-op
    }

    public void markUserSignedOut() {
        cancelPendingOfflineUpdate();
        updateUserPresence(false);
        cachedUserDocument = null;
        lastPresenceState = null;
        MessagingTokenManager.clearSyncedUser(getApplicationContext());
    }

    private void cancelPendingOfflineUpdate() {
        presenceHandler.removeCallbacks(offlineRunnable);
    }

    private void scheduleOfflineUpdate() {
        cancelPendingOfflineUpdate();
        presenceHandler.postDelayed(offlineRunnable, BACKGROUND_PRESENCE_GRACE_PERIOD_MS);
    }

    private void updateUserPresence(boolean isOnline) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            cachedUserDocument = null;
            lastPresenceState = null;
            return;
        }

        if (Objects.equals(lastPresenceState, isOnline) && cachedUserDocument != null) {
            return;
        }

        if (cachedUserDocument != null) {
            writePresence(cachedUserDocument, isOnline);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("email", currentUser.getEmail())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> handleUserLookupResult(querySnapshot, isOnline))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to resolve user document for presence tracking", e));
    }

    private void handleUserLookupResult(QuerySnapshot querySnapshot, boolean isOnline) {
        if (querySnapshot == null || querySnapshot.isEmpty()) {
            Log.w(TAG, "No Firestore user document found for authenticated account while updating presence");
            return;
        }
        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
        cachedUserDocument = document.getReference();
        writePresence(cachedUserDocument, isOnline);
    }

    private void writePresence(DocumentReference documentReference, boolean isOnline) {
        if (documentReference == null) {
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("online", isOnline);
        updates.put("lastActiveAt", FieldValue.serverTimestamp());
        lastPresenceState = isOnline;
        documentReference.update(updates)
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to update user presence", e);
                    lastPresenceState = null;
                });
    }

    private void applySavedThemePreference() {
        SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        int savedMode = preferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(savedMode);
    }

    private void applySavedFontScale() {
        SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        float savedScale = preferences.getFloat("font_scale", 1.0f);
        Configuration configuration = new Configuration(getResources().getConfiguration());
        if (Math.abs(configuration.fontScale - savedScale) < 0.01f) {
            return;
        }
        configuration.fontScale = savedScale;
        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
    }

    private void initializeMessagingToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    MessagingTokenManager.storeToken(getApplicationContext(), token);
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    String email = currentUser != null ? currentUser.getEmail() : null;
                    MessagingTokenManager.syncTokenIfNecessary(getApplicationContext(), email);
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to fetch FCM registration token", e));
    }
}

