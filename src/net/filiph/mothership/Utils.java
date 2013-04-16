package net.filiph.mothership;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class providing methods for storing array to Preferences.
 */
public class Utils {
    public static boolean saveArray(String[] array, String arrayName, Context mContext) {
        SharedPreferences preferences = mContext.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(arrayName + "_size", array.length);
        for (int i = 0; i < array.length; i++)
            editor.putString(arrayName + "_" + i, array[i]);
        return editor.commit();
    }

    public static String[] loadArray(String arrayName, Context mContext) {
        SharedPreferences preferences = mContext.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        int size = preferences.getInt(arrayName + "_size", 0);
        String array[] = new String[size];
        for (int i = 0; i < size; i++)
            array[i] = preferences.getString(arrayName + "_" + i, null);
        return array;
    }

    public static boolean addToArray(String string, String arrayName, Context mContext) {
        SharedPreferences preferences = mContext.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();
        int size = preferences.getInt(arrayName + "_size", 0);
        editor.putInt(arrayName + "_size", size + 1);

        editor.putString(arrayName + "_" + size, string);
        return editor.commit();
    }

    public static boolean clearArray(String arrayName, Context mContext) {
        SharedPreferences preferences = mContext.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();
        int size = preferences.getInt(arrayName + "_size", 0);

        for (int i = 0; i < size; i++) {
            editor.remove(arrayName + "_" + i);
        }
        editor.remove(arrayName + "_size");
        return editor.commit();
    }

    public static String getLastStringFromArray(String arrayName, Context mContext) {
        SharedPreferences preferences = mContext.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        int size = preferences.getInt(arrayName + "_size", 0);
        return preferences.getString(arrayName + "_" + (size - 1), null);
    }
}
