package org.pinwheel.agility2.view.celllayout;

import org.json.JSONArray;
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

    public static Cell load(JSONObject json) throws Exception {
        final int version = json.getInt("version");
        if (1 == version) {
            return new DefaultParser().parse(json);
        } else {
            throw new RuntimeException("can't found this special version parser ! version:" + version);
        }
    }

    private static final class DefaultParser implements IParser {
        @Override
        public Cell parse(JSONObject json) throws Exception {
            return parse(json.getJSONObject("root"), null);
        }

        private Cell parse(JSONObject args, CellGroup owner) throws Exception {
            // params
            final CellGroup.Params p;
            if (owner instanceof GridGroup) {
                p = new GridGroup.Params(args);
            } else if (owner instanceof LinearGroup) {
                p = new LinearGroup.Params(args);
            } else {
                p = new CellGroup.Params(args);
            }
            // type
            final String type = args.getString("type");
            final Cell cell;
            if ("grid".equalsIgnoreCase(type)) {
                cell = new GridGroup(args);
            } else if ("linear".equalsIgnoreCase(type)) {
                cell = new LinearGroup(args);
            } else {
                cell = new Cell(args);
            }
            if (cell instanceof CellGroup) {
                final JSONArray subArgsList = args.optJSONArray("cells");
                final int size = null != subArgsList ? subArgsList.length() : 0;
                for (int i = 0; i < size; i++) {
                    parse(subArgsList.getJSONObject(i), (CellGroup) cell);
                }
            }
            if (null != owner) {
                owner.addCellNoAttach(cell, p);
            }
            return cell;
        }

    }

    private interface IParser {
        Cell parse(JSONObject json) throws Exception;
    }

}