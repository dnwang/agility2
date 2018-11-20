package org.pinwheel.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.pinwheel.agility2.utils.IOUtils;
import org.pinwheel.agility2.view.ViewHolder;
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
            if (null != oldFocus && cellLayout == oldFocus.getParent()) {
                oldFocus.setScaleX(1.0f);
                oldFocus.setScaleY(1.0f);
            }
            if (null != newFocus && cellLayout == newFocus.getParent()) {
                cellLayout.moveToCenter(newFocus, true);
                newFocus.bringToFront();
                newFocus.setScaleX(1.1f);
                newFocus.setScaleY(1.1f);
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
                final View view;
                if (getViewPoolId(cell) > 0) {
                    ImageButton image = new ImageButton(CellLayoutActivity.this);
                    image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    view = image;
                } else {
                    view = LayoutInflater.from(CellLayoutActivity.this).inflate(R.layout.item_style_0, cellLayout, false);
                }
                view.setTag(new ViewHolder(view));
                return view;
            }

            @Override
            public void onBindView(@NonNull View view, @NonNull Cell cell) {
                Log.e("CellLayoutActivity", "onBindView: " + cell);
                final long cellId = cell.getId();
                final Bundle data = cellDataMap.get(cellId);
                final String title = null == data ? String.valueOf(cellId) : data.getString("title");
                final ViewHolder holder = (ViewHolder) view.getTag();
                if (getViewPoolId(cell) > 0) {
                    ((ImageView) holder.getContentView()).setImageResource(R.mipmap.ic_launcher);
                } else {
                    holder.getTextView(R.id.text1).setText(title);
                    holder.getTextView(R.id.text2).setText(String.valueOf(cellId));
                    holder.getImageView(R.id.image).setImageResource(R.mipmap.ic_launcher);
                }
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.e("onClick", "v: " + v);
                        Toast.makeText(v.getContext(), String.valueOf(cellId), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onViewRecycled(@NonNull View view, @NonNull Cell cell) {
                Log.e("CellLayoutActivity", "onViewRecycled: " + cell);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(getColor());
                }
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