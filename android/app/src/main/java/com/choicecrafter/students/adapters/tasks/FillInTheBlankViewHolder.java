package com.choicecrafter.students.adapters.tasks;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
 
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.choicecrafter.students.R;
import com.choicecrafter.students.models.tasks.FillInTheBlank;
import com.choicecrafter.students.models.tasks.SupportingContent;
import com.choicecrafter.students.utils.AiAnswerValidationService;
import com.choicecrafter.students.utils.AiHintService;
import com.choicecrafter.students.utils.HintDialogUtil;

import java.util.ArrayList;
import java.util.List;

public class FillInTheBlankViewHolder extends RecyclerView.ViewHolder {
    private LinearLayout questionContainer;
    private Context context;
    private List<EditText> answerFields;
    private final TextView descriptionView;
    private final TextView sentenceView;
    private final TextView supportingTextView;
    private final ImageView supportingImageView;

    private FillInTheBlank currentTask;
    private final AiHintService aiHintService;
    private final AiAnswerValidationService aiAnswerValidationService;
    private boolean aiHintShown;
    private boolean hintInProgress;
    private boolean validationInProgress;
    private String solutionText;
    private boolean isRestoringState;
    private StateListener stateListener;

    public interface ValidationCallback {
        void onResult(boolean isCorrect);
    }

    public interface StateListener {
        void onStateChanged(List<String> currentAnswers, boolean hintShown);
    }

    public FillInTheBlankViewHolder(View itemView) {
        super(itemView);
        context = itemView.getContext();
        questionContainer = itemView.findViewById(R.id.blanksContainer);
        descriptionView = itemView.findViewById(R.id.task_description);
        sentenceView = itemView.findViewById(R.id.sentenceTextView);
        supportingTextView = itemView.findViewById(R.id.supporting_text);
        supportingImageView = itemView.findViewById(R.id.supporting_image);
        answerFields = new ArrayList<>();
        aiHintService = AiHintService.getInstance(itemView.getContext());
        aiAnswerValidationService = AiAnswerValidationService.getInstance(itemView.getContext());
    }

