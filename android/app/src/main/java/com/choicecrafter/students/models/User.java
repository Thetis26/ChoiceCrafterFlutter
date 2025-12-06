package com.choicecrafter.studentapp.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import com.choicecrafter.studentapp.utils.Avatar;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class User implements Parcelable {
    private String name;
    private String email;
    private Avatar anonymousAvatar;
    private boolean online;
    private int totalScore;
    private int streak;

    // points gained from learning paths
    private int learningPathPoints;
    // badges awarded for completed learning paths
    private List<String> badges;

    //key = yyyy-MM-dd, value = score of the day
    private Map<String, Long> scores;

    public User() {
        this.anonymousAvatar = new Avatar("Anonymous", "https://example.com/default_avatar.png");
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
        this.scores = new HashMap<>();
        this.learningPathPoints = 0;
        this.badges = new ArrayList<>();
    }

    public User(String name, String email, Avatar anonymousAvatar, Map<String, Long> scores) {
        this.name = name;
        this.email = email;
        this.anonymousAvatar = anonymousAvatar;
        this.scores = scores != null ? scores : new HashMap<>();
        this.totalScore = computeTotalScore();
        this.streak = computeStreak();
        this.learningPathPoints = 0;
        this.badges = new ArrayList<>();
    }

    protected User(Parcel in) {
        name = in.readString();
        email = in.readString();
        anonymousAvatar = in.readParcelable(Avatar.class.getClassLoader());
        online = in.readByte() != 0;
        totalScore = in.readInt();
        streak = in.readInt();
        learningPathPoints = in.readInt();
        badges = in.createStringArrayList();
        if (badges == null) {
            badges = new ArrayList<>();
        }
        scores = new HashMap<>();
        in.readMap(scores, Long.class.getClassLoader());
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getStreak() {
        return streak;
    }

    public Map<String, Long> getScores() {
        return scores;
    }

    public int getLearningPathPoints() {
        return learningPathPoints;
    }

    public List<String> getBadges() {
        return badges;
    }

    public Avatar getAnonymousAvatar() {
        return anonymousAvatar;
    }
    public boolean isOnline() {
        return online;
    }

    public void updateDailyScore(String day) {
        if (!scores.containsKey(day)) {
            scores.put(day, 0L);
            streak = computeStreak();
        }
        scores.compute(day, (k, currentScore) -> currentScore + 1L);
        computeTotalScore();
    }

    public void addLearningPathPoints(int points) {
        this.learningPathPoints += points;
    }

    public void addBadge(String badge) {
        if (badges == null) {
            badges = new ArrayList<>();
        }
        badges.add(badge);
    }

    public void setBadges(List<String> badges) {
        if (this.badges == null) {
            this.badges = new ArrayList<>();
        } else {
            this.badges.clear();
        }
        if (badges != null) {
            for (String badge : badges) {
                if (badge != null && !badge.isEmpty()) {
                    this.badges.add(badge);
                }
            }
        }
    }

    public int computeTotalScore() {
        totalScore = 0;
        for (Long score : scores.values()) {
            totalScore += score.intValue();
        }
        return totalScore;
    }

    public int computeStreak() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        TreeSet<String> sortedDays = new TreeSet<>(scores.keySet());
        int streak = 0;
        Date today = new Date();
        String todayStr = sdf.format(today);
        Log.i("ComputeStreak", "Today: " + todayStr);
        Log.i("ComputeStreak", "Sorted days: " + sortedDays);

        try {
            // Check if today is in the records
            if (sortedDays.contains(todayStr)) {
                streak = 1; // Start streak from today
            } else {
                // Check if yesterday is in the records
                Date yesterday = new Date(today.getTime() - (24 * 60 * 60 * 1000));
                String yesterdayStr = sdf.format(yesterday);
                if (sortedDays.contains(yesterdayStr)) {
                    streak = 1; // Start streak from yesterday
                } else {
                    return 0; // No streak if neither today nor yesterday has activity
                }
            }

            // Calculate streak backward from today or yesterday
            Date previousDate = sdf.parse(todayStr);
            for (String day : sortedDays.descendingSet()) {
                Date currentDate = sdf.parse(day);
                long diff = (previousDate.getTime() - currentDate.getTime()) / (1000 * 60 * 60 * 24);

                if (diff == 1) {
                    streak++;
                    previousDate = currentDate;
                } else if (diff > 1) {
                    break; // Streak is broken
                }
            }
        } catch (ParseException e) {
            Log.e("ComputeStreak", "Error parsing date", e);
        }

        return streak;
    }

    public static User extractUserFromDoc(QueryDocumentSnapshot doc) {
        Map<String, Long> scores = new HashMap<>();
        Object scoresObj = doc.get("scores");
        if (scoresObj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) scoresObj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                try {
                    scores.put(String.valueOf(entry.getKey()), Long.parseLong(String.valueOf(entry.getValue())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        Avatar avatar = doc.get("anonymousAvatar", Avatar.class);
        User user = new User(doc.getString("name"), doc.getString("email"), avatar, scores);
        Object badgesObj = doc.get("badges");
        if (badgesObj instanceof List<?> list) {
            List<String> badgeValues = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    badgeValues.add(String.valueOf(entry));
                }
            }
            user.setBadges(badgeValues);
        }
        return user;
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", anonymousAvatar=" + anonymousAvatar +
                ", totalScore=" + totalScore +
                ", streak=" + streak +
                ", learningPathPoints=" + learningPathPoints +
                ", badges=" + badges +
                ", scores=" + scores +
                '}';
    }

    public void setScores(Map<String, Long> scores) {
        Log.i("UserRepository", "Setting scores: " + scores);
        if (scores == null) {
            return;
        }
        if (this.scores == null) {
            this.scores = new HashMap<>();
        } else {
            this.scores.clear();
        }
        this.scores.putAll(scores);
        computeTotalScore();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(email);
        dest.writeParcelable(anonymousAvatar, flags);
        dest.writeByte((byte) (online ? 1 : 0));
        dest.writeInt(totalScore);
        dest.writeInt(streak);
        dest.writeInt(learningPathPoints);
        dest.writeStringList(badges);
        Map<String, Long> scoresToWrite = scores != null ? scores : new HashMap<>();
        dest.writeMap(scoresToWrite);
    }
}
