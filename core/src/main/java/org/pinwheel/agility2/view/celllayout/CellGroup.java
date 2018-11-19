package org.pinwheel.agility2.view.celllayout;

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

    CellGroup() {
        super();
    }

    public void addCell(Cell cell) {
        addCell(cell, getDefaultParams());
    }

    protected CellGroup.Params getDefaultParams() {
        return new Params();
    }

    public void addCell(Cell cell, Params p) {
        final long id = null == cell ? -1 : cell.getId();
        if (id <= 0) {
            throw new IllegalStateException("cell id error !");
        }
        if (null == p) {
            throw new IllegalStateException("cell must be have Params !");
        }
        if (null != cell.getOwner()) {
            throw new IllegalStateException("already has owner !");
        }
        cell.setOwner(this);
        cell.setParams(p);
        subCells.add(cell);
    }

    public boolean removeCell(Cell cell) {
        if (null == cell) {
            return false;
        }
        final boolean has = subCells.contains(cell);
        if (!has) {
            return false;
        }
        subCells.remove(cell);
        cell.setOwner(null);
        return true;
    }

    @Override
    public void offset(int dx, int dy) {
        super.offset(dx, dy);
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            getCellAt(i).offset(dx, dy);
        }
    }

    public Cell getCellAt(int order) {
        return subCells.get(order);
    }

    public int getSubCellCount() {
        return subCells.size();
    }

    @Override
    public Cell findCellById(long id) {
        Cell target = super.findCellById(id);
        if (null == target) {
            for (Cell cell : subCells) {
                if (cell.getId() == id) {
                    target = cell;
                    break;
                } else if (cell instanceof CellGroup) {
                    target = cell.findCellById(id);
                    if (null != target) {
                        break;
                    }
                }
            }
        }
        return target;
    }

    public final void foreachSubCells(Filter<Cell> filter) {
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            if (filter.call(getCellAt(i))) {
                break;
            }
        }
    }

    public final void foreachAllCells(boolean withGroup, Filter<Cell> filter) {
        _foreachAllCells(withGroup, this, filter);
    }

    private boolean _foreachAllCells(boolean withGroup, CellGroup group, Filter<Cell> filter) {
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
        @Attribute
        public int width, height;
        @Attribute
        public int marginLeft, marginTop, marginRight, marginBottom;

        Params() {
            this(0, 0);
        }

        public Params(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

}