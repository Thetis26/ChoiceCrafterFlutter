package com.choicecrafter.students.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.choicecrafter.students.models.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Utility methods for building human readable chat titles based on the
 * conversation participants.
 */
public final class ConversationTitleHelper {

    private ConversationTitleHelper() {
    }

    /**
     * Builds a display title by listing the other participants in the
     * conversation. The current user is excluded and, when more than two
     * participants are present, only the first two are shown followed by an
     * ellipsis.
     */
    @NonNull
    public static String buildParticipantTitle(
            @Nullable List<String> participants,
            @Nullable List<String> formerParticipants,
            @Nullable String currentUserId,
            @Nullable Collection<User> knownUsers) {
        List<String> otherParticipantIds = collectOtherParticipantIds(
                participants,
                formerParticipants,
                currentUserId);

        if (otherParticipantIds.isEmpty()) {
            return "";
        }

        List<String> displayNames = new ArrayList<>();
        for (String participantId : otherParticipantIds) {
            String displayName = resolveDisplayName(participantId, knownUsers);
            if (TextUtils.isEmpty(displayName)) {
                displayName = participantId;
            }
            if (!TextUtils.isEmpty(displayName)) {
                displayNames.add(displayName);
            }
        }

        if (displayNames.isEmpty()) {
            return "";
        }

        if (displayNames.size() > 2) {
            return displayNames.get(0) + ", " + displayNames.get(1) + ", ...";
        }
        return TextUtils.join(", ", displayNames);
    }

    @NonNull
    private static List<String> collectOtherParticipantIds(
            @Nullable List<String> participants,
            @Nullable List<String> formerParticipants,
            @Nullable String currentUserId) {
        LinkedHashSet<String> orderedIds = new LinkedHashSet<>();
        if (participants != null) {
            for (String participant : participants) {
                if (!TextUtils.isEmpty(participant)) {
                    orderedIds.add(participant);
                }
            }
        }

        List<String> otherParticipantIds = new ArrayList<>();
        for (String participant : orderedIds) {
            if (!TextUtils.isEmpty(participant) && !TextUtils.equals(participant, currentUserId)) {
                otherParticipantIds.add(participant);
            }
        }

        if (!otherParticipantIds.isEmpty()) {
            return otherParticipantIds;
        }

        if (formerParticipants != null) {
            for (String formerParticipant : formerParticipants) {
                if (!TextUtils.isEmpty(formerParticipant)
                        && !TextUtils.equals(formerParticipant, currentUserId)
                        && !otherParticipantIds.contains(formerParticipant)) {
                    otherParticipantIds.add(formerParticipant);
                }
            }
        }

        return otherParticipantIds;
    }

    @Nullable
    private static String resolveDisplayName(@Nullable String userId,
                                             @Nullable Collection<User> knownUsers) {
        if (TextUtils.isEmpty(userId) || knownUsers == null) {
            return null;
        }
        for (User user : knownUsers) {
            if (user == null || TextUtils.isEmpty(user.getEmail())) {
                continue;
            }
            if (user.getEmail().equalsIgnoreCase(userId)) {
                if (!TextUtils.isEmpty(user.getName())) {
                    return user.getName();
                }
                return user.getEmail();
            }
        }
        return null;
    }
}

