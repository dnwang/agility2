package org.pinwheel.agility2.view.celllayout;

import android.util.LongSparseArray;

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

    private final LongSparseArray<Cell> subCells;

    public CellGroup() {
        super();
        this.subCells = new LongSparseArray<>();
    }

    public void addCell(Cell cell, Params p) {
        final long id = null == cell ? -1 : cell.getId();
        if (id <= 0) {
            throw new IllegalStateException("cell id error !");
        }
        if (null != cell.getOwner()) {
            throw new IllegalStateException("already has owner !");
        }
        cell.setParams(p);
        cell.setOwner(this);
        subCells.put(id, cell);
    }

    public Cell removeCell(long id) {
        if (id <= 0) {
            throw new IllegalStateException("cell id error !");
        }
        final Cell cell = subCells.get(id);
        if (null == cell) {
            throw new IllegalStateException("group can't find special cell by id ! id: " + id);
        }
        subCells.delete(id);
        cell.setOwner(null);
        return cell;
    }

    public Cell getCell(int index) {
        return subCells.valueAt(index);
    }

    public Cell findCell(long id) {
        Cell cell = subCells.get(id);
        if (null != cell) {
            return cell;
        } else {
            final int size = getCellCount();
            for (int i = 0; i < size; i++) {
                Cell subCell = getCell(i);
                if (subCell instanceof CellGroup) {
                    cell = ((CellGroup) subCell).findCell(id);
                    if (null != cell) {
                        break;
                    }
                }
            }
            return cell;
        }
    }

    public int getCellCount() {
        return subCells.size();
    }

    @Override
    public void refresh() {
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            getCell(i).refresh();
        }
    }

    public static class Params {
        int width, height;
        int marginLeft, marginTop, marginRight, marginBottom;

        public Params(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

}