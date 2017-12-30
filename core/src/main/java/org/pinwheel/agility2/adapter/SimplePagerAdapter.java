package org.pinwheel.agility2.adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C), 2015 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 */
public final class SimplePagerAdapter extends PagerAdapter {

    private ArrayList<View> mViews;

    public SimplePagerAdapter() {
        mViews = new ArrayList<View>(0);
    }

    public SimplePagerAdapter(List<? extends View> views) {
        mViews = new ArrayList<View>(views);
    }

    public SimplePagerAdapter add(View v) {
        mViews.add(v);
        return this;
    }

    public SimplePagerAdapter add(int index, View v) {
        mViews.add(index, v);
        return this;
    }

    public SimplePagerAdapter addAll(int index, List<View> v) {
        mViews.addAll(index, v);
        return this;
    }

    public SimplePagerAdapter remove(int index) {
        mViews.remove(index);
        return this;
    }

    public SimplePagerAdapter removeAll() {
        mViews.clear();
        return this;
    }

    public View getItem(int index) {
        if (index < 0 || index >= mViews.size()) {
            return null;
        }
        return mViews.get(index);
    }

    public int getIndexOfItem(View view) {
        return mViews.indexOf(view);
    }

    @Override
    public int getCount() {
        return mViews.size();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View v = mViews.get(position);
        container.addView(v);
        return v;
    }
}
