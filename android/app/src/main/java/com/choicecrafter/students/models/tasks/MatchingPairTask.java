package com.choicecrafter.studentapp.models.tasks;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Map;

public class MatchingPairTask extends Task {
    private List<String> leftItems;
    private List<String> rightItems;
    private Map<String, String> correctMatches;

    public MatchingPairTask() {}

    public MatchingPairTask(String title, String description, String type, String status,
                            List<String> leftItems, List<String> rightItems,
                            Map<String, String> correctMatches) {
        super(title, description, type, status);
        this.leftItems = leftItems;
        this.rightItems = rightItems;
        this.correctMatches = correctMatches;
    }

    protected MatchingPairTask(Parcel in) {
        super(in);
        leftItems = in.createStringArrayList();
        rightItems = in.createStringArrayList();
        correctMatches = in.readHashMap(String.class.getClassLoader());
    }

    public static final Parcelable.Creator<MatchingPairTask> CREATOR = new Parcelable.Creator<>() {
        @Override
        public MatchingPairTask createFromParcel(Parcel in) {
            return new MatchingPairTask(in);
        }

        @Override
        public MatchingPairTask[] newArray(int size) {
            return new MatchingPairTask[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeStringList(leftItems);
        dest.writeStringList(rightItems);
        dest.writeMap(correctMatches);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public List<String> getLeftItems() { return leftItems; }
    public void setLeftItems(List<String> leftItems) { this.leftItems = leftItems; }

    public List<String> getRightItems() { return rightItems; }
    public void setRightItems(List<String> rightItems) { this.rightItems = rightItems; }

    public Map<String, String> getCorrectMatches() { return correctMatches; }
    public void setCorrectMatches(Map<String, String> correctMatches) { this.correctMatches = correctMatches; }
}
