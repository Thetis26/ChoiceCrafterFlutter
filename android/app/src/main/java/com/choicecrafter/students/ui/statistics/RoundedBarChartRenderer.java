package com.choicecrafter.students.ui.statistics;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class RoundedBarChartRenderer extends BarChartRenderer {

    private final float radius;

    public RoundedBarChartRenderer(BarDataProvider chart,
                                   ChartAnimator animator,
                                   ViewPortHandler viewPortHandler,
                                   float radiusPx) {
        super(chart, animator, viewPortHandler);
        this.radius = radiusPx;
    }

    @Override
    public void initBuffers() {
        super.initBuffers();
    }

    @Override
    protected void drawDataSet(Canvas c, IBarDataSet dataSet, int index) {
        if (mChart == null || mChart.getBarData() == null) {
            return;
        }

        if (mBarBuffers == null || index >= mBarBuffers.length || mBarBuffers[index] == null) {
            initBuffers();
        }

        if (mBarBuffers == null || index >= mBarBuffers.length || mBarBuffers[index] == null) {
            return;
        }
        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        mBarBorderPaint.setColor(dataSet.getBarBorderColor());
        mBarBorderPaint.setStrokeWidth(Utils.convertDpToPixel(dataSet.getBarBorderWidth()));
        boolean drawBorder = dataSet.getBarBorderWidth() > 0f;

        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        BarBuffer buffer = mBarBuffers[index];
        buffer.setPhases(phaseX, phaseY);
        buffer.setDataSet(index);
        buffer.setInverted(mChart.isInverted(dataSet.getAxisDependency()));
        buffer.setBarWidth(mChart.getBarData().getBarWidth());
        buffer.feed(dataSet);

        if (buffer.buffer == null || buffer.size() == 0) return;

        trans.pointValuesToPixel(buffer.buffer);

        boolean isSingleColor = dataSet.getColors().size() == 1;
        if (isSingleColor) {
            mRenderPaint.setColor(dataSet.getColor());
        }

        for (int j = 0; j < buffer.size(); j += 4) {
            float left = buffer.buffer[j];
            float top = buffer.buffer[j + 1];
            float right = buffer.buffer[j + 2];
            float bottom = buffer.buffer[j + 3];

            if (!mViewPortHandler.isInBoundsLeft(right)) continue;
            if (!mViewPortHandler.isInBoundsRight(left)) break;

            if (!isSingleColor) {
                mRenderPaint.setColor(dataSet.getColor(j / 4));
            }

            RectF rect = new RectF(left, top, right, bottom);
            c.drawRoundRect(rect, radius, radius, mRenderPaint);

            if (drawBorder) {
                c.drawRoundRect(rect, radius, radius, mBarBorderPaint);
            }
        }
    }
}