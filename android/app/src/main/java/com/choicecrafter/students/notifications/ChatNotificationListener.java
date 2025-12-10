package com.choicecrafter.students.notifications;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.choicecrafter.students.R;
import com.choicecrafter.students.models.Conversation;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatNotificationListener {
    private final Context context;
    private final NotificationHelper notificationHelper;
    private final Map<String, Long> lastNotifiedTimestamps = new HashMap<>();
    private ListenerRegistration conversationsRegistration;
    private String userId;

    public ChatNotificationListener(Context context) {
        this.context = context.getApplicationContext();
        this.notificationHelper = new NotificationHelper(this.context);
    }

    public void start(@Nullable String userId) {
        stop();
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        this.userId = userId;
        conversationsRegistration = FirebaseFirestore.getInstance()
                .collection("conversations")
                .whereArrayContains("participants", userId)
                .addSnapshotListener(this::handleConversationsSnapshot);
    }

    private void handleConversationsSnapshot(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
        if (error != null || snapshots == null) {
            return;
        }
        for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
            Conversation conversation = snapshot.toObject(Conversation.class);
            if (conversation == null) {
                continue;
            }
            conversation.setId(snapshot.getId());
            if (!shouldNotify(conversation)) {
                continue;
            }
            String title = !TextUtils.isEmpty(conversation.getTitle())
                    ? conversation.getTitle()
                    : context.getString(R.string.app_name);
            String body = !TextUtils.isEmpty(conversation.getLastMessage())
                    ? conversation.getLastMessage()
                    : context.getString(R.string.notification_chat_message_fallback);
            notificationHelper.sendChatMessageNotification(conversation.getId(), title, body);
            lastNotifiedTimestamps.put(conversation.getId(), conversation.getTimestamp());
        }
    }

    private boolean shouldNotify(Conversation conversation) {
        if (userId == null) {
            return false;
        }
        List<String> unreadBy = conversation.getUnreadBy();
        if (unreadBy == null || !unreadBy.contains(userId)) {
            return false;
        }
        if (userId.equals(conversation.getLastMessageSenderId())) {
            return false;
        }
        long timestamp = conversation.getTimestamp();
        Long previousTimestamp = lastNotifiedTimestamps.get(conversation.getId());
        if (previousTimestamp != null && previousTimestamp >= timestamp) {
            return false;
        }
        return true;
    }

    public void stop() {
        if (conversationsRegistration != null) {
            conversationsRegistration.remove();
            conversationsRegistration = null;
        }
        lastNotifiedTimestamps.clear();
        userId = null;
    }
}
