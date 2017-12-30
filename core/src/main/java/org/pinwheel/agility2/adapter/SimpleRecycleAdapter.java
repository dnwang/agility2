package org.pinwheel.agility2.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.pinwheel.agility2.view.ViewHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 */
public abstract class SimpleRecycleAdapter<T> extends RecyclerView.Adapter<SimpleRecycleAdapter.RecyclerViewItemHolder> {

    private List<T> data;

    public SimpleRecycleAdapter() {
        data = new ArrayList<>();
    }

    public SimpleRecycleAdapter(List<T> list) {
        this();
        addAll(list);
    }

    public SimpleRecycleAdapter(T... list) {
        this();
        addAll(list);
    }

    public List<T> getDatas() {
        return data;
    }

    public T getItem(int index) {
        if (index >= 0 && index < data.size()) {
            return data.get(index);
        }
        return null;
    }

    public SimpleRecycleAdapter<T> addItem(T obj) {
        if (obj != null) {
            data.add(obj);
        }
        return this;
    }

    public SimpleRecycleAdapter<T> addItem(T obj, int index) {
        if (obj != null && index >= 0 && index < data.size()) {
            data.add(index, obj);
        }
        return this;
    }

    public SimpleRecycleAdapter<T> addAll(List<T> list) {
        if (list != null && list.size() > 0) {
            data.addAll(list);
        }
        return this;
    }

    public SimpleRecycleAdapter<T> addAll(T... list) {
        if (list != null && list.length > 0) {
            for (T t : list) {
                if (t instanceof Collection) {
                    if (t instanceof List) {
                        addAll((List<T>) t);
                    } else {
                        throw new ClassCastException("SimpleRecycleAdapter addAll(T...)");
                    }
                } else {
                    data.add(t);
                }
            }
        }
        return this;
    }

    public SimpleRecycleAdapter<T> reset(List<T> list) {
        return removeAll().addAll(list);
    }

    public boolean remove(T obj) {
        if (null != obj) {
            return data.remove(obj);
        }
        return false;
    }

    public T remove(int index) {
        if (index >= 0 && index < data.size()) {
            return data.remove(index);
        }
        return null;
    }

    public SimpleRecycleAdapter<T> removeAll() {
        data.clear();
        return this;
    }

    public SimpleRecycleAdapter<T> removeAll(List<T> list) {
        if (list != null) {
            data.removeAll(list);
        }
        return this;
    }

    @Override
    public RecyclerViewItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View contentView = onCreateView(LayoutInflater.from(parent.getContext()), parent, viewType);
        if (null == contentView) {
            throw new NullPointerException("SimpleRecycleAdapter onCreateView can't get null !");
        }
        return new RecyclerViewItemHolder(contentView);
    }

    @Override
    public void onBindViewHolder(RecyclerViewItemHolder holder, int position) {
        onBindViewHolder(holder.views, position);
    }

    public abstract View onCreateView(LayoutInflater inflater, ViewGroup parent, int viewType);

    public abstract void onBindViewHolder(ViewHolder views, int position);

    @Override
    public int getItemCount() {
        return data.size();
    }

    public int getCount() {
        return getItemCount();
    }

    public boolean isEmpty() {
        return getItemCount() <= 0;
    }

    /**
     * ViewHolder
     */
    static final class RecyclerViewItemHolder extends RecyclerView.ViewHolder {
        private ViewHolder views;

        RecyclerViewItemHolder(View itemView) {
            super(itemView);
            views = new ViewHolder(itemView);
        }
    }

}
