package com.choicecrafter.students.ui.inbox;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.students.R;
import com.choicecrafter.students.adapters.NotificationAdapter;
import com.choicecrafter.students.models.Notification;
import com.choicecrafter.students.repositories.NotificationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class InboxFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inbox, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fetchNotifications();
        requireActivity().setTitle("Inbox");

        return view;
    }

    private void fetchNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userEmail = user.getEmail();
            if (userEmail != null) {
                Log.d("InboxFragment", "Fetching userId for email: " + userEmail);
                FirebaseFirestore.getInstance().collection("users")
                        .whereEqualTo("email", userEmail)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                String userId = queryDocumentSnapshots.getDocuments().get(0).getId();
                                Log.d("InboxFragment", "Found userId: " + userId + " for email: " + userEmail);
                                fetchNotificationsByUserId(userId);
                            } else {
                                Log.e("InboxFragment", "No user found with email: " + userEmail);
                            }
                        })
                        .addOnFailureListener(e -> Log.e("InboxFragment", "Error fetching userId by email", e));
            } else {
                Log.e("InboxFragment", "Authenticated user's email is null.");
            }
        } else {
            Log.e("InboxFragment", "No authenticated user found.");
        }
    }

    private void fetchNotificationsByUserId(String userId) {
        NotificationRepository repository = new NotificationRepository();
        repository.getNotificationsForUser(userId, new NotificationRepository.NotificationCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                notifications.sort((n1, n2) -> n2.getTimestamp().compareTo(n1.getTimestamp()));
                Log.d("InboxFragment", "Successfully fetched " + notifications.size() + " notifications.");
                for (Notification notification : notifications) {
                    Log.d("InboxFragment", "Notification: " + notification.getType() + ", CourseId: " + notification.getCourseId());
                }
                adapter = new NotificationAdapter(notifications);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("InboxFragment", "Error fetching notifications", e);
            }
        });
    }
}