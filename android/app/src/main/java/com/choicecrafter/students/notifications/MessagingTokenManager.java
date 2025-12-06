package com.choicecrafter.studentapp.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Persists and synchronises the Firebase Cloud Messaging registration token with Firestore.
 */
public final class MessagingTokenManager {
    private static final String TAG = "MessagingTokenMgr";
    private static final String PREFS_NAME = "messaging";
    private static final String KEY_REGISTERED_TOKEN = "registered_token";
    private static final String KEY_LAST_SYNCED_USER = "last_synced_user";
    private static final String KEY_LAST_SYNCED_TOKEN = "last_synced_token";

    private MessagingTokenManager() {
        // No instances.
    }

    public static void storeToken(@NonNull Context context, @Nullable String token) {
        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String existingToken = preferences.getString(KEY_REGISTERED_TOKEN, null);
        if (TextUtils.equals(existingToken, token)) {
            Log.d(TAG, "FCM token unchanged. Skipping persistence update.");
            return;
        }
        preferences.edit()
                .putString(KEY_REGISTERED_TOKEN, token)
                .remove(KEY_LAST_SYNCED_TOKEN)
                .remove(KEY_LAST_SYNCED_USER)
                .apply();
    }

    public static void clearSyncedUser(@NonNull Context context) {
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit()
                .remove(KEY_LAST_SYNCED_TOKEN)
                .remove(KEY_LAST_SYNCED_USER)
                .apply();
    }

    public static void syncTokenIfNecessary(@NonNull Context context, @Nullable String userEmail) {
        if (TextUtils.isEmpty(userEmail)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String storedToken = preferences.getString(KEY_REGISTERED_TOKEN, null);
        if (TextUtils.isEmpty(storedToken)) {
            Log.d(TAG, "No FCM token stored. Skipping sync.");
            return;
        }
        String lastSyncedUser = preferences.getString(KEY_LAST_SYNCED_USER, null);
        String lastSyncedToken = preferences.getString(KEY_LAST_SYNCED_TOKEN, null);
        if (TextUtils.equals(lastSyncedUser, userEmail) && TextUtils.equals(lastSyncedToken, storedToken)) {
            Log.d(TAG, "FCM token already synced for user: " + userEmail);
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("email", userEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.w(TAG, "No user document found while syncing messaging token for " + userEmail);
                        return;
                    }
                    querySnapshot.getDocuments().get(0).getReference()
                            .update("messagingTokens", FieldValue.arrayUnion(storedToken))
                            .addOnSuccessListener(unused -> preferences.edit()
                                    .putString(KEY_LAST_SYNCED_USER, userEmail)
                                    .putString(KEY_LAST_SYNCED_TOKEN, storedToken)
                                    .apply())
                            .addOnFailureListener(e -> Log.w(TAG, "Failed to update messaging tokens for " + userEmail, e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to resolve user document while syncing token", e));
    }
}
