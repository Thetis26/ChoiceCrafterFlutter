package com.choicecrafter.students.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.choicecrafter.students.R;
import com.choicecrafter.students.models.tasks.CodingChallengeExample;
import com.choicecrafter.students.models.tasks.CodingChallengeTask;
import com.choicecrafter.students.models.tasks.FillInTheBlank;
import com.choicecrafter.students.models.tasks.InfoCardTask;
import com.choicecrafter.students.models.tasks.MatchingPairTask;
import com.choicecrafter.students.models.tasks.MultipleChoiceQuestion;
import com.choicecrafter.students.models.tasks.OrderingTask;
import com.choicecrafter.students.models.tasks.SpotTheErrorTask;
import com.choicecrafter.students.models.tasks.Task;
import com.choicecrafter.students.models.tasks.TrueFalseTask;
import com.choicecrafter.students.ui.ConfettiView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles celebratory dialogs and explanation retrieval when learners answer tasks correctly.
 */
public final class TaskCelebrationManager {

    private static TaskCelebrationManager instance;

    private final Context applicationContext;
    private final AiExplanationService aiExplanationService;

    private TaskCelebrationManager(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.aiExplanationService = AiExplanationService.getInstance(applicationContext);
    }

    public static synchronized TaskCelebrationManager getInstance(Context context) {
        if (instance == null) {
            instance = new TaskCelebrationManager(context);
        }
        return instance;
    }

    public void celebrate(View anchorView, Task task, @Nullable Runnable onContinue) {
        Context context = anchorView.getContext();
        if (task == null) {
            showDialog(context, null, applicationContext.getString(R.string.task_explanation_default_message), onContinue);
            return;
        }

        String explanation = task.getExplanation();
        if (!TextUtils.isEmpty(explanation)) {
            showDialog(context, task.getTitle(), explanation, onContinue);
            return;
        }

        Toast.makeText(context, R.string.task_explanation_generating, Toast.LENGTH_SHORT).show();
        String prompt = buildPrompt(task);
        if (TextUtils.isEmpty(prompt)) {
            showDialog(context, task.getTitle(), applicationContext.getString(R.string.task_explanation_fallback_message), onContinue);
            return;
        }

        aiExplanationService.requestExplanation(prompt, new AiExplanationService.ExplanationCallback() {
            @Override
            public void onSuccess(String generatedExplanation) {
                String finalExplanation = !TextUtils.isEmpty(generatedExplanation)
                        ? generatedExplanation
                        : applicationContext.getString(R.string.task_explanation_fallback_message);
                task.setExplanation(finalExplanation);
                showDialog(context, task.getTitle(), finalExplanation, onContinue);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                String fallback = applicationContext.getString(R.string.task_explanation_fallback_message);
                task.setExplanation(fallback);
                showDialog(context, task.getTitle(), fallback, onContinue);
            }
        });
    }

