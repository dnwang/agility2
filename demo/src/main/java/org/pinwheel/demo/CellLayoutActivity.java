package org.pinwheel.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import org.json.JSONObject;
import org.pinwheel.agility2.utils.IOUtils;
import org.pinwheel.agility2.view.celllayout.Cell;
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
        setContentView(getTestLayout());

        cellLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    String jsonString = IOUtils.stream2String(getResources().getAssets().open("layout.json"));
                    cellLayout.load(new JSONObject(jsonString));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 1000);
    }

    private CellLayout getTestLayout() {
        cellLayout = new CellLayout(this);
        cellLayout.setBackgroundColor(Color.DKGRAY);
        cellLayout.setAdapter(new CellLayout.Adapter() {
            @Override
            public View onCreateView(Cell cell) {
                View view = new View(CellLayoutActivity.this);
                view.setBackgroundColor(Color.rgb(
                        (int) (Math.random() * 255),
                        (int) (Math.random() * 255),
                        (int) (Math.random() * 255)
                ));
                return view;
            }

            @Override
            public void onUpdate(View view, Cell cell) {

            }
        });
        return cellLayout;
    }

}