package com.choicecrafter.studentapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.choicecrafter.studentapp.MainActivity;
import com.choicecrafter.studentapp.R;
import com.choicecrafter.studentapp.models.Course;
import com.choicecrafter.studentapp.repositories.CourseRepository;

import java.util.List;

public class LoadingActivity extends AppCompatActivity {
    ProgressBar progressBar;
    TextView textProgress;
    private final CourseRepository courseRepository = new CourseRepository();
    private boolean isAnimationComplete = false;
    private boolean isCoursesLoaded = false;
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        progressBar = findViewById(R.id.progress);
        textProgress = findViewById(R.id.textProgress);
        progressBar.setMax(100);
        progressBar.setScaleY(3f);

        progressAnimation();
        loadCoursesData();
    }

    public void progressAnimation(){
        ProgressBarAnimation anim = new ProgressBarAnimation(progressBar, textProgress, 0f, 100f, () -> {
            isAnimationComplete = true;
            maybeNavigateToMain();
        });
        anim.setDuration(5000);
        progressBar.startAnimation(anim);
    }

    private void loadCoursesData() {
        courseRepository.fetchCourses(true, new CourseRepository.Callback<List<Course>>() {
            @Override
            public void onSuccess(List<Course> result) {
                runOnUiThread(() -> {
                    isCoursesLoaded = true;
                    maybeNavigateToMain();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    isCoursesLoaded = true;
                    maybeNavigateToMain();
                });
            }
        });
    }

    private synchronized void maybeNavigateToMain() {
        if (hasNavigated) {
            return;
        }
        if (isAnimationComplete && isCoursesLoaded) {
            hasNavigated = true;
            startActivity(new Intent(LoadingActivity.this, MainActivity.class));
            finish();
        }
    }
}