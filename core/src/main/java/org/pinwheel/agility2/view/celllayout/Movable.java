package org.pinwheel.agility2.view.celllayout;

import android.graphics.Rect;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/16,22:35
 */
interface Movable {
    void scrollBy(int dx, int dxy);

    void scrollTo(int x, int y);

    int getScrollX();

    int getScrollY();

    Rect getContentRect();
}
