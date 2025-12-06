package com.choicecrafter.studentapp.ui.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskSessionState {

    private final Map<String, MultipleChoiceState> multipleChoiceStates = new HashMap<>();
    private final Map<String, FillInTheBlankState> fillInTheBlankStates = new HashMap<>();
    private final Map<String, CodingChallengeState> codingChallengeStates = new HashMap<>();

    public MultipleChoiceState getMultipleChoiceState(String taskKey) {
        return multipleChoiceStates.get(taskKey);
    }

    public void updateMultipleChoiceState(String taskKey, int selectedIndex, boolean hintShown) {
        if (taskKey == null) {
            return;
        }
        MultipleChoiceState state = multipleChoiceStates.get(taskKey);
        if (state == null) {
            state = new MultipleChoiceState();
            multipleChoiceStates.put(taskKey, state);
        }
        state.setSelectedIndex(selectedIndex);
        state.setHintShown(hintShown);
    }

    public FillInTheBlankState getFillInTheBlankState(String taskKey) {
        return fillInTheBlankStates.get(taskKey);
    }

    public void updateFillInTheBlankState(String taskKey, List<String> answers, boolean hintShown) {
        if (taskKey == null) {
            return;
        }
        FillInTheBlankState state = fillInTheBlankStates.get(taskKey);
        if (state == null) {
            state = new FillInTheBlankState();
            fillInTheBlankStates.put(taskKey, state);
        }
        state.setAnswers(answers);
        state.setHintShown(hintShown);
    }

    public CodingChallengeState getCodingChallengeState(String taskKey) {
        return codingChallengeStates.get(taskKey);
    }

    public void updateCodingChallengeState(String taskKey,
                                           String language,
                                           String code,
                                           String customInput,
                                           boolean hintShown,
                                           String lastOutput,
                                           String lastStdErr,
                                           boolean lastRunSuccessful) {
        if (taskKey == null) {
            return;
        }
        CodingChallengeState state = codingChallengeStates.get(taskKey);
        if (state == null) {
            state = new CodingChallengeState();
            codingChallengeStates.put(taskKey, state);
        }
        state.setSelectedLanguage(language);
        state.setCodeText(code);
        state.setCustomInput(customInput);
        state.setHintShown(hintShown);
        state.setLastOutput(lastOutput);
        state.setLastStdErr(lastStdErr);
        state.setLastRunSuccessful(lastRunSuccessful);
    }

    public void clearState(String taskKey) {
        if (taskKey == null) {
            return;
        }
        multipleChoiceStates.remove(taskKey);
        fillInTheBlankStates.remove(taskKey);
        codingChallengeStates.remove(taskKey);
    }

    public void clear() {
        multipleChoiceStates.clear();
        fillInTheBlankStates.clear();
        codingChallengeStates.clear();
    }

    public static class MultipleChoiceState {
        private int selectedIndex = -1;
        private boolean hintShown;

        public int getSelectedIndex() {
            return selectedIndex;
        }

        public void setSelectedIndex(int selectedIndex) {
            this.selectedIndex = selectedIndex;
        }

        public boolean isHintShown() {
            return hintShown;
        }

        public void setHintShown(boolean hintShown) {
            this.hintShown = hintShown;
        }
    }

    public static class FillInTheBlankState {
        private List<String> answers = new ArrayList<>();
        private boolean hintShown;

        public List<String> getAnswers() {
            return Collections.unmodifiableList(answers);
        }

        public void setAnswers(List<String> answers) {
            this.answers.clear();
            if (answers != null) {
                for (String answer : answers) {
                    this.answers.add(answer != null ? answer : "");
                }
            }
        }

        public boolean isHintShown() {
            return hintShown;
        }

        public void setHintShown(boolean hintShown) {
            this.hintShown = hintShown;
        }
    }

    public static class CodingChallengeState {
        private String selectedLanguage = "python";
        private String codeText = "";
        private String customInput = "";
        private boolean hintShown;
        private String lastOutput = "";
        private String lastStdErr = "";
        private boolean lastRunSuccessful;

        public String getSelectedLanguage() {
            return selectedLanguage;
        }

        public void setSelectedLanguage(String selectedLanguage) {
            this.selectedLanguage = selectedLanguage != null ? selectedLanguage : "python";
        }

        public String getCodeText() {
            return codeText;
        }

        public void setCodeText(String codeText) {
            this.codeText = codeText != null ? codeText : "";
        }

        public String getCustomInput() {
            return customInput;
        }

        public void setCustomInput(String customInput) {
            this.customInput = customInput != null ? customInput : "";
        }

        public boolean isHintShown() {
            return hintShown;
        }

        public void setHintShown(boolean hintShown) {
            this.hintShown = hintShown;
        }

        public String getLastOutput() {
            return lastOutput;
        }

        public void setLastOutput(String lastOutput) {
            this.lastOutput = lastOutput != null ? lastOutput : "";
        }

        public String getLastStdErr() {
            return lastStdErr;
        }

        public void setLastStdErr(String lastStdErr) {
            this.lastStdErr = lastStdErr != null ? lastStdErr : "";
        }

        public boolean isLastRunSuccessful() {
            return lastRunSuccessful;
        }

        public void setLastRunSuccessful(boolean lastRunSuccessful) {
            this.lastRunSuccessful = lastRunSuccessful;
        }
    }
}
