package org.pinwheel.agility2.view.celllayout;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/16,8:32
 */
final class CellDirector {

    private Cell root;
    private LifeCycleCallback callback;

    boolean hasRoot() {
        return null != root;
    }

    void attach(Cell cell) {
        detachRoot();// detach old
        root = cell;
        foreachAllCells(true, new Filter() {
            @Override
            public boolean call(Cell cell) {
                cell.attach(CellDirector.this);
                return false;
            }
        });
    }

    private void detachRoot() {
        foreachAllCells(true, new Filter() {
            @Override
            public boolean call(Cell cell) {
                cell.removeFromOwner();
                cell.detach();
                return false;
            }
        });
    }

    Cell getRoot() {
        return root;
    }

    void setCallback(LifeCycleCallback callback) {
        this.callback = callback;
    }

    Cell findCellById(long id) {
        return hasRoot() ? root.findCellById(id) : null;
    }

    private Cell tmp = null;

    Cell findCellByPosition(final int x, final int y) {
        tmp = null;
        foreachAllCells(false, new Filter() {
            @Override
            public boolean call(Cell cell) {
                if (cell.getRect().contains(x, y)) {
                    tmp = cell;
                    return true;
                }
                return false;
            }
        });
        return tmp;
    }

    void foreachAllCells(boolean withGroup, Filter filter) {
        if (hasRoot()) {
            if (root instanceof CellGroup) {
                ((CellGroup) root).foreachAllCells(withGroup, filter);
            } else {
                filter.call(root);
            }
        }
    }

    LinearGroup findLinearGroupBy(Cell cell, final int orientation) {
        Cell owner = null != cell ? cell.getOwner() : null;
        if (owner instanceof LinearGroup
                && orientation == ((LinearGroup) owner).getOrientation()) {
            return (LinearGroup) owner;
        } else if (null != owner) {
            return findLinearGroupBy(owner, orientation);
        } else {
            return null;
        }
    }

    private Cell focusCell = null;

    void setFocusCell(Cell cell) {
        focusCell = cell;
    }

    Cell getFocusCell() {
        return focusCell;
    }

    void move(Movable target, int dx, int dy) {
        if (null == target || 0 == dx || 0 == dy) {
            return;
        }
        target.scrollBy(dx, dy);
    }

    void measure(int width, int height) {
        if (hasRoot()) {
            root.setSize(width, height);
        }
    }

    void layout(boolean changed, int l, int t, int r, int b) {
        if (hasRoot()) {
            root.setPosition(l, t);
        }
    }

    void notifyAttached(Cell cell) {
        callback.onAttached(cell);
    }

    void notifyDetached(Cell cell) {
        callback.onDetached(cell);
    }

    void notifyPositionChanged(Cell cell, int fromX, int fromY) {
        callback.onPositionChanged(cell, fromX, fromY);
    }

    void notifyVisibleChanged(Cell cell) {
        callback.onVisibleChanged(cell);
    }

    interface LifeCycleCallback {
        void onAttached(Cell cell);

        void onPositionChanged(Cell cell, int fromX, int fromY);

        void onVisibleChanged(Cell cell);

        void onDetached(Cell cell);
    }

}