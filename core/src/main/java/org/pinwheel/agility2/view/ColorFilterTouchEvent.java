package org.pinwheel.agility2.view;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Copyright (C), 2017 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 28/11/2017,21:13
 */
public final class ColorFilterTouchEvent implements View.OnTouchListener {

    private int filterColor;

    public ColorFilterTouchEvent() {
        this(Color.parseColor("#FFDCDCDC"));
    }

    public ColorFilterTouchEvent(int color) {
        this.filterColor = color;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                Drawable bg = v.getBackground();
                if (null != bg) {
                    bg.setColorFilter(filterColor, PorterDuff.Mode.MULTIPLY);
                }
                if (v instanceof TextView) {
                    TextView text = (TextView) v;
                    Drawable[] compoundDrawables = text.getCompoundDrawables();
                    for (Drawable icon : compoundDrawables) {
                        if (null != icon) {
                            icon.setColorFilter(filterColor, PorterDuff.Mode.MULTIPLY);
                        }
                    }
                } else if (v instanceof ImageView) {
                    ImageView image = (ImageView) v;
                    Drawable src = image.getDrawable();
                    if (null != src) {
                        src.setColorFilter(filterColor, PorterDuff.Mode.MULTIPLY);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                Drawable bg = v.getBackground();
                if (null != bg) {
                    bg.clearColorFilter();
                }
                if (v instanceof TextView) {
                    TextView text = (TextView) v;
                    Drawable[] compoundDrawables = text.getCompoundDrawables();
                    for (Drawable icon : compoundDrawables) {
                        if (null != icon) {
                            icon.clearColorFilter();
                        }
                    }
                } else if (v instanceof ImageView) {
                    ImageView image = (ImageView) v;
                    Drawable src = image.getDrawable();
                    if (null != src) {
                        src.clearColorFilter();
                    }
                }
                break;
            }
        }
        return false;
    }
}