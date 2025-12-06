package com.choicecrafter.studentapp.models.tasks;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class MultipleChoiceQuestion extends Task {

    private String question;
    private int correctAnswer;
    private List<String> options;
    private SupportingContent supportingContent;

    public MultipleChoiceQuestion() {
    }

    public MultipleChoiceQuestion(String title, String description, String type, String status, String question, List<String> options, int correctAnswer) {
        super(title, description, type, status);
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    protected MultipleChoiceQuestion(Parcel in) {
        super(in);
        question = in.readString();
        options = in.createStringArrayList();
        correctAnswer = in.readInt();
        supportingContent = in.readParcelable(SupportingContent.class.getClassLoader());
    }

    public static final Parcelable.Creator<MultipleChoiceQuestion> CREATOR = new Parcelable.Creator<MultipleChoiceQuestion>() {
        @Override
        public MultipleChoiceQuestion createFromParcel(Parcel in) {
            return new MultipleChoiceQuestion(in);
        }

        @Override
        public MultipleChoiceQuestion[] newArray(int size) {
            return new MultipleChoiceQuestion[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(question);
        dest.writeStringList(options);
        dest.writeInt(correctAnswer);
        dest.writeParcelable(supportingContent, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public int getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(int correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public SupportingContent getSupportingContent() {
        return supportingContent;
    }

    public void setSupportingContent(SupportingContent supportingContent) {
        this.supportingContent = supportingContent;
    }
}