package com.choicecrafter.students.models.tasks;

import android.os.Parcel;
import android.os.Parcelable;

public class TrueFalseTask extends Task {

    private String statement;
    private boolean correctAnswer;

    public TrueFalseTask() {
    }

    public TrueFalseTask(String title,
                         String description,
                         String type,
                         String status,
                         String statement,
                         boolean correctAnswer,
                         String explanation) {
        super(title, description, type, status, explanation);
        this.statement = statement;
        this.correctAnswer = correctAnswer;
    }

    protected TrueFalseTask(Parcel in) {
        super(in);
        statement = in.readString();
        correctAnswer = in.readByte() != 0;
    }

    public static final Parcelable.Creator<TrueFalseTask> CREATOR = new Parcelable.Creator<TrueFalseTask>() {
        @Override
        public TrueFalseTask createFromParcel(Parcel in) {
            return new TrueFalseTask(in);
        }

        @Override
        public TrueFalseTask[] newArray(int size) {
            return new TrueFalseTask[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(statement);
        dest.writeByte((byte) (correctAnswer ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public boolean isCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(boolean correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return super.getExplanation();
    }

    public void setExplanation(String explanation) {
        super.setExplanation(explanation);
    }
}
