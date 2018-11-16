package org.pinwheel.agility2.view.celllayout;

import android.view.ViewGroup;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,14:01
 */
public class CellGroup extends Cell {

    private final List<Cell> subCells = new ArrayList<>();

    public CellGroup() {
        super();
    }

    CellGroup(JSONObject args) {
        super(args);
    }

    void addCellNoAttach(Cell cell, Params p) {
        final long id = null == cell ? -1 : cell.getId();
        if (id <= 0) {
            throw new IllegalStateException("cell id error !");
        }
        if (null != cell.getOwner()) {
            throw new IllegalStateException("already has owner !");
        }
        subCells.add(cell);
        cell.setOwner(this);
        cell.setParams(p);
    }

    public void addCell(Cell cell, Params p) {
        addCellNoAttach(cell, p);
        cell.attach(getDirector());
    }

    public Cell removeCell(Cell cell) {
        if (null == cell) {
            throw new IllegalStateException("can't find empty cell !");
        }
        final boolean has = subCells.contains(cell);
        if (!has) {
            throw new IllegalStateException("group can't find special cell !");
        }
        subCells.remove(cell);
        cell.detach();
        return cell;
    }

    public Cell getCellAt(int order) {
        return subCells.get(order);
    }

    public Cell findCellById(long id) {
        Cell target = getId() == id ? this : null;
        if (null == target) {
            for (Cell cell : subCells) {
                if (cell.getId() == id) {
                    target = cell;
                    break;
                } else if (cell instanceof CellGroup) {
                    target = ((CellGroup) cell).findCellById(id);
                    if (null != target) {
                        break;
                    }
                }
            }
        }
        return target;
    }

    public int getSubCellCount() {
        return subCells.size();
    }

    private int scrollX, scrollY;

    public void scrollBy(int dx, int dy) {
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            cell.setPosition(cell.getLeft() + dx, cell.getTop() + dy);
        }
        scrollX += dx;
        scrollY += dy;
    }

    public void scrollTo(int x, int y) {
        int dx = x - (getLeft() + scrollX);
        int dy = y - (getLeft() + scrollY);
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            cell.setPosition(cell.getLeft() + dx, cell.getTop() + dy);
        }
        scrollX = x;
        scrollY = y;
    }

    @Override
    public int getLeft() {
        return super.getLeft() + getScrollX();
    }

    @Override
    public int getTop() {
        return super.getTop() + getScrollY();
    }

    public int getScrollX() {
        return scrollX;
    }

    public int getScrollY() {
        return scrollY;
    }

    public static class Params {
        int width, height;
        int marginLeft, marginTop, marginRight, marginBottom;

        public Params(int width, int height) {
            this.width = width;
            this.height = height;
        }

        Params(JSONObject args) {
            this.width = args.optInt("width", ViewGroup.LayoutParams.MATCH_PARENT);
            this.height = args.optInt("height", ViewGroup.LayoutParams.MATCH_PARENT);
            this.marginLeft = args.optInt("marginLeft", 0);
            this.marginTop = args.optInt("marginTop", 0);
            this.marginRight = args.optInt("marginRight", 0);
            this.marginBottom = args.optInt("marginBottom", 0);
        }
    }

}