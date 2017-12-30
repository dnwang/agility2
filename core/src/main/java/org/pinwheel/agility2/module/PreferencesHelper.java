package org.pinwheel.agility2.module;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import org.pinwheel.agility2.utils.CommonTools;


/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 8/4/16,22:31
 * @see
 */
public enum PreferencesHelper {

    INSTANCE(CommonTools.getApplication());

    private SharedPreferences preferences;

    PreferencesHelper(Context context) {
        preferences = context.getSharedPreferences("common", Context.MODE_PRIVATE);
    }

    public String getString(String key) {
        return getString(key, "");
    }

    public String getString(String key, String def) {
        return preferences.getString(key, def);
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean def) {
        return preferences.getBoolean(key, def);
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int def) {
        return preferences.getInt(key, def);
    }

    public long getLong(String key) {
        return getLong(key, 0);
    }

    public long getLong(String key, long def) {
        return preferences.getLong(key, def);
    }

    public int getFloat(String key) {
        return getInt(key, 0);
    }

    public float getFloat(String key, float def) {
        return preferences.getFloat(key, def);
    }

    public <T> T getObject(String key, Class<T> cls) {
        String json = getString(key, null);
        if (null == json || "".equals(json)) {
            return null;
        } else {
            return new Gson().fromJson(json, cls);
        }
    }

    public PreferencesHelper setString(String key, String value) {
        if (null != key) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(key, value);
            editor.apply();
        }
        return this;
    }

    public PreferencesHelper setBoolean(String key, boolean value) {
        if (null != key) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(key, value);
            editor.apply();
        }
        return this;
    }

    public PreferencesHelper setInt(String key, int value) {
        if (null != key) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(key, value);
            editor.apply();
        }
        return this;
    }

    public PreferencesHelper setFloat(String key, float value) {
        if (null != key) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putFloat(key, value);
            editor.apply();
        }
        return this;
    }

    public PreferencesHelper setLong(String key, long value) {
        if (null != key) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(key, value);
            editor.apply();
        }
        return this;
    }

    public PreferencesHelper setObject(String key, Object obj) {
        if (null != key) {
            if (null != obj) {
                String json = new Gson().toJson(obj);
                return setString(key, json);
            } else {
                return remove(key);
            }
        }
        return this;
    }

    public PreferencesHelper remove(String key) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.apply();
        return this;
    }

    public PreferencesHelper clear() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
        return this;
    }

}