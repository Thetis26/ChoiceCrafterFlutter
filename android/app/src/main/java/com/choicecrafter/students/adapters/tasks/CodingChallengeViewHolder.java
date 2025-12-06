package com.choicecrafter.studentapp.adapters.tasks;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.tasks.CodingChallengeExample;
import com.choicecrafter.studentapp.models.tasks.CodingChallengeTask;
import com.choicecrafter.studentapp.ui.activity.TaskSessionState;
import com.choicecrafter.studentapp.utils.AiHintService;
import com.choicecrafter.studentapp.utils.CodeExecutionService;
import com.choicecrafter.studentapp.utils.HintDialogUtil;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CodingChallengeViewHolder extends RecyclerView.ViewHolder {

    public interface StateListener {
        void onStateChanged(String language,
                             String code,
                             String customInput,
                             boolean hintShown,
                             String lastOutput,
                             String lastStdErr,
                             boolean lastRunSuccessful);
    }

    public interface ValidationCallback {
        void onResult(boolean isCorrect, double completionRatio);
    }

    private static final String[] LANGUAGE_DISPLAY_NAMES = {"Python", "Java", "C++"};
    private static final String[] LANGUAGE_KEYS = {"python", "java", "cpp"};

    private final TextView taskTitleView;
    private final TextView taskDescriptionView;
    private final TextView problemDescriptionView;
    private final TextView expectedOutputDescriptionView;
    private final TextView expectedOutputLabel;
    private final LinearLayout examplesContainer;
    private final TextView examplesLabel;
    private final Spinner languageSpinner;
    private final EditText codeEditor;
    private final EditText customInputField;
    private final Button runButton;
    private final Button showSolutionButton;
    private final ProgressBar executionProgress;
    private final TextView executionStatusView;
    private final TextView executionOutputView;

    private final AiHintService aiHintService;
    private final CodeExecutionService codeExecutionService;

    private CodingChallengeTask currentTask;
    private StateListener stateListener;
    private boolean aiHintShown;
    private boolean hintInProgress;
    private boolean solutionGenerationInProgress;
    private boolean isRestoringState;
    private boolean executionInProgress;
    private boolean hasUserEditedCode;
    private String lastStdout = "";
    private String lastStderr = "";
    private boolean lastRunSuccessful;

    public CodingChallengeViewHolder(@NonNull View itemView) {
        super(itemView);
        taskTitleView = itemView.findViewById(R.id.task_title);
        taskDescriptionView = itemView.findViewById(R.id.task_description);
        problemDescriptionView = itemView.findViewById(R.id.problem_description);
        expectedOutputDescriptionView = itemView.findViewById(R.id.expected_output_description);
        expectedOutputLabel = itemView.findViewById(R.id.expected_output_label);
        examplesContainer = itemView.findViewById(R.id.examples_container);
        examplesLabel = itemView.findViewById(R.id.examples_label);
        languageSpinner = itemView.findViewById(R.id.language_spinner);
        codeEditor = itemView.findViewById(R.id.code_editor);
        customInputField = itemView.findViewById(R.id.custom_input_field);
        runButton = itemView.findViewById(R.id.run_code_button);
        showSolutionButton = itemView.findViewById(R.id.show_solution_button);
        executionProgress = itemView.findViewById(R.id.execution_progress);
        executionStatusView = itemView.findViewById(R.id.execution_status);
        executionOutputView = itemView.findViewById(R.id.execution_output);
        aiHintService = AiHintService.getInstance(itemView.getContext());
        codeExecutionService = CodeExecutionService.getInstance(itemView.getContext());
        setupLanguageSpinner(itemView.getContext());
        setupListeners();
    }

    public void bind(@NonNull CodingChallengeTask task) {
        currentTask = task;
        aiHintShown = false;
        hintInProgress = false;
        solutionGenerationInProgress = false;
        executionInProgress = false;
        hasUserEditedCode = false;
        lastStdout = "";
        lastStderr = "";
        lastRunSuccessful = false;
        executionStatusView.setText("");
        executionOutputView.setText("");
        executionOutputView.setVisibility(View.GONE);

        if (!TextUtils.isEmpty(task.getTitle())) {
            taskTitleView.setVisibility(View.VISIBLE);
            taskTitleView.setText(task.getTitle());
        } else {
            taskTitleView.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(task.getDescription())) {
            taskDescriptionView.setVisibility(View.VISIBLE);
            taskDescriptionView.setText(task.getDescription());
        } else {
            taskDescriptionView.setVisibility(View.GONE);
        }

        problemDescriptionView.setText(!TextUtils.isEmpty(task.getProblemDescription())
                ? task.getProblemDescription()
                : itemView.getContext().getString(R.string.coding_challenge_problem_placeholder));

        if (!TextUtils.isEmpty(task.getExpectedOutputDescription())) {
            expectedOutputLabel.setVisibility(View.VISIBLE);
            expectedOutputDescriptionView.setVisibility(View.VISIBLE);
            expectedOutputDescriptionView.setText(task.getExpectedOutputDescription());
        } else {
            expectedOutputLabel.setVisibility(View.GONE);
            expectedOutputDescriptionView.setVisibility(View.GONE);
        }

        populateExamples(task.getExamples());
        String initialLanguage = resolveInitialLanguage(task);
        int initialIndex = indexOfLanguage(initialLanguage);
        if (initialIndex < 0) {
            initialIndex = 0;
        }
        isRestoringState = true;
        languageSpinner.setSelection(initialIndex, false);
        isRestoringState = false;
        applyStarterCodeForLanguage(LANGUAGE_KEYS[initialIndex], true);
        notifyStateChanged();
    }

    public void setStateListener(StateListener listener) {
        this.stateListener = listener;
    }

    public void restoreState(@NonNull TaskSessionState.CodingChallengeState state) {
        isRestoringState = true;
        setSpinnerSelection(state.getSelectedLanguage());
        codeEditor.setText(state.getCodeText());
        customInputField.setText(state.getCustomInput());
        aiHintShown = state.isHintShown();
        lastStdout = state.getLastOutput();
        lastStderr = state.getLastStdErr();
        lastRunSuccessful = state.isLastRunSuccessful();
        if (!TextUtils.isEmpty(lastStdout) || !TextUtils.isEmpty(lastStderr)) {
            updateExecutionResultViews(lastStdout, lastStderr, lastRunSuccessful, false);
        }
        isRestoringState = false;
        hasUserEditedCode = !TextUtils.isEmpty(state.getCodeText());
        notifyStateChanged();
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
        showExpectedAnswer();
    }

    public void validateSolution(@NonNull ValidationCallback callback) {
        if (currentTask == null) {
            callback.onResult(false, 0.0);
            return;
        }
        String code = codeEditor.getText() != null ? codeEditor.getText().toString() : "";
        if (TextUtils.isEmpty(code.trim())) {
            Toast.makeText(itemView.getContext(), R.string.coding_challenge_missing_code, Toast.LENGTH_SHORT).show();
            callback.onResult(false, 0.0);
            return;
        }
        if (executionInProgress) {
            Toast.makeText(itemView.getContext(), R.string.coding_challenge_execution_in_progress, Toast.LENGTH_SHORT).show();
            callback.onResult(false, 0.0);
            return;
        }
        List<CodingChallengeExample> examples = currentTask.getExamples();
        if (examples == null || examples.isEmpty()) {
            Toast.makeText(itemView.getContext(), R.string.coding_challenge_missing_examples, Toast.LENGTH_SHORT).show();
            callback.onResult(false, 0.0);
            return;
        }
        CodingChallengeExample referenceExample = examples.get(0);
        executeCode(referenceExample.getInput(), true, new ExecutionResultListener() {
            @Override
            public void onSuccess(String stdout, String stderr) {
                String normalizedStdout = stdout != null ? stdout.trim() : "";
                String expected = referenceExample.getOutput() != null
                        ? referenceExample.getOutput().trim()
                        : "";
                boolean matches = normalizedStdout.equals(expected) && TextUtils.isEmpty(stderr);
                if (matches) {
                    Toast.makeText(itemView.getContext(), R.string.coding_challenge_validation_success, Toast.LENGTH_SHORT).show();
                } else {
                    String message = itemView.getContext().getString(R.string.coding_challenge_validation_failure, expected, normalizedStdout);
                    Toast.makeText(itemView.getContext(), message, Toast.LENGTH_LONG).show();
                }
                callback.onResult(matches, matches ? 1.0 : 0.0);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(itemView.getContext(), message, Toast.LENGTH_LONG).show();
                callback.onResult(false, 0.0);
            }
        });
    }

    private void setupLanguageSpinner(Context context) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, LANGUAGE_DISPLAY_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        languageSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> {
            if (isRestoringState) {
                return;
            }
            applyStarterCodeForLanguage(LANGUAGE_KEYS[position], false);
            notifyStateChanged();
        }));

        codeEditor.addTextChangedListener(new TextWatcher() {
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
                hasUserEditedCode = true;
                notifyStateChanged();
            }
        });

        customInputField.addTextChangedListener(new SimpleTextWatcher(() -> {
            if (!isRestoringState) {
                notifyStateChanged();
            }
        }));

        runButton.setOnClickListener(v -> {
            if (executionInProgress) {
                Toast.makeText(itemView.getContext(), R.string.coding_challenge_execution_in_progress, Toast.LENGTH_SHORT).show();
                return;
            }
            String code = codeEditor.getText().toString();
            if (TextUtils.isEmpty(code.trim())) {
                Toast.makeText(itemView.getContext(), R.string.coding_challenge_missing_code, Toast.LENGTH_SHORT).show();
                return;
            }
            executeCode(customInputField.getText().toString(), false, new ExecutionResultListener() {
                @Override
                public void onSuccess(String stdout, String stderr) {
                    Toast.makeText(itemView.getContext(), R.string.coding_challenge_run_success, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(itemView.getContext(), message, Toast.LENGTH_LONG).show();
                }
            });
        });

        if (showSolutionButton != null) {
            showSolutionButton.setOnClickListener(v -> showSolution());
        }
    }

    private void requestHintFromAi() {
        hintInProgress = true;
        Toast.makeText(itemView.getContext(), R.string.ai_hint_generating, Toast.LENGTH_SHORT).show();
        aiHintService.requestHint(buildHintPrompt(), new AiHintService.HintCallback() {
            @Override
            public void onSuccess(String hint) {
                hintInProgress = false;
                aiHintShown = true;
                notifyStateChanged();
                HintDialogUtil.showHintDialog(itemView.getContext(),
                        itemView.getContext().getString(R.string.coding_challenge_hint_title),
                        hint);
            }

            @Override
            public void onError(String errorMessage) {
                hintInProgress = false;
                Toast.makeText(itemView.getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showExpectedAnswer() {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(currentTask.getExpectedOutputDescription())) {
            builder.append(currentTask.getExpectedOutputDescription());
        }
        List<CodingChallengeExample> examples = currentTask.getExamples();
        if (examples != null && !examples.isEmpty()) {
            CodingChallengeExample example = examples.get(0);
            if (!TextUtils.isEmpty(example.getInput()) || !TextUtils.isEmpty(example.getOutput())) {
                if (builder.length() > 0) {
                    builder.append("\n\n");
                }
                builder.append(itemView.getContext().getString(R.string.coding_challenge_example_answer_label));
                builder.append("\n");
                if (!TextUtils.isEmpty(example.getInput())) {
                    builder.append(itemView.getContext().getString(R.string.coding_challenge_example_input, example.getInput()));
                    builder.append("\n");
                }
                if (!TextUtils.isEmpty(example.getOutput())) {
                    builder.append(itemView.getContext().getString(R.string.coding_challenge_example_output, example.getOutput()));
                }
            }
        }
        if (builder.length() == 0) {
            builder.append(itemView.getContext().getString(R.string.coding_challenge_no_answer_available));
        }
        HintDialogUtil.showHintDialog(itemView.getContext(),
                itemView.getContext().getString(R.string.coding_challenge_answer_title),
                builder.toString(),
                R.drawable.lightbulb_on);
        notifyStateChanged();
    }

    private void showSolution() {
        if (currentTask == null) {
            return;
        }
        Map<String, String> solutions = currentTask.getSolutionCodeByLanguage();
        String selectedLanguageKey = getSelectedLanguageKey();
        String solutionCode = solutions.get(selectedLanguageKey);
        if (TextUtils.isEmpty(solutionCode) && !solutions.isEmpty()) {
            Map.Entry<String, String> first = solutions.entrySet().iterator().next();
            solutionCode = first.getValue();
            if (!TextUtils.isEmpty(first.getKey())) {
                setSpinnerSelection(first.getKey());
            }
        }
        String solutionInput = !TextUtils.isEmpty(currentTask.getSolutionInput())
                ? currentTask.getSolutionInput()
                : getDefaultSolutionInput();
        if (TextUtils.isEmpty(solutionCode)) {
            requestSolutionFromAi(selectedLanguageKey, solutionInput);
            return;
        }
        applySolutionToEditor(solutionCode, solutionInput);
    }

    private void requestSolutionFromAi(String languageKey, String solutionInput) {
        if (solutionGenerationInProgress) {
            Toast.makeText(itemView.getContext(), R.string.coding_challenge_ai_solution_generating, Toast.LENGTH_SHORT).show();
            return;
        }
        solutionGenerationInProgress = true;
        Toast.makeText(itemView.getContext(), R.string.coding_challenge_ai_solution_generating, Toast.LENGTH_SHORT).show();
        aiHintService.requestSolution(buildSolutionPrompt(languageKey), new AiHintService.HintCallback() {
            @Override
            public void onSuccess(String hint) {
                solutionGenerationInProgress = false;
                applySolutionToEditor(hint, solutionInput);
            }

            @Override
            public void onError(String errorMessage) {
                solutionGenerationInProgress = false;
                Toast.makeText(itemView.getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applySolutionToEditor(String solutionCode, String solutionInput) {
        if (TextUtils.isEmpty(solutionCode) && TextUtils.isEmpty(solutionInput)) {
            Toast.makeText(itemView.getContext(), R.string.coding_challenge_no_solution_available, Toast.LENGTH_SHORT).show();
            return;
        }
        isRestoringState = true;
        if (!TextUtils.isEmpty(solutionCode)) {
            codeEditor.setText(solutionCode);
            hasUserEditedCode = true;
        }
        if (!TextUtils.isEmpty(solutionInput)) {
            customInputField.setText(solutionInput);
        }
        isRestoringState = false;
        notifyStateChanged();
        Toast.makeText(itemView.getContext(), R.string.coding_challenge_solution_applied, Toast.LENGTH_SHORT).show();
    }

    private void executeCode(String stdin,
                             boolean fromValidation,
                             @NonNull ExecutionResultListener listener) {
        executionInProgress = true;
        executionProgress.setVisibility(View.VISIBLE);
        runButton.setEnabled(false);
        String code = codeEditor.getText() != null ? codeEditor.getText().toString() : "";
        String languageKey = getSelectedLanguageKey();
        codeExecutionService.execute(languageKey, code, stdin, new CodeExecutionService.ExecutionCallback() {
            @Override
            public void onSuccess(@NonNull String stdout, @NonNull String stderr) {
                executionInProgress = false;
                executionProgress.setVisibility(View.GONE);
                runButton.setEnabled(true);
                lastStdout = stdout;
                lastStderr = stderr;
                lastRunSuccessful = TextUtils.isEmpty(stderr);
                updateExecutionResultViews(stdout, stderr, lastRunSuccessful, fromValidation);
                notifyStateChanged();
                listener.onSuccess(stdout, stderr);
            }

            @Override
            public void onError(@NonNull String message) {
                executionInProgress = false;
                executionProgress.setVisibility(View.GONE);
                runButton.setEnabled(true);
                listener.onError(message);
            }
        });
    }

    private void populateExamples(List<CodingChallengeExample> examples) {
        examplesLabel.setVisibility(View.VISIBLE);
        examplesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
        if (examples == null || examples.isEmpty()) {
            TextView placeholder = new TextView(itemView.getContext());
            placeholder.setText(R.string.coding_challenge_missing_examples);
            placeholder.setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Body1);
            examplesContainer.addView(placeholder);
            return;
        }
        for (int i = 0; i < examples.size(); i++) {
            CodingChallengeExample example = examples.get(i);
            View exampleView = inflater.inflate(R.layout.view_coding_example, examplesContainer, false);
            TextView title = exampleView.findViewById(R.id.example_title);
            TextView inputView = exampleView.findViewById(R.id.example_input);
            TextView outputView = exampleView.findViewById(R.id.example_output);
            TextView explanationView = exampleView.findViewById(R.id.example_explanation);
            title.setText(itemView.getContext().getString(R.string.coding_challenge_example_title, i + 1));
            inputView.setText(!TextUtils.isEmpty(example.getInput())
                    ? example.getInput()
                    : itemView.getContext().getString(R.string.coding_challenge_example_input_empty));
            outputView.setText(!TextUtils.isEmpty(example.getOutput())
                    ? example.getOutput()
                    : itemView.getContext().getString(R.string.coding_challenge_example_output_empty));
            if (!TextUtils.isEmpty(example.getExplanation())) {
                explanationView.setVisibility(View.VISIBLE);
                explanationView.setText(example.getExplanation());
            } else {
                explanationView.setVisibility(View.GONE);
            }
            examplesContainer.addView(exampleView);
        }
    }

    private void applyStarterCodeForLanguage(String languageKey, boolean force) {
        if (currentTask == null) {
            return;
        }
        String starter = currentTask.getStarterCodeByLanguage().get(languageKey);
        boolean shouldApply = force || !hasUserEditedCode
                || TextUtils.isEmpty(codeEditor.getText() != null ? codeEditor.getText().toString().trim() : "");
        if (shouldApply) {
            isRestoringState = true;
            codeEditor.setText(!TextUtils.isEmpty(starter) ? starter : "");
            isRestoringState = false;
            hasUserEditedCode = false;
        }
    }

    private String resolveInitialLanguage(CodingChallengeTask task) {
        String preferred = task.getDefaultLanguage();
        if (!TextUtils.isEmpty(preferred)) {
            return preferred.toLowerCase(Locale.US);
        }
        return LANGUAGE_KEYS[0];
    }

    private void setSpinnerSelection(String languageKey) {
        int index = indexOfLanguage(languageKey);
        if (index >= 0 && index < LANGUAGE_KEYS.length) {
            isRestoringState = true;
            languageSpinner.setSelection(index, false);
            isRestoringState = false;
        }
    }

    private int indexOfLanguage(String languageKey) {
        if (TextUtils.isEmpty(languageKey)) {
            return -1;
        }
        for (int i = 0; i < LANGUAGE_KEYS.length; i++) {
            if (LANGUAGE_KEYS[i].equalsIgnoreCase(languageKey)) {
                return i;
            }
        }
        return -1;
    }

    private String getSelectedLanguageKey() {
        int position = languageSpinner.getSelectedItemPosition();
        if (position < 0 || position >= LANGUAGE_KEYS.length) {
            return LANGUAGE_KEYS[0];
        }
        return LANGUAGE_KEYS[position];
    }

    private void notifyStateChanged() {
        if (stateListener == null) {
            return;
        }
        stateListener.onStateChanged(getSelectedLanguageKey(),
                codeEditor.getText() != null ? codeEditor.getText().toString() : "",
                customInputField.getText() != null ? customInputField.getText().toString() : "",
                aiHintShown,
                lastStdout,
                lastStderr,
                lastRunSuccessful);
    }

    private String buildHintPrompt() {
        StringBuilder builder = new StringBuilder();
        builder.append("Provide a concise hint for the following coding challenge without giving away the full solution.\n");
        builder.append("Problem: ").append(currentTask.getProblemDescription()).append("\n");
        if (!TextUtils.isEmpty(currentTask.getExpectedOutputDescription())) {
            builder.append("Expected outcome: ").append(currentTask.getExpectedOutputDescription()).append("\n");
        }
        List<CodingChallengeExample> examples = currentTask.getExamples();
        if (examples != null && !examples.isEmpty()) {
            CodingChallengeExample example = examples.get(0);
            if (!TextUtils.isEmpty(example.getInput())) {
                builder.append("Sample input: ").append(example.getInput()).append("\n");
            }
            if (!TextUtils.isEmpty(example.getOutput())) {
                builder.append("Expected output: ").append(example.getOutput()).append("\n");
            }
        }
        return builder.toString();
    }

    private String buildSolutionPrompt(String languageKey) {
        StringBuilder builder = new StringBuilder();
        int languageIndex = indexOfLanguage(languageKey);
        String languageName = languageIndex >= 0 && languageIndex < LANGUAGE_DISPLAY_NAMES.length
                ? LANGUAGE_DISPLAY_NAMES[languageIndex]
                : languageKey;
        builder.append("Write a complete solution in ").append(languageName).append(" for the following coding challenge. ");
        builder.append("Return only the code without explanations.\n");
        builder.append("Problem: ").append(currentTask.getProblemDescription()).append("\n");
        if (!TextUtils.isEmpty(currentTask.getExpectedOutputDescription())) {
            builder.append("Expected outcome: ").append(currentTask.getExpectedOutputDescription()).append("\n");
        }
        List<CodingChallengeExample> examples = currentTask.getExamples();
        if (examples != null && !examples.isEmpty()) {
            CodingChallengeExample example = examples.get(0);
            if (!TextUtils.isEmpty(example.getInput())) {
                builder.append("Sample input: ").append(example.getInput()).append("\n");
            }
            if (!TextUtils.isEmpty(example.getOutput())) {
                builder.append("Expected output: ").append(example.getOutput()).append("\n");
            }
        }
        builder.append("Use clear variable names and avoid extra commentary.");
        return builder.toString();
    }

    private String getDefaultSolutionInput() {
        List<CodingChallengeExample> examples = currentTask.getExamples();
        if (examples != null && !examples.isEmpty()) {
            CodingChallengeExample example = examples.get(0);
            if (!TextUtils.isEmpty(example.getInput())) {
                return example.getInput();
            }
        }
        return "";
    }

    private void updateExecutionResultViews(String stdout,
                                            String stderr,
                                            boolean success,
                                            boolean fromValidation) {
        if (!TextUtils.isEmpty(stdout)) {
            executionOutputView.setVisibility(View.VISIBLE);
            executionOutputView.setText(stdout.trim());
        } else {
            executionOutputView.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(stderr)) {
            executionStatusView.setText(itemView.getContext().getString(R.string.coding_challenge_run_failed, stderr.trim()));
        } else if (success) {
            executionStatusView.setText(fromValidation
                    ? itemView.getContext().getString(R.string.coding_challenge_validation_output_success)
                    : itemView.getContext().getString(R.string.coding_challenge_run_output_success));
        } else {
            executionStatusView.setText("");
        }
    }

    public boolean hasUsedHint() {
        return aiHintShown;
    }

    private interface ExecutionResultListener {
        void onSuccess(String stdout, String stderr);

        void onError(String message);
    }

    private static class SimpleTextWatcher implements TextWatcher {

        private final Runnable afterChange;

        private SimpleTextWatcher(Runnable afterChange) {
            this.afterChange = afterChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            afterChange.run();
        }
    }

    private static class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {

        private final SelectionCallback callback;

        private SimpleItemSelectedListener(SelectionCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            callback.onSelected(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private interface SelectionCallback {
        void onSelected(int position);
    }
}
