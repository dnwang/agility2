package org.pinwheel.agility2.view.celllayout;

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
public class GridGroup extends CellGroup {

    private int divider;

    private final int row, column;

    public GridGroup(int row, int column) {
        super();
        this.row = row;
        this.column = column;
        this.divider = 0;
    }

    GridGroup(JSONObject args) {
        super(args);
        this.row = args.optInt("row", 0);
        this.column = args.optInt("column", 0);
        this.divider = args.optInt("divider", 0);
    }

    @Override
    protected void setSize(int width, int height) {
        super.setSize(width, height);
        final int blockW = getWidth() / column;
        final int blockH = getHeight() / row;
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            Params p = (GridGroup.Params) cell.getParams();
            cell.setSize(blockW * p.weightX, blockH * p.weightY);
        }
    }

    @Override
    protected void setPosition(int x, int y) {
        super.setPosition(x, y);
        final int blockW = getWidth() / column;
        final int blockH = getHeight() / row;
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            Params p = (GridGroup.Params) cell.getParams();
            cell.setPosition(
                    getLeft() + getScrollX() + p.x * blockW,
                    getTop() + getScrollY() + p.y * blockH
            );
        }
    }

    public static class Params extends CellGroup.Params {
        int x, y;
        int weightX, weightY;

        public Params(int x, int y, int weightX, int weightY) {
            super(0, 0);
            this.x = x;
            this.y = y;
            this.weightX = weightX;
            this.weightY = weightY;
        }

        Params(JSONObject args) {
            super(args);
            this.x = args.optInt("x", 0);
            this.y = args.optInt("y", 0);
            this.weightX = args.optInt("weightX", 0);
            this.weightY = args.optInt("weightY", 0);
        }
    }
}