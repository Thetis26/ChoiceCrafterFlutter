package com.choicecrafter.students.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.choicecrafter.students.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Utility class that displays a lightweight dialog for AI hints and answers.
 */
public final class HintDialogUtil {

    private HintDialogUtil() {
        // Utility class
    }

    public static void showHintDialog(@NonNull Context context,
                                      @NonNull String title,
                                      @NonNull String message) {
        showHintDialog(context, title, message, R.drawable.lightbulb_on, null, null);
    }

    public static void showHintDialog(@NonNull Context context,
                                      @NonNull String title,
                                      @NonNull String message,
                                      @DrawableRes int iconRes) {
        showHintDialog(context, title, message, iconRes, null, null);
    }

    public static void showHintDialog(@NonNull Context context,
                                      @NonNull String title,
                                      @NonNull String message,
                                      @DrawableRes int iconRes,
                                      @Nullable Runnable onRequestAnotherHint,
                                      @Nullable Runnable onShowAnswer) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_hint, null, false);
        TextView titleView = dialogView.findViewById(R.id.hint_dialog_title);
        TextView messageView = dialogView.findViewById(R.id.hint_dialog_message);
        ImageView iconView = dialogView.findViewById(R.id.hint_dialog_icon);

        titleView.setText(title);
        messageView.setText(message);

        if (iconRes != 0) {
            iconView.setImageResource(iconRes);
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setPositiveButton(R.string.hint_dialog_got_it, (dialogInterface, which) -> dialogInterface.dismiss());

        if (onRequestAnotherHint != null) {
            builder.setNegativeButton(R.string.hint_dialog_need_another_hint,
                    (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                        onRequestAnotherHint.run();
                    });
        }

        if (onShowAnswer != null) {
            builder.setNeutralButton(R.string.hint_dialog_show_answer,
                    (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                        onShowAnswer.run();
                    });
        }

        Dialog dialog = builder.create();
        dialog.show();
    }
}
