package com.choicecrafter.students.ui.auth;

import android.text.method.PasswordTransformationMethod;

import androidx.annotation.Nullable;

import com.choicecrafter.students.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Utility class to ensure password end icons correctly reflect the visibility state across
 * authentication screens.
 */
final class PasswordVisibilityToggleHelper {

    private PasswordVisibilityToggleHelper() {
        // Utility class.
    }

    static void apply(@Nullable TextInputLayout inputLayout, @Nullable TextInputEditText editText) {
        if (inputLayout == null || editText == null) {
            return;
        }

        inputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        inputLayout.setEndIconDrawable(R.drawable.ic_visibility_off);
        inputLayout.setEndIconContentDescription(
                inputLayout.getContext().getString(R.string.content_description_password_hidden));
        inputLayout.setEndIconOnClickListener(v -> {
            boolean isPasswordHidden = editText.getTransformationMethod() instanceof PasswordTransformationMethod;
            if (isPasswordHidden) {
                editText.setTransformationMethod(null);
                inputLayout.setEndIconDrawable(R.drawable.ic_visibility);
                inputLayout.setEndIconContentDescription(
                        inputLayout.getContext().getString(R.string.content_description_password_visible));
            } else {
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                inputLayout.setEndIconDrawable(R.drawable.ic_visibility_off);
                inputLayout.setEndIconContentDescription(
                        inputLayout.getContext().getString(R.string.content_description_password_hidden));
            }

            if (editText.getText() != null) {
                editText.setSelection(editText.getText().length());
            }
        });
    }
}
