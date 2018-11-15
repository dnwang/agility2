package org.pinwheel.agility2.view.celllayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,18:51
 */
public final class CellFactory {

    public static Cell load(JSONObject json, Callback callback) throws JSONException {
        int version = json.getInt("version");
        JSONObject root = json.getJSONObject("root");

        final String type = root.getString("type");
        final int row = root.getInt("row");
        final int column = root.getInt("column");
        final GridGroup gridGroup = new GridGroup(row, column);

        final JSONArray items = root.getJSONArray("items");
        final int size = items.length();
        for (int i = 0; i < size; i++) {
            JSONObject itemObj = items.getJSONObject(i);
            Cell cell = new Cell();
            GridGroup.Params p = new GridGroup.Params(
                    itemObj.getInt("x"),
                    itemObj.getInt("y"),
                    itemObj.getInt("weightX"),
                    itemObj.getInt("weightY")
            );
            callback.onLoadCell(cell);
            gridGroup.addCell(cell, p);
        }
        return gridGroup;
    }

    public interface Callback {
        void onLoadCell(Cell cell);
    }

}