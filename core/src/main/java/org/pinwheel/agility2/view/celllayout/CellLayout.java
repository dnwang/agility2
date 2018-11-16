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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CellLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init();
    }

    private LongSparseArray<View> cellViewHolder = new LongSparseArray<>();

    private final CellDirector director = new CellDirector();
    private Adapter adapter;

    private final CellDirector.LifeCycleCallback lifecycle = new CellDirector.LifeCycleCallback() {
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
    };

    private void init() {
        director.setCallback(lifecycle);
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    public void setRoot(CellGroup root) {
        director.setRoot(root);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!director.hasRoot()) {
            return;
        }
        director.getRoot().setSize(getMeasuredWidth(), getMeasuredHeight());
        // sync view
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View view = getChildAt(i);
            long cellId = cellViewHolder.keyAt(cellViewHolder.indexOfValue(view));
            Cell cell = director.getRoot().findCellById(cellId);
            view.measure(cell.getWidth(), cell.getHeight());
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!director.hasRoot()) {
            return;
        }
        director.getRoot().setPosition(l, t);
        // sync view
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View view = getChildAt(i);
            long cellId = cellViewHolder.keyAt(cellViewHolder.indexOfValue(view));
            Cell cell = director.getRoot().findCellById(cellId);
            view.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }

    private static final int MOVE_SLOP = 10;

    private LinearGroup moveCell;

    private final Point tmpPoint = new Point();

    public boolean isMoving() {
        return null != moveCell;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean superState = super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                tmpPoint.set((int) event.getX(), (int) event.getY());
                findLinearGroupBy(tmpPoint.x, tmpPoint.y);
                break;
            case MotionEvent.ACTION_MOVE:
                if (isMoving()) {
                    int dx = (int) event.getX() - tmpPoint.x;
                    int dy = (int) event.getY() - tmpPoint.y;
                    int absDx = Math.abs(dx);
                    int absDy = Math.abs(dy);
                    if (LinearGroup.HORIZONTAL == moveCell.getOrientation() && absDx > MOVE_SLOP) {
                        moveCell(moveCell, dx, 0);
                    } else if (LinearGroup.VERTICAL == moveCell.getOrientation() && absDy > MOVE_SLOP) {
                        moveCell(moveCell, 0, dy);
                    }
                    tmpPoint.set((int) event.getX(), (int) event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                moveCell = null;
                break;
            default:
                return superState;
        }
        return true;
    }

    protected final void moveCell(CellGroup cell, int dx, int dy) {
        if (0 != dx) {
            if (dx > -cell.getScrollX()) {
                dx = -cell.getScrollX();
            }
        }
        if (0 != dy) {
            if (dy > -cell.getScrollY()) {
                dy = -cell.getScrollY();
            }
        }
        cell.scrollBy(dx, dy);
    }

    private void findLinearGroupBy(final int x, final int y) {
        director.getRoot().foreachAllCells(false, new Filter() {
            @Override
            public boolean call(Cell cell) {
                if (cell.getRect().contains(x, y)) {
                    moveCell = findLinearGroupBy(cell);
                    return true;
                }
                return false;
            }
        });
    }

    private LinearGroup findLinearGroupBy(Cell cell) {
        Cell owner = null != cell ? cell.getOwner() : null;
        if (owner instanceof LinearGroup) {
            return (LinearGroup) owner;
        } else if (null != owner) {
            return findLinearGroupBy(owner);
        } else {
            return null;
        }
    }

    public interface Adapter {
        View onCreateView(Cell cell);

        void onUpdate(View view, Cell cell);
    }

}