package com.choicecrafter.studentapp.models.tasks;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class SpotTheErrorTask extends Task {

    private String prompt;
    private String codeSnippet;
    private List<String> options;
    private int correctAnswer;

    public SpotTheErrorTask() {
    }

    public SpotTheErrorTask(String title,
                             String description,
                             String type,
                             String status,
                             String prompt,
                             String snippet,
                             List<String> options,
                             int correctOptionIndex,
                             String explanation) {
        super(title, description, type, status, explanation);
        this.prompt = prompt;
        this.codeSnippet = snippet;
        this.options = options;
        this.correctAnswer = correctOptionIndex;
    }

    protected SpotTheErrorTask(Parcel in) {
        super(in);
        prompt = in.readString();
        codeSnippet = in.readString();
        options = in.createStringArrayList();
        correctAnswer = in.readInt();
    }

    public static final Parcelable.Creator<SpotTheErrorTask> CREATOR = new Parcelable.Creator<SpotTheErrorTask>() {
        @Override
        public SpotTheErrorTask createFromParcel(Parcel in) {
            return new SpotTheErrorTask(in);
        }

        @Override
        public SpotTheErrorTask[] newArray(int size) {
            return new SpotTheErrorTask[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(prompt);
        dest.writeString(codeSnippet);
        dest.writeStringList(options);
        dest.writeInt(correctAnswer);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSnippet() {
        return codeSnippet;
    }

    public void setSnippet(String snippet) {
        this.codeSnippet = snippet;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getCorrectOptionIndex() {
        return correctAnswer;
    }

    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctAnswer = correctOptionIndex;
    }

    public String getExplanation() {
        return super.getExplanation();
    }

    public void setExplanation(String explanation) {
        super.setExplanation(explanation);
    }
}
