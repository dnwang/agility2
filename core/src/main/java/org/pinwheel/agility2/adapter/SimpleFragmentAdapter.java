package org.pinwheel.agility2.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

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
public final class SimpleFragmentAdapter extends FragmentPagerAdapter {
    private ArrayList<Fragment> fragments;

    public SimpleFragmentAdapter(FragmentManager fm) {
        super(fm);
        this.fragments = new ArrayList<>(0);
    }

    public SimpleFragmentAdapter(FragmentManager fm, List<Fragment> fragmentList) {
        super(fm);
        this.fragments = new ArrayList<>(fragmentList);
    }

    public SimpleFragmentAdapter add(Fragment fragment) {
        if (null != fragment) {
            this.fragments.add(fragment);
        }
        return this;
    }

    public SimpleFragmentAdapter add(Fragment fragment, int index) {
        if (null != fragment && index >= 0) {
            this.fragments.add(index, fragment);
        }
        return this;
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public int getItemPosition(Object object) {
        return super.getItemPosition(object);
    }
}
