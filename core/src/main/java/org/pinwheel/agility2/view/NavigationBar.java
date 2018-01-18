package org.pinwheel.agility2.view;

import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.HorizontalScrollView;

import org.pinwheel.agility2.adapter.SimplePagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 26/10/2016,00:07
 */
public final class NavigationBar implements ViewPager.OnPageChangeListener {

    private SliderContainer slider;
    private ViewPager viewPager;
    private List<Pair<View, View>> bindMap = new ArrayList<>();

    private ViewGroup autoScrollContainer;

    public NavigationBar setViewPager(ViewPager viewPager) {
        if (null != this.viewPager) {
            this.viewPager.removeOnPageChangeListener(this);
            PagerAdapter adapter = this.viewPager.getAdapter();
            if (adapter instanceof SimplePagerAdapter) {
                ((SimplePagerAdapter) adapter).removeAll();
            }
        }
        bindMap.clear();
        this.viewPager = viewPager;
        this.viewPager.addOnPageChangeListener(this);
        this.viewPager.setAdapter(new SimplePagerAdapter());
        return this;
    }

    public NavigationBar setSlider(SliderContainer slider) {
        this.slider = slider;
        return this;
    }

    public NavigationBar bind(View naviBtn, View page) {
        if (null != naviBtn && null != page) {
            bindMap.add(new Pair<>(naviBtn, page));
            ((SimplePagerAdapter) viewPager.getAdapter()).add(page).notifyDataSetChanged();
            naviBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewPager.setCurrentItem(getIndexByNaviBtn(v));
                }
            });
        }
        return this;
    }

    public NavigationBar select(int index) {
        selectedNaviBtn(index);
        viewPager.setCurrentItem(index);
        return this;
    }

    public int getCurrIndex() {
        return viewPager.getCurrentItem();
    }

    public View getCurrPage() {
        return ((SimplePagerAdapter) viewPager.getAdapter()).getItem(getCurrIndex());
    }

    public NavigationBar setNaviBtnsAutoScrollContainer(ViewGroup autoScrollContainer) {
        this.autoScrollContainer = autoScrollContainer;
        return this;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        selectedNaviBtn(position);
        Pair<View, View> selectedPair = bindMap.get(position);
        if (null != autoScrollContainer && autoScrollContainer instanceof HorizontalScrollView) {
            final int[] location = new int[2];
            selectedPair.first.getLocationInWindow(location);
            ((HorizontalScrollView) autoScrollContainer).smoothScrollTo(location[0], 0);
        }
        final int size = bindMap.size();
        for (int i = 0; i < size; i++) {
            Pair<View, View> pair = bindMap.get(i);
            if (pair.second instanceof Selectable) {
                ((Selectable) pair.second).onSelected(i == position);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void selectedNaviBtn(int index) {
        View selectedItem = null;
        final int size = bindMap.size();
        for (int i = 0; i < size; i++) {
            Pair<View, View> pair = bindMap.get(i);
            pair.first.setSelected(index == i);
            if (pair.first instanceof Checkable) {
                ((Checkable) pair.first).setChecked(index == i);
            }
            if (index == i) {
                selectedItem = pair.first;
            }
        }
        if (null != slider) {
            slider.setPosition(selectedItem);
        }
    }

    private int getIndexByNaviBtn(View naviBtn) {
        final int size = bindMap.size();
        for (int i = 0; i < size; i++) {
            Pair<View, View> pair = bindMap.get(i);
            if (pair.first == naviBtn) {
                return i;
            }
        }
        return -1;
    }

    private int getIndexByPage(View page) {
        final int size = bindMap.size();
        for (int i = 0; i < size; i++) {
            Pair<View, View> pair = bindMap.get(i);
            if (pair.second == page) {
                return i;
            }
        }
        return -1;
    }

}