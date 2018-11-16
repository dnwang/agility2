package org.pinwheel.agility2.view.celllayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PointF;
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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
            if (!(cell instanceof CellGroup)) {
                Log.e(TAG, "onAttached: " + cell);
                View view = adapter.onCreateView(cell);
                addView(view);
                cellViewHolder.put(cell.getId(), view);
            }
        }

        @Override
        public void onPositionChanged(Cell cell) {
            View view = cellViewHolder.get(cell.getId());
            if (null != view) {
                view.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
            }
        }

        @Override
        public void onVisibleChanged(Cell cell) {
            Log.e(TAG, "onVisibleChanged: " + cell + ", is: " + cell.isVisible());
        }

        @Override
        public void onDetached(Cell cell) {
            Log.e(TAG, "onDetached: " + cell);
            cellViewHolder.remove(cell.getId());
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
        if (!director.hasRoot()) {
            return;
        }
        director.getRoot().setSize(getMeasuredWidth(), getMeasuredHeight());
        // sync view
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View view = getChildAt(i);
            long cellId = cellViewHolder.keyAt(cellViewHolder.indexOfValue(view));
            Cell cell = director.findCellById(cellId);
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
            Cell cell = director.findCellById(cellViewHolder.keyAt(cellViewHolder.indexOfValue(view)));
            view.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
        }
    }

    private static final int MOVE_SLOP = 10;

    private final PointF lastPoint = new PointF();
    private Cell moveTarget;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean superState = super.onInterceptTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return superState;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return superState;
            default:
                return superState;
        }
    }

    private int movingOrientation = -1;
    private int tmpOrientation = -1;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean superState = super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastPoint.set(event.getX(), event.getY());
                findMoveCell((int) lastPoint.x, (int) lastPoint.y);
                if (null != moveTarget) {
                    tmpOrientation = ((LinearGroup) moveTarget).getOrientation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (movingOrientation < 0 || movingOrientation == tmpOrientation) {
                    movingOrientation = tmpOrientation;
                    float xDiff = event.getX() - lastPoint.x;
                    float yDiff = event.getY() - lastPoint.y;
                    float absXDiff = Math.abs(xDiff);
                    float absYDiff = Math.abs(yDiff);
                    if (movingOrientation == LinearGroup.HORIZONTAL && absXDiff > absYDiff) {
                        moveCell(moveTarget, (int) xDiff, 0);
                    } else if (movingOrientation == LinearGroup.VERTICAL && absYDiff > absXDiff) {
                        moveCell(moveTarget, 0, (int) yDiff);
                    }
                }
                lastPoint.set(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                moveTarget = null;
                movingOrientation = -1;
                tmpOrientation = -1;
                break;
            default:
                return superState;
        }
        return true;
    }

    protected final void moveCell(Cell target, int dx, int dy) {
        if (target instanceof CellGroup) {
            ((CellGroup) target).scrollBy(dx, dy);
        }
    }

    private void findMoveCell(final int x, final int y) {
        director.foreachAllCell(director.getRoot(), new CellDirector.CellFilter() {
            @Override
            public boolean call(Cell cell) {
                if (!(cell instanceof CellGroup) && cell.getRect().contains(x, y)) {
                    moveTarget = findLinearGroupBy(cell);
                    return true;
                }
                return false;
            }
        });
    }

    private Cell findLinearGroupBy(Cell cell) {
        Cell owner = null != cell ? cell.getOwner() : null;
        if (owner instanceof LinearGroup) {
            return owner;
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