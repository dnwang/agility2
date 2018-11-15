package org.pinwheel.agility2.view.celllayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C), 2018 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 2018/11/15,11:21
 */
public class CellLayout extends ViewGroup {

    public CellLayout(Context context) {
        super(context);
        this.init();
    }

    public CellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CellLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init();
    }

    private Adapter adapter;

    private void init() {
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
    }

    private List<Holder> cellViewHolder = new ArrayList<>();

    public void load(JSONObject json) throws JSONException {
        root = CellFactory.load(json, new CellFactory.Callback() {
            @Override
            public void onLoadCell(Cell cell) {
                View view = adapter.onCreateView(cell);
                Holder holder = new Holder(view, cell);
                cellViewHolder.add(holder);
            }
        });
        requestLayout();
    }

    private Cell root;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (null == root) {
            return;
        }
        root.setSize(getMeasuredWidth(), getMeasuredHeight());
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            Holder holder = cellViewHolder.get(i);
            Cell cell = holder.cell;
            holder.view.measure(cell.getWidth(), cell.getHeight());
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (null == root) {
            return;
        }
        root.setPosition(l, t);
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            Holder holder = cellViewHolder.get(i);
            Cell cell = holder.cell;
            holder.view.layout(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
        }
    }

    private static class Holder {
        View view;
        Cell cell;

        Holder(View view, Cell cell) {
            this.view = view;
            this.cell = cell;
        }
    }

    public interface Adapter {
        View onCreateView(Cell cell);

        void onUpdate(View view, Cell cell);
    }

}