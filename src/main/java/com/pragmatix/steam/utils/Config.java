package com.pragmatix.steam.utils;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Vladimir
 * Date: 18.07.13 16:48
 */
public strictfp class Config {

    public static String getString(Map config, String name) {
        return toString(config.get(name));
    }

    public static String getStringDefault(Map config, String name, String defaultValue) {
        return config.containsKey(name) ? getString(config, name) : defaultValue;
    }

    public static String[] getStringArray(Map config, String name) {
        return toStringArray((List) config.get(name));
    }

    public static String[] toStringArray(List list) {
        String[] res = new String[list.size()];
        int i = 0;
        for(Object value : list) {
            res[i++] = value != null ? value.toString() : null;
        }
        return res;
    }

    public static String[] getStringArrayDefault(Map config, String name, String[] defaultValue) {
        return config.containsKey(name) ? getStringArray(config, name) : defaultValue;
    }

    public static String toString(Object value) {
        if(value instanceof String) {
            return (String) value;
        } else {
            return value.toString();
        }
    }

    public static double getDouble(Map config, String name) {
        return toDouble(config.get(name));
    }

    public static double getDoubleDefault(Map config, String name, double defaultValue) {
        return config.containsKey(name) ? getDouble(config, name) : defaultValue;
    }

    public static double toDouble(Object value) {
        if(value == null) {
            throw new NullPointerException();
        } else if(value instanceof Double) {
            return (Double) value;
        } else if(value instanceof Integer) {
            return (Integer) value;
        } else if(value instanceof Long) {
            return (Long) value;
        } else {
            return Double.parseDouble(value.toString());
        }
    }

    public static double[] getDoubleArray(Map config, String name) {
        return toDoubleArray((List) config.get(name));
    }

    public static double[] toDoubleArray(List list) {
        double[] res = new double[list.size()];
        int i = 0;
        for(Object value : list) {
            res[i++] = toDouble(value);
        }
        return res;
    }

    public static double[] getDoubleArrayDefault(Map config, String name, double[] defaultValue) {
        return config.containsKey(name) ? getDoubleArray(config, name) : defaultValue;
    }

    public static long toLong(Object value) {
        if(value instanceof Long) {
            return (Long) value;
        } else if(value == null) {
            throw new NullPointerException();
        } else if(value instanceof Double) {
            return DoubleUtils.round((Double) value);
        } else if(value instanceof Integer) {
            return (Integer) value;
        } else if(value instanceof Short) {
            return (Short) value;
        } else if(value instanceof Byte) {
            return (Byte) value;
        } else {
            return Integer.parseInt(value.toString());
        }
    }

    public static int getInt(Map config, String name) {
        return toInt(config.get(name));
    }

    public static int getIntDefault(Map config, String name, int defaultValue) {
        return config.containsKey(name) ? getInt(config, name) : defaultValue;
    }

    public static int toInt(Object value) {
        if(value == null) {
            throw new NullPointerException();
        } else if(value instanceof Integer) {
            return (Integer) value;
        } else if(value instanceof Long) {
            return ((Long) value).intValue();
        } else if(value instanceof Double) {
            return ((Double) value).intValue();
        } else if(value instanceof Short) {
            return (Short) value;
        } else if(value instanceof Byte) {
            return (Byte) value;
        } else {
            return Integer.parseInt(value.toString());
        }
    }

    public static int[] getIntArray(Map config, String name) {
        return toIntArray((List) config.get(name));
    }

    public static int[] toIntArray(List list) {
        int[] res = new int[list.size()];
        int i = 0;
        for(Object value : list) {
            res[i++] = toInt(value);
        }
        return res;
    }

    public static int[] getIntArrayDefault(Map config, String name, int[] defaultValue) {
        return config.containsKey(name) ? getIntArray(config, name) : defaultValue;
    }

    public static short getShort(Map config, String name) {
        return toShort(config.get(name));
    }

    public static short getShortDefault(Map config, String name, short defaultValue) {
        return config.containsKey(name) ? getShort(config, name) : defaultValue;
    }

    public static short toShort(Object value) {
        if(value == null) {
            throw new NullPointerException();
        } else if(value instanceof Integer) {
            return ((Integer) value).shortValue();
        } else {
            return Short.parseShort(value.toString());
        }
    }

    public static short[] getShortArray(Map config, String name) {
        return toShortArray((List) config.get(name));
    }

    public static short[] toShortArray(List list) {
        short[] res = new short[list.size()];
        int i = 0;
        for(Object value : list) {
            res[i++] = toShort(value);
        }
        return res;
    }

    public static short[] getShortArrayDefault(Map config, String name, short[] defaultValue) {
        return config.containsKey(name) ? getShortArray(config, name) : defaultValue;
    }

    public static boolean getBool(Map config, String name) {
        return toBool(config.get(name));
    }

    public static boolean getBoolDefault(Map config, String name, boolean defaultValue) {
        return config.containsKey(name) ? getBool(config, name) : defaultValue;
    }

    public static boolean toBool(Object value) {
        if(value == null) {
            throw new NullPointerException();
        } else if(value instanceof Boolean) {
            return (Boolean) value;
        } else if(value instanceof Integer) {
            return 0 != (Integer) value;
        } else if(value instanceof Long) {
            return 0 != (Long) value;
        } else {
            return Boolean.parseBoolean(value.toString());
        }
    }

    public static boolean[] getBoolArrayDefault(Map config, String name, boolean[] defaultValue) {
        return config.containsKey(name) ? getBoolArray(config, name) : defaultValue;
    }

    public static boolean[] getBoolArray(Map config, String name) {
        List list = (List) config.get(name);
        boolean[] res = new boolean[list.size()];
        int i = 0;
        for(Object value : list) {
            res[i++] = toBool(value);
        }
        return res;
    }

    public static <T> T getObjectDefault(Constructor<T> constructor, Map config, String name, T defaultValue) {
        return config.containsKey(name) ? getObject(constructor, config, name) : defaultValue;
    }

    public static <T> T getObject(Constructor<T> constructor, Map config, String name) {
        return constructor.construct(config.get(name));
    }

    public static <T> T[] getArrayDefault(ArrayConstructor<T> constructor, Map config, String name, T[] defaultValue) {
        return config.containsKey(name) ? getArray(constructor, config, name) : defaultValue;
    }

    public static <T> T[] getArray(ArrayConstructor<T> constructor, Map config, String name) {
        List list = (List) config.get(name);

        if(list == null) {
            return constructor.array(0);
        }

        T[] res = constructor.array(list.size());
        int ix = 0;
        for(Object m : list) {
            T t = constructor.construct(m);
            constructor.setIx(t, ix);
            res[ix++] = t;
        }

        return res;
    }

    public static abstract strictfp class Constructor<T> {
        public abstract T construct(Object aConfig);
    }

    public static abstract strictfp class ArrayConstructor<T> extends Constructor<T> {
        public abstract T[] array(int length);

        public void setIx(T t, int ix) {

        }
    }

    public static <T extends Enum> T getEnum(Class<T> enumClass, String value) {
        for(T t : enumClass.getEnumConstants()) {
            if(t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException(value);
    }

    public static <T extends Enum> T getEnum(Class<T> enumClass, Map config, String propertyName) {
        return getEnum(enumClass, getString(config, propertyName));
    }

    public static <T extends Enum> T getEnumDefault(Class<T> enumClass, Map config, String propertyName, T defaultValue) {
        String s = getStringDefault(config, propertyName, null);

        return s != null ? getEnum(enumClass, s) : defaultValue;
    }

    public static <T extends Enum> T[] getEnumArrayDefault(Class<T> enumClass, Map config, String propertyName, T[] defaultValue) {
        return config.containsKey(propertyName) ? getEnumArray(enumClass, config, propertyName) : defaultValue;
    }

    public static <T extends Enum> T[] getEnumArray(Class<T> enumClass, Map config, String name) {
        List<String> values = (List<String>) config.get(name);
        T[] res = (T[]) Array.newInstance(enumClass, values.size());
        int i = 0;
        for(String value : values) {
            res[i++] = getEnum(enumClass, value);
        }
        return res;
    }

    public static <T> T[] getDictionaryValues(Class<T> valueClass, Map config, String propertyName, Map<String, T> dict) {
        List<String> keys = (List<String>) config.get(propertyName);
        T[] res = (T[]) Array.newInstance(valueClass, keys.size());
        int i = 0;
        for(String key : keys) {
            T value = dict.get(key);
            if(value == null) {
                throw new IllegalArgumentException(key);
            }
            res[i++] = value;
        }
        return res;
    }

    public static <T> T[] getDictionaryValuesDefault(Class<T> valueClass, Map config, String propertyName, Map<String, T> dict, T[] defaultValue) {
        if(config.containsKey(propertyName)) {
            return getDictionaryValues(valueClass, config, propertyName, dict);
        } else {
            return defaultValue;
        }
    }

    public static <T> T getDictionaryValue(Map config, String propertyName, Map<String, T> dict) {
        String key = (String) config.get(propertyName);
        T res;
        if(key == null || (res = dict.get(key)) == null) {
            throw new IllegalArgumentException(key);
        }
        return res;
    }


    public static Map<String, Integer> toIntMap(Map config) {
        Map<String, Integer> res = new HashMap<>();
        for(Object key : config.keySet()) {
            res.put((String) key, getInt(config, (String) key));
        }
        return res;
    }

}
