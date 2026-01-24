package com.choicecrafter.students.utils;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Avatar implements Parcelable {
    private static final String LEGACY_PLACEHOLDER_URL = "https://example.com/default_avatar.png";
    private String name;
    private String imageUrl;

    public Avatar() {
        // Default constructor required for calls to DataSnapshot.getValue(Avatar.class)
    }

    public Avatar(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    protected Avatar(Parcel in) {
        name = in.readString();
        imageUrl = in.readString();
    }

    public static final Creator<Avatar> CREATOR = new Creator<Avatar>() {
        @Override
        public Avatar createFromParcel(Parcel in) {
            return new Avatar(in);
        }

        @Override
        public Avatar[] newArray(int size) {
            return new Avatar[size];
        }
    };

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        if (LEGACY_PLACEHOLDER_URL.equals(imageUrl)) {
            return null;
        }
        return imageUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(imageUrl);
    }

    @NonNull
    @Override
    public String toString() {
        return "Avatar{" +
                "name='" + name + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
