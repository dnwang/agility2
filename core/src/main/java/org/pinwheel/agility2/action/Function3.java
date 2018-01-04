package org.pinwheel.agility2.action;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 27/09/2016,22:34
 */
public interface Function3<R, T, K, N> {

    R call(T obj0, K obj1, N obj2);

}
