package com.choicecrafter.students.models.tasks;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents optional supporting content that can accompany a task.
 * The content can be plain text, an image, or both.
 */
public class SupportingContent implements Parcelable {

    private String text;
    private String imageUrl;

    public SupportingContent() {
    }

    public SupportingContent(@Nullable String text, @Nullable String imageUrl) {
        this.text = text;
        this.imageUrl = imageUrl;
    }

    protected SupportingContent(Parcel in) {
        text = in.readString();
        imageUrl = in.readString();
    }

    public static final Creator<SupportingContent> CREATOR = new Creator<SupportingContent>() {
        @Override
        public SupportingContent createFromParcel(Parcel in) {
            return new SupportingContent(in);
        }

        @Override
        public SupportingContent[] newArray(int size) {
            return new SupportingContent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeString(imageUrl);
    }

    @Nullable
    public String getText() {
        return text;
    }

    public void setText(@Nullable String text) {
        this.text = text;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public String toString() {
        return "SupportingContent{" +
                "text='" + text + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
