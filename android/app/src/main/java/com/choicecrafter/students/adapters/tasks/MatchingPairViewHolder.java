package com.choicecrafter.students.adapters.tasks;

import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.Gravity;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.choicecrafter.students.models.tasks.MatchingPairTask;
import com.choicecrafter.students.R;
import com.choicecrafter.students.utils.AiHintService;
import com.choicecrafter.students.utils.HintDialogUtil;

import java.util.HashMap;
import java.util.Map;

public class MatchingPairViewHolder extends RecyclerView.ViewHolder {
    private final LinearLayout leftColumn;
    private final LinearLayout rightColumn;
    private final TextView descriptionView;
    private MatchingPairTask currentTask;

    private TextView selectedLeft;
    private TextView selectedRight;
    private final Map<String, Boolean> matched = new HashMap<>();
    private final Map<String, Boolean> matchedRight = new HashMap<>();
    private final AiHintService aiHintService;
    private boolean aiHintShown;
    private boolean hintInProgress;
    private String solutionText;

    public MatchingPairViewHolder(View itemView) {
        super(itemView);
        leftColumn = itemView.findViewById(com.choicecrafter.students.R.id.left_column);
        rightColumn = itemView.findViewById(com.choicecrafter.students.R.id.right_column);
        descriptionView = itemView.findViewById(R.id.task_description);
        aiHintService = AiHintService.getInstance(itemView.getContext());
    }

    public void bind(MatchingPairTask task) {
        currentTask = task;
        matched.clear();
        matchedRight.clear();
        leftColumn.removeAllViews();
        rightColumn.removeAllViews();
        aiHintShown = false;
        hintInProgress = false;

        if (descriptionView != null) {
            if (!TextUtils.isEmpty(task.getDescription())) {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(task.getDescription());
            } else {
                descriptionView.setVisibility(View.GONE);
            }
        }

        for (String left : task.getLeftItems()) {
            TextView tv = createOptionTextView(left);
            tv.setOnClickListener(v -> onLeftClicked(tv));
            leftColumn.addView(tv);
        }

        for (String right : task.getRightItems()) {
            TextView tv = createOptionTextView(right);
            tv.setOnClickListener(v -> onRightClicked(tv));
            rightColumn.addView(tv);
        }

        solutionText = buildSolutionText();
    }

    private TextView createOptionTextView(String text) {
        TextView tv = new TextView(itemView.getContext());
        tv.setTag(text);
        tv.setText(text);
        tv.setGravity(Gravity.CENTER);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setTextSize(15f);
        tv.setCompoundDrawablePadding(dp(12));
        tv.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
        Typeface base = ResourcesCompat.getFont(itemView.getContext(), R.font.segoe);
        tv.setTypeface(base, Typeface.BOLD);
        styleAsDefault(tv);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(12), 0, dp(12));
        tv.setLayoutParams(params);
        return tv;
    }

    private void onLeftClicked(TextView tv) {
        if (matched.get(tv.getTag().toString()) != null) return;
        if (selectedLeft != null) {
            styleAsDefault(selectedLeft);
        }
        selectedLeft = tv;
        styleAsSelected(tv);
        checkPair();
    }

    private void onRightClicked(TextView tv) {
        if (matchedRight.get(tv.getTag().toString()) != null) return;
        if (selectedRight != null) {
            styleAsDefault(selectedRight);
        }
        selectedRight = tv;
        styleAsSelected(tv);
        checkPair();
    }

    private void checkPair() {
        if (selectedLeft != null && selectedRight != null) {
            final TextView leftView = selectedLeft;
            final TextView rightView = selectedRight;
            String left = leftView.getTag().toString();
            String right = rightView.getTag().toString();
            String correct = currentTask.getCorrectMatches().get(left);
            boolean isCorrect = right.equals(correct);
            if (isCorrect) {
                styleAsCorrect(leftView);
                styleAsCorrect(rightView);
                matched.put(left, true);
                matchedRight.put(right, true);
                leftView.setClickable(false);
                rightView.setClickable(false);
            }
            else {
                styleAsIncorrect(leftView);
                styleAsIncorrect(rightView);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (matched.get(leftView.getTag().toString()) == null) {
                        styleAsDefault(leftView);
                    }
                    if (matchedRight.get(rightView.getTag().toString()) == null) {
                        styleAsDefault(rightView);
                    }
                }, 600);
            }
            selectedLeft = null;
            selectedRight = null;
        }
    }

    public boolean validateAnswer() {
        return matched.size() == currentTask.getLeftItems().size();
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
        builder.append("Provide a helpful hint for matching the following concepts. Do not reveal which items pair together.\n");
        builder.append("Left column: \n");
        for (String left : currentTask.getLeftItems()) {
            builder.append("- ").append(left).append('\n');
        }
        builder.append("Right column: \n");
        for (String right : currentTask.getRightItems()) {
            builder.append("- ").append(right).append('\n');
        }
        if (currentTask.getDescription() != null && !currentTask.getDescription().isEmpty()) {
            builder.append("Description: ").append(currentTask.getDescription());
        }
        return builder.toString();
    }

    private String buildSolutionText() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : currentTask.getCorrectMatches().entrySet()) {
            builder.append(entry.getKey()).append(" -> ").append(entry.getValue()).append('\n');
        }
        return builder.toString().trim();
    }

    private void styleAsDefault(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_task_card_matching_pairs);
        tv.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
    }

    private void styleAsSelected(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_matching_option_selected);
        tv.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
    }

    private void styleAsCorrect(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_matching_option_correct);
        tv.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
    }

    private void styleAsIncorrect(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_matching_option_incorrect);
        tv.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                itemView.getResources().getDisplayMetrics()));
    }

    private void showDialog(String title, String message, int iconRes) {
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
        showDialog(itemView.getContext().getString(R.string.ai_answer_title), solutionText, R.drawable.trophy);
    }
}
