package com.choicecrafter.students.models.tasks;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

/**
 * Represents a single example used inside a coding challenge task.
 */
public class CodingChallengeExample implements Parcelable {

    private String input;
    private String output;
    private String explanation;

    public CodingChallengeExample() {
    }

    public CodingChallengeExample(String input, String output, @Nullable String explanation) {
        this.input = input;
        this.output = output;
        this.explanation = explanation;
    }

    protected CodingChallengeExample(Parcel in) {
        input = in.readString();
        output = in.readString();
        explanation = in.readString();
    }

    public static final Creator<CodingChallengeExample> CREATOR = new Creator<CodingChallengeExample>() {
        @Override
        public CodingChallengeExample createFromParcel(Parcel in) {
            return new CodingChallengeExample(in);
        }

        @Override
        public CodingChallengeExample[] newArray(int size) {
            return new CodingChallengeExample[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(input);
        dest.writeString(output);
        dest.writeString(explanation);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    @Nullable
    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(@Nullable String explanation) {
        this.explanation = explanation;
    }
}
