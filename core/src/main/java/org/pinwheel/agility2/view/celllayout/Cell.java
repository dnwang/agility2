package org.pinwheel.agility2.view.celllayout;

import android.graphics.Rect;

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

    public int paddingLeft, paddingTop, paddingRight, paddingBottom;

    private final long id;
    private int x, y;
    private int width, height;

    private CellGroup.Params p;
    private CellGroup owner;

    public Cell() {
        this.id = ++ID_OFFSET;
    }

    public long getId() {
        return id;
    }

    protected void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    protected void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getLeft() {
        return x;
    }

    public int getTop() {
        return y;
    }

    public int getRight() {
        return x + width;
    }

    public int getBottom() {
        return y + height;
    }

    public Rect getRect() {
        return new Rect(x, y, x + width, y + height);
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

    public void refresh() {
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