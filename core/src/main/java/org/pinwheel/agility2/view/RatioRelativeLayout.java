package org.pinwheel.agility2.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import org.pinwheel.agility2.utils.CommonTools;

/**
 * Copyright (C), 2017 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 24/10/2017,22:54
 */
public final class RatioRelativeLayout extends RelativeLayout {
    public RatioRelativeLayout(Context context) {
        super(context);
    }

    public RatioRelativeLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RatioRelativeLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ratio = CommonTools.obj2Float(getTag(), -1);
    }

    private float ratio = -1;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (ratio > 0) {
            final int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = (int) (width * ratio);
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
