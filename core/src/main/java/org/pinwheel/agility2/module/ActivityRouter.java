package org.pinwheel.agility2.module;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.pinwheel.agility2.action.Action1;
import org.pinwheel.agility2.action.Function1;
import org.pinwheel.agility2.utils.CommonTools;
import org.pinwheel.agility2.utils.FieldUtils;
import org.pinwheel.agility2.utils.LogUtils;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copyright (C), 2016 <br>
 * <br>
 * All rights reserved <br>
 * <br>
 *
 * @author dnwang
 * @version 19/10/2016,23:43
 */
public enum ActivityRouter {

    INSTANCE;

    private static final String TAG = ActivityRouter.class.getSimpleName();

    private static final String SCHEME = "router";

    public static void init(Context ctx) {
        INSTANCE.pathMaps.clear();
        try {
            PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            for (ActivityInfo aInfo : packageInfo.activities) {
                try {
                    Class cls = Class.forName(aInfo.name);
                    if (cls.isAnnotationPresent(Path.class)) {
                        String path = ((Path) cls.getAnnotation(Path.class)).value();
                        if (!TextUtils.isEmpty(path)) {
                            INSTANCE.pathMaps.put(path, cls);
                        }
                    }
                } catch (Exception e) {
                    LogUtils.e(TAG, e.getMessage());
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.e(TAG, e.getMessage());
        }
    }

    public static LaunchTaskGroup build(@NonNull String uri) {
        return build(Uri.parse(uri));
    }

    public static LaunchTaskGroup build(@NonNull Uri uri) {
        final String scheme = uri.getScheme();
        final List<String> pathSegments = uri.getPathSegments();
        final int size = pathSegments.size();
        if (SCHEME.equals(scheme) && size > 0) {
            Bundle args = null;
            final Set<String> paramKeys = uri.getQueryParameterNames();
            if (paramKeys.size() > 0) {
                args = new Bundle();
                for (String key : paramKeys) {
                    args.putString(key, uri.getQueryParameter(key));
                }
            }
            final LaunchTask[] tasks = new LaunchTask[pathSegments.size()];
            for (int i = 0; i < size; i++) {
                tasks[i] = new LaunchTask(INSTANCE.pathMaps.get(pathSegments.get(i)), args);
            }
            return new LaunchTaskGroup(tasks);
        } else {
            return new LaunchTaskGroup((LaunchTask[]) null);
        }
    }

    public static LaunchTask build(@NonNull Class<? extends Activity> cls) {
        return new LaunchTask(cls, null);
    }

    public static void injectFields(final Activity target, final Bundle args) {
        if (null == target || null == args || args.isEmpty()) {
            return;
        }
        FieldUtils.foreachAllField(target.getClass(), new Action1<Field>() {
            @Override
            public void call(Field field) {
                if (field.isAnnotationPresent(Inject.class)) {
                    final String customKey = field.getAnnotation(Inject.class).value();
                    final String key = TextUtils.isEmpty(customKey) ? field.getName() : customKey;
                    if (args.containsKey(key)) {
                        field.setAccessible(true);
                        try {
                            field.set(target, formatType(field, args.get(key)));
                        } catch (Exception e) {
                            LogUtils.e(TAG, e.getMessage());
                        }
                    }
                }
            }
        });
    }

    private static Intent createIntent(Context ctx, LaunchTask task) {
        final Intent intent = new Intent(ctx, task.cls);
        if (0 != task.flags) {
            intent.setFlags(task.flags);
        }
        if (null != task.args && !task.args.isEmpty()) {
            intent.putExtras(task.args);
        }
        return intent;
    }

    private static Object formatType(Field field, Object value) {
        if (null != value) {
            final Class fieldType = field.getType();
            final Class valueType = value.getClass();
            if (!fieldType.equals(valueType)) {
                if (String.class.equals(fieldType)) {
                    value = String.valueOf(value);
                } else if (String.class.equals(valueType)) {
                    final String tmp = (String) value;
                    if (tmp.contains(".")) {
                        if (float.class.equals(fieldType) || Float.class.equals(fieldType)) {
                            value = Float.valueOf(tmp);
                        } else if (double.class.equals(fieldType) || Double.class.equals(fieldType)) {
                            value = Double.valueOf(tmp);
                        }
                    } else {
                        if (int.class.equals(fieldType) || Integer.class.equals(fieldType)) {
                            value = Integer.valueOf(tmp);
                        } else if (long.class.equals(fieldType) || Long.class.equals(fieldType)) {
                            value = Long.valueOf(tmp);
                        } else if (short.class.equals(fieldType) || Short.class.equals(fieldType)) {
                            value = Long.valueOf(tmp);
                        } else if (boolean.class.equals(fieldType) || Boolean.class.equals(fieldType)) {
                            value = Long.valueOf(tmp);
                        }
                    }
                }
            }
        }
        return value;
    }

    private final Map<String, Class<? extends Activity>> pathMaps = new HashMap<>();

    private final Set<Function1<Boolean, Intent>> filters = new HashSet<>(2);

    public void registerFilter(Function1<Boolean, Intent> filter) {
        if (null != filter && !filters.contains(filter)) {
            filters.add(filter);
        }
    }

    public void unregisterFilter(Function1<Boolean, Intent> filter) {
        if (null != filter && filters.contains(filter)) {
            filters.remove(filter);
        }
    }

    public void start(final LaunchTask... tasks) {
        if (null == tasks || 0 == tasks.length) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    start(tasks);
                }
            });
            return;
        }
        final Context ctx = CommonTools.getTopActivity();
        if (null != ctx) {
            if (1 == tasks.length) {
                Intent intent = createIntent(ctx, tasks[0]);
                if (!executeFilters(intent)) {
                    ctx.startActivity(intent);
                }
            } else {
                Intent[] intents = new Intent[tasks.length];
                int i = 0;
                for (; i < tasks.length; i++) {
                    intents[i] = createIntent(ctx, tasks[i]);
                    if (executeFilters(intents[i])) {
                        break;
                    }
                }
                if (i != intents.length) {
                    intents = Arrays.copyOf(intents, i);
                }
                if (intents.length > 0) {
                    ctx.startActivities(intents);
                }
            }
        }
    }

    private boolean executeFilters(Intent intent) {
        if (filters.isEmpty()) {
            return false;
        }
        for (Function1<Boolean, Intent> func : filters) {
            if (func.call(intent)) {
                return true;
            }
        }
        return false;
    }

    @Inherited
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Inject {
        String value();
    }

    @Inherited
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Path {
        String value();
    }

    public static final class LaunchTask {
        private final Class<? extends Activity> cls;
        private Bundle args = null;
        private int flags = 0;

        private LaunchTask(@NonNull Class<? extends Activity> cls, Bundle args) {
            this.cls = cls;
            this.args = args;
            this.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP;
        }

        public LaunchTask with(@NonNull String key, Object obj) {
            if (!TextUtils.isEmpty(key)) {
                if (null == args) {
                    args = new Bundle();
                }
                if (null == obj) {
                    args.remove(key);
                } else {
                    if (obj instanceof Parcelable) {
                        args.putParcelable(key, (Parcelable) obj);
                    } else {
                        args.putSerializable(key, (Serializable) obj);
                    }
                }
            }
            return this;
        }

        public LaunchTask addFlags(int flags) {
            this.flags |= flags;
            return this;
        }

        public LaunchTask setFlags(int flags) {
            this.flags = flags;
            return this;
        }

        public void start() {
            INSTANCE.start(this);
        }
    }

    public static final class LaunchTaskGroup {
        public final LaunchTask[] tasks;

        private LaunchTaskGroup(LaunchTask... tasks) {
            this.tasks = tasks;
        }

        public void start() {
            INSTANCE.start(tasks);
        }
    }

}