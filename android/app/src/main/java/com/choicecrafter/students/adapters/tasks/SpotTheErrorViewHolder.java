package com.choicecrafter.studentapp.adapters.tasks;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.tasks.SpotTheErrorTask;
import com.choicecrafter.studentapp.utils.AiHintService;
import com.choicecrafter.studentapp.utils.HintDialogUtil;

public class SpotTheErrorViewHolder extends RecyclerView.ViewHolder {

    private final TextView promptView;
    private final TextView descriptionView;
    private final TextView snippetLabelView;
    private final TextView snippetView;
    private final RadioGroup optionsGroup;
    private SpotTheErrorTask currentTask;

    private final AiHintService aiHintService;
    private boolean aiHintShown;
    private boolean hintInProgress;

    public SpotTheErrorViewHolder(View itemView) {
        super(itemView);
        promptView = itemView.findViewById(R.id.spot_error_prompt);
        descriptionView = itemView.findViewById(R.id.task_description);
        snippetLabelView = itemView.findViewById(R.id.spot_error_snippet_label);
        snippetView = itemView.findViewById(R.id.spot_error_snippet);
        optionsGroup = itemView.findViewById(R.id.options_group);
        aiHintService = AiHintService.getInstance(itemView.getContext());
    }

    public void bind(SpotTheErrorTask task) {
        currentTask = task;
        promptView.setText(!TextUtils.isEmpty(task.getPrompt()) ? task.getPrompt() : itemView.getContext().getString(R.string.task_spot_error_default_prompt));
        if (descriptionView != null) {
            if (!TextUtils.isEmpty(task.getDescription())) {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(task.getDescription());
            } else {
                descriptionView.setVisibility(View.GONE);
            }
        }
        if (!TextUtils.isEmpty(task.getSnippet())) {
            snippetView.setText(task.getSnippet());
            snippetView.setVisibility(View.VISIBLE);
            if (snippetLabelView != null) {
                snippetLabelView.setVisibility(View.VISIBLE);
            }
        } else {
            snippetView.setVisibility(View.GONE);
            if (snippetLabelView != null) {
                snippetLabelView.setVisibility(View.GONE);
            }
        }
        optionsGroup.removeAllViews();
        if (task.getOptions() != null) {
            int textColor = ContextCompat.getColor(itemView.getContext(), R.color.text_primary);
            ColorStateList tintList = ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary));
            for (String option : task.getOptions()) {
                RadioButton optionButton = new RadioButton(itemView.getContext());
                optionButton.setText(option);
                optionButton.setTextColor(textColor);
                optionButton.setButtonTintList(tintList);
                RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                        RadioGroup.LayoutParams.MATCH_PARENT,
                        RadioGroup.LayoutParams.WRAP_CONTENT);
                optionButton.setLayoutParams(params);
                optionButton.setPadding(0, 12, 0, 12);
                optionsGroup.addView(optionButton);
            }
        }
        optionsGroup.clearCheck();
        aiHintShown = false;
        hintInProgress = false;
    }

    public boolean validateAnswer() {
        int selectedId = optionsGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(itemView.getContext(), R.string.task_spot_error_select_option, Toast.LENGTH_SHORT).show();
            return false;
        }
        RadioButton selected = itemView.findViewById(selectedId);
        int selectedIndex = optionsGroup.indexOfChild(selected);
        boolean isCorrect = selectedIndex == currentTask.getCorrectOptionIndex();
        if (!isCorrect) {
            Toast.makeText(itemView.getContext(), R.string.task_answer_incorrect, Toast.LENGTH_SHORT).show();
        }
        return isCorrect;
    }

    public boolean hasUsedHint() {
        return aiHintShown;
    }

    public void showHint() {
        if (currentTask == null) {
            return;
        }
        if (hintInProgress) {
            Toast.makeText(itemView.getContext(), R.string.ai_hint_generating, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!aiHintShown) {
            requestHintFromAi();
            return;
        }
        showFinalAnswerDialog();
    }

    private String buildPrompt() {
        StringBuilder builder = new StringBuilder();
        builder.append("Provide a hint that helps the learner identify the error in the scenario without revealing the correct option.\n");
        if (!TextUtils.isEmpty(currentTask.getPrompt())) {
            builder.append("Prompt: ").append(currentTask.getPrompt()).append('\n');
        }
        if (!TextUtils.isEmpty(currentTask.getSnippet())) {
            builder.append("Snippet: ").append(currentTask.getSnippet()).append('\n');
        }
        if (currentTask.getOptions() != null && !currentTask.getOptions().isEmpty()) {
            builder.append("Options:\n");
            for (int i = 0; i < currentTask.getOptions().size(); i++) {
                builder.append(i + 1).append(") ").append(currentTask.getOptions().get(i)).append('\n');
            }
        }
        if (!TextUtils.isEmpty(currentTask.getDescription())) {
            builder.append("Additional context: ").append(currentTask.getDescription());
        }
        return builder.toString();
    }

    private String buildAnswerMessage() {
        StringBuilder builder = new StringBuilder();
        if (currentTask.getOptions() != null && currentTask.getCorrectOptionIndex() >= 0 && currentTask.getCorrectOptionIndex() < currentTask.getOptions().size()) {
            String correct = currentTask.getOptions().get(currentTask.getCorrectOptionIndex());
            builder.append(itemView.getContext().getString(R.string.task_spot_error_correct_answer, correct));
        }
        if (!TextUtils.isEmpty(currentTask.getExplanation())) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(currentTask.getExplanation());
        }
        return builder.toString();
    }

    private void showTemporaryDialog(String title, String message, int iconRes) {
        HintDialogUtil.showHintDialog(itemView.getContext(), title, message, iconRes);
    }

    private void showHintDialogWithActions(String hint) {
        HintDialogUtil.showHintDialog(
                itemView.getContext(),
                itemView.getContext().getString(R.string.ai_hint_title),
                hint,
                R.drawable.lightbulb_on,
                this::handleRequestAnotherHint,
                this::showFinalAnswerDialog);
    }

    private void requestHintFromAi() {
        hintInProgress = true;
        Toast.makeText(itemView.getContext(), R.string.ai_hint_generating, Toast.LENGTH_SHORT).show();
        aiHintService.requestHint(buildPrompt(), new AiHintService.HintCallback() {
            @Override
            public void onSuccess(String hint) {
                hintInProgress = false;
                aiHintShown = true;
                showHintDialogWithActions(hint);
            }

            @Override
            public void onError(String errorMessage) {
                hintInProgress = false;
                Toast.makeText(itemView.getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleRequestAnotherHint() {
        if (hintInProgress) {
            Toast.makeText(itemView.getContext(), R.string.ai_hint_generating, Toast.LENGTH_SHORT).show();
            return;
        }
        requestHintFromAi();
    }

    private void showFinalAnswerDialog() {
        showTemporaryDialog(itemView.getContext().getString(R.string.ai_answer_title),
                buildAnswerMessage(), R.drawable.trophy);
    }
}
