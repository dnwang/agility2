package org.pinwheel.agility2.utils;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.pinwheel.agility2.action.Action1;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * Copyright (C), 2015 <br>
 * <br>
 * All rights reserved
 *
 * @author dnwang
 */
public final class FieldUtils {

    @Inherited
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Ignore {
        String value() default "";
    }

    @Inherited
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Mark {
        String value() default "";
    }

    private FieldUtils() {
        throw new AssertionError();
    }

    @Nullable
    public static <T> T getFieldValue(Object obj, String fieldName) {
        if (null == obj || TextUtils.isEmpty(fieldName)) {
            return null;
        }
        Field field = getField(obj.getClass(), fieldName);
        if (null != field) {
            field.setAccessible(true);
            try {
                Object result = field.get(obj);
                return (null != result) ? (T) result : null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Nullable
    public static Field getField(Class cls, String fieldName) {
        if (null == cls || TextUtils.isEmpty(fieldName)) {
            return null;
        }
        Field field = null;
        do {
            try {
                field = cls.getDeclaredField(fieldName);
            } catch (Exception ignore) {
            }
            if (null != field) {
                break;
            }
        } while ((cls = cls.getSuperclass()) != null);
        return field;
    }

    @Nullable
    public static <T> T invokeMethod(Object obj, String methodName, Object... args) {
        if (null == obj || TextUtils.isEmpty(methodName)) {
            return null;
        }
        final Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            final Object arg = args[i];
            if (null == arg) {
                System.out.println("FieldUtils.invokeMethod(): have unknown args! index: " + i);
            } else {
                types[i] = clsUnBoxing(arg.getClass());
            }
        }
        Method method = getMethod(obj.getClass(), methodName, types);
        if (null != method) {
            method.setAccessible(true);
            try {
                Object result = method.invoke(obj, args);
                return (null != result) ? (T) result : null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Nullable
    public static <T> T invokeStaticMethod(Class<?> cls, String methodName, Object... args) {
        if (null == cls || TextUtils.isEmpty(methodName)) {
            return null;
        }
        final Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            final Object arg = args[i];
            if (null == arg) {
                System.out.println("FieldUtils.invokeStaticMethod(): have unknown args! index: " + i);
            } else {
                types[i] = clsUnBoxing(arg.getClass());
            }
        }
        Method method = getMethod(cls, methodName, types);
        if (null != method) {
            method.setAccessible(true);
            try {
                Object result = method.invoke(null, args);
                return (null != result) ? (T) result : null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Nullable
    public static Method getMethod(Class<?> cls, String methodName, Class<?>... parameterTypes) {
        if (null == cls || TextUtils.isEmpty(methodName)) {
            return null;
        }
        Method method = null;
        do {
            try {
                method = cls.getDeclaredMethod(methodName, parameterTypes);
            } catch (Exception ignore) {
            }
            if (null != method) {
                break;
            }
        } while ((cls = cls.getSuperclass()) != null);
        return method;
    }

    public static HashMap<String, Object> obj2Map(Object obj) {
        HashMap<String, Object> values = new HashMap<>();
        Class cls = obj.getClass();
        for (Field field : cls.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Ignore.class)) {
                try {
                    field.setAccessible(true);
                    values.put(field.getName(), field.get(obj));
                } catch (Exception ignore) {
                }
            }
        }
        return values;
    }

    public static Type getGenericClass(Object obj) {
        if (obj == null) {
            return null;
        }
        Type genType = obj.getClass().getGenericSuperclass();
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        return params[0];
    }

    public static void foreachAllField(Class cls, Action1<Field> action) {
        if (null == cls || null == action) {
            return;
        }
        do {
            Field fields[] = cls.getDeclaredFields();
            if (null != fields && fields.length > 0) {
                for (Field f : fields) {
                    action.call(f);
                }
            }
        } while ((cls = cls.getSuperclass()) != null);
    }

    public static void foreachAllMethod(Class cls, Action1<Method> action) {
        if (null == cls || null == action) {
            return;
        }
        do {
            Method methods[] = cls.getDeclaredMethods();
            if (null != methods && methods.length > 0) {
                for (Method m : methods) {
                    action.call(m);
                }
            }
        } while ((cls = cls.getSuperclass()) != null);
    }

    public static <T> HashMap<String, Object> getFieldWithoutIgnore(T obj) {
        if (null == obj) {
            return null;
        }
        return getFieldWithoutIgnore((Class) obj.getClass(), obj);
    }

    public static <T> HashMap<String, Object> getFieldWithoutIgnore(Class<T> cls, final T obj) {
        if (null == obj || null == cls) {
            return null;
        }
        final HashMap<String, Object> params = new HashMap<>();
        foreachAllField(cls, new Action1<Field>() {
            @Override
            public void call(Field field) {
                if (!field.isAnnotationPresent(Ignore.class)) {
                    try {
                        field.setAccessible(true);
                        params.put(field.getName(), field.get(obj));
                    } catch (Exception ignore) {
                    }
                }
            }
        });
        return params;
    }

    public static HashMap<String, Object> getFieldWithMark(Object obj) {
        if (null == obj) {
            return null;
        }
        return getFieldWithMark((Class) obj.getClass(), obj);
    }

    public static <T> HashMap<String, Object> getFieldWithMark(Class<T> cls, final T obj) {
        if (null == obj || null == cls) {
            return null;
        }
        final HashMap<String, Object> params = new HashMap<>();
        foreachAllField(cls, new Action1<Field>() {
            @Override
            public void call(Field field) {
                if (field.isAnnotationPresent(Mark.class)) {
                    try {
                        field.setAccessible(true);
                        params.put(field.getName(), field.get(obj));
                    } catch (Exception ignore) {
                    }
                }
            }
        });
        return params;
    }

    private static Class clsUnBoxing(Class cls) {
        if (Integer.class.getName().equals(cls.getName())) {
            return int.class;
        } else if (Short.class.getName().equals(cls.getName())) {
            return short.class;
        } else if (Long.class.getName().equals(cls.getName())) {
            return long.class;
        } else if (Float.class.getName().equals(cls.getName())) {
            return float.class;
        } else if (Double.class.getName().equals(cls.getName())) {
            return double.class;
        } else if (Boolean.class.getName().equals(cls.getName())) {
            return boolean.class;
        }
        return cls;
    }

}