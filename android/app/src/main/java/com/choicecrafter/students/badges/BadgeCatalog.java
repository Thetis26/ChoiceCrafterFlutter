package com.choicecrafter.studentapp.badges;

import com.choicecrafter.studentapp.models.badges.BadgeDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Centralized catalog that lists all badge definitions.
 */
public final class BadgeCatalog {

    public static final String ID_FIRST_ACTIVITY = "first_activity_completed";
    public static final String ID_FIRST_MODULE = "first_module_completed";
    public static final String ID_STREAK_5 = "streak_5_days";
    public static final String ID_STREAK_10 = "streak_10_days";
    public static final String ID_STREAK_30 = "streak_30_days";
    public static final String ID_STREAK_50 = "streak_50_days";
    public static final String ID_FIRST_COMMENT = "first_feed_comment";
    public static final String ID_CHATBOT_DISCUSSION = "first_chatbot_discussion";

    private static final List<BadgeDefinition> BADGES;
    private static final Map<String, BadgeDefinition> BADGES_BY_ID;
    private static final Map<String, BadgeDefinition> BADGES_BY_TITLE;

    static {
        List<BadgeDefinition> definitions = new ArrayList<>();
        definitions.add(new BadgeDefinition(
                ID_FIRST_ACTIVITY,
                "Prima activitate finalizată",
                "Finalizează orice activitate din cursurile la care ești înscris.",
                20));
        definitions.add(new BadgeDefinition(
                ID_FIRST_MODULE,
                "Primul modul finalizat",
                "Parcurge toate activitățile dintr-un modul de curs.",
                60));
        definitions.add(new BadgeDefinition(
                ID_STREAK_5,
                "Serie de 5 zile",
                "Menține activitatea timp de cinci zile consecutive.",
                48));
        definitions.add(new BadgeDefinition(
                ID_STREAK_10,
                "Serie de 10 zile",
                "Rămâi activ zece zile la rând.",
                80));
        definitions.add(new BadgeDefinition(
                ID_STREAK_30,
                "Serie de 30 de zile",
                "Demonstrează consecvență timp de o lună întreagă.",
                160));
        definitions.add(new BadgeDefinition(
                ID_STREAK_50,
                "Serie de 50 de zile",
                "Devino campionul seriilor cu cincizeci de zile continue.",
                240));
        definitions.add(new BadgeDefinition(
                ID_FIRST_COMMENT,
                "Primul comentariu în feed",
                "Scrie un comentariu la o activitate din feed.",
                32));
        definitions.add(new BadgeDefinition(
                ID_CHATBOT_DISCUSSION,
                "Prima discuție cu asistentul",
                "Adresează o întrebare asistentului de recomandări.",
                48));
        BADGES = Collections.unmodifiableList(definitions);

        Map<String, BadgeDefinition> byId = new HashMap<>();
        Map<String, BadgeDefinition> byTitle = new HashMap<>();
        for (BadgeDefinition definition : BADGES) {
            byId.put(definition.getId(), definition);
            byTitle.put(definition.getTitle().trim().toLowerCase(Locale.US), definition);
        }
        BADGES_BY_ID = Collections.unmodifiableMap(byId);
        BADGES_BY_TITLE = Collections.unmodifiableMap(byTitle);
    }

    private BadgeCatalog() {
    }

    public static List<BadgeDefinition> getAll() {
        return BADGES;
    }

    public static BadgeDefinition findById(String id) {
        if (id == null) {
            return null;
        }
        return BADGES_BY_ID.get(id);
    }

    /**
     * Attempts to normalize a badge value stored in the user document.
     * Values can be stored either as badge IDs or as badge titles.
     *
     * @param storedValue the stored value
     * @return the normalized badge ID if found, {@code null} otherwise
     */
    public static String normalizeBadgeValue(String storedValue) {
        if (storedValue == null) {
            return null;
        }
        String trimmed = storedValue.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        BadgeDefinition byId = BADGES_BY_ID.get(trimmed);
        if (byId != null) {
            return byId.getId();
        }
        BadgeDefinition byTitle = BADGES_BY_TITLE.get(trimmed.toLowerCase(Locale.US));
        if (byTitle != null) {
            return byTitle.getId();
        }
        return null;
    }

    /**
     * Provides a human-readable title for the given stored badge value.
     */
    public static String toDisplayName(String storedValue) {
        String normalized = normalizeBadgeValue(storedValue);
        if (normalized != null) {
            BadgeDefinition definition = findById(normalized);
            if (definition != null) {
                return definition.getTitle();
            }
        }
        return storedValue;
    }
}
