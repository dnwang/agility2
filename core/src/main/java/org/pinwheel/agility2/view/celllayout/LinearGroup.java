package org.pinwheel.agility2.view.celllayout;

import android.graphics.Rect;
import android.widget.LinearLayout;

import org.json.JSONObject;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,11:32
 */
public class LinearGroup extends CellGroup implements Movable {

    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;

    private int divider;
    private int orientation;

    public LinearGroup(int orientation) {
        super();
        this.orientation = orientation;
        this.divider = 0;
    }

    LinearGroup(JSONObject args) {
        super(args);
        this.orientation = args.optInt("orientation", VERTICAL);
        this.divider = args.optInt("divider", 0);
    }

    @Override
    protected void setSize(int width, int height) {
        super.setSize(width, height);
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            Params p = (LinearGroup.Params) cell.getParams();
            if (HORIZONTAL == orientation) {
                int h = height - paddingTop - paddingBottom - p.marginTop - p.marginBottom;
                cell.setSize(p.width, h);
            } else {
                int w = width - paddingLeft - paddingRight - p.marginLeft - p.marginRight;
                cell.setSize(w, p.height);
            }
        }
    }

    @Override
    protected void setPosition(int x, int y) {
        super.setPosition(x, y);
        int tmp;
        if (HORIZONTAL == orientation) {
            tmp = getLeft() + getScrollX() + paddingLeft;
        } else {
            tmp = getTop() + getScrollY() + paddingTop;
        }
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            Params p = (LinearGroup.Params) cell.getParams();
            if (HORIZONTAL == orientation) {
                tmp += 0 == i ? 0 : divider;
                tmp += p.marginLeft;
                cell.setPosition(tmp, getTop() + paddingTop + p.marginTop);
                tmp += (cell.getWidth() + p.marginRight);
            } else {
                tmp += 0 == i ? 0 : divider;
                tmp += p.marginTop;
                cell.setPosition(getLeft() + paddingLeft + p.marginLeft, tmp);
                tmp += (cell.getHeight() + p.marginBottom);
            }
        }
    }

    public int getOrientation() {
        return orientation;
    }

    private int scrollX, scrollY;

    @Override
    public void scrollBy(int dx, int dy) {
        dx = VERTICAL == orientation ? 0 : dx;
        dy = HORIZONTAL == orientation ? 0 : dy;
        if (0 == dx && 0 == dy) {
            return;
        }
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            cell.setPosition(cell.getLeft() + dx, cell.getTop() + dy);
        }
        scrollX += dx;
        scrollY += dy;
    }

    @Override
    public void scrollTo(int x, int y) {
        if (scrollX == x && scrollY == y) {
            return;
        }
        final int dx = x - (getLeft() + getScrollX());
        final int dy = y - (getTop() + getScrollY());
        scrollBy(dx, dy);
    }

    @Override
    public int getScrollX() {
        return scrollX;
    }

    @Override
    public int getScrollY() {
        return scrollY;
    }

    @Override
    public Rect getContentRect() {
        final Rect rect = new Rect();
        final int size = getSubCellCount();
        int widthSum = 0, heightSum = 0;
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            widthSum += cell.getWidth();
            heightSum += cell.getHeight();
        }
        if (HORIZONTAL == orientation) {
            widthSum += paddingLeft + paddingRight + Math.max(0, size - 1) * divider;
            rect.set(getLeft(), getTop(), widthSum, getBottom());
        } else {
            heightSum += paddingTop + paddingBottom + Math.max(0, size - 1) * divider;
            rect.set(getLeft(), getTop(), getRight(), heightSum);
        }
        rect.offset(getScrollX(), getScrollY());
        return rect;
    }

    public static class Params extends CellGroup.Params {
        public Params(int width, int height) {
            super(width, height);
        }

        Params(JSONObject args) {
            super(args);
        }
    }

}