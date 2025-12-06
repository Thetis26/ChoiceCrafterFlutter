package com.choicecrafter.studentapp.models.tasks;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public abstract class Task implements Parcelable {

    private String id;
    private String title;
    private String description;
    private String type;
    private String status;
    private String explanation;

    public Task() {
    }

    public Task(String title, String description, String type, String status) {
        this(title, description, type, status, null);
    }

    public Task(String title, String description, String type, String status, String explanation) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.status = status;
        this.explanation = explanation;
    }

    protected Task(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        type = in.readString();
        status = in.readString();
        explanation = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(type);
        dest.writeString(status);
        dest.writeString(explanation);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Task> CREATOR = new Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel in) {
            return new Task(in) {};
        }

        @Override
        public Task[] newArray(int size) {
            return new Task[size];
        }
    };

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    @Exclude
    public static Task fromDocumentSnapshot(DocumentSnapshot document) {
        String type = document.getString("type");
        if ("multiple_choice".equals(type) || "MultipleChoice".equals(type)) {
            return document.toObject(MultipleChoiceQuestion.class);
        } else if ("fill_in_the_blank".equals(type) || "FillInTheBlank".equals(type)) {
            return document.toObject(FillInTheBlank.class);
        } else if ("matching_pair".equals(type) || "MatchingPair".equals(type)) {
            return document.toObject(MatchingPairTask.class);
        } else if ("ordering".equals(type) || "Ordering".equals(type)) {
            return document.toObject(OrderingTask.class);
        } else if ("info_card".equals(type) || "InfoCard".equals(type)) {
            return document.toObject(InfoCardTask.class);
        } else if ("true_false".equals(type) || "TrueFalse".equals(type) || "true_or_false".equals(type)) {
            return document.toObject(TrueFalseTask.class);
        } else if ("spot_the_error".equals(type) || "SpotError".equals(type) || "spot_error".equals(type)) {
            return document.toObject(SpotTheErrorTask.class);
        } else if ("coding_challenge".equals(type) || "CodingChallenge".equals(type)) {
            return document.toObject(CodingChallengeTask.class);
        } else {
            throw new IllegalArgumentException("Unknown task type: " + type);
        }
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}