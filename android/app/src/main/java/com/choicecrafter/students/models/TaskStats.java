package com.choicecrafter.students.models;

public class TaskStats {
    private String attemptDateTime;
    private String timeSpent;
    private Integer retries;
    private Boolean success;
    private Boolean hintsUsed;
    private Double completionRatio;
    private Double scoreRatio;

    // No-argument constructor
    public TaskStats() {
    }

    public TaskStats(String attemptDateTime,
                     String timeSpent,
                     Integer retries,
                     Boolean success,
                     Boolean hintsUsed) {
        this(attemptDateTime, timeSpent, retries, success, hintsUsed, null, null);
    }

    public TaskStats(String attemptDateTime,
                     String timeSpent,
                     Integer retries,
                     Boolean success,
                     Boolean hintsUsed,
                     Double completionRatio) {
        this(attemptDateTime, timeSpent, retries, success, hintsUsed, completionRatio, null);
    }

    public TaskStats(String attemptDateTime,
                     String timeSpent,
                     Integer retries,
                     Boolean success,
                     Boolean hintsUsed,
                     Double completionRatio,
                     Double scoreRatio) {
        this.attemptDateTime = attemptDateTime;
        this.timeSpent = timeSpent;
        this.retries = retries;
        this.success = success;
        this.hintsUsed = hintsUsed;
        this.completionRatio = completionRatio;
        this.scoreRatio = scoreRatio;
    }

    public String getAttemptDateTime() {
        return attemptDateTime;
    }

    public void setAttemptDateTime(String attemptDateTime) {
        this.attemptDateTime = attemptDateTime;
    }

    public String getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(String timeSpent) {
        this.timeSpent = timeSpent;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Boolean getHintsUsed() {
        return hintsUsed;
    }

    public void setHintsUsed(Boolean hintsUsed) {
        this.hintsUsed = hintsUsed;
    }

    public Double getCompletionRatio() {
        return completionRatio;
    }

    public void setCompletionRatio(Double completionRatio) {
        this.completionRatio = completionRatio;
    }

    public double resolveCompletionRatio() {
        if (completionRatio != null) {
            return Math.max(0.0, Math.min(1.0, completionRatio));
        }
        if (Boolean.TRUE.equals(success)) {
            return 1.0;
        }
        return 0.0;
    }

    public Double getScoreRatio() {
        return scoreRatio;
    }

    public void setScoreRatio(Double scoreRatio) {
        this.scoreRatio = scoreRatio;
    }

    public double resolveScoreRatio() {
        if (scoreRatio != null) {
            return Math.max(0.0, Math.min(1.0, scoreRatio));
        }
        return resolveCompletionRatio();
    }

    public boolean isCompleted() {
        if (Boolean.TRUE.equals(success)) {
            return true;
        }

        double completion = resolveCompletionRatio();
        if (completion >= 1.0) {
            return true;
        }

        if (completion > 0.0) {
            if (scoreRatio == null) {
                return true;
            }
            if (scoreRatio > 0.0) {
                return true;
            }
        }

        if (scoreRatio != null) {
            return scoreRatio > 0.0;
        }

        return false;
    }

    public boolean isFullyCorrect() {
        return resolveScoreRatio() >= 1.0;
    }
}
