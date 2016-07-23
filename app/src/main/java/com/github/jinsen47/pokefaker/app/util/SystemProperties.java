package com.github.jinsen47.pokefaker.app.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by sunbreak on 1/7/16.
 */
public class SystemProperties {

    public static final String PROXY_CLASS_NAME = "android.os.SystemProperties";
    public static final String PROPERTY_SEARCH = "miuibbs.app.search";

    public static String get(String key) {
        ClassLoader classLoader = SystemProperties.class.getClassLoader();
        try {
            Class<?> clz = classLoader.loadClass(PROXY_CLASS_NAME);
            Class[] params = {String.class};
            Method mGet = clz.getMethod("get", params);
            Object[] arguments = {key};
            return (String) mGet.invoke(clz, arguments);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String get(String key, String defValue) {
        ClassLoader classLoader = SystemProperties.class.getClassLoader();
        try {
            Class<?> clz = classLoader.loadClass(PROXY_CLASS_NAME);
            Class[] params = {String.class, String.class};
            Method mGet = clz.getMethod("get", params);
            Object[] arguments = {key, defValue};
            return (String) mGet.invoke(clz, arguments);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return defValue;
    }

    public static int getInt(String key, int defValue) {
        ClassLoader classLoader = SystemProperties.class.getClassLoader();
        try {
            Class<?> clz = classLoader.loadClass(PROXY_CLASS_NAME);
            Class[] params = {String.class, int.class};
            Method mGet = clz.getMethod("getInt", params);
            Object[] arguments = {key, defValue};
            return (Integer) mGet.invoke(clz, arguments);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return defValue;
    }
}

