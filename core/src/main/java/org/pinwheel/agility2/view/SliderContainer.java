package org.pinwheel.agility2.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C), 2017 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 03/12/2017,10:22
 */
public final class SliderContainer extends RadioGroup {

    public SliderContainer(Context context) {
        super(context);
        this.init();
    }

    public SliderContainer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    private View slider = null;
    private View target = null;

    private void init() {
        slider = new View(getContext());
        // 0: first child
        addViewInLayout(slider, 0, new ViewGroup.LayoutParams(0, 0));
        registerListenerOnChildClick();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        registerListenerOnChildClick();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        cancelMove();
        super.onLayout(changed, l, t, r, b);
        // selected default
        if (null == target && getChildCount() > 1) {
            target = getChildAt(1);
        }
        if (null != target) {
            slider.layout(target.getLeft(), target.getTop(), target.getRight(), target.getBottom());
        }
    }

    private void registerListenerOnChildClick() {
        // TODO: 03/12/2017
    }

    public void setPosition(View v) {
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            if (v == getChildAt(i)) {
                target = v;
                moveTo(target);
                break;
            }
        }
    }

    public void setSliderResource(@DrawableRes int resId) {
        slider.setBackgroundResource(resId);
    }

    private void cancelMove() {
        if (null != animatorSet && animatorSet.isRunning()) {
            animatorSet.cancel();
        }
    }

    private AnimatorSet animatorSet = null;

    private void moveTo(View target) {
        if (null == target) {
            return;
        }
        cancelMove();
        final List<Animator> animators = new ArrayList<>(4);
        if (slider.getLeft() != target.getLeft()) {
            animators.add(ObjectAnimator.ofInt(slider, "left", slider.getLeft(), target.getLeft()));
        }
        if (slider.getTop() != target.getTop()) {
            animators.add(ObjectAnimator.ofInt(slider, "top", slider.getTop(), target.getTop()));
        }
        if (slider.getRight() != target.getRight()) {
            animators.add(ObjectAnimator.ofInt(slider, "right", slider.getRight(), target.getRight()));
        }
        if (slider.getBottom() != target.getBottom()) {
            animators.add(ObjectAnimator.ofInt(slider, "bottom", slider.getBottom(), target.getBottom()));
        }
        if (!animators.isEmpty()) {
            animatorSet = new AnimatorSet();
            animatorSet.playTogether(animators);
            animatorSet.setDuration(300);
            animatorSet.start();
        }
    }

}