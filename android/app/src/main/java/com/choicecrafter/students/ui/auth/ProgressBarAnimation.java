package com.choicecrafter.studentapp.ui.auth;

import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProgressBarAnimation extends Animation {
    private final ProgressBar progressBar;
    private final TextView textProgress;
    private final float from;
    private final float to;
    private final Runnable onAnimationComplete;
    private boolean completionNotified = false;

    public ProgressBarAnimation(ProgressBar progressBar,
                                TextView textProgress,
                                float from,
                                float to,
                                Runnable onAnimationComplete) {
        this.progressBar = progressBar;
        this.textProgress = textProgress;
        this.from = from;
        this.to = to;
        this.onAnimationComplete = onAnimationComplete;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        float value = from + (to - from) * interpolatedTime;
        progressBar.setProgress((int) value);
        textProgress.setText((int) value + " %");
        if (!completionNotified && value >= to) {
            completionNotified = true;
            if (onAnimationComplete != null) {
                onAnimationComplete.run();
            }
        }
    }
}