    public void bind(FillInTheBlank task) {
        currentTask = task;
        questionContainer.removeAllViews(); // Clear previous views
        answerFields.clear();
        aiHintShown = false;
        hintInProgress = false;
        validationInProgress = false;
        isRestoringState = false;

        if (descriptionView != null) {
            if (!TextUtils.isEmpty(task.getDescription())) {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(task.getDescription());
            } else {
                descriptionView.setVisibility(View.GONE);
            }
        }

        String blankedText = task.getText();
        List<String> missingSegments = task.getMissingSegments();
        bindSupportingContent(task.getSupportingContent());

        // 1. Show the full sentence with blanks
        sentenceView.setText(blankedText);

        // 2. Add input fields for each blank below the sentence
        for (int i = 0; i < missingSegments.size(); i++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_task_card_fill_blank);

            EditText editText = new EditText(context);
            editText.setHint(context.getString(R.string.task_fill_blank_hint, 10));
            editText.setTextSize(16f);
            editText.setBackgroundResource(R.drawable.bg_blank_input);
            editText.setPadding(dp(20), dp(12), dp(20), dp(12));
            editText.setTextColor(ContextCompat.getColor(context, R.color.black));
            editText.setHintTextColor(ContextCompat.getColor(context, R.color.black));
            editText.setInputType(InputType.TYPE_CLASS_TEXT);

            Typeface customFont = ResourcesCompat.getFont(context, R.font.segoe);
            if (customFont != null) {
                editText.setTypeface(customFont);
            }

            LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );
            editText.setLayoutParams(editParams);
            row.addView(editText);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, dp(8), 0, dp(8));
            row.setLayoutParams(rowParams);

            answerFields.add(editText);
            questionContainer.addView(row);

            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isRestoringState) {
                        return;
                    }
                    notifyStateChanged();
                }
            };
            editText.addTextChangedListener(watcher);
        }

        solutionText = buildSolutionText(task);
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

    public void setStateListener(StateListener listener) {
        this.stateListener = listener;
    }

    public void restoreState(List<String> answers, boolean hintShown) {
        aiHintShown = hintShown;
        if (answers == null) {
            return;
        }
        isRestoringState = true;
        for (int i = 0; i < Math.min(answers.size(), answerFields.size()); i++) {
            answerFields.get(i).setText(answers.get(i));
        }
        isRestoringState = false;
    }

    public void validateAnswers(ValidationCallback callback) {
        if (validationInProgress) {
            Toast.makeText(context, R.string.ai_answer_validation_in_progress, Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onResult(false);
            }
            return;
        }
        List<String> userAnswers = new ArrayList<>();
        boolean hasEmpty = false;
        for (EditText answerField : answerFields) {
            userAnswers.add(answerField.getText().toString().trim());
            answerField.setBackgroundResource(R.drawable.bg_blank_input);
            answerField.setTextColor(ContextCompat.getColor(context, R.color.gamified_text_primary));
            if (TextUtils.isEmpty(answerField.getText().toString().trim())) {
                hasEmpty = true;
            }
        }

        List<String> correctAnswers = currentTask.getMissingSegments();

        if (userAnswers.size() != correctAnswers.size() || hasEmpty) {
            Toast.makeText(context, "Please fill in all the blanks", Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onResult(false);
            }
            return;
        }

        boolean allCorrect = true;
        for (int i = 0; i < userAnswers.size(); i++) {
            String userAnswer = userAnswers.get(i);
            String correctAnswer = correctAnswers.get(i);

            if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                answerFields.get(i).setBackgroundResource(R.drawable.bg_blank_input_correct);
                answerFields.get(i).setTextColor(ContextCompat.getColor(context, R.color.gamified_text_primary));
            } else {
                answerFields.get(i).setBackgroundResource(R.drawable.bg_blank_input_incorrect);
                answerFields.get(i).setTextColor(ContextCompat.getColor(context, R.color.gamified_text_primary));
                allCorrect = false;
            }
        }

        if (allCorrect) {
            if (callback != null) {
                callback.onResult(true);
            }
            return;
        }

        validationInProgress = true;
        Toast.makeText(context, R.string.ai_answer_validation_in_progress, Toast.LENGTH_SHORT).show();
        aiAnswerValidationService.evaluateFillInTheBlank(
                currentTask.getText(),
                correctAnswers,
                userAnswers,
                new AiAnswerValidationService.ValidationCallback() {
                    @Override
                    public void onSuccess(AiAnswerValidationService.ValidationResult result) {
                        validationInProgress = false;
                        applyAiFeedback(result);
                        if (callback != null) {
                            callback.onResult(result.isOverallCorrect());
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        validationInProgress = false;
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                        if (callback != null) {
                            callback.onResult(false);
                        }
                    }
                }
        );
    }

    private void applyAiFeedback(AiAnswerValidationService.ValidationResult result) {
        List<Boolean> perBlank = result.getPerBlankCorrectness();
        if (perBlank != null && perBlank.size() == answerFields.size()) {
            for (int i = 0; i < perBlank.size(); i++) {
                if (perBlank.get(i)) {
                    answerFields.get(i).setBackgroundResource(R.drawable.bg_blank_input_correct);
                } else {
                    answerFields.get(i).setBackgroundResource(R.drawable.bg_blank_input_incorrect);
                }
                answerFields.get(i).setTextColor(ContextCompat.getColor(context, R.color.gamified_text_primary));
            }
        }

        if (result.isOverallCorrect()) {
            return;
        } else if (!TextUtils.isEmpty(result.getFeedback())) {
            Toast.makeText(context, context.getString(R.string.ai_answer_validation_feedback, result.getFeedback()), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, "Some answers are incorrect!", Toast.LENGTH_SHORT).show();
        }
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
        builder.append("Provide a concise hint for the following fill in the blank activity. There are ")
                .append(currentTask.getMissingSegments().size())
                .append(" blanks. Do not reveal the missing words.\n");
        builder.append("Sentence: ").append(currentTask.getText()).append('\n');
        if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
            builder.append("Description: ").append(currentTask.getDescription());
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
        showTemporaryDialog(itemView.getContext().getString(R.string.ai_answer_title), solutionText,
                R.drawable.trophy);
        if (!aiHintShown) {
            aiHintShown = true;
            notifyStateChanged();
        }
    }

    private String buildSolutionText(FillInTheBlank task) {
        String textWithAnswers = task.getText();
        List<String> missingSegments = task.getMissingSegments();
        for (String segment : missingSegments) {
            textWithAnswers = textWithAnswers.replaceFirst("____", segment);
        }
        return textWithAnswers;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    private void notifyStateChanged() {
        if (stateListener == null) {
            return;
        }
        List<String> answers = new ArrayList<>();
        for (EditText answerField : answerFields) {
            answers.add(answerField.getText().toString());
        }
        stateListener.onStateChanged(answers, aiHintShown);
    }
}