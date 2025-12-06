package com.choicecrafter.studentapp.adapters.tasks;

import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.tasks.TrueFalseTask;
import com.choicecrafter.studentapp.utils.AiHintService;
import com.choicecrafter.studentapp.utils.HintDialogUtil;

public class TrueFalseViewHolder extends RecyclerView.ViewHolder {

    private final TextView statementView;
    private final TextView descriptionView;
    private final TextView instructionsView;
    private final RadioGroup optionsGroup;
    private final RadioButton trueOption;
    private final RadioButton falseOption;
    private final AiHintService aiHintService;

    private TrueFalseTask currentTask;
    private boolean aiHintShown;
    private boolean hintInProgress;

    public TrueFalseViewHolder(View itemView) {
        super(itemView);
        statementView = itemView.findViewById(R.id.true_false_statement);
        descriptionView = itemView.findViewById(R.id.task_description);
        instructionsView = itemView.findViewById(R.id.true_false_instructions);
        optionsGroup = itemView.findViewById(R.id.true_false_group);
        trueOption = itemView.findViewById(R.id.true_option);
        falseOption = itemView.findViewById(R.id.false_option);
        aiHintService = AiHintService.getInstance(itemView.getContext());
    }

    public void bind(TrueFalseTask task) {
        currentTask = task;
        String statement = !TextUtils.isEmpty(task.getStatement())
                ? task.getStatement()
                : itemView.getContext().getString(R.string.task_true_false_default_statement);
        statementView.setText(statement);
        if (descriptionView != null) {
            if (!TextUtils.isEmpty(task.getDescription())) {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(task.getDescription());
            } else {
                descriptionView.setVisibility(View.GONE);
            }
        }
        if (instructionsView != null) {
            instructionsView.setVisibility(View.VISIBLE);
        }
        optionsGroup.clearCheck();
        aiHintShown = false;
        hintInProgress = false;
    }

    public boolean validateAnswer() {
        int selectedId = optionsGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(itemView.getContext(), R.string.task_true_false_select_option, Toast.LENGTH_SHORT).show();
            return false;
        }

        boolean userAnswerIsTrue = selectedId == trueOption.getId();
        boolean isCorrect = userAnswerIsTrue == currentTask.isCorrectAnswer();

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
        builder.append("Provide a brief hint for the following true or false statement without revealing whether it is true or false.\n");
        builder.append("Statement: ").append(currentTask.getStatement()).append('\n');
        if (!TextUtils.isEmpty(currentTask.getDescription())) {
            builder.append("Context: ").append(currentTask.getDescription()).append('\n');
        }
        return builder.toString();
    }

    private String buildAnswerMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append(itemView.getContext().getString(R.string.task_true_false_correct_answer,
                itemView.getContext().getString(currentTask.isCorrectAnswer() ? R.string.task_true_option_label : R.string.task_false_option_label)));
        if (!TextUtils.isEmpty(currentTask.getExplanation())) {
            builder.append('\n').append(currentTask.getExplanation());
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
