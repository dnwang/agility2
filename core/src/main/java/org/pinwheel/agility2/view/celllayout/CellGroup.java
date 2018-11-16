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

    public int getSubCellCount() {
        return subCells.size();
    }

    private int scrollX, scrollY;

    public void scrollBy(int dx, int dy) {
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

    public void scrollTo(int x, int y) {
        if (scrollX == x && scrollY == y) {
            return;
        }
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

    public int getScrollX() {
        return scrollX;
    }

    public int getScrollY() {
        return scrollY;
    }

    public final Cell findCellById(long id) {
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

    public final void foreachSubCells(Filter filter) {
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            if (filter.call(getCellAt(i))) {
                break;
            }
        }
    }

    public final void foreachAllCells(boolean withGroup, Filter filter) {
        _foreachAllCells(withGroup, this, filter);
    }

    private boolean _foreachAllCells(boolean withGroup, CellGroup group, Filter filter) {
        boolean intercept = withGroup && filter.call(group);
        if (intercept) {
            return true;
        }
        final int size = group.getSubCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = group.getCellAt(i);
            if (cell instanceof CellGroup) {
                intercept = _foreachAllCells(withGroup, (CellGroup) cell, filter);
            } else {
                intercept = filter.call(cell);
            }
            if (intercept) {
                break;
            }
        }
        return intercept;
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