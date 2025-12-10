package com.choicecrafter.students.models.tasks;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class FillInTheBlank extends Task {

    private String text;
    private List<String> missingSegments;
    private List<Integer> segmentPositions;
    private SupportingContent supportingContent;

    public FillInTheBlank() {
    }

    public FillInTheBlank(String title, String description, String type, String status, String blankedText, List<String> missingSegments, List<Integer> missingSegmentPositions) {
        super(title, description, type, status);
        this.text = blankedText;
        this.missingSegments = missingSegments;
        this.segmentPositions = missingSegmentPositions;
    }

    protected FillInTheBlank(Parcel in) {
        super(in);
        text = in.readString();
        missingSegments = in.createStringArrayList();
        segmentPositions = in.readArrayList(Integer.class.getClassLoader());
        supportingContent = in.readParcelable(SupportingContent.class.getClassLoader());
    }

    public static final Parcelable.Creator<FillInTheBlank> CREATOR = new Parcelable.Creator<FillInTheBlank>() {
        @Override
        public FillInTheBlank createFromParcel(Parcel in) {
            return new FillInTheBlank(in);
        }

        @Override
        public FillInTheBlank[] newArray(int size) {
            return new FillInTheBlank[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(text);
        dest.writeStringList(missingSegments);
        dest.writeList(segmentPositions);
        dest.writeParcelable(supportingContent, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getMissingSegments() {
        return missingSegments;
    }

    public void setMissingSegments(List<String> missingSegments) {
        this.missingSegments = missingSegments;
    }

    public List<Integer> getSegmentPositions() {
        return segmentPositions;
    }

    public void setSegmentPositions(List<Integer> segmentPositions) {
        this.segmentPositions = segmentPositions;
    }

    public SupportingContent getSupportingContent() {
        return supportingContent;
    }

    public void setSupportingContent(SupportingContent supportingContent) {
        this.supportingContent = supportingContent;
    }
}