package org.pinwheel.agility2.view.celllayout;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    private static final String TAG = "CellLayout";

    private static final int TRANSITION_ADD = 0;
    private static final int TRANSITION_REMOVE = 1;
    private static final int TRANSITION_UPDATE = 2;

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

    private final CellDirector director = new CellDirector();
    private final ViewManager manager = new ViewManager();

    private final ViewTreeObserver.OnGlobalFocusChangeListener focusListener = new ViewTreeObserver.OnGlobalFocusChangeListener() {
        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
            if (CellLayout.this == newFocus.getParent()) {
                director.setFocusCell(manager.findCellByView(newFocus));
                director.moveToCenter(director.getFocusCell());
            }
        }
    };

    private void init() {
        director.setCallback(manager);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalFocusChangeListener(focusListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnGlobalFocusChangeListener(focusListener);
        super.onDetachedFromWindow();
    }

    public void setAdapter(ViewAdapter adapter) {
        manager.setAdapter(adapter);
    }

    public void setRootCell(Cell root) {
        director.attach(root);
        requestLayout();
    }

    public Cell findCellById(long id) {
        return director.findCellById(id);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.e(TAG, "onMeasure");
        // don't support wrap_content
        director.measure(getMeasuredWidth(), getMeasuredHeight());
        // sync view
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View view = getChildAt(i);
            Cell cell = manager.findCellByView(view);
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
            Cell cell = manager.findCellByView(view);
            if (null != cell) {
                view.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
            }
        }
    }

    private static int MOVE_SLOP = 10;
    private final Point tmpPoint = new Point();
    private Cell touchCell = null;
    private boolean isMoving = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final boolean superState = super.dispatchTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                tmpPoint.set((int) event.getX(), (int) event.getY());
                if (null == touchCell) {
                    touchCell = director.findCellByPosition(tmpPoint.x, tmpPoint.y);
                }
                return true;// can not return superState.
            case MotionEvent.ACTION_MOVE:
                int dx = (int) event.getX() - tmpPoint.x;
                int dy = (int) event.getY() - tmpPoint.y;
                int absDx = Math.abs(dx);
                int absDy = Math.abs(dy);
                if (isMoving || absDx > MOVE_SLOP || absDy > MOVE_SLOP) {
                    isMoving = true;
                    int dir = absDx > absDy ? LinearGroup.HORIZONTAL : LinearGroup.VERTICAL;
                    director.move(director.findLinearGroupBy(touchCell, dir), dx, dy);
                    tmpPoint.set((int) event.getX(), (int) event.getY());
                } else {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return superState;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
//                director.moveToCenter(touchCell);
                touchCell = null;
                isMoving = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                return superState;
            default:
                return superState;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean superState = super.onInterceptTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                return isMoving;
            default:
                return superState;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return super.dispatchKeyEvent(event);
    }

    public interface ViewAdapter {
        int getViewPoolId(@NonNull Cell cell);

        @NonNull
        View onCreateView(@NonNull Cell cell);

        void onBindView(@NonNull View view, @NonNull Cell cell);
    }

    private final class ViewManager implements CellDirector.LifeCycleCallback {
        private ViewAdapter adapter;

        private final SparseArray<ViewPool> cellPoolMap = new SparseArray<>();

        private final Map<Cell, View> cellViews = new HashMap<>();

        void setAdapter(ViewAdapter adapter) {
            this.adapter = adapter;
        }

        View findViewByCellId(long cellId) {
            Set<Map.Entry<Cell, View>> entrySet = cellViews.entrySet();
            for (Map.Entry<Cell, View> entry : entrySet) {
                if (entry.getKey().getId() == cellId) {
                    return entry.getValue();
                }
            }
            return null;
        }

        Cell findCellByView(View view) {
            Set<Map.Entry<Cell, View>> entrySet = cellViews.entrySet();
            for (Map.Entry<Cell, View> entry : entrySet) {
                if (entry.getValue() == view) {
                    return entry.getKey();
                }
            }
            return null;
        }

        @Override
        public void onAttached(Cell cell) {
            Log.d(TAG, "onAttached: " + cell);
        }

        @Override
        public void onPositionChanged(Cell cell, int fromX, int fromY) {
            final View view = findViewByCellId(cell.getId());
            if (null != view) {
                view.offsetLeftAndRight(cell.getLeft() - fromX);
                view.offsetTopAndBottom(cell.getTop() - fromY);
            }
        }

        @Override
        public void onVisibleChanged(Cell cell) {
            if (cell instanceof CellGroup) {
                return;
            }
            Log.d(TAG, "onVisibleChanged: " + cell + ", is: " + cell.isVisible());
            final int poolId = adapter.getViewPoolId(cell);
            ViewPool pool = cellPoolMap.get(poolId);
            if (null == pool) {
                pool = new ViewPool();
                cellPoolMap.put(poolId, pool);
            }
            if (cell.isVisible()) {
                // add
                View view = pool.obtain();
                if (null == view) {
                    view = adapter.onCreateView(cell);
                    pool.add(view);
                }
                adapter.onBindView(view, cell);
                if (CellLayout.this != view.getParent()) {
                    CellLayout.this.addView(view);
                }
                cellViews.put(cell, view);
            } else {
                // remove
                View view = findViewByCellId(cell.getId());
                if (null != view) {
                    CellLayout.this.removeView(view);
                }
            }
        }

        @Override
        public void onDetached(Cell cell) {
            Log.d(TAG, "onDetached: " + cell);
        }

    }

    private static final class ViewPool {
        View obtain() {
            return null;
        }

        void add(View view) {

        }
    }

}