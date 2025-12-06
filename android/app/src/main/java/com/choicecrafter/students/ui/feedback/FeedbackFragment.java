package com.choicecrafter.studentapp.ui.feedback;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.choicecrafter.studentapp.MainViewModel;
import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment that collects feedback from the user and uploads it as a JSON document
 * to Firebase Storage.
 */
public class FeedbackFragment extends Fragment {

    private static final String STORAGE_FOLDER = "user-feedback";

    private TextInputLayout subjectLayout;
    private TextInputLayout messageLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout nameLayout;
    private TextInputEditText subjectInput;
    private TextInputEditText messageInput;
    private TextInputEditText emailInput;
    private TextInputEditText nameInput;
    private MaterialButton submitButton;

    private FirebaseStorage storage;
    private MainViewModel mainViewModel;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = FirebaseStorage.getInstance();
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);
        bindViews(view);
        observeUserDetails();
        prefillFromFirebaseUser();
        submitButton.setOnClickListener(v -> submitFeedback());
        return view;
    }

    private void bindViews(@NonNull View view) {
        subjectLayout = view.findViewById(R.id.feedbackSubjectLayout);
        messageLayout = view.findViewById(R.id.feedbackMessageLayout);
        emailLayout = view.findViewById(R.id.feedbackEmailLayout);
        nameLayout = view.findViewById(R.id.feedbackNameLayout);
        subjectInput = view.findViewById(R.id.feedbackSubjectInput);
        messageInput = view.findViewById(R.id.feedbackMessageInput);
        emailInput = view.findViewById(R.id.feedbackEmailInput);
        nameInput = view.findViewById(R.id.feedbackNameInput);
        submitButton = view.findViewById(R.id.feedbackSubmitButton);
    }

    private void observeUserDetails() {
        if (mainViewModel != null) {
            mainViewModel.getUser().observe(getViewLifecycleOwner(), this::applyUserDetails);
        }
    }

    private void prefillFromFirebaseUser() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            if (TextUtils.isEmpty(getInputText(nameInput))) {
                String displayName = firebaseUser.getDisplayName();
                if (!TextUtils.isEmpty(displayName)) {
                    nameInput.setText(displayName);
                }
            }
            if (TextUtils.isEmpty(getInputText(emailInput))) {
                String email = firebaseUser.getEmail();
                if (!TextUtils.isEmpty(email)) {
                    emailInput.setText(email);
                }
            }
        }
    }

    private void applyUserDetails(@Nullable User user) {
        if (user == null) {
            return;
        }
        if (!TextUtils.isEmpty(user.getName()) && TextUtils.isEmpty(getInputText(nameInput))) {
            nameInput.setText(user.getName());
        }
        if (!TextUtils.isEmpty(user.getEmail()) && TextUtils.isEmpty(getInputText(emailInput))) {
            emailInput.setText(user.getEmail());
        }
    }

    private void submitFeedback() {
        clearErrors();

        String subject = getInputText(subjectInput);
        String message = getInputText(messageInput);
        String email = getInputText(emailInput);
        String name = getInputText(nameInput);

        boolean hasError = false;
        if (TextUtils.isEmpty(subject)) {
            subjectLayout.setError(getString(R.string.feedback_subject_error));
            hasError = true;
        }
        if (TextUtils.isEmpty(message)) {
            messageLayout.setError(getString(R.string.feedback_message_error));
            hasError = true;
        }
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.feedback_email_error));
            hasError = true;
        }

        if (hasError) {
            return;
        }

        setLoading(true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("subject", subject);
        payload.put("message", message);
        payload.put("email", email);
        if (!TextUtils.isEmpty(name)) {
            payload.put("name", name);
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && !TextUtils.isEmpty(firebaseUser.getUid())) {
            payload.put("uid", firebaseUser.getUid());
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZ", Locale.US).format(new Date());
        payload.put("submittedAt", timestamp);

        String jsonPayload = gson.toJson(payload);
        String sanitizedEmail = email.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (TextUtils.isEmpty(sanitizedEmail)) {
            sanitizedEmail = "anonymous";
        }
        String fileName = timestamp + "_" + sanitizedEmail + ".json";

        StorageReference reference = storage.getReference()
                .child(STORAGE_FOLDER)
                .child(fileName);

        UploadTask uploadTask = reference.putBytes(jsonPayload.getBytes(StandardCharsets.UTF_8));
        uploadTask.addOnSuccessListener(taskSnapshot -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(), R.string.feedback_submission_success, Toast.LENGTH_LONG).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(), R.string.feedback_submission_error, Toast.LENGTH_LONG).show();
                })
                .addOnCompleteListener(task -> {
                    if (isAdded()) {
                        setLoading(false);
                    }
                });
    }

    private void clearErrors() {
        subjectLayout.setError(null);
        messageLayout.setError(null);
        emailLayout.setError(null);
        nameLayout.setError(null);
    }

    private void clearForm() {
        subjectInput.setText("");
        messageInput.setText("");
    }

    private void setLoading(boolean loading) {
        submitButton.setEnabled(!loading);
    }

    private static String getInputText(@Nullable TextInputEditText input) {
        if (input == null || input.getText() == null) {
            return "";
        }
        return input.getText().toString().trim();
    }
}
