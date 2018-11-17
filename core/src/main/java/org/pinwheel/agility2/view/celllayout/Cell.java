package org.pinwheel.agility2.view.celllayout;

import android.graphics.Rect;

import org.json.JSONObject;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,13:35
 */
public class Cell {

    private static long ID_OFFSET = 0;
    private final long id;
    //
    private CellDirector director;
    //
    @Attribute
    public int paddingLeft, paddingTop, paddingRight, paddingBottom;
    //
    private int x, y;
    private int width, height;
    //
    private boolean stateVisible;
    //
    private CellGroup owner;
    private CellGroup.Params p;

    public Cell() {
        this.id = ++ID_OFFSET;
    }

    Cell(JSONObject args) {
        this();
        final int padding = args.optInt("padding", 0);
        paddingLeft = args.optInt("paddingLeft", padding);
        paddingTop = args.optInt("paddingTop", padding);
        paddingRight = args.optInt("paddingRight", padding);
        paddingBottom = args.optInt("paddingBottom", padding);
    }

    final void attach(CellDirector director) {
        this.director = director;
        this.director.notifyAttached(this);
    }

    final void detach() {
        owner = null;
        stateVisible = false;
        if (null != director) {
            director.notifyDetached(this);
        }
        director = null;
    }

    final void removeFromOwner() {
        if (null != owner) {
            owner.removeCell(this);
        }
    }

    protected void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    protected void setPosition(int x, int y) {
        if (this.x == x && this.y == y) {
            return;
        }
        int fromX = this.x;
        int fromY = this.y;
        this.x = x;
        this.y = y;
        director.notifyPositionChanged(this, fromX, fromY);
        // tak the root cell as a reference
        Cell root = director.getRoot();
        updateVisible(root.getLeft(), root.getTop(), root.getRight(), root.getBottom());
    }

    protected final void setParams(CellGroup.Params p) {
        this.p = p;
    }

    protected final void setOwner(CellGroup owner) {
        this.owner = owner;
    }

    protected void updateVisible(int left, int top, int right, int bottom) {
        final int l = getLeft(), t = getTop(), r = getRight(), b = getBottom();
        if (right > left && bottom > top && r > l && b > t) {
            if (r < left || b < top || l > right || t > bottom) {
                setVisible(false);
            } else {
                setVisible(true);
            }
        } else {
            setVisible(false);
        }
    }

    private void setVisible(boolean is) {
        if (stateVisible == is) {
            return;
        }
        stateVisible = is;
        director.notifyVisibleChanged(this);
    }

    public long getId() {
        return id;
    }

    public int getLeft() {
        return x;
    }

    public int getTop() {
        return y;
    }

    public int getRight() {
        return getLeft() + getWidth();
    }

    public int getBottom() {
        return getTop() + getHeight();
    }

    public Rect getRect() {
        return new Rect(getLeft(), getTop(), getRight(), getBottom());
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public final CellGroup.Params getParams() {
        return p;
    }

    public final CellGroup getOwner() {
        return owner;
    }

    public final CellDirector getDirector() {
        return director;
    }

    public final boolean isVisible() {
        return stateVisible;
    }

    public Cell findCellById(long id) {
        return getId() == id ? this : null;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Cell) o).id;
    }

    @Override
    public final int hashCode() {
        return Long.valueOf(id).hashCode();
    }

}