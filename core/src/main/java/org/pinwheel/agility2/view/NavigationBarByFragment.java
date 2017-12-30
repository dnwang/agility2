package org.pinwheel.agility2.view;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.HorizontalScrollView;

import org.pinwheel.agility2.adapter.SimpleFragmentAdapter;

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
 * @see
 */
public final class NavigationBarByFragment implements ViewPager.OnPageChangeListener {

    private SliderContainer slider;
    private ViewPager viewPager;
    private List<Pair<View, Fragment>> bindMap = new ArrayList<>();

    private ViewGroup autoScrollContainer;

    public NavigationBarByFragment setViewPager(ViewPager viewPager) {
        if (null != this.viewPager) {
            this.viewPager.removeOnPageChangeListener(this);
        }
        bindMap.clear();
        this.viewPager = viewPager;
        this.viewPager.addOnPageChangeListener(this);
        this.viewPager.setAdapter(new SimpleFragmentAdapter(findFragmentManager()));
        return this;
    }

    /**
     * Fragment内嵌Fragment需要getChildFragmentManager获取FragmentManager
     */
    public NavigationBarByFragment setViewPager(ViewPager viewPager, FragmentManager fragmentManager) {
        if (null != this.viewPager) {
            this.viewPager.removeOnPageChangeListener(this);
        }
        bindMap.clear();
        this.viewPager = viewPager;
        this.viewPager.addOnPageChangeListener(this);
        this.viewPager.setAdapter(new SimpleFragmentAdapter(fragmentManager));
        return this;
    }

    public NavigationBarByFragment setSlider(SliderContainer slider) {
        this.slider = slider;
        return this;
    }

    private FragmentManager findFragmentManager() {
        Context context = this.viewPager.getContext();
        if (context instanceof FragmentActivity) {
            return ((FragmentActivity) context).getSupportFragmentManager();
        } else {
            return null;
        }
    }

    public NavigationBarByFragment bind(View naviBtn, Fragment fragment) {
        if (null != naviBtn && null != fragment) {
            bindMap.add(new Pair<>(naviBtn, fragment));
            PagerAdapter adapter = viewPager.getAdapter();
            if (adapter instanceof SimpleFragmentAdapter) {
                ((SimpleFragmentAdapter) adapter).add(fragment).notifyDataSetChanged();
            }
            naviBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewPager.setCurrentItem(getIndexByNaviBtn(v));
                }
            });
        }
        return this;
    }

    public NavigationBarByFragment select(int index) {
        selectedNaviBtn(index);
        viewPager.setCurrentItem(index);
        return this;
    }

    public int getCurrIndex() {
        return viewPager.getCurrentItem();
    }

    public Fragment getCurrFragment() {
        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter instanceof SimpleFragmentAdapter) {
            return ((SimpleFragmentAdapter) adapter).getItem(getCurrIndex());
        } else {
            return null;
        }
    }

    public NavigationBarByFragment setNaviBtnsAutoScrollContainer(ViewGroup autoScrollContainer) {
        this.autoScrollContainer = autoScrollContainer;
        return this;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        selectedNaviBtn(position);
        Pair<View, Fragment> selectedPair = bindMap.get(position);
        if (null != autoScrollContainer && autoScrollContainer instanceof HorizontalScrollView) {
            final int[] location = new int[2];
            selectedPair.first.getLocationInWindow(location);
            ((HorizontalScrollView) autoScrollContainer).smoothScrollTo(location[0], 0);
        }
        final int size = bindMap.size();
        for (int i = 0; i < size; i++) {
            Pair<View, Fragment> pair = bindMap.get(i);
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
            Pair<View, Fragment> pair = bindMap.get(i);
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
            Pair<View, Fragment> pair = bindMap.get(i);
            if (pair.first == naviBtn) {
                return i;
            }
        }
        return -1;
    }

    private int getIndexByFragment(Fragment fragment) {
        final int size = bindMap.size();
        for (int i = 0; i < size; i++) {
            Pair<View, Fragment> pair = bindMap.get(i);
            if (pair.second == fragment) {
                return i;
            }
        }
        return -1;
    }

    public interface Selectable {
        void onSelected(boolean isSelected);
    }

}