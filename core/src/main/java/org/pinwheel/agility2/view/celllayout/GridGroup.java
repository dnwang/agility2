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
public class GridGroup extends CellGroup {

    private int divider;

    private final int row, column;

    public GridGroup(int row, int column) {
        super();
        this.row = row;
        this.column = column;
    }

    @Override
    protected void setSize(int width, int height) {
        super.setSize(width, height);
        final int size = getCellCount();
        final int blockW = getWidth() / column;
        final int blockH = getHeight() / row;
        for (int i = 0; i < size; i++) {
            Cell cell = getCell(i);
            Params p = (GridGroup.Params) cell.getParams();
            if (null == p) {
                cell.setSize(0, 0);
            } else {
                cell.setSize(blockW * p.weightX, blockH * p.weightY);
            }
        }
    }

    @Override
    protected void setPosition(int x, int y) {
        super.setPosition(x, y);
        final int size = getCellCount();
        final int blockW = getWidth() / column;
        final int blockH = getHeight() / row;
        for (int i = 0; i < size; i++) {
            Cell cell = getCell(i);
            Params p = (GridGroup.Params) cell.getParams();
            if (null == p) {
                cell.setPosition(x, y);
            } else {
                cell.setPosition(x + p.x * blockW, y + p.y * blockH);
            }
        }
    }

    public static class Params extends CellGroup.Params {
        final int x, y;
        final int weightX, weightY;

        public Params(int x, int y, int weightX, int weightY) {
            super(0, 0);
            this.x = x;
            this.y = y;
            this.weightX = weightX;
            this.weightY = weightY;
        }
    }

}