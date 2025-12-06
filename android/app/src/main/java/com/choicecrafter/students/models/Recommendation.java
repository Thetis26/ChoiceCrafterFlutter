package com.choicecrafter.studentapp.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Recommendation implements Parcelable {
    private String type; // e.g., "youtube", "webpage", "firestore"
    private String url;  // URL or Firestore document reference

    public Recommendation() {
        // Default constructor for Firestore
    }
    public Recommendation(String type, String url) {
        this.type = type;
        this.url = url;
    }

    protected Recommendation(Parcel in) {
        type = in.readString();
        url = in.readString();
    }

    public static final Creator<Recommendation> CREATOR = new Creator<Recommendation>() {
        @Override
        public Recommendation createFromParcel(Parcel in) {
            return new Recommendation(in);
        }

        @Override
        public Recommendation[] newArray(int size) {
            return new Recommendation[size];
        }
    };

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(url);
    }
}