package com.choicecrafter.studentapp.ui.messages;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.adapters.ConnectedUsersAdapter;
import com.choicecrafter.studentapp.adapters.ConversationAdapter;
import com.choicecrafter.studentapp.adapters.UserSelectionAdapter;
import com.choicecrafter.studentapp.databinding.FragmentMessagesBinding;
import com.choicecrafter.studentapp.models.Conversation;
import com.choicecrafter.studentapp.models.User;
import com.choicecrafter.studentapp.utils.ConversationTitleHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment implements ConversationAdapter.OnConversationClickListener {

    private FragmentMessagesBinding binding;
    private ConnectedUsersAdapter usersAdapter;
    private ConversationAdapter conversationAdapter;
    private final List<User> users = new ArrayList<>();
    private final List<Conversation> conversations = new ArrayList<>();
    private final List<User> allUsers = new ArrayList<>();
    private final Map<String, Conversation> activeConversations = new HashMap<>();
    private final Map<String, Conversation> readOnlyConversations = new HashMap<>();
    private FirebaseFirestore db;
    private String currentUserId;
    private UserSelectionAdapter newChatAdapter;
    private TextView newChatEmptyView;
    private String currentSearchQuery = "";
    private AlertDialog newChatDialog;
    private ListenerRegistration activeConversationsRegistration;
    private ListenerRegistration readOnlyConversationsRegistration;
    private ListenerRegistration onlineUsersRegistration;
    private ListenerRegistration allUsersRegistration;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        requireActivity().setTitle("Messages");

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : null;

        usersAdapter = new ConnectedUsersAdapter(getContext());
        binding.connectedUsersRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.connectedUsersRecycler.setAdapter(usersAdapter);

        conversationAdapter = new ConversationAdapter(getContext(), this);
        binding.conversationRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.conversationRecycler.setAdapter(conversationAdapter);

        binding.newChatFab.setOnClickListener(v -> showNewChatDialog());

        //sendDummyData();
        listenForUsers();
        listenForAllUsers();
        listenForConversations();
        return root;
    }

    private void showNewChatDialog() {
        if (getContext() == null) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_new_chat, null);
        RecyclerView usersRecycler = dialogView.findViewById(R.id.newChatUsersRecycler);
        newChatEmptyView = dialogView.findViewById(R.id.newChatEmptyView);
        EditText searchInput = dialogView.findViewById(R.id.newChatSearchInput);

        usersRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        newChatAdapter = new UserSelectionAdapter(getContext(), user -> {
            startConversationWithUser(user);
            if (newChatDialog != null) {
                newChatDialog.dismiss();
            }
        });
        usersRecycler.setAdapter(newChatAdapter);

        currentSearchQuery = "";
        applyUserFilter("");

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString() : "";
                applyUserFilter(currentSearchQuery);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        newChatDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.new_chat)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        newChatDialog.setOnDismissListener(dialog -> {
            newChatAdapter = null;
            newChatEmptyView = null;
            currentSearchQuery = "";
            newChatDialog = null;
        });
        newChatDialog.show();

        if (newChatDialog.getWindow() != null) {
            newChatDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void listenForUsers() {
        Log.i("MessagesFragment", "listenForUsers called");
        if (onlineUsersRegistration != null) {
            onlineUsersRegistration.remove();
        }
        onlineUsersRegistration = db.collection("users").whereEqualTo("online", true)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        Log.i("MessagesFragment", "Received user snapshots: " + value.size());
                        users.clear();
                        for (DocumentSnapshot snapshot : value.getDocuments()) {
                            Log.i("MessagesFragment", "Received user: " + snapshot);
                            try {
                                Log.i("MessagesFragment", "User data: " + snapshot.getData());
                                User user = snapshot.toObject(User.class);
                                if (user != null) {
                                    users.add(user);
                                }
                            } catch (Exception e) {
                                Log.e("MessagesFragment", "Error getting user data", e);
                            }
                        }
                        usersAdapter.setUsers(users);
                        if (conversationAdapter != null) {
                            conversationAdapter.addOrUpdateUserAvatars(users);
                        }
                    }
                });
    }

    private void listenForAllUsers() {
        if (allUsersRegistration != null) {
            allUsersRegistration.remove();
        }
        allUsersRegistration = db.collection("users")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        allUsers.clear();
                        for (DocumentSnapshot snapshot : value.getDocuments()) {
                            User user = snapshot.toObject(User.class);
                            if (user != null && (currentUserId == null || user.getEmail() == null || !currentUserId.equals(user.getEmail()))) {
                                allUsers.add(user);
                            }
                        }
                        if (conversationAdapter != null) {
                            conversationAdapter.setUserAvatars(allUsers);
                            conversationAdapter.addOrUpdateUserAvatars(users);
                        }
                        mergeConversations();
                        applyUserFilter(currentSearchQuery);
                    }
                });
    }

    private void listenForConversations() {
        Log.i("MessagesFragment", "listenForConversations called");
        stopConversationListeners();
        if (currentUserId == null) {
            Log.w("MessagesFragment", "Current user is null, clearing conversations");
            conversations.clear();
            conversationAdapter.setConversations(conversations);
            return;
        }

        activeConversationsRegistration = db.collection("conversations")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((value, error) -> handleConversationSnapshot(value, error, activeConversations, false));

        readOnlyConversationsRegistration = db.collection("conversations")
                .whereArrayContains("formerParticipants", currentUserId)
                .addSnapshotListener((value, error) -> handleConversationSnapshot(value, error, readOnlyConversations, true));
    }

    private void handleConversationSnapshot(QuerySnapshot value, FirebaseFirestoreException error,
                                            Map<String, Conversation> target, boolean readOnly) {
        if (error != null) {
            Log.e("MessagesFragment", "Error listening for conversations", error);
            return;
        }
        target.clear();
        if (value != null) {
            for (DocumentSnapshot snapshot : value.getDocuments()) {
                Conversation conversation = snapshot.toObject(Conversation.class);
                if (conversation == null) {
                    continue;
                }
                conversation.setId(snapshot.getId());
                boolean isParticipant = isCurrentParticipant(conversation);
                conversation.setReadOnly(readOnly || !isParticipant);
                conversation.setUnread(isConversationUnreadForCurrentUser(conversation));
                applyDisplayTitle(conversation);
                target.put(conversation.getId(), conversation);
            }
        }
        mergeConversations();
    }

    private boolean isCurrentParticipant(Conversation conversation) {
        if (currentUserId == null || conversation.getParticipants() == null) {
            return false;
        }
        return conversation.getParticipants().contains(currentUserId);
    }

    private boolean isConversationUnreadForCurrentUser(Conversation conversation) {
        if (currentUserId == null) {
            return false;
        }
        if (conversation.getUnreadBy() != null && conversation.getUnreadBy().contains(currentUserId)) {
            return true;
        }
        return conversation.isUnread();
    }

    private void mergeConversations() {
        Map<String, Conversation> merged = new HashMap<>(readOnlyConversations);
        merged.putAll(activeConversations);

        conversations.clear();
        for (Conversation conversation : merged.values()) {
            applyDisplayTitle(conversation);
            conversations.add(conversation);
        }
        conversations.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        conversationAdapter.setConversations(conversations);
    }

    private void applyDisplayTitle(Conversation conversation) {
        if (conversation == null) {
            return;
        }
        String storedTitle = conversation.getTitle();
        if (!TextUtils.isEmpty(storedTitle)) {
            return;
        }
        String dynamicTitle = ConversationTitleHelper.buildParticipantTitle(
                conversation.getParticipants(),
                conversation.getFormerParticipants(),
                currentUserId,
                allUsers);
        if (TextUtils.isEmpty(dynamicTitle)) {
            dynamicTitle = getString(R.string.app_name);
        }
        conversation.setTitle(dynamicTitle);
    }

    private void stopConversationListeners() {
        if (activeConversationsRegistration != null) {
            activeConversationsRegistration.remove();
            activeConversationsRegistration = null;
        }
        if (readOnlyConversationsRegistration != null) {
            readOnlyConversationsRegistration.remove();
            readOnlyConversationsRegistration = null;
        }
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        openChat(conversation.getId(), conversation.getTitle(), conversation.isReadOnly());
    }

    private void openChat(String conversationId, String title, boolean readOnly) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("conversationId", conversationId);
        intent.putExtra("title", title);
        intent.putExtra("readOnly", readOnly);
        startActivity(intent);
    }

    private void applyUserFilter(String query) {
        if (newChatAdapter == null) {
            return;
        }
        List<User> filtered = new ArrayList<>();
        String lowerQuery = query != null ? query.toLowerCase() : "";
        for (User user : allUsers) {
            String name = user.getName() != null ? user.getName().toLowerCase() : "";
            String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
            if (lowerQuery.isEmpty() || name.contains(lowerQuery) || email.contains(lowerQuery)) {
                filtered.add(user);
            }
        }
        newChatAdapter.setUsers(filtered);
        if (newChatEmptyView != null) {
            newChatEmptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        if (newChatDialog != null && newChatDialog.isShowing()) {
            newChatDialog.dismiss();
            newChatDialog = null;
        }
        stopUserListeners();
        stopConversationListeners();
        super.onDestroyView();
        binding = null;
    }

    private void stopUserListeners() {
        if (onlineUsersRegistration != null) {
            onlineUsersRegistration.remove();
            onlineUsersRegistration = null;
        }
        if (allUsersRegistration != null) {
            allUsersRegistration.remove();
            allUsersRegistration = null;
        }
    }

    private void startConversationWithUser(User user) {
        if (db == null) {
            return;
        }
        if (currentUserId == null || user.getEmail() == null) {
            Toast.makeText(getContext(), getString(R.string.new_chat_error_start), Toast.LENGTH_SHORT).show();
            return;
        }

        String otherUserId = user.getEmail();
        String conversationId = generateConversationId(currentUserId, otherUserId);
        String chatTitle = user.getName() != null && !user.getName().isEmpty() ? user.getName() : otherUserId;
        DocumentReference conversationRef = db.collection("conversations").document(conversationId);
        conversationRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Conversation existingConversation = documentSnapshot.toObject(Conversation.class);
                String initialTitle = chatTitle;
                if (existingConversation != null) {
                    String storedTitle = existingConversation.getTitle();
                    if (!TextUtils.isEmpty(storedTitle)) {
                        initialTitle = storedTitle;
                    } else {
                        String resolvedTitle = ConversationTitleHelper.buildParticipantTitle(
                                existingConversation.getParticipants(),
                                existingConversation.getFormerParticipants(),
                                currentUserId,
                                allUsers);
                        if (!TextUtils.isEmpty(resolvedTitle)) {
                            initialTitle = resolvedTitle;
                        }
                    }
                }
                openChat(conversationId, initialTitle, false);
            } else {
                Map<String, Object> conversationData = new HashMap<>();
                conversationData.put("title", "");
                conversationData.put("lastMessage", "");
                conversationData.put("timestamp", System.currentTimeMillis());
                conversationData.put("unread", false);
                conversationData.put("participants", Arrays.asList(currentUserId, otherUserId));
                conversationData.put("formerParticipants", new ArrayList<String>());
                conversationData.put("unreadBy", new ArrayList<String>());
                conversationData.put("lastMessageSenderId", null);
                conversationRef.set(conversationData)
                        .addOnSuccessListener(unused -> openChat(conversationId, chatTitle, false))
                        .addOnFailureListener(e -> Toast.makeText(getContext(), getString(R.string.new_chat_error_create), Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e -> Toast.makeText(getContext(), getString(R.string.new_chat_error_load), Toast.LENGTH_SHORT).show());
    }

    private String generateConversationId(String firstUser, String secondUser) {
        if (firstUser.compareToIgnoreCase(secondUser) < 0) {
            return firstUser + "_" + secondUser;
        } else {
            return secondUser + "_" + firstUser;
        }
    }

    // Call this method to populate Firestore with dummy chat data for testing
    private void sendDummyData() {
        Map<String, Object> alice = new HashMap<>();
        alice.put("name", "Alice");
        alice.put("profileUrl", "");
        alice.put("online", true);
        db.collection("chatUsers").document("alice").set(alice);

        Map<String, Object> bob = new HashMap<>();
        bob.put("name", "Bob");
        bob.put("profileUrl", "");
        bob.put("online", true);
        db.collection("chatUsers").document("bob").set(bob);

        Map<String, Object> conversation = new HashMap<>();
        conversation.put("title", "Alice and Bob");
        conversation.put("lastMessage", "Hey Bob!");
        conversation.put("timestamp", new Date().getTime());
        conversation.put("unread", true);
        conversation.put("participants", Arrays.asList("alice", "bob"));
        db.collection("conversations").document("conversation1").set(conversation);

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", "alice");
        message.put("text", "Hey Bob!");
        message.put("timestamp", new Date().getTime());
        db.collection("conversations").document("conversation1").collection("messages").add(message);
    }
}
