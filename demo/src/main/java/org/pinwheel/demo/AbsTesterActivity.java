package org.pinwheel.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.pinwheel.agility2.adapter.SimpleArrayAdapter;
import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.view.ViewHolder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

abstract class AbsTesterActivity extends Activity {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    protected @interface Tester {
        String title();

        String desc() default "";
    }

    private final SimpleArrayAdapter<Method> adapter = new SimpleArrayAdapter<Method>() {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Method method = getItem(position);
            if (null == convertView) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            final ViewHolder holder = CommonTools.getHolderBy(convertView);
            final Tester tester = method.getAnnotation(Tester.class);
            final String desc = tester.desc();
            holder.select().setBackgroundColor(Color.WHITE);
            holder.select(android.R.id.text1).setText(tester.title());
            holder.select(android.R.id.text2).setText(TextUtils.isEmpty(desc) ? method.getName() : desc);
            return convertView;
        }
    };

    private final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
            Object obj = parent.getItemAtPosition(pos);
            try {
                ((Method) obj).invoke(AbsTesterActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Method[] methods = this.getClass().getDeclaredMethods();
        for (Method tmp : methods) {
            if (tmp.isAnnotationPresent(Tester.class)) {
                tmp.setAccessible(true);
                adapter.addItem(tmp);
            }
        }
        ListView list = new ListView(this);
        list.setOnItemClickListener(onItemClickListener);
        list.setAdapter(adapter);
        setContentView(list);
    }

}