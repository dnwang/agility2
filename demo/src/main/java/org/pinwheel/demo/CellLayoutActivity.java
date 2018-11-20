package org.pinwheel.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
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

    private CellLayout cellLayout;
    private LongSparseArray<Bundle> cellDataMap;

    private final ViewTreeObserver.OnGlobalFocusChangeListener focusListener = new ViewTreeObserver.OnGlobalFocusChangeListener() {
        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
            if (newFocus.hasFocus() && cellLayout == newFocus.getParent()) {
                cellLayout.moveToCenter(newFocus, true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(getTestLayout());
        cellLayout.getViewTreeObserver().addOnGlobalFocusChangeListener(focusListener);
        try {
            CellFactory.CellBundle bundle = CellFactory.load(IOUtils.stream2String(getResources().getAssets().open("layout.json")));
            cellDataMap = bundle.dataMap;
            cellLayout.setRootCell(bundle.root);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        cellLayout.getViewTreeObserver().removeOnGlobalFocusChangeListener(focusListener);
        super.onDestroy();
    }

    private CellLayout getTestLayout() {
        cellLayout = new CellLayout(this);
        cellLayout.setBackgroundColor(Color.BLACK);
        cellLayout.setAdapter(new CellLayout.ViewAdapter() {
            @Override
            public int getViewPoolId(@NonNull Cell cell) {
                final Bundle data = cellDataMap.get(cell.getId());
                return (null == data) ? 0 : data.getInt("layoutId", 0);
            }

            @Override
            public View onCreateView(@NonNull Cell cell) {
                Log.e("CellLayoutActivity", "onCreateView: " + cell);
                if (getViewPoolId(cell) > 0) {
                    ImageButton image = new ImageButton(CellLayoutActivity.this);
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    return image;
                } else {
                    TextView text = new Button(CellLayoutActivity.this);
                    text.setGravity(Gravity.CENTER);
                    text.setTextColor(Color.BLACK);
                    return text;
                }
            }

            @Override
            public void onBindView(@NonNull View view, @NonNull Cell cell) {
                Log.e("CellLayoutActivity", "onBindView: " + cell);
                final long cellId = cell.getId();
                if (getViewPoolId(cell) > 0) {
                    ImageView image = (ImageView) view;
                    image.setImageResource(R.mipmap.ic_launcher);
                } else {
                    TextView text = (TextView) view;
                    text.setText(cellId + "");
                }
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(v.getContext(), "" + cellId, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onViewRecycled(@NonNull View view, @NonNull Cell cell) {
                Log.e("CellLayoutActivity", "onViewRecycled: " + cell);
//                view.setBackgroundColor(getColor());
            }
        });
        return cellLayout;
    }

    private static int getColor() {
        return Color.rgb(
                (int) (Math.random() * 255),
                (int) (Math.random() * 255),
                (int) (Math.random() * 255)
        );
    }

}