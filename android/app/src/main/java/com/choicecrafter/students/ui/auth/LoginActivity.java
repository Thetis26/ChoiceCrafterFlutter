package com.choicecrafter.studentapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.choicecrafter.studentapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private boolean isLoginInProgress = false;

    private static final int MIN_PASSWORD_LENGTH = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailEditText = findViewById(R.id.emailEditTextLogin);
        passwordEditText = findViewById(R.id.passwordEditTextLogin);
        loginButton = findViewById(R.id.loginButton);
        CustomTextView registerButton = findViewById(R.id.toRegistration);

        PasswordVisibilityToggleHelper.apply(passwordInputLayout, passwordEditText);

        setupInputValidation();

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
            String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
            loginUser(email, password);
        });


        registerButton.setOnClickListener(v -> Log.i("LoginActivity", "Register link clicked"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        }
    }

    private void loginUser(String email, String password) {
        if (isLoginInProgress) {
            return;
        }

        if (!validateEmail(email) | !validatePassword(password)) {
            return;
        }

        isLoginInProgress = true;
        loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        isLoginInProgress = false;
                        loginButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            startActivity(new Intent(LoginActivity.this, LoadingActivity.class));
            finish();
        }
    }

    private void setupInputValidation() {
        loginButton.setEnabled(false);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkLoginButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op
            }
        };

        emailEditText.addTextChangedListener(watcher);
        passwordEditText.addTextChangedListener(watcher);

        checkLoginButtonState();
    }

    private void checkLoginButtonState() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
        boolean isEmailValid = isEmailFormatValid(email);
        boolean isPasswordValid = isPasswordLengthValid(password);

        if (isEmailValid || TextUtils.isEmpty(email)) {
            emailInputLayout.setError(null);
        }

        if (isPasswordValid || TextUtils.isEmpty(password)) {
            passwordInputLayout.setError(null);
        }

        loginButton.setEnabled(isEmailValid && isPasswordValid && !isLoginInProgress);
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            emailInputLayout.setError(getString(R.string.error_email_required));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.error_email_invalid));
            return false;
        } else {
            emailInputLayout.setError(null);
            return true;
        }
    }

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError(getString(R.string.error_password_required));
            return false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            passwordInputLayout.setError(getString(R.string.error_password_length, MIN_PASSWORD_LENGTH));
            return false;
        } else {
            passwordInputLayout.setError(null);
            return true;
        }
    }

    private boolean isEmailFormatValid(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordLengthValid(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= MIN_PASSWORD_LENGTH;
    }
}
