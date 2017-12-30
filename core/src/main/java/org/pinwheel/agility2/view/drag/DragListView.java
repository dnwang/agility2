package org.pinwheel.agility2.view.drag;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

import org.pinwheel.agility2.utils.UIUtils;


/**
 * Copyright (C), 2015 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 */
public class DragListView extends ListView implements Draggable {

    private static final int DEFAULT_MAX_INERTIA = 48;

    private DragHelper dragHelper;

    private final Movable mover = new Movable() {
        @Override
        public void move(float offset) {
            DragListView.this.setTranslationY(offset);
        }
    };

    public DragListView(Context context) {
        super(context);
        init();
    }

    public DragListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DragListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.dragHelper = new DragHelper(mover);
        this.setOverScrollMode(getOverScrollMode());
//        super.setOverScrollMode(OVER_SCROLL_NEVER);
    }

    @Override
    public void setOverScrollMode(int mode) {
        if (dragHelper == null) {
            super.setOverScrollMode(mode);
            return;
        }
        if (mode == OVER_SCROLL_NEVER) {
            setMaxInertiaDistance(0);
        } else {
            setMaxInertiaDistance(UIUtils.dip2px(getContext(), DEFAULT_MAX_INERTIA));
        }
        super.setOverScrollMode(OVER_SCROLL_NEVER);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastPoint.set(event.getRawX(), event.getRawY());
                if (dragHelper.isHolding()) {
                    resetToBorder();
                    return false;
                } else if (getState() == STATE_INERTIAL) {
                    return false;
                } else {
                    super.dispatchTouchEvent(event);
                    return true;
                }
            default:
                return super.dispatchTouchEvent(event);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int state = getState();
        final float oldDy = getDistance();
        final float absOldDy = Math.abs(oldDy);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                final boolean downSuperState = super.onTouchEvent(event);
                isTouchDragging = false;
                return downSuperState;
            case MotionEvent.ACTION_MOVE:
                final float yDiff = event.getRawY() - lastPoint.y;
//                final float xDiff = event.getRawX() - lastPoint.x;
//                final float absYDiff = Math.abs(yDiff);
//                final float absXDiff = Math.abs(xDiff);
                lastPoint.set(event.getRawX(), event.getRawY());

                if (isTouchDragging) {
                    float offset;
                    if (oldDy * yDiff < 0) {
                        offset = yDiff;
                        if (Math.abs(yDiff) > absOldDy) {
                            offset = (yDiff > 0 ? absOldDy : -absOldDy);
                        }
                    } else {
                        offset = yDiff / (Math.abs(oldDy) / 100 + getRatio());
                    }

                    final float newDy = oldDy + offset;
                    if ((newDy == 0 && absOldDy > 0) || (newDy * oldDy < 0 && absOldDy > 0)) {
                        move(-oldDy);
                        setState(STATE_NONE);
                        final boolean moveSuperState = super.onTouchEvent(event); // (1)
                        isTouchDragging = false; // (2)
                        return moveSuperState; // (3)
                    } else {
                        move(offset);
                        return true;
                    }
                } else {
                    return super.onTouchEvent(event);
                }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                if (isTouchDragging && absOldDy > 0) {
                    if (isOverHoldPosition()) {
                        switch (state) {
                            case STATE_DRAGGING_TOP:
                                hold(true);
                                break;
                            case STATE_DRAGGING_BOTTOM:
                                hold(false);
                                break;
                            default:
                                resetToBorder();
                                break;
                        }
                    } else {
                        resetToBorder();
                    }
                }
                final boolean cancelSuperState = super.onTouchEvent(event);
                isTouchDragging = false;
                return cancelSuperState;
            default:
                return super.onTouchEvent(event);
        }
    }

    private final PointF lastPoint = new PointF();
    private boolean isTouchDragging;

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        if (isTouchDragging || Math.abs((int) getDistance()) > 0) {
            return false;
        }
        int absDeltaY = Math.abs(deltaY);
        if (absDeltaY <= 0) {
            return false;
        }
        isTouchDragging = isTouchEvent;
        setPosition(deltaY > 0 ? EDGE_BOTTOM : EDGE_TOP);
        if (isTouchEvent) {
            setState(deltaY > 0 ? STATE_DRAGGING_BOTTOM : STATE_DRAGGING_TOP);
        } else {
            final int maxInertiaDistance = getMaxInertiaDistance();
            if (maxInertiaDistance > 0) {
                deltaY /= getInertiaWeight();
                if (Math.abs(deltaY) > UIUtils.dip2px(getContext(), 12)) {
                    deltaY = deltaY < 0 ? Math.max(-maxInertiaDistance, deltaY) : Math.min(deltaY, maxInertiaDistance);
                    inertial(-deltaY);
                }
            }
        }
        return false;
    }

    @Override
    public int getMaxInertiaDistance() {
        return dragHelper.getMaxInertiaDistance();
    }

    @Override
    public void setMaxInertiaDistance(int maxInertiaDistance) {
        dragHelper.setMaxInertiaDistance(maxInertiaDistance);
    }

    @Override
    public float getResetVelocity() {
        return dragHelper.getResetVelocity();
    }

    @Override
    public void setResetVelocity(float resetVelocity) {
        dragHelper.setResetVelocity(resetVelocity);
    }

    @Override
    public float getInertiaVelocity() {
        return dragHelper.getInertiaVelocity();
    }

    @Override
    public void setInertiaVelocity(float inertiaVelocity) {
        dragHelper.setInertiaVelocity(inertiaVelocity);
    }

    @Override
    public float getInertiaWeight() {
        return dragHelper.getInertiaWeight();
    }

    @Override
    public void setInertiaWeight(float inertiaWeight) {
        dragHelper.setInertiaWeight(inertiaWeight);
    }

    @Override
    public float getInertiaResetVelocity() {
        return dragHelper.getInertiaResetVelocity();
    }

    @Override
    public void setInertiaResetVelocity(float inertiaResetVelocity) {
        dragHelper.setInertiaResetVelocity(inertiaResetVelocity);
    }

    @Override
    public void setRatio(int ratio) {
        dragHelper.setRatio(ratio);
    }

    @Override
    public float getRatio() {
        return dragHelper.getRatio();
    }

    @Override
    public boolean isOverHoldPosition() {
        return dragHelper.isOverHoldPosition();
    }

    @Override
    public void hold(boolean isTopPosition) {
        setSelection(isTopPosition ? 0 : (getAdapter() == null ? 0 : getAdapter().getCount() - 1));
        dragHelper.hold(isTopPosition);
    }

    @Override
    public void resetToBorder() {
        dragHelper.resetToBorder();
    }

    @Override
    public void inertial(int distance) {
        dragHelper.inertial(distance);
    }

    @Override
    public void move(float offset) {
        dragHelper.move(offset);
    }

    @Override
    public void stopMove() {
        dragHelper.stopMove();
    }

    @Override
    public void addOnDragListener(Draggable.OnDragListener listener) {
        dragHelper.addOnDragListener(listener);
    }

    @Override
    public void removeOnDragListener(Draggable.OnDragListener listener) {
        dragHelper.removeOnDragListener(listener);
    }

    @Override
    public void setOrientation(int orientation) {
        dragHelper.setOrientation(orientation);
    }

    @Override
    public int getOrientation() {
        return dragHelper.getOrientation();
    }

    @Override
    public void setHoldDistance(int dTop, int dBottom) {
        dragHelper.setHoldDistance(dTop, dBottom);
    }

    @Override
    public int getTopHoldDistance() {
        return dragHelper.getTopHoldDistance();
    }

    @Override
    public int getBottomHoldDistance() {
        return dragHelper.getBottomHoldDistance();
    }

    @Override
    public void setState(int state) {
        dragHelper.setState(state);
    }

    @Override
    public int getState() {
        return dragHelper.getState();
    }

    @Deprecated
    @Override
    public void setPosition(int position) {
        dragHelper.setPosition(position);
    }

    @Override
    public int getPosition() {
        return dragHelper.getPosition();
    }

    @Override
    public float getDistance() {
        return dragHelper.getDistance();
    }

//    private boolean isAtBottom() {
//        final Adapter adapter = this.getAdapter();
//        if (null == adapter || adapter.isEmpty()) {
//            return true;
//        }
//
//        final int lastItemPosition = adapter.getCount() - 1;
//        final int lastVisiblePosition = getLastVisiblePosition();
//        if (lastVisiblePosition >= lastItemPosition) {
//            final int childIndex = lastVisiblePosition - getFirstVisiblePosition();
//            final int childCount = getChildCount();
//            final int index = Math.min(childIndex, childCount - 1);
//            final View lastVisibleChild = getChildAt(index);
//            if (lastVisibleChild != null) {
//                return lastVisibleChild.getBottom() <= getMeasuredHeight() - getPaddingBottom(); //padding
//            }
//        }
//        return false;
//    }

//    private boolean isAtTop() {
//        final Adapter adapter = this.getAdapter();
//        if (null == adapter || adapter.isEmpty()) {
//            return true;
//        }
//        int firstPos = getFirstVisiblePosition();
//        if (firstPos == 0 && getChildAt(firstPos).getTop() >= getPaddingTop()) {//padding
//            return true;
//        }
//        return false;
//    }

}
