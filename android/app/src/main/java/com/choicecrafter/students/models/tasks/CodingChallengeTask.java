package com.choicecrafter.studentapp.models.tasks;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CodingChallengeTask extends Task {

    private String problemDescription;
    @SerializedName("expectedOutput")
    private String expectedOutputDescription;
    private List<CodingChallengeExample> examples = new ArrayList<>();
    @SerializedName(value = "starterCode", alternate = {"starterTemplates"})
    private Map<String, String> starterCodeByLanguage = new HashMap<>();
    @SerializedName(value = "solutionCode", alternate = {"solutions"})
    private Map<String, String> solutionCodeByLanguage = new HashMap<>();
    private String solutionInput;
    private String defaultLanguage;

    public CodingChallengeTask() {
    }

    public CodingChallengeTask(String title,
                               String description,
                               String type,
                               String status,
                               String problemDescription,
                               @Nullable String expectedOutputDescription,
                               @Nullable List<CodingChallengeExample> examples,
                               @Nullable Map<String, String> starterCodeByLanguage,
                               @Nullable Map<String, String> solutionCodeByLanguage,
                               @Nullable String solutionInput,
                               @Nullable String defaultLanguage,
                               @Nullable String explanation) {
        super(title, description, type, status, explanation);
        this.problemDescription = problemDescription;
        this.expectedOutputDescription = expectedOutputDescription;
        if (examples != null) {
            this.examples = new ArrayList<>(examples);
        }
        if (starterCodeByLanguage != null) {
            this.starterCodeByLanguage = new HashMap<>(starterCodeByLanguage);
        }
        if (solutionCodeByLanguage != null) {
            this.solutionCodeByLanguage = new HashMap<>(solutionCodeByLanguage);
        }
        this.solutionInput = solutionInput;
        this.defaultLanguage = defaultLanguage;
    }

    protected CodingChallengeTask(Parcel in) {
        super(in);
        problemDescription = in.readString();
        expectedOutputDescription = in.readString();
        examples = in.createTypedArrayList(CodingChallengeExample.CREATOR);
        int size = in.readInt();
        starterCodeByLanguage = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            String value = in.readString();
            if (key != null && value != null) {
                starterCodeByLanguage.put(key, value);
            }
        }
        int solutionsSize = in.readInt();
        solutionCodeByLanguage = new HashMap<>();
        for (int i = 0; i < solutionsSize; i++) {
            String key = in.readString();
            String value = in.readString();
            if (key != null && value != null) {
                solutionCodeByLanguage.put(key, value);
            }
        }
        solutionInput = in.readString();
        defaultLanguage = in.readString();
    }

    public static final Creator<CodingChallengeTask> CREATOR = new Creator<CodingChallengeTask>() {
        @Override
        public CodingChallengeTask createFromParcel(Parcel in) {
            return new CodingChallengeTask(in);
        }

        @Override
        public CodingChallengeTask[] newArray(int size) {
            return new CodingChallengeTask[size];
        }
    };

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(problemDescription);
        dest.writeString(expectedOutputDescription);
        dest.writeTypedList(examples);
        dest.writeInt(starterCodeByLanguage != null ? starterCodeByLanguage.size() : 0);
        if (starterCodeByLanguage != null) {
            for (Map.Entry<String, String> entry : starterCodeByLanguage.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeString(entry.getValue());
            }
        }
        dest.writeInt(solutionCodeByLanguage != null ? solutionCodeByLanguage.size() : 0);
        if (solutionCodeByLanguage != null) {
            for (Map.Entry<String, String> entry : solutionCodeByLanguage.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeString(entry.getValue());
            }
        }
        dest.writeString(solutionInput);
        dest.writeString(defaultLanguage);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    @Nullable
    public String getExpectedOutputDescription() {
        return expectedOutputDescription;
    }

    public void setExpectedOutputDescription(@Nullable String expectedOutputDescription) {
        this.expectedOutputDescription = expectedOutputDescription;
    }

    @NonNull
    public List<CodingChallengeExample> getExamples() {
        return examples != null ? examples : new ArrayList<>();
    }

    public void setExamples(@Nullable List<CodingChallengeExample> examples) {
        this.examples = examples != null ? new ArrayList<>(examples) : new ArrayList<>();
    }

    @NonNull
    public Map<String, String> getStarterCodeByLanguage() {
        Map<String, String> normalized = new HashMap<>();
        if (starterCodeByLanguage != null) {
            for (Map.Entry<String, String> entry : starterCodeByLanguage.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                normalized.put(entry.getKey().toLowerCase(Locale.US), entry.getValue());
            }
        }
        return normalized;
    }

    public void setStarterCodeByLanguage(@Nullable Map<String, String> starterCodeByLanguage) {
        this.starterCodeByLanguage = new HashMap<>();
        if (starterCodeByLanguage != null) {
            for (Map.Entry<String, String> entry : starterCodeByLanguage.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String key = entry.getKey().toLowerCase(Locale.US);
                this.starterCodeByLanguage.put(key, entry.getValue());
            }
        }
    }

    @NonNull
    public Map<String, String> getSolutionCodeByLanguage() {
        Map<String, String> normalized = new HashMap<>();
        if (solutionCodeByLanguage != null) {
            for (Map.Entry<String, String> entry : solutionCodeByLanguage.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                normalized.put(entry.getKey().toLowerCase(Locale.US), entry.getValue());
            }
        }
        return normalized;
    }

    public void setSolutionCodeByLanguage(@Nullable Map<String, String> solutionCodeByLanguage) {
        this.solutionCodeByLanguage = new HashMap<>();
        if (solutionCodeByLanguage != null) {
            for (Map.Entry<String, String> entry : solutionCodeByLanguage.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String key = entry.getKey().toLowerCase(Locale.US);
                this.solutionCodeByLanguage.put(key, entry.getValue());
            }
        }
    }

    @Nullable
    public String getSolutionInput() {
        return solutionInput;
    }

    public void setSolutionInput(@Nullable String solutionInput) {
        this.solutionInput = solutionInput;
    }

    @Nullable
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(@Nullable String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
}