    private void showDialog(Context context,
                            @Nullable String taskTitle,
                            String explanation,
                            @Nullable Runnable onContinue) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_task_explanation, null, false);
        TextView titleView = dialogView.findViewById(R.id.task_explanation_dialog_title);
        TextView subtitleView = dialogView.findViewById(R.id.task_explanation_dialog_task_title);
        TextView messageView = dialogView.findViewById(R.id.task_explanation_dialog_message);
        ImageView iconView = dialogView.findViewById(R.id.task_explanation_dialog_icon);
        MaterialButton continueButton = dialogView.findViewById(R.id.task_explanation_dialog_continue);

        titleView.setText(R.string.task_explanation_dialog_title);
        if (!TextUtils.isEmpty(taskTitle)) {
            subtitleView.setText(taskTitle);
            subtitleView.setVisibility(View.VISIBLE);
        } else {
            subtitleView.setVisibility(View.GONE);
        }
        messageView.setText(explanation);
        iconView.setImageResource(R.drawable.trophy);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        continueButton.setText(R.string.task_explanation_dialog_continue);
        continueButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (onContinue != null) {
                onContinue.run();
            }
        });
        dialog.show();
    }

    private void startCupAnimation(ImageView iconView) {
        iconView.setScaleX(0f);
        iconView.setScaleY(0f);
        iconView.setRotation(-15f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 0f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 0f, 1.2f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(iconView, View.ROTATION, -15f, 8f, -4f, 0f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(650L);
        animatorSet.setInterpolator(new OvershootInterpolator());
        animatorSet.playTogether(scaleX, scaleY, rotation);
        animatorSet.start();
    }

    private String buildPrompt(Task task) {
        StringBuilder builder = new StringBuilder();
        builder.append("Create a short, celebratory explanation (maximum 80 words) that reinforces why the learner's correct answer works. Use a positive second-person tone and avoid lists.\n");
        if (!TextUtils.isEmpty(task.getTitle())) {
            builder.append("Task title: ").append(task.getTitle()).append('\n');
        }
        if (!TextUtils.isEmpty(task.getDescription())) {
            builder.append("Task description: ").append(task.getDescription()).append('\n');
        }
        if (!TextUtils.isEmpty(task.getType())) {
            builder.append("Task type: ").append(task.getType()).append('\n');
        }

        if (task instanceof MultipleChoiceQuestion multipleChoiceQuestion) {
            builder.append("Question: ").append(multipleChoiceQuestion.getQuestion()).append('\n');
            List<String> options = multipleChoiceQuestion.getOptions();
            int correctIndex = multipleChoiceQuestion.getCorrectAnswer();
            if (options != null && correctIndex >= 0 && correctIndex < options.size()) {
                builder.append("Correct answer: ").append(options.get(correctIndex)).append('\n');
            }
        } else if (task instanceof FillInTheBlank fillInTheBlank) {
            builder.append("Sentence: ").append(fillInTheBlank.getText()).append('\n');
            List<String> missingSegments = fillInTheBlank.getMissingSegments();
            if (missingSegments != null && !missingSegments.isEmpty()) {
                builder.append("Correct blanks: ").append(missingSegments).append('\n');
            }
        } else if (task instanceof MatchingPairTask matchingPairTask) {
            Map<String, String> matches = matchingPairTask.getCorrectMatches();
            if (matches != null && !matches.isEmpty()) {
                builder.append("Correct pairs:\n");
                for (Map.Entry<String, String> entry : matches.entrySet()) {
                    builder.append(entry.getKey()).append(" -> ").append(entry.getValue()).append('\n');
                }
            }
        } else if (task instanceof OrderingTask orderingTask) {
            List<String> orderedItems = resolveOrderedItems(orderingTask);
            if (!orderedItems.isEmpty()) {
                builder.append("Correct order: ").append(orderedItems).append('\n');
            }
        } else if (task instanceof TrueFalseTask trueFalseTask) {
            builder.append("Statement: ").append(trueFalseTask.getStatement()).append('\n');
            builder.append("Answer: ").append(trueFalseTask.isCorrectAnswer() ? "True" : "False").append('\n');
        } else if (task instanceof SpotTheErrorTask spotTheErrorTask) {
            builder.append("Prompt: ").append(spotTheErrorTask.getPrompt()).append('\n');
            if (!TextUtils.isEmpty(spotTheErrorTask.getSnippet())) {
                builder.append("Snippet: ").append(spotTheErrorTask.getSnippet()).append('\n');
            }
            List<String> options = spotTheErrorTask.getOptions();
            int correctIndex = spotTheErrorTask.getCorrectOptionIndex();
            if (options != null && correctIndex >= 0 && correctIndex < options.size()) {
                builder.append("Correct option: ").append(options.get(correctIndex)).append('\n');
            }
        } else if (task instanceof CodingChallengeTask codingChallengeTask) {
            builder.append("Problem: ").append(codingChallengeTask.getProblemDescription()).append('\n');
            if (!TextUtils.isEmpty(codingChallengeTask.getExpectedOutputDescription())) {
                builder.append("Expected outcome: ")
                        .append(codingChallengeTask.getExpectedOutputDescription())
                        .append('\n');
            }
            List<CodingChallengeExample> examples = codingChallengeTask.getExamples();
            if (examples != null && !examples.isEmpty()) {
                CodingChallengeExample example = examples.get(0);
                if (!TextUtils.isEmpty(example.getInput())) {
                    builder.append("Sample input: ").append(example.getInput()).append('\n');
                }
                if (!TextUtils.isEmpty(example.getOutput())) {
                    builder.append("Expected output: ").append(example.getOutput()).append('\n');
                }
            }
        } else if (task instanceof InfoCardTask) {
            // Info cards do not require generated explanations.
            return null;
        }

        return builder.toString();
    }

    private List<String> resolveOrderedItems(OrderingTask orderingTask) {
        List<String> result = new ArrayList<>();
        if (orderingTask.getItems() == null || orderingTask.getCorrectOrder() == null) {
            return result;
        }
        List<String> items = orderingTask.getItems();
        for (Integer index : orderingTask.getCorrectOrder()) {
            if (index != null && index >= 0 && index < items.size()) {
                result.add(items.get(index));
            }
        }
        return result;
    }
}
