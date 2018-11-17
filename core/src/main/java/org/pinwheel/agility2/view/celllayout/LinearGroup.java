package org.pinwheel.agility2.view.celllayout;

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

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    @Attribute
    private int divider;
    @Attribute
    private int orientation;

    LinearGroup() {
        this(VERTICAL);
    }

    public LinearGroup(int orientation) {
        super();
        this.orientation = orientation;
        this.divider = 0;
    }

    @Override
    public CellGroup.Params getDefaultParams() {
        return new LinearGroup.Params();
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
        setContentSize();
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
        dx = (VERTICAL == orientation || contentWidth < getWidth()) ? 0 : dx;
        dy = (HORIZONTAL == orientation || contentHeight < getHeight()) ? 0 : dy;
        if (0 == dx && 0 == dy) {
            return;
        }
        // fix dx
        int tmp = scrollX + dx;
        int max = -(contentWidth - getWidth());
        if (tmp > 0) {
            dx = -scrollX;
        } else if (tmp < max) {
            dx = max - scrollX;
        }
        // fix dy
        tmp = scrollY + dy;
        max = -(contentHeight - getHeight());
        if (tmp > 0) {
            dy = -scrollY;
        } else if (tmp < max) {
            dy = max - scrollY;
        }
        // move
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

    public int getContentWidth() {
        return contentWidth;
    }

    public int getContentHeight() {
        return contentHeight;
    }

    private int contentWidth, contentHeight;

    private void setContentSize() {
        contentWidth = 0;
        contentHeight = 0;
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            contentWidth += cell.getWidth();
            contentHeight += cell.getHeight();
        }
        if (HORIZONTAL == orientation) {
            contentWidth += paddingLeft + paddingRight + Math.max(0, size - 1) * divider;
            contentHeight = getHeight();
        } else {
            contentWidth = getWidth();
            contentHeight += paddingTop + paddingBottom + Math.max(0, size - 1) * divider;
        }
    }

    public static class Params extends CellGroup.Params {
        Params() {
            this(0, 0);
        }

        public Params(int width, int height) {
            super(width, height);
        }
    }

}