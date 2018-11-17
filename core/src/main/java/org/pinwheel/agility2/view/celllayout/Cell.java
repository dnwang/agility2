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
    public int paddingLeft, paddingTop, paddingRight, paddingBottom;
    //
    private int x, y;
    private int width, height;
    //
    private boolean stateVisible;
    //
    private CellGroup.Params p;
    private CellGroup owner;

    public Cell() {
        this.id = ++ID_OFFSET;
    }

    Cell(JSONObject args) {
        this();
        paddingLeft = args.optInt("paddingLeft", 0);
        paddingTop = args.optInt("paddingTop", 0);
        paddingRight = args.optInt("paddingRight", 0);
        paddingBottom = args.optInt("paddingBottom", 0);
    }

    void attach(CellDirector director) {
        this.director = director;
        this.director.notifyAttached(this);
    }

    void detach() {
        owner = null;
        stateVisible = false;
        if (null != director) {
            director.notifyDetached(this);
        }
        director = null;
    }

    void removeFromOwner() {
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

    protected void setParams(CellGroup.Params p) {
        this.p = p;
    }

    public CellGroup.Params getParams() {
        return p;
    }

    protected void setOwner(CellGroup owner) {
        this.owner = owner;
    }

    public CellGroup getOwner() {
        return owner;
    }

    public CellDirector getDirector() {
        return director;
    }

    public boolean isVisible() {
        return stateVisible;
    }

    public void setVisible(boolean is) {
        if (stateVisible == is) {
            return;
        }
        stateVisible = is;
        director.notifyVisibleChanged(this);
    }

    public Cell findCellById(long id) {
        return getId() == id ? this : null;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Cell) o).id;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(id).hashCode();
    }
}