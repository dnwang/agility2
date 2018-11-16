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

    void setRoot(Cell cell) {
        if (null != root) {
            // detach
            foreachAllCell(root, new CellFilter() {
                @Override
                public boolean call(Cell cell) {
                    Cell owner = cell.getOwner();
                    if (null != owner) {
                        ((CellGroup) owner).removeCell(cell);
                    }
                    return false;
                }
            });
        }
        root = cell;
        if (null != root) {
            // attach
            foreachAllCell(root, new CellFilter() {
                @Override
                public boolean call(Cell cell) {
                    cell.attach(CellDirector.this);
                    return false;
                }
            });
        }
    }

    Cell findCellById(long id) {
        if (root instanceof CellGroup) {
            return ((CellGroup) root).findCellById(id);
        } else {
            return root.getId() == id ? root : null;
        }
    }

    void measure(int measureWidth, int measureHeight) {
        if (null == root) {
            return;
        }
        root.setSize(measureWidth, measureHeight);
    }

    void layout(int l, int t, int r, int b) {
        if (null == root) {
            return;
        }
        root.setPosition(l, t);
    }

    void setCallback(LifeCycleCallback callback) {
        this.callback = callback;
    }

    void notifyAttached(Cell cell) {
        callback.onAttached(cell);
    }

    void notifyDetached(Cell cell) {
        callback.onDetached(cell);
    }

    void notifyVisibleChanged(Cell cell) {
        callback.onVisibleChanged(cell);
    }

    private boolean foreachAllCell(Cell root, CellFilter filter) {
        boolean intercept = false;
        if (root instanceof CellGroup) {
            intercept = filter.call(root);
            if (!intercept) {
                CellGroup group = (CellGroup) root;
                final int size = group.getSubCellCount();
                for (int i = 0; i < size; i++) {
                    intercept = foreachAllCell(group.getCellAt(i), filter);
                    if (intercept) {
                        break;
                    }
                }
            }
        } else if (null != root) {
            intercept = filter.call(root);
        }
        return intercept;
    }

    private interface CellFilter {
        boolean call(Cell cell);
    }

    interface LifeCycleCallback {
        void onAttached(Cell cell);

        void onVisibleChanged(Cell cell);

        void onDetached(Cell cell);
    }

}