package org.pinwheel.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.json.JSONObject;
import org.pinwheel.agility2.utils.IOUtils;
import org.pinwheel.agility2.view.celllayout.Cell;
import org.pinwheel.agility2.view.celllayout.CellFactory;
import org.pinwheel.agility2.view.celllayout.CellGroup;
import org.pinwheel.agility2.view.celllayout.CellLayout;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,11:18
 */
public final class CellLayoutActivity extends Activity {

    private CellLayout cellLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(getTestLayout());
        try {
            String jsonString = IOUtils.stream2String(getResources().getAssets().open("layout.json"));
            cellLayout.setRoot((CellGroup) CellFactory.load(new JSONObject(jsonString)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CellLayout getTestLayout() {
        cellLayout = new CellLayout(this);
        cellLayout.setBackgroundColor(Color.LTGRAY);
        cellLayout.setAdapter(new CellLayout.Adapter() {
            @Override
            public View onCreateView(Cell cell) {
                TextView text = new TextView(CellLayoutActivity.this);
                text.setText(String.valueOf(cell.getId()));
                text.setGravity(Gravity.CENTER);
                text.setBackgroundColor(Color.rgb(
                        (int) (Math.random() * 255),
                        (int) (Math.random() * 255),
                        (int) (Math.random() * 255)
                ));
                return text;
            }

            @Override
            public void onUpdate(View view, Cell cell) {

            }
        });
        return cellLayout;
    }

}