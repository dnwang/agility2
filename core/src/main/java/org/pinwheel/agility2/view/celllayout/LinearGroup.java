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
public class LinearGroup extends CellGroup {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private int divider;
    private int orientation;

    public LinearGroup(int orientation) {
        super();
        this.orientation = orientation;
    }

    @Override
    protected void setSize(int width, int height) {
        super.setSize(width, height);
        final int size = getCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCell(i);
            Params p = (LinearGroup.Params) cell.getParams();
            if (null == p) {
                cell.setSize(0, 0);
            } else {
                if (HORIZONTAL == orientation) {
                    cell.setSize(p.width, height);
                } else if (VERTICAL == orientation) {
                    cell.setSize(width, p.height);
                } else {
                    cell.setSize(p.width, p.height);
                }
            }
        }
    }

    @Override
    protected void setPosition(int x, int y) {
        super.setPosition(x, y);
        final int size = getCellCount();
        int tmp = 0;
        for (int i = 0; i < size; i++) {
            Cell cell = getCell(i);
            Params p = (LinearGroup.Params) cell.getParams();
            if (null == p) {
                cell.setPosition(x, y);
            } else {
                if (HORIZONTAL == orientation) {
                    cell.setPosition(tmp, 0);
                    tmp += cell.getWidth();
                } else if (VERTICAL == orientation) {
                    cell.setPosition(0, tmp);
                    tmp += cell.getHeight();
                } else {
                    cell.setPosition(0, 0);
                }
            }
        }
    }

    public static class Params extends CellGroup.Params {
        public Params(int width, int height) {
            super(width, height);
        }
    }

}