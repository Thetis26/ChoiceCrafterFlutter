package com.choicecrafter.students.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.choicecrafter.students.R;
import com.choicecrafter.students.models.User;
import com.choicecrafter.students.repositories.CourseEnrollmentRepository;
import com.choicecrafter.students.repositories.NudgePreferencesRepository;
import com.choicecrafter.students.repositories.UserCourseAvailabilityRepository;
import com.choicecrafter.students.utils.AnonymousAvatars;
import com.choicecrafter.students.utils.Avatar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputLayout nameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout registrationCodeInputLayout;
    private TextInputEditText nameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText registrationCodeEditText;
    private Button registerButton;
    private final CourseEnrollmentRepository courseEnrollmentRepository = new CourseEnrollmentRepository();
    private final UserCourseAvailabilityRepository userCourseAvailabilityRepository = new UserCourseAvailabilityRepository();
    private static final String DEFAULT_ENROLLMENT_COURSE_ID = "FfwkHveow9h8bpwjdEQl";
    private static final String DEFAULT_AVATAR_URL = "https://example.com/default_avatar.png";
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int REGISTRATION_CODE_LENGTH = 6;
    private static final String REQUIRED_REGISTRATION_CODE = "274973";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        nameInputLayout = findViewById(R.id.nameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        registrationCodeInputLayout = findViewById(R.id.registrationCodeInputLayout);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registrationCodeEditText = findViewById(R.id.registrationCodeEditText);
        registerButton = findViewById(R.id.registerButton);
        CustomTextView loginTextView = findViewById(R.id.toLogin);

        PasswordVisibilityToggleHelper.apply(passwordInputLayout, passwordEditText);

        setupInputValidation();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
                String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
                String name = nameEditText.getText() != null ? nameEditText.getText().toString().trim() : "";
                String registrationCode = registrationCodeEditText.getText() != null ? registrationCodeEditText.getText().toString().trim() : "";

                if (!validateName(name) | !validateEmail(email) | !validatePassword(password) | !validateRegistrationCode(registrationCode)) {
                    return;
                }

                View termsView = LayoutInflater.from(RegisterActivity.this)
                        .inflate(R.layout.dialog_terms_privacy, null, false);

                AlertDialog termsDialog = new AlertDialog.Builder(RegisterActivity.this)
                        .setView(termsView)
                        .create();

                MaterialButton agreeButton = termsView.findViewById(R.id.termsAgreeButton);
                MaterialButton cancelButton = termsView.findViewById(R.id.termsCancelButton);

                agreeButton.setOnClickListener(view1 -> {
                    termsDialog.dismiss();
                    registerUser(name, email, password);
                });

                cancelButton.setOnClickListener(view12 -> termsDialog.dismiss());

                termsDialog.show();
            }
        });
        loginTextView.setOnClickListener(v -> Log.i("RegisterActivity", "Login link clicked"));
    }

    private void registerUser(String name, String email, String password) {
        registerButton.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    registerButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        Toast.makeText(RegisterActivity.this, R.string.registration_successful, Toast.LENGTH_SHORT).show();
                        if (firebaseUser != null) {
                            // Save additional user data to Firebase Realtime Database
                            saveUserToDatabase(name, email);
                        }
                    } else {
                        Log.w("RegisterActivity", "User registration failed", task.getException());
                        Toast.makeText(RegisterActivity.this, R.string.registration_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(String name, String email) {
        AnonymousAvatars anonymousAvatars = new AnonymousAvatars();
        anonymousAvatars.getAllAvatars(result -> {
            Avatar selectedAvatar = selectRandomAvatar(result);
            Log.i("RegisterActivity", "Anonymous avatar selected: " + selectedAvatar.getName());

            User user = new User(name, email, selectedAvatar, new HashMap<>());

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .add(user)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, R.string.registration_successful, Toast.LENGTH_SHORT).show();
                            createDefaultNudgePreferences(email);
                            createDefaultAvailableCourses(email);
                        } else {
                            Toast.makeText(RegisterActivity.this, R.string.registration_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void createDefaultNudgePreferences(String email) {
        if (email == null || email.isEmpty()) {
            return;
        }
        new NudgePreferencesRepository().createDefaultPreferences(email)
                .addOnFailureListener(e -> Log.w("RegisterActivity", "Failed to create default nudge preferences", e));
    }

    private void createDefaultAvailableCourses(String email) {
        if (email == null || email.isEmpty()) {
            enrollNewUserInDefaultCourse(email);
            return;
        }
        userCourseAvailabilityRepository.ensureDefaultCourseAccess(email, new UserCourseAvailabilityRepository.Callback<>() {
            @Override
            public void onSuccess(Void unused) {
                enrollNewUserInDefaultCourse(email);
            }

            @Override
            public void onFailure(Exception e) {
                Log.w("RegisterActivity", "Failed to initialize course availability for user " + email, e);
                enrollNewUserInDefaultCourse(email);
            }
        });
    }

    private Avatar selectRandomAvatar(List<Avatar> avatars) {
        if (avatars != null && !avatars.isEmpty()) {
            int randomIndex = new Random().nextInt(avatars.size());
            return avatars.get(randomIndex);
        }
        Log.w("RegisterActivity", "No avatars available. Using default avatar.");
        return new Avatar("Anonymous", DEFAULT_AVATAR_URL);
    }

    private void enrollNewUserInDefaultCourse(String userId) {
        if (userId == null || userId.isEmpty()) {
            updateUI(mAuth.getCurrentUser());
            return;
        }

        courseEnrollmentRepository.enrollUserInCourse(userId, DEFAULT_ENROLLMENT_COURSE_ID, new CourseEnrollmentRepository.Callback<>() {
            @Override
            public void onSuccess(Void unused) {
                Log.i("RegisterActivity", "User " + userId + " auto-enrolled in course " + DEFAULT_ENROLLMENT_COURSE_ID);
                updateUI(mAuth.getCurrentUser());
            }

            @Override
            public void onFailure(Exception e) {
                Log.w("RegisterActivity", "Failed to auto-enroll user " + userId + " in course " + DEFAULT_ENROLLMENT_COURSE_ID, e);
                Toast.makeText(RegisterActivity.this, R.string.auto_enroll_welcome_course_failed, Toast.LENGTH_SHORT).show();
                updateUI(mAuth.getCurrentUser());
            }
        });
    }

    private void setupInputValidation() {
        registerButton.setEnabled(false);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRegisterButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        nameEditText.addTextChangedListener(watcher);
        emailEditText.addTextChangedListener(watcher);
        passwordEditText.addTextChangedListener(watcher);
        registrationCodeEditText.addTextChangedListener(watcher);

        updateRegisterButtonState();
    }

    private void updateRegisterButtonState() {
        String name = nameEditText.getText() != null ? nameEditText.getText().toString().trim() : "";
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        String registrationCode = registrationCodeEditText.getText() != null ? registrationCodeEditText.getText().toString().trim() : "";

        boolean isNameValid = !TextUtils.isEmpty(name);
        boolean isEmailValid = !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
        boolean isPasswordValid = !TextUtils.isEmpty(password) && password.length() >= MIN_PASSWORD_LENGTH;
        boolean hasRequiredLength = registrationCode.length() == REGISTRATION_CODE_LENGTH;
        boolean isRegistrationCodeValid = hasRequiredLength && TextUtils.equals(registrationCode, REQUIRED_REGISTRATION_CODE);

        if (isNameValid || TextUtils.isEmpty(name)) {
            nameInputLayout.setError(null);
        }

        if (isEmailValid || TextUtils.isEmpty(email)) {
            emailInputLayout.setError(null);
        }

        if (isPasswordValid || TextUtils.isEmpty(password)) {
            passwordInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(registrationCode)) {
            registrationCodeInputLayout.setError(null);
        } else if (!hasRequiredLength) {
            registrationCodeInputLayout.setError(null);
        } else if (!isRegistrationCodeValid) {
            registrationCodeInputLayout.setError(getString(R.string.error_registration_code_invalid));
        } else {
            registrationCodeInputLayout.setError(null);
        }

        registerButton.setEnabled(isNameValid && isEmailValid && isPasswordValid && isRegistrationCodeValid);
    }

    private boolean validateName(String name) {
        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError(getString(R.string.error_name_required));
            return false;
        }
        nameInputLayout.setError(null);
        return true;
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.error_email_required));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.error_email_invalid));
            return false;
        }
        emailInputLayout.setError(null);
        return true;
    }

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError(getString(R.string.error_password_required));
            return false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            passwordInputLayout.setError(getString(R.string.error_password_length, MIN_PASSWORD_LENGTH));
            return false;
        }
        passwordInputLayout.setError(null);
        return true;
    }

    private boolean validateRegistrationCode(String registrationCode) {
        if (TextUtils.isEmpty(registrationCode)) {
            registrationCodeInputLayout.setError(getString(R.string.error_registration_code_required));
            return false;
        } else if (!TextUtils.equals(registrationCode, REQUIRED_REGISTRATION_CODE)) {
            registrationCodeInputLayout.setError(getString(R.string.error_registration_code_invalid));
            Toast.makeText(this, R.string.error_registration_code_invalid, Toast.LENGTH_SHORT).show();
            return false;
        }
        registrationCodeInputLayout.setError(null);
        return true;
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            startActivity(new Intent(RegisterActivity.this, LoadingActivity.class));
            finish();
        }
    }
}