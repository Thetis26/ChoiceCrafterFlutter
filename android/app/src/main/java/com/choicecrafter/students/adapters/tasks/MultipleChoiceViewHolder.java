package com.choicecrafter.studentapp.adapters.tasks;


import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.tasks.MultipleChoiceQuestion;
import com.choicecrafter.studentapp.models.tasks.SupportingContent;
import com.choicecrafter.studentapp.utils.AiHintService;
import com.choicecrafter.studentapp.utils.HintDialogUtil;

public class MultipleChoiceViewHolder extends RecyclerView.ViewHolder {

    public interface StateListener {
        void onStateChanged(int selectedIndex, boolean hintShown);
    }

    private final TextView questionTitle;
    private final RadioGroup optionsGroup;
    private final TextView descriptionView;
    private final TextView supportingTextView;
    private final ImageView supportingImageView;
    private MultipleChoiceQuestion currentTask;
    private final AiHintService aiHintService;
    private boolean aiHintShown;
    private boolean hintInProgress;
    private int partialCreditIndex;
    private double lastCompletionRatio;
    private StateListener stateListener;
    private boolean isRestoringState;

    public MultipleChoiceViewHolder(View itemView) {
        super(itemView);
        questionTitle = itemView.findViewById(R.id.question_title);
        descriptionView = itemView.findViewById(R.id.task_description);
        supportingTextView = itemView.findViewById(R.id.supporting_text);
        supportingImageView = itemView.findViewById(R.id.supporting_image);
        optionsGroup = itemView.findViewById(R.id.options_group);
        aiHintService = AiHintService.getInstance(itemView.getContext());
    }

    public void bind(MultipleChoiceQuestion task) {
        currentTask = task;
        questionTitle.setText(task.getQuestion());
        if (descriptionView != null) {
            if (!TextUtils.isEmpty(task.getDescription())) {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(task.getDescription());
            } else {
                descriptionView.setVisibility(View.GONE);
            }
        }
        Log.i("MCQ", "supporting text = " + task.getSupportingContent() );
        bindSupportingContent(task.getSupportingContent());
        optionsGroup.setOnCheckedChangeListener(null);
        optionsGroup.clearCheck();
        optionsGroup.removeAllViews();
        partialCreditIndex = -1;
        int textColor = ContextCompat.getColor(itemView.getContext(), R.color.text_primary);
        int tintColor = ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary);
        lastCompletionRatio = 0.0;
        for (String option : task.getOptions()) {
            RadioButton radioButton = new RadioButton(optionsGroup.getContext());
            radioButton.setText(option);
            radioButton.setTextColor(textColor);
            radioButton.setButtonTintList(ColorStateList.valueOf(tintColor));
            radioButton.setPadding(0, 12, 0, 12);
            radioButton.setId(View.generateViewId());
            optionsGroup.addView(radioButton);
        }
        RadioButton partialCreditButton = new RadioButton(optionsGroup.getContext());
        partialCreditButton.setText(itemView.getContext().getString(R.string.multiple_choice_option_i_dont_know));
        partialCreditButton.setTextColor(textColor);
        partialCreditButton.setButtonTintList(ColorStateList.valueOf(tintColor));
        partialCreditButton.setPadding(0, 12, 0, 12);
        partialCreditIndex = optionsGroup.getChildCount();
        partialCreditButton.setId(View.generateViewId());
        optionsGroup.addView(partialCreditButton);
        aiHintShown = false;
        hintInProgress = false;
        optionsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (isRestoringState) {
                return;
            }
            notifyStateChanged();
        });
    }

    private void bindSupportingContent(SupportingContent supportingContent) {
        if (supportingTextView != null) {
            if (supportingContent != null && !TextUtils.isEmpty(supportingContent.getText())) {
                supportingTextView.setVisibility(View.VISIBLE);
                supportingTextView.setText(supportingContent.getText());
            } else {
                supportingTextView.setVisibility(View.GONE);
            }
        }

        if (supportingImageView != null) {
            if (supportingContent != null && !TextUtils.isEmpty(supportingContent.getImageUrl())) {
                supportingImageView.setVisibility(View.VISIBLE);
                Glide.with(supportingImageView.getContext())
                        .load(supportingContent.getImageUrl())
                        .centerCrop()
                        .into(supportingImageView);
            } else {
                Glide.with(supportingImageView.getContext()).clear(supportingImageView);
                supportingImageView.setVisibility(View.GONE);
            }
        }
    }

    public boolean validateAnswer() {
        if (currentTask == null) {
            lastCompletionRatio = 0.0;
            return false;
        }
        int selectedId = optionsGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(optionsGroup.getContext(), "Please select an answer", Toast.LENGTH_SHORT).show();
            lastCompletionRatio = 0.0;
            return false;
        }

        RadioButton selectedRadioButton = itemView.findViewById(selectedId);
        int selectedAnswerIndex = optionsGroup.indexOfChild(selectedRadioButton);
        if (selectedAnswerIndex == partialCreditIndex) {
            lastCompletionRatio = 0.5;
            Toast.makeText(optionsGroup.getContext(),
                    itemView.getContext().getString(R.string.multiple_choice_partial_credit_toast),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        boolean isCorrect = selectedAnswerIndex == currentTask.getCorrectAnswer();

        if (isCorrect) {
            lastCompletionRatio = 1.0;
            return true;
        }

        Toast.makeText(optionsGroup.getContext(), R.string.task_answer_incorrect, Toast.LENGTH_SHORT).show();
        lastCompletionRatio = 0.0;
        return false;
    }

    public double getLastCompletionRatio() {
        return lastCompletionRatio;
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

    public void setStateListener(StateListener listener) {
        this.stateListener = listener;
    }

    public void restoreState(int selectedIndex, boolean hintShown) {
        isRestoringState = true;
        aiHintShown = hintShown;
        if (selectedIndex >= 0 && selectedIndex < optionsGroup.getChildCount()) {
            View view = optionsGroup.getChildAt(selectedIndex);
            if (view instanceof RadioButton radioButton) {
                optionsGroup.check(radioButton.getId());
            }
        } else {
            optionsGroup.clearCheck();
        }
        isRestoringState = false;
    }

    private void notifyStateChanged() {
        if (stateListener != null) {
            stateListener.onStateChanged(getSelectedIndex(), aiHintShown);
        }
    }

    private int getSelectedIndex() {
        int checkedId = optionsGroup.getCheckedRadioButtonId();
        if (checkedId == -1) {
            return -1;
        }
        View checkedView = itemView.findViewById(checkedId);
        return optionsGroup.indexOfChild(checkedView);
    }

    private String buildPrompt() {
        StringBuilder builder = new StringBuilder();
        builder.append("Provide a concise hint for the following multiple choice question without revealing the answer.\n");
        builder.append("Question: ").append(currentTask.getQuestion()).append('\n');
        builder.append("Options:\n");
        for (int i = 0; i < currentTask.getOptions().size(); i++) {
            builder.append(i + 1).append(") ").append(currentTask.getOptions().get(i)).append('\n');
        }
        if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
            builder.append("Description: ").append(currentTask.getDescription()).append('\n');
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
                notifyStateChanged();
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
                getFinalAnswerMessage(), R.drawable.trophy);
        if (!aiHintShown) {
            aiHintShown = true;
            notifyStateChanged();
        }
    }

    private String getFinalAnswerMessage() {
        int correctIndex = currentTask.getCorrectAnswer();
        String correctAnswer = currentTask.getOptions().get(correctIndex);
        return "Correct answer: " + correctAnswer;
    }
}

