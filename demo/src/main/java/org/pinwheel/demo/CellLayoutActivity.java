package org.pinwheel.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.LongSparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
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
    private static final String TAG = "CellLayoutActivity";

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

    private int count = 0;

    private CellLayout getTestLayout() {
        cellLayout = new CellLayout(this);
        cellLayout.setBackgroundColor(Color.BLACK);
        cellLayout.setAdapter(new CellLayout.Adapter() {
            @Override
            public View onCreate(ViewGroup parent, View view, Cell cell) {
                final long cellId = cell.getId();
                final Bundle data = cellDataMap.get(cellId);
                if (null == view) {
                    view = createView(data);
                }
                updateView(view, data);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(v.getContext(), "" + cellId, Toast.LENGTH_SHORT).show();
                    }
                });
                count++;
                return view;
            }

            @Override
            public void onRecycled(View view, Cell cell) {
                count--;
            }

            private View createView(Bundle data) {
                final int style = getStyle(data);
                if (style > 0) {
                    ImageView image = new ImageView(CellLayoutActivity.this);
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    return image;
                } else {
                    TextView text = new TextView(CellLayoutActivity.this);
                    text.setBackgroundColor(randomColor());
                    text.setGravity(Gravity.CENTER);
                    text.setTextColor(Color.BLACK);
                    return text;
                }
            }

            private void updateView(View view, Bundle data) {
                final int style = getStyle(data);
                if (style > 0) {
                    ((ImageView) view).setImageResource(R.mipmap.ic_launcher);
                } else {
                    ((TextView) view).setText(null == data ? "" : data.getString("title", ""));
                }
            }

            private int getStyle(Bundle data) {
                return (null == data) ? 0 : data.getInt("layoutId", 0);
            }
        });
        return cellLayout;
    }

    private int randomColor() {
        return Color.rgb(
                (int) (Math.random() * 255),
                (int) (Math.random() * 255),
                (int) (Math.random() * 255)
        );
    }

}