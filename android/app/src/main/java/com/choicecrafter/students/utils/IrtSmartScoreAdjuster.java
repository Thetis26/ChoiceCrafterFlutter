package com.choicecrafter.students.utils;

import androidx.annotation.Nullable;

import com.choicecrafter.students.models.TaskStats;
import com.choicecrafter.students.models.tasks.MultipleChoiceQuestion;
import com.choicecrafter.students.models.tasks.Task;

import static com.choicecrafter.students.utils.TaskStatsKeyUtils.findStatsForTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements a lightweight 3-parameter logistic model so one-off guesses do not
 * artificially increase the measured ability of a learner.
 */
public final class IrtSmartScoreAdjuster {

    private static final int MAX_ITERATIONS = 15;
    private static final double LEARNING_RATE = 0.8;

    private IrtSmartScoreAdjuster() {
        // Utility class
    }

    public static double estimateAbility(@Nullable List<Task> tasks,
                                         @Nullable Map<String, TaskStats> statsMap) {
        List<IrtItem> items = extractItems(tasks, statsMap);
        if (items.isEmpty()) {
            return 0.0;
        }

        double theta = 0.0;
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            double gradient = 0.0;
            for (IrtItem item : items) {
                double probability = item.probability(theta);
                double slope = item.slope(theta);
                gradient += (item.observed - probability) * slope;
            }
            double step = LEARNING_RATE * gradient;
            theta += step;
            if (Math.abs(step) < 1e-5) {
                break;
            }
        }
        return theta;
    }

    public static double adjustScore(MultipleChoiceQuestion question,
                                     TaskStats stats,
                                     double abilityEstimate) {
        if (question == null || stats == null) {
            return 0.0;
        }
        double observed = clampProbability(stats.resolveScoreRatio());
        if (observed <= 0.0) {
            return 0.0;
        }

        IrtItem item = buildItem(question, stats);
        if (item == null) {
            return observed;
        }

        double probability = item.probability(abilityEstimate);
        double normalized = 0.0;
        if (probability > item.guessing) {
            normalized = (probability - item.guessing) / (1.0 - item.guessing);
        }
        normalized = clampProbability(normalized);
        return observed * normalized;
    }

    private static List<IrtItem> extractItems(@Nullable List<Task> tasks,
                                              @Nullable Map<String, TaskStats> statsMap) {
        List<IrtItem> items = new ArrayList<>();
        if (tasks == null || tasks.isEmpty() || statsMap == null || statsMap.isEmpty()) {
            return items;
        }

        for (Task task : tasks) {
            if (!(task instanceof MultipleChoiceQuestion mcQuestion)) {
                continue;
            }

            TaskStats stats = findStatsForTask(statsMap, task);
            if (stats == null || RapidGuessingDetector.isRapidGuess(stats)) {
                continue;
            }

            IrtItem item = buildItem(mcQuestion, stats);
            if (item == null) {
                continue;
            }
            item.observed = clampProbability(stats.resolveScoreRatio());
            items.add(item);
        }

        return items;
    }

    private static IrtItem buildItem(MultipleChoiceQuestion question, @Nullable TaskStats stats) {
        if (question == null) {
            return null;
        }

        double guessing = computeGuessProbability(question);
        double difficulty = estimateDifficulty(question, stats);
        double discrimination = 1.2;

        return new IrtItem(discrimination, difficulty, guessing);
    }

    private static double computeGuessProbability(MultipleChoiceQuestion question) {
        List<String> options = question.getOptions();
        int optionCount = options != null && !options.isEmpty() ? options.size() : 4;
        int effectiveOptions = Math.max(optionCount, 2);
        double probability = 1.0 / effectiveOptions;
        return clampProbability(probability);
    }

    private static double estimateDifficulty(MultipleChoiceQuestion question, @Nullable TaskStats stats) {
        double difficulty = 0.0;
        List<String> options = question.getOptions();
        if (options != null) {
            difficulty += 0.15 * Math.max(0, options.size() - 4);
        }

        if (stats != null) {
            Integer retries = stats.getRetries();
            if (retries != null) {
                difficulty += 0.1 * Math.min(retries, 5);
            }
            if (Boolean.TRUE.equals(stats.getHintsUsed())) {
                difficulty += 0.2;
            }
        }

        return clampRange(difficulty, -2.0, 2.0);
    }

    private static double clampProbability(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private static double clampRange(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class IrtItem {
        private final double discrimination;
        private final double difficulty;
        private final double guessing;
        private double observed;

        private IrtItem(double discrimination, double difficulty, double guessing) {
            this.discrimination = discrimination;
            this.difficulty = difficulty;
            this.guessing = clampProbability(guessing);
        }

        private double probability(double theta) {
            double exponent = -discrimination * (theta - difficulty);
            double logistic = 1.0 / (1.0 + Math.exp(exponent));
            return guessing + (1.0 - guessing) * logistic;
        }

        private double slope(double theta) {
            double exponent = -discrimination * (theta - difficulty);
            double logistic = 1.0 / (1.0 + Math.exp(exponent));
            double derivativeLogistic = discrimination * logistic * (1.0 - logistic);
            return (1.0 - guessing) * derivativeLogistic;
        }
    }
}
