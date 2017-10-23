package ru.gdgkazan.popularmovies.utils;

import android.util.Log;

/**
 * Created by dima on 19.08.17.
 */

public class Logger {

    public static String getClassTag(Class<?> someClass) {
        return someClass.getName();
    }

    public static void e(String tag, Exception e) {
        Log.e(tag, e.toString());
    }

    public static void t(String tag, Throwable t) {
        Log.e(tag, t.toString());
    }

    public static void d(String tag, String message) {
        Log.d(tag, message);
    }
}
