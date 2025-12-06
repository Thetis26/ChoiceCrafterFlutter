package com.choicecrafter.studentapp.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.choicecrafter.studentapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lightweight confetti effect that renders colorful falling rectangles.
 */
public class ConfettiView extends View {

    private static final int DEFAULT_CONFETTI_COUNT = 40;
    private static final float MIN_SPEED_DP_PER_SEC = 120f;
    private static final float MAX_SPEED_DP_PER_SEC = 220f;
    private static final float MIN_SIZE_DP = 6f;
    private static final float MAX_SIZE_DP = 12f;

    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<ConfettiPiece> pieces = new ArrayList<>();

    private final float minSpeedPx;
    private final float maxSpeedPx;
    private final float minSizePx;
    private final float maxSizePx;

    private ValueAnimator animator;
    private long lastFrameTimeMs;
    private int confettiCount = DEFAULT_CONFETTI_COUNT;
    private int[] confettiColors;

    public ConfettiView(Context context) {
        this(context, null);
    }

    public ConfettiView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConfettiView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        minSpeedPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_SPEED_DP_PER_SEC, dm);
        maxSpeedPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_SPEED_DP_PER_SEC, dm);
        minSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_SIZE_DP, dm);
        maxSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_SIZE_DP, dm);
        resolveAttributes(context, attrs, defStyleAttr);
    }

    private void resolveAttributes(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        if (attrs == null) {
            confettiColors = getDefaultColors(context);
            return;
        }
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ConfettiView, defStyleAttr, 0);
        confettiCount = array.getInt(R.styleable.ConfettiView_confettiCount, DEFAULT_CONFETTI_COUNT);
        int colorsRes = array.getResourceId(R.styleable.ConfettiView_confettiColors, 0);
        if (colorsRes != 0) {
            confettiColors = context.getResources().getIntArray(colorsRes);
        }
        array.recycle();
        if (confettiColors == null || confettiColors.length == 0) {
            confettiColors = getDefaultColors(context);
        }
    }

    private int[] getDefaultColors(Context context) {
        return context.getResources().getIntArray(R.array.confetti_colors);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            initPieces(w, h);
        }
    }

    private void initPieces(int width, int height) {
        pieces.clear();
        for (int i = 0; i < confettiCount; i++) {
            pieces.add(createPiece(width, height, true));
        }
    }

    private ConfettiPiece createPiece(int width, int height, boolean randomY) {
        float startX = random.nextFloat() * width;
        float startY = randomY ? random.nextFloat() * height : -maxSizePx;
        float speed = minSpeedPx + random.nextFloat() * (maxSpeedPx - minSpeedPx);
        float size = minSizePx + random.nextFloat() * (maxSizePx - minSizePx);
        float rotation = random.nextFloat() * 360f;
        float rotationSpeed = (random.nextFloat() - 0.5f) * 120f; // degrees per second
        int color = confettiColors[random.nextInt(confettiColors.length)];
        return new ConfettiPiece(startX, startY, speed, size, rotation, rotationSpeed, color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (ConfettiPiece piece : pieces) {
            paint.setColor(piece.color);
            canvas.save();
            canvas.rotate(piece.rotation, piece.bounds.centerX(), piece.bounds.centerY());
            canvas.drawRoundRect(piece.bounds, piece.cornerRadius, piece.cornerRadius, paint);
            canvas.restore();
        }
    }

    private void updatePieces(float deltaSeconds) {
        int height = getHeight();
        int width = getWidth();
        for (ConfettiPiece piece : pieces) {
            piece.bounds.offset(0f, piece.speed * deltaSeconds);
            piece.rotation = (piece.rotation + piece.rotationSpeed * deltaSeconds) % 360f;
            if (piece.bounds.top > height) {
                float resetX = random.nextFloat() * width;
                float size = minSizePx + random.nextFloat() * (maxSizePx - minSizePx);
                piece.bounds.set(resetX, -size, resetX + size, -size + size);
                piece.cornerRadius = size / 3f;
                piece.speed = minSpeedPx + random.nextFloat() * (maxSpeedPx - minSpeedPx);
                piece.rotation = random.nextFloat() * 360f;
                piece.rotationSpeed = (random.nextFloat() - 0.5f) * 120f;
                piece.color = confettiColors[random.nextInt(confettiColors.length)];
            }
        }
        invalidate();
    }

    private void ensureAnimator() {
        if (animator != null) {
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2000L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            long now = System.currentTimeMillis();
            if (lastFrameTimeMs == 0L) {
                lastFrameTimeMs = now;
                return;
            }
            float deltaSeconds = (now - lastFrameTimeMs) / 1000f;
            lastFrameTimeMs = now;
            updatePieces(deltaSeconds);
        });
    }

    public void start() {
        ensureAnimator();
        if (animator != null && !animator.isStarted()) {
            if (pieces.isEmpty() && getWidth() > 0 && getHeight() > 0) {
                initPieces(getWidth(), getHeight());
            }
            lastFrameTimeMs = 0L;
            animator.start();
        }
    }

    public void stop() {
        if (animator != null && animator.isStarted()) {
            animator.cancel();
        }
        lastFrameTimeMs = 0L;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    private static final class ConfettiPiece {
        final RectF bounds = new RectF();
        float speed;
        float rotation;
        float rotationSpeed;
        float cornerRadius;
        int color;

        ConfettiPiece(float x, float y, float speed, float size, float rotation, float rotationSpeed, int color) {
            this.speed = speed;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
            this.color = color;
            this.cornerRadius = size / 3f;
            bounds.set(x, y, x + size, y + size);
        }
    }
}
