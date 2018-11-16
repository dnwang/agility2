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

    private CellGroup root;
    private LifeCycleCallback callback;

    boolean hasRoot() {
        return null != root;
    }

    void attach(CellGroup group) {
        if (null != root) {
            // detach old
            root.foreachAllCells(true, new Filter() {
                @Override
                public boolean call(Cell cell) {
                    cell.removeFromOwner();
                    return false;
                }
            });
        }
        root = group;
        if (null != root) {
            // attach new
            root.foreachAllCells(true, new Filter() {
                @Override
                public boolean call(Cell cell) {
                    cell.attach(CellDirector.this);
                    return false;
                }
            });
        }
    }

    CellGroup getRoot() {
        return root;
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