package org.pinwheel.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.pinwheel.agility2.utils.IOUtils;
import org.pinwheel.agility2.view.celllayout.Cell;
import org.pinwheel.agility2.view.celllayout.CellFactory;
import org.pinwheel.agility2.view.celllayout.CellLayout;

import java.io.IOException;

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
            cellLayout.setRoot(CellFactory.load(new JSONObject(jsonString)));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private CellLayout getTestLayout() {
        cellLayout = new CellLayout(this);
        cellLayout.setBackgroundColor(Color.BLACK);
        cellLayout.setAdapter(new CellLayout.Adapter() {
            @Override
            public View onCreateView(final Cell cell) {
                ImageView image = new ImageView(CellLayoutActivity.this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setImageResource(R.mipmap.ic_launcher);
                image.setBackgroundColor(Color.LTGRAY);
                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(v.getContext(), String.valueOf(cell.getId()), Toast.LENGTH_SHORT).show();
                    }
                });
                return image;
            }

            @Override
            public void onUpdate(View view, Cell cell) {

            }
        });
        return cellLayout;
    }

}