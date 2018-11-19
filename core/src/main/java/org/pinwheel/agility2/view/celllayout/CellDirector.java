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
        foreachAllCells(true, new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                cell.attach(CellDirector.this);
                return false;
            }
        });
    }

    private void detachRoot() {
        foreachAllCells(true, new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                cell.removeFromParent();
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
        foreachAllCells(false, new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                if (cell.contains(x, y)) {
                    tmp = cell;
                    return true;
                }
                return false;
            }
        });
        return tmp;
    }

    void foreachAllCells(boolean withGroup, Filter<Cell> filter) {
        if (hasRoot()) {
            if (root instanceof CellGroup) {
                ((CellGroup) root).foreachAllCells(withGroup, filter);
            } else {
                filter.call(root);
            }
        }
    }

    LinearGroup findLinearGroupBy(Cell cell, final int orientation) {
        Cell parent = null != cell ? cell.getParent() : null;
        if (parent instanceof LinearGroup
                && orientation == ((LinearGroup) parent).getOrientation()) {
            return (LinearGroup) parent;
        } else if (null != parent) {
            return findLinearGroupBy(parent, orientation);
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
        if (null == target || (0 == dx && 0 == dy)) {
            return;
        }
        if (target instanceof LinearGroup) {
            LinearGroup linear = (LinearGroup) target;
            final int contentWidth = linear.getContentWidth();
            final int contentHeight = linear.getContentHeight();
            // fix dx
            int tmp = linear.getScrollX() + dx;
            int max = -(contentWidth - linear.getWidth());
            if (tmp > 0) {
                dx = -linear.getScrollX();
            } else if (tmp < max) {
                dx = max - linear.getScrollX();
            }
            // fix dy
            tmp = linear.getScrollY() + dy;
            max = -(contentHeight - linear.getHeight());
            if (tmp > 0) {
                dy = -linear.getScrollY();
            } else if (tmp < max) {
                dy = max - linear.getScrollY();
            }
        }
        target.scrollBy(dx, dy);
    }

    void moveToCenter(Cell cell) {
        if (null == cell || !hasRoot()) {
            return;
        }
        final int centerX = root.getLeft() + root.getWidth() / 2;
        final int centerY = root.getTop() + root.getHeight() / 2;
        final int cellCenterX = cell.getLeft() + cell.getWidth() / 2;
        final int cellCenterY = cell.getTop() + cell.getHeight() / 2;
        int tmp = centerY - cellCenterY;
        if (Math.abs(tmp) > 10) {
            move(findLinearGroupBy(cell, LinearGroup.VERTICAL), 0, tmp);
        }
        tmp = centerX - cellCenterX;
        if (Math.abs(tmp) > 10) {
            move(findLinearGroupBy(cell, LinearGroup.HORIZONTAL), tmp, 0);
        }
    }

    void measure(int width, int height) {
        if (hasRoot() && cellChanged) {
            root.measure(width, height);
        }
    }

    private boolean cellChanged = true;

    void layout(boolean viewChanged, int l, int t, int r, int b) {
        if (hasRoot() && (viewChanged || cellChanged)) {
            root.layout(l, t);
            cellChanged = false;
        }
    }

    void refresh() {
        foreachAllCells(true, new Filter<Cell>() {
            @Override
            public boolean call(Cell cell) {
                cell.updateVisibleSate();
                return false;
            }
        });
    }

    void notifyAttached(Cell cell) {
        cellChanged = true;
        if (null != callback) {
            callback.onAttached(cell);
        }
    }

    void notifyDetached(Cell cell) {
        cellChanged = true;
        if (null != callback) {
            callback.onDetached(cell);
        }
    }

    void notifyPositionChanged(Cell cell, int fromX, int fromY) {
        if (null != callback) {
            callback.onPositionChanged(cell, fromX, fromY);
        }
    }

    void notifyVisibleChanged(Cell cell) {
        if (null != callback) {
            callback.onVisibleChanged(cell);
        }
    }

    interface LifeCycleCallback {
        void onAttached(Cell cell);

        void onPositionChanged(Cell cell, int fromX, int fromY);

        void onVisibleChanged(Cell cell);

        void onDetached(Cell cell);
    }

}