package org.pinwheel.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.LongSparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
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

    private LongSparseArray<Bundle> cellDataMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(getTestLayout());
        try {
            CellFactory.CellBundle bundle = CellFactory.load(IOUtils.stream2String(getResources().getAssets().open("layout.json")));
            cellDataMap = bundle.dataMap;
            cellLayout.setRoot(bundle.root);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private CellLayout getTestLayout() {
        cellLayout = new CellLayout(this);
        cellLayout.setBackgroundColor(Color.BLACK);
        cellLayout.setAdapter(new CellLayout.Adapter() {
            @Override
            public View getView(ViewGroup parent, Cell cell) {
                final long cellId = cell.getId();
                final Bundle data = cellDataMap.get(cellId);
                final int layoutId = (null == data) ? 0 : data.getInt("layoutId", 0);
                View view;
                if (layoutId > 0) {
                    ImageView image = new ImageView(CellLayoutActivity.this);
                    image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    image.setImageResource(R.mipmap.ic_launcher);
                    view = image;
                } else {
                    view = new View(CellLayoutActivity.this);
                }
                view.setBackgroundColor(Color.LTGRAY);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(v.getContext(), "" + cellId, Toast.LENGTH_SHORT).show();
                    }
                });
                return view;
            }
        });
        return cellLayout;
    }

}