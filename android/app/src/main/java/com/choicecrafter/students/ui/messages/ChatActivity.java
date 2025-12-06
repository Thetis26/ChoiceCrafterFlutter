package com.choicecrafter.studentapp.ui.messages;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.adapters.ChatMessageAdapter;
import com.choicecrafter.studentapp.adapters.UserSelectionAdapter;
import com.choicecrafter.studentapp.models.ChatMessage;
import com.choicecrafter.studentapp.models.Conversation;
import com.choicecrafter.studentapp.models.User;
import com.choicecrafter.studentapp.utils.ConversationTitleHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private ChatMessageAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private FirebaseFirestore db;
    private String conversationId;
    private String conversationTitle;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private String currentUserId;
    private DocumentReference conversationRef;
    private View messageInputContainer;
    private TextView noAccessView;
    private final List<String> participantIds = new ArrayList<>();
    private final Set<String> participantIdSet = new HashSet<>();
    private final List<String> formerParticipantIds = new ArrayList<>();
    private final Set<String> formerParticipantIdSet = new HashSet<>();
    private final List<User> allUsers = new ArrayList<>();
    private UserSelectionAdapter addParticipantAdapter;
    private AlertDialog addParticipantDialog;
    private TextView addParticipantEmptyView;
    private String addParticipantQuery = "";
    private ListenerRegistration conversationRegistration;
    private ListenerRegistration usersRegistration;
    private ListenerRegistration messagesRegistration;
    private boolean hasAccess = true;
    private boolean hasLoadedReadOnlyMessages = false;
    private boolean isLeavingChat = false;
    private boolean hasCustomTitle = false;
    private String storedConversationTitle = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.messagesRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : null;
        adapter = new ChatMessageAdapter(currentUserId);
        recyclerView.setAdapter(adapter);

        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        messageInputContainer = findViewById(R.id.messageInputContainer);
        noAccessView = findViewById(R.id.noAccessView);
        if (sendButton != null) {
            sendButton.setOnClickListener(v -> sendMessage());
        }
        if (messageInput != null) {
            messageInput.setOnEditorActionListener((TextView textView, int actionId, android.view.KeyEvent keyEvent) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    return true;
                }
                return false;
            });
        }

        View rootView = findViewById(R.id.chatRoot);
        if (rootView != null) {
            final int baseRootPaddingStart = rootView.getPaddingStart();
            final int baseRootPaddingTop = rootView.getPaddingTop();
            final int baseRootPaddingEnd = rootView.getPaddingEnd();
            final int baseRootPaddingBottom = rootView.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                int bottomInset = Math.max(systemInsets.bottom, imeInsets.bottom);
                v.setPaddingRelative(baseRootPaddingStart, baseRootPaddingTop, baseRootPaddingEnd,
                        baseRootPaddingBottom + bottomInset);
                return insets;
            });
            ViewCompat.requestApplyInsets(rootView);
        }

        conversationId = getIntent().getStringExtra("conversationId");
        conversationTitle = getIntent().getStringExtra("title");
        if (TextUtils.isEmpty(conversationTitle)) {
            conversationTitle = getString(R.string.app_name);
        }

        if (conversationId == null) {
            Toast.makeText(this, R.string.chat_missing_conversation_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setTitle(conversationTitle);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        }

        db = FirebaseFirestore.getInstance();
        conversationRef = db.collection("conversations").document(conversationId);

        boolean initialReadOnly = getIntent().getBooleanExtra("readOnly", false);
        if (initialReadOnly) {
            updateAccessState(false, true);
        } else {
            updateAccessState(true);
        }

        listenForConversationMetadata();
        listenForAllUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        markConversationAsRead();
    }

    private void sendMessage() {
        if (db == null || conversationId == null || messageInput == null || sendButton == null || conversationRef == null) {
            return;
        }

        if (!hasAccess) {
            return;
        }

        String text = messageInput.getText() != null ? messageInput.getText().toString().trim() : "";
        if (text.isEmpty()) {
            return;
        }

        if (currentUserId == null) {
            Toast.makeText(this, R.string.chat_send_error, Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();
        ChatMessage message = new ChatMessage(currentUserId, text, timestamp);

        sendButton.setEnabled(false);
        conversationRef.collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    messageInput.setText("");
                    sendButton.setEnabled(true);
                    updateConversationMetadata(text, timestamp);
                    if (recyclerView != null) {
                        recyclerView.scrollToPosition(Math.max(messages.size() - 1, 0));
                    }
                })
                .addOnFailureListener(e -> {
                    sendButton.setEnabled(true);
                    Toast.makeText(this, R.string.chat_send_error, Toast.LENGTH_SHORT).show();
                });
    }

    private void updateConversationMetadata(String lastMessage, long timestamp) {
        if (conversationRef == null || currentUserId == null) {
            return;
        }
        conversationRef.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(conversationRef);
            List<String> unreadParticipants = new ArrayList<>();
            Object participantsObj = snapshot.get("participants");
            if (participantsObj instanceof List<?>) {
                for (Object item : (List<?>) participantsObj) {
                    if (item == null) {
                        continue;
                    }
                    String participantId = item.toString();
                    if (!participantId.equals(currentUserId)) {
                        unreadParticipants.add(participantId);
                    }
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessage", lastMessage);
            updates.put("timestamp", timestamp);
            updates.put("lastMessageSenderId", currentUserId);
            updates.put("unreadBy", unreadParticipants);
            updates.put("unread", !unreadParticipants.isEmpty());
            transaction.set(conversationRef, updates, SetOptions.merge());
            return null;
        }).addOnFailureListener(e -> Log.w(TAG, "Failed to update conversation metadata", e));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (item.getItemId() == R.id.action_edit_chat_name) {
            showEditTitleDialog();
            return true;
        }
        if (item.getItemId() == R.id.action_add_participant) {
            showAddParticipantDialog();
            return true;
        }
        if (item.getItemId() == R.id.action_view_participants) {
            showParticipantsDialog();
            return true;
        }
        if (item.getItemId() == R.id.action_leave_chat) {
            confirmLeaveChat();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem addParticipantItem = menu.findItem(R.id.action_add_participant);
        if (addParticipantItem != null) {
            addParticipantItem.setVisible(hasAccess);
        }
        MenuItem renameItem = menu.findItem(R.id.action_edit_chat_name);
        if (renameItem != null) {
            renameItem.setVisible(hasAccess);
        }
        MenuItem leaveItem = menu.findItem(R.id.action_leave_chat);
        if (leaveItem != null) {
            leaveItem.setVisible(hasAccess);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void listenForConversationMetadata() {
        if (conversationRef == null) {
            return;
        }
        conversationRegistration = conversationRef.addSnapshotListener((value, error) -> {
            if (value != null && value.exists()) {
                Conversation conversation = value.toObject(Conversation.class);
                if (conversation != null) {
                    storedConversationTitle = conversation.getTitle();
                    hasCustomTitle = !TextUtils.isEmpty(storedConversationTitle);

                    participantIds.clear();
                    participantIdSet.clear();
                    if (conversation.getParticipants() != null) {
                        participantIds.addAll(conversation.getParticipants());
                        participantIdSet.addAll(conversation.getParticipants());
                    }
                    formerParticipantIds.clear();
                    formerParticipantIdSet.clear();
                    if (conversation.getFormerParticipants() != null) {
                        formerParticipantIds.addAll(conversation.getFormerParticipants());
                        formerParticipantIdSet.addAll(conversation.getFormerParticipants());
                    }
                    applyAddParticipantFilter(addParticipantQuery);
                    updateDisplayTitle();

                    boolean isParticipant = currentUserId != null && participantIdSet.contains(currentUserId) && !isLeavingChat;
                    boolean isFormerParticipant = currentUserId != null && formerParticipantIdSet.contains(currentUserId);
                    if (isParticipant) {
                        updateAccessState(true);
                        markConversationAsRead();
                    } else if (isFormerParticipant) {
                        updateAccessState(false, true);
                    } else {
                        updateAccessState(false);
                    }
                }
            }
        });
    }

    private void listenForAllUsers() {
        if (db == null) {
            return;
        }
        usersRegistration = db.collection("users")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        allUsers.clear();
                        for (DocumentSnapshot snapshot : value.getDocuments()) {
                            User user = snapshot.toObject(User.class);
                            if (user != null) {
                                allUsers.add(user);
                            }
                        }
                        applyAddParticipantFilter(addParticipantQuery);
                        updateDisplayTitle();
                    }
                });
    }

    private void updateAccessState(boolean newHasAccess) {
        updateAccessState(newHasAccess, false);
    }

    private void updateAccessState(boolean newHasAccess, boolean readOnly) {
        hasAccess = newHasAccess;
        if (hasAccess) {
            hasLoadedReadOnlyMessages = false;
            if (messageInputContainer != null) {
                messageInputContainer.setVisibility(View.VISIBLE);
            }
            if (messageInput != null) {
                messageInput.setEnabled(true);
            }
            if (sendButton != null) {
                sendButton.setEnabled(true);
            }
            if (noAccessView != null) {
                noAccessView.setVisibility(View.GONE);
                noAccessView.setText(R.string.chat_no_access);
            }
            startListeningForMessages();
        } else {
            stopListeningForMessages();
            if (messageInput != null) {
                messageInput.setText("");
                messageInput.setEnabled(false);
            }
            if (sendButton != null) {
                sendButton.setEnabled(false);
            }
            if (messageInputContainer != null) {
                messageInputContainer.setVisibility(View.GONE);
            }
            if (noAccessView != null) {
                noAccessView.setVisibility(View.VISIBLE);
                int messageRes = readOnly ? R.string.chat_read_only : R.string.chat_no_access;
                noAccessView.setText(messageRes);
            }
            if (addParticipantDialog != null && addParticipantDialog.isShowing()) {
                addParticipantDialog.dismiss();
            }
            if (readOnly) {
                if (!messages.isEmpty()) {
                    hasLoadedReadOnlyMessages = true;
                }
                loadReadOnlyMessagesIfNeeded();
            } else {
                hasLoadedReadOnlyMessages = false;
            }
        }
        invalidateOptionsMenu();
    }

    private void startListeningForMessages() {
        if (conversationRef == null || messagesRegistration != null) {
            return;
        }
        messagesRegistration = conversationRef.collection("messages").orderBy("timestamp")
                .addSnapshotListener((value, error) -> {
                    if (value != null && hasAccess) {
                        messages.clear();
                        for (DocumentSnapshot snapshot : value.getDocuments()) {
                            ChatMessage message = snapshot.toObject(ChatMessage.class);
                            if (message != null) {
                                messages.add(message);
                            }
                        }
                        adapter.setMessages(messages);
                        if (recyclerView != null) {
                            recyclerView.scrollToPosition(Math.max(messages.size() - 1, 0));
                        }
                        markConversationAsRead();
                    }
                });
    }

    private void stopListeningForMessages() {
        if (messagesRegistration != null) {
            messagesRegistration.remove();
            messagesRegistration = null;
        }
    }

    private void loadReadOnlyMessagesIfNeeded() {
        if (conversationRef == null) {
            return;
        }
        if (hasLoadedReadOnlyMessages || !messages.isEmpty()) {
            hasLoadedReadOnlyMessages = true;
            return;
        }
        conversationRef.collection("messages").orderBy("timestamp")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    messages.clear();
                    for (DocumentSnapshot snapshot : querySnapshot.getDocuments()) {
                        ChatMessage message = snapshot.toObject(ChatMessage.class);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                    adapter.setMessages(messages);
                    if (recyclerView != null) {
                        recyclerView.scrollToPosition(Math.max(messages.size() - 1, 0));
                    }
                    hasLoadedReadOnlyMessages = true;
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.chat_load_history_error, Toast.LENGTH_SHORT).show());
    }

    private void markConversationAsRead() {
        if (conversationRef == null || currentUserId == null || !hasAccess) {
            return;
        }
        conversationRef.getFirestore().runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(conversationRef);
            List<String> unreadParticipants = new ArrayList<>();
            Object unreadObj = snapshot.get("unreadBy");
            if (unreadObj instanceof List<?>) {
                for (Object item : (List<?>) unreadObj) {
                    if (item != null) {
                        unreadParticipants.add(item.toString());
                    }
                }
            } else if (Boolean.TRUE.equals(snapshot.getBoolean("unread"))) {
                Object participantsObj = snapshot.get("participants");
                if (participantsObj instanceof List<?>) {
                    String lastSenderId = snapshot.getString("lastMessageSenderId");
                    for (Object participant : (List<?>) participantsObj) {
                        if (participant == null) {
                            continue;
                        }
                        String participantId = participant.toString();
                        if (!TextUtils.isEmpty(lastSenderId) && lastSenderId.equals(participantId)) {
                            continue;
                        }
                        unreadParticipants.add(participantId);
                    }
                }
            }

            if (!unreadParticipants.contains(currentUserId)) {
                if (!(unreadObj instanceof List<?>)) {
                    Map<String, Object> initialization = new HashMap<>();
                    initialization.put("unreadBy", unreadParticipants);
                    initialization.put("unread", !unreadParticipants.isEmpty());
                    transaction.update(conversationRef, initialization);
                }
                return null;
            }

            unreadParticipants.remove(currentUserId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("unreadBy", unreadParticipants);
            updates.put("unread", !unreadParticipants.isEmpty());
            transaction.update(conversationRef, updates);
            return null;
        }).addOnFailureListener(e -> Log.w(TAG, "Failed to mark conversation as read", e));
    }

    private void showEditTitleDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_edit_chat_title, null);
        TextInputEditText titleInput = dialogView.findViewById(R.id.editChatTitleInput);
        if (titleInput != null) {
            titleInput.setText(conversationTitle);
            titleInput.setSelection(titleInput.getText() != null ? titleInput.getText().length() : 0);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.chat_edit_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                .setPositiveButton(R.string.chat_edit_title_save, null)
                .create();

        dialog.setOnShowListener(d -> {
            if (titleInput == null) {
                return;
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newTitle = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
                if (newTitle.isEmpty()) {
                    titleInput.setError(getString(R.string.chat_edit_title_error));
                    return;
                }
                updateChatTitle(newTitle);
                dialog.dismiss();
            });
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void updateChatTitle(String newTitle) {
        if (conversationRef == null) {
            return;
        }
        conversationRef.update("title", newTitle)
                .addOnSuccessListener(unused -> {
                    storedConversationTitle = newTitle;
                    hasCustomTitle = true;
                    conversationTitle = newTitle;
                    setTitle(newTitle);
                    Toast.makeText(this, R.string.chat_edit_title_success, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.chat_edit_title_failure, Toast.LENGTH_SHORT).show());
    }

    private void updateDisplayTitle() {
        String displayTitle;
        if (hasCustomTitle && !TextUtils.isEmpty(storedConversationTitle)) {
            displayTitle = storedConversationTitle;
        } else {
            displayTitle = ConversationTitleHelper.buildParticipantTitle(
                    participantIds,
                    formerParticipantIds,
                    currentUserId,
                    allUsers);
        }

        if (TextUtils.isEmpty(displayTitle)) {
            displayTitle = getString(R.string.app_name);
        }

        conversationTitle = displayTitle;
        setTitle(displayTitle);
    }

    private void showAddParticipantDialog() {
        if (allUsers.isEmpty() && usersRegistration == null) {
            listenForAllUsers();
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_participant, null);
        RecyclerView usersRecycler = dialogView.findViewById(R.id.addParticipantUsersRecycler);
        addParticipantEmptyView = dialogView.findViewById(R.id.addParticipantEmptyView);
        TextInputEditText searchInput = dialogView.findViewById(R.id.addParticipantSearchInput);

        usersRecycler.setLayoutManager(new LinearLayoutManager(this));
        addParticipantAdapter = new UserSelectionAdapter(this, user -> {
            addUserToConversation(user);
            if (addParticipantDialog != null) {
                addParticipantDialog.dismiss();
            }
        });
        usersRecycler.setAdapter(addParticipantAdapter);

        addParticipantQuery = "";
        applyAddParticipantFilter("");

        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    addParticipantQuery = s != null ? s.toString() : "";
                    applyAddParticipantFilter(addParticipantQuery);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        addParticipantDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.chat_add_participant)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();

        addParticipantDialog.setOnDismissListener(dialog -> {
            addParticipantAdapter = null;
            addParticipantEmptyView = null;
            addParticipantQuery = "";
            addParticipantDialog = null;
        });

        addParticipantDialog.show();

        if (addParticipantDialog.getWindow() != null) {
            addParticipantDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void applyAddParticipantFilter(String query) {
        if (addParticipantAdapter == null) {
            return;
        }
        List<User> filtered = new ArrayList<>();
        String lowerQuery = query != null ? query.toLowerCase() : "";
        for (User user : allUsers) {
            String email = user.getEmail() != null ? user.getEmail() : "";
            if (email.isEmpty()) {
                continue;
            }
            if (currentUserId != null && currentUserId.equals(email)) {
                continue;
            }
            if (participantIdSet.contains(email)) {
                continue;
            }
            String name = user.getName() != null ? user.getName() : "";
            if (lowerQuery.isEmpty()
                    || name.toLowerCase().contains(lowerQuery)
                    || email.toLowerCase().contains(lowerQuery)) {
                filtered.add(user);
            }
        }
        addParticipantAdapter.setUsers(filtered);
        if (addParticipantEmptyView != null) {
            addParticipantEmptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void addUserToConversation(User user) {
        if (conversationRef == null) {
            return;
        }
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, R.string.chat_add_participant_failure, Toast.LENGTH_SHORT).show();
            return;
        }
        if (participantIdSet.contains(email)) {
            Toast.makeText(this, R.string.chat_add_participant_already_in, Toast.LENGTH_SHORT).show();
            return;
        }

        conversationRef.update("participants", FieldValue.arrayUnion(email))
                .addOnSuccessListener(unused -> {
                    String displayName = user.getName() != null && !user.getName().isEmpty() ? user.getName() : email;
                    Toast.makeText(this, getString(R.string.chat_add_participant_success, displayName), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, R.string.chat_add_participant_failure, Toast.LENGTH_SHORT).show());
    }

    private void showParticipantsDialog() {
        List<String> displayNames = new ArrayList<>();
        for (String participantId : participantIds) {
            displayNames.add(getParticipantDisplayName(participantId));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.chat_participants_title)
                .setPositiveButton(android.R.string.ok, null);

        if (displayNames.isEmpty()) {
            builder.setMessage(R.string.chat_participants_empty);
        } else {
            CharSequence[] items = displayNames.toArray(new CharSequence[0]);
            builder.setItems(items, null);
        }

        builder.show();
    }

    private void confirmLeaveChat() {
        if (!hasAccess) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.chat_leave_title)
                .setMessage(R.string.chat_leave_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.chat_leave_confirm, (dialog, which) -> leaveChat())
                .show();
    }

    private void leaveChat() {
        if (conversationRef == null || currentUserId == null) {
            Toast.makeText(this, R.string.chat_leave_failure, Toast.LENGTH_SHORT).show();
            return;
        }
        isLeavingChat = true;
        updateAccessState(false, true);
        conversationRef.update("participants", FieldValue.arrayRemove(currentUserId),
                        "formerParticipants", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(unused -> {
                    isLeavingChat = false;
                    Toast.makeText(this, R.string.chat_leave_success, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.chat_leave_failure, Toast.LENGTH_SHORT).show();
                    boolean stillParticipant = participantIdSet.contains(currentUserId);
                    isLeavingChat = false;
                    boolean stillFormerParticipant = formerParticipantIdSet.contains(currentUserId);
                    updateAccessState(stillParticipant, !stillParticipant && stillFormerParticipant);
                });
    }

    private String getParticipantDisplayName(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }
        for (User user : allUsers) {
            if (email.equals(user.getEmail())) {
                String name = user.getName();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
                break;
            }
        }
        return email;
    }

    @Override
    protected void onDestroy() {
        if (conversationRegistration != null) {
            conversationRegistration.remove();
            conversationRegistration = null;
        }
        if (usersRegistration != null) {
            usersRegistration.remove();
            usersRegistration = null;
        }
        stopListeningForMessages();
        if (addParticipantDialog != null && addParticipantDialog.isShowing()) {
            addParticipantDialog.dismiss();
        }
        super.onDestroy();
    }
}
