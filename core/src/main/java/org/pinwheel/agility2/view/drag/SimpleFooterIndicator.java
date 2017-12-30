package org.pinwheel.agility2.view.drag;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import org.pinwheel.agility2.utils.UIUtils;
import org.pinwheel.agility2.view.SweetProgress;


/**
 * Copyright (C), 2015 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 */
class SimpleFooterIndicator extends BaseDragIndicator {

    private SweetProgress progress;

    public SimpleFooterIndicator(Context context) {
        super(context);
        this.init();
    }

    public SimpleFooterIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public SimpleFooterIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    private void init() {
        progress = new SweetProgress(getContext());
        final int edges = UIUtils.dip2px(getContext(), 28);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(edges, edges);
        params.gravity = Gravity.CENTER;
        int margin = UIUtils.dip2px(getContext(), 8);
        params.setMargins(0, margin, 0, margin);
        addView(progress, params);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                moveTo(0.0f);
                getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        progress.setVisibility(visibility);
    }

    @Override
    public void setBackgroundColor(int color) {
        progress.setBackgroundColor(color);
    }

    @Override
    public void onMove(float distance, float offset) {
        final int position = getDraggable().getPosition();
        final int state = getDraggable().getState();
        if (position != Draggable.EDGE_BOTTOM || state == Draggable.STATE_INERTIAL) {
            return;
        }
        final int height = getMeasuredHeight();
        final float percent = Math.min(Math.abs(distance), height) / height;

        moveTo(percent);
    }

    @Override
    public void onHold() {
        super.onHold();
        progress.spin();
    }

    @Override
    public void reset() {
        super.reset();
        moveTo(0.0f);
    }

    private void moveTo(float percent) {
        setTranslationY(getMeasuredHeight() * (1 - percent));
        if (!isHolding()) {
            progress.setProgress(percent);
        }
    }

}
