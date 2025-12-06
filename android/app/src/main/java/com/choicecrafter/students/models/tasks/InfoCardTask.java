package com.choicecrafter.studentapp.models.tasks;

import android.os.Parcel;
import android.os.Parcelable;

public class InfoCardTask extends Task {

    private String contentType;
    private String contentText;
    private String mediaUrl;
    private String interactiveUrl;
    private String actionText;

    public InfoCardTask() {
    }

    public InfoCardTask(String title,
                        String description,
                        String type,
                        String status,
                        String contentType,
                        String contentText,
                        String mediaUrl,
                        String interactiveUrl,
                        String actionText) {
        super(title, description, type, status);
        this.contentType = contentType;
        this.contentText = contentText;
        this.mediaUrl = mediaUrl;
        this.interactiveUrl = interactiveUrl;
        this.actionText = actionText;
    }

    protected InfoCardTask(Parcel in) {
        super(in);
        contentType = in.readString();
        contentText = in.readString();
        mediaUrl = in.readString();
        interactiveUrl = in.readString();
        actionText = in.readString();
    }

    public static final Parcelable.Creator<InfoCardTask> CREATOR = new Parcelable.Creator<InfoCardTask>() {
        @Override
        public InfoCardTask createFromParcel(Parcel in) {
            return new InfoCardTask(in);
        }

        @Override
        public InfoCardTask[] newArray(int size) {
            return new InfoCardTask[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(contentType);
        dest.writeString(contentText);
        dest.writeString(mediaUrl);
        dest.writeString(interactiveUrl);
        dest.writeString(actionText);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getInteractiveUrl() {
        return interactiveUrl;
    }

    public void setInteractiveUrl(String interactiveUrl) {
        this.interactiveUrl = interactiveUrl;
    }

    public String getActionText() {
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }
}
