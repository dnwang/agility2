package org.pinwheel.agility2.view.celllayout;

import android.widget.LinearLayout;

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
public class LinearGroup extends CellGroup {

    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;

    private int divider;
    private int orientation;

    public LinearGroup(int orientation) {
        super();
        this.orientation = orientation;
        this.divider = 0;
    }

    LinearGroup(JSONObject args) {
        super(args);
        this.orientation = args.optInt("orientation", VERTICAL);
        this.divider = args.optInt("divider", 0);
    }

    @Override
    protected void setSize(int width, int height) {
        super.setSize(width, height);
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            Params p = (LinearGroup.Params) cell.getParams();
            if (HORIZONTAL == orientation) {
                cell.setSize(p.width, height);
            } else {
                cell.setSize(width, p.height);
            }
        }
    }

    @Override
    protected void setPosition(int x, int y) {
        super.setPosition(x, y);
        int tmp;
        if (HORIZONTAL == orientation) {
            tmp = getLeft() + getScrollX();
        } else {
            tmp = getTop() + getScrollY();
        }
        final int size = getSubCellCount();
        for (int i = 0; i < size; i++) {
            Cell cell = getCellAt(i);
            if (HORIZONTAL == orientation) {
                cell.setPosition(tmp, getTop());
                tmp += cell.getWidth();
            } else {
                cell.setPosition(getLeft(), tmp);
                tmp += cell.getHeight();
            }
        }
    }

    public int getOrientation() {
        return orientation;
    }

    public static class Params extends CellGroup.Params {
        public Params(int width, int height) {
            super(width, height);
        }

        Params(JSONObject args) {
            super(args);
        }
    }

}