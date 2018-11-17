package org.pinwheel.agility2.view.celllayout;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,11:21
 */
public class CellLayout extends ViewGroup implements CellDirector.LifeCycleCallback {
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CellLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init();
    }

    private LongSparseArray<View> cellViewHolder = new LongSparseArray<>();

    private final CellDirector director = new CellDirector();
    private Adapter adapter;

    private void init() {
        director.setCallback(this);
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public void setRoot(Cell root) {
        director.attach(root);
        requestLayout();
    }

    public Cell findCellById(long id) {
        return director.findCellById(id);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.e(TAG, "onMeasure");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        director.measure(getMeasuredWidth(), getMeasuredHeight());
        // sync view
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View view = getChildAt(i);
            long cellId = cellViewHolder.keyAt(cellViewHolder.indexOfValue(view));
            Cell cell = director.findCellById(cellId);
            if (null != cell) {
                view.measure(cell.getWidth(), cell.getHeight());
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.e(TAG, "onLayout: " + changed);
        director.layout(changed, l, t, r, b);
        // sync view
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View view = getChildAt(i);
            long cellId = cellViewHolder.keyAt(cellViewHolder.indexOfValue(view));
            Cell cell = director.findCellById(cellId);
            if (null != cell) {
                view.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
            }
        }
    }

    private final Point tmpPoint = new Point();
    private Cell touchFocus = null;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean superState = super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                tmpPoint.set((int) event.getX(), (int) event.getY());
                if (null == touchFocus) {
                    touchFocus = director.findCellByPosition(tmpPoint.x, tmpPoint.y);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (null != touchFocus) {
                    int dx = (int) event.getX() - tmpPoint.x;
                    int dy = (int) event.getY() - tmpPoint.y;
                    int dir = Math.abs(dx) > Math.abs(dy) ? LinearGroup.HORIZONTAL : LinearGroup.VERTICAL;
                    director.move(director.findLinearGroupBy(touchFocus, dir), dx, dy);
                    tmpPoint.set((int) event.getX(), (int) event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchFocus = null;
                break;
            default:
                return superState;
        }
        return true;
    }

    @Override
    public void onAttached(Cell cell) {
        if (cell instanceof CellGroup) {
            return;
        }
        Log.d(TAG, "onAttached: " + cell);
        View view = adapter.onCreateView(cell);
        addView(view);
        cellViewHolder.put(cell.getId(), view);
    }

    @Override
    public void onPositionChanged(Cell cell, int fromX, int fromY) {
        View view = cellViewHolder.get(cell.getId());
        if (null != view) {
            view.offsetLeftAndRight(cell.getLeft() - fromX);
            view.offsetTopAndBottom(cell.getTop() - fromY);
        }
    }

    @Override
    public void onVisibleChanged(Cell cell) {
        Log.d(TAG, "onVisibleChanged: " + cell + ", is: " + cell.isVisible());
    }

    @Override
    public void onDetached(Cell cell) {
        if (cell instanceof CellGroup) {
            return;
        }
        Log.d(TAG, "onDetached: " + cell);
        cellViewHolder.remove(cell.getId());
        // TODO: 2018/11/16
    }

    public interface Adapter {
        View onCreateView(Cell cell);

        void onUpdate(View view, Cell cell);
    }

}