package com.choicecrafter.students.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.choicecrafter.students.models.Activity;
import com.choicecrafter.students.models.Recommendation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides a small curated list of evergreen learning resources that can be
 * attached to an activity when explicit recommendations are not available in
 * the course definition. This keeps the recommendations card populated with
 * helpful links students can explore for additional context.
 */
public final class DefaultRecommendationsProvider {

    private DefaultRecommendationsProvider() {
        // Utility class
    }

    /**
     * Ensures the supplied activity has at least a default set of
     * recommendations. If the activity already contains recommendations, the
     * list is left untouched.
     */
    public static void ensureDefaults(@Nullable Activity activity) {
        if (activity == null) {
            return;
        }
        List<Recommendation> recommendations = activity.getRecommendations();
        if (recommendations == null) {
            recommendations = new ArrayList<>();
            activity.setRecommendations(recommendations);
        }
        if (!recommendations.isEmpty()) {
            return;
        }
        recommendations.addAll(buildDefaults(activity));
    }

    @NonNull
    private static List<Recommendation> buildDefaults(@Nullable Activity activity) {
        Map<String, String> orderedResources = new LinkedHashMap<>();

        orderedResources.put("https://www.learncpp.com/", "web");
        orderedResources.put("https://cplusplus.com/doc/tutorial/", "web");
        orderedResources.put("https://www.youtube.com/watch?v=vLnPwxZdW4Y", "youtube");
        orderedResources.put("https://www.cs.cmu.edu/~pattis/15-1XX/common/handouts/usingcpp.pdf", "pdf");

        String topic = "";
        if (activity != null) {
            if (activity.getTitle() != null) {
                topic = activity.getTitle().toLowerCase(Locale.US);
            } else if (activity.getDescription() != null) {
                topic = activity.getDescription().toLowerCase(Locale.US);
            }
        }

        if (topic.contains("variab")) {
            orderedResources.put("https://www.learncpp.com/cpp-tutorial/introduction-to-variables/", "web");
        }

        if (topic.contains("structura") || topic.contains("program")) {
            orderedResources.put("https://www.learncpp.com/cpp-tutorial/first-program/", "web");
        }

        if (topic.contains("control") || topic.contains("conditional")) {
            orderedResources.put("https://www.learncpp.com/cpp-tutorial/basic-if-statements/", "web");
        }

        if (topic.contains("bucla") || topic.contains("loop")) {
            orderedResources.put("https://www.learncpp.com/cpp-tutorial/loops-and-loop-statements/", "web");
        }

        List<Recommendation> defaults = new ArrayList<>();
        for (Map.Entry<String, String> entry : orderedResources.entrySet()) {
            defaults.add(new Recommendation(entry.getValue(), entry.getKey()));
        }
        return defaults;
    }
}
