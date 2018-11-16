package org.pinwheel.agility2.view.celllayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,11:21
 */
public class CellLayout extends ViewGroup {
    public static final String TAG = "CellLayout";

    public CellLayout(Context context) {
        super(context);
        this.init();
    }

    public CellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CellLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init();
    }

    private List<Holder> cellViewHolder = new ArrayList<>();

    private final CellDirector director = new CellDirector();
    private Adapter adapter;

    private final CellDirector.LifeCycleCallback lifecycle = new CellDirector.LifeCycleCallback() {
        @Override
        public void onAttached(Cell cell) {
            if (!(cell instanceof CellGroup)) {
                Log.e(TAG, "onAttached: " + cell);
                Holder holder = new Holder(adapter.onCreateView(cell), cell);
                addView(holder.view);
                cellViewHolder.add(holder);
            }
        }

        @Override
        public void onVisibleChanged(Cell cell) {
            Log.e(TAG, "onVisibleChanged: " + cell + ", is: " + cell.isVisible());
        }

        @Override
        public void onDetached(Cell cell) {
            Log.e(TAG, "onDetached: " + cell);
            cellViewHolder.remove(new Holder(null, cell));
        }
    };

    private void init() {
        director.setCallback(lifecycle);
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public void setRoot(Cell root) {
        director.setRoot(root);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        director.measure(getMeasuredWidth(), getMeasuredHeight());
        // sync view
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            Holder holder = cellViewHolder.get(i);
            Cell cell = holder.cell;
            holder.view.measure(cell.getWidth(), cell.getHeight());
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        director.layout(l, t, r, b);
        // sync view
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            Holder holder = cellViewHolder.get(i);
            Cell cell = holder.cell;
            holder.view.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
        }
    }

    private static class Holder {
        View view;
        Cell cell;
        Bundle args;

        Holder(View view, Cell cell) {
            this.view = view;
            this.cell = cell;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Holder that = (Holder) o;
            return cell.equals(that.cell);
        }

        @Override
        public int hashCode() {
            return cell.hashCode();
        }
    }

    public interface Adapter {
        View onCreateView(Cell cell);

        void onUpdate(View view, Cell cell);
    }

}