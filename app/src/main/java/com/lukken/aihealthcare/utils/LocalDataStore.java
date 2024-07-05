package com.lukken.aihealthcare.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalDataStore {
    private Context context;
    private SharedPreferences settings;
    public static final String PREFERENCES_FILE_NAME = "AIHealthCare";

    /**
     * 옵션값
     */
    public static final String SP_OP_CIS_CAM = "cis_camera";    //int 카메라id


    private static LocalDataStore instance;
    private LocalDataStore(Context c)
    {
        context = c.getApplicationContext();
        settings = context.getApplicationContext().getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static LocalDataStore getInstance(Context c)
    {
        if (instance == null)
            instance = new LocalDataStore(c);
        return instance;
    }

    public void preferenceClear()
    {
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();
    }

    public String getValueString(String key)
    {
        return settings.getString(key, "");
    }
    public String getValueString(String key, String defaultValue)
    {
        return settings.getString(key, defaultValue);
    }
    public int getValueInt(String key){ return settings.getInt(key, 0); }
    public int getValueInt(String key, int defaultValue){ return settings.getInt(key, defaultValue); }
    public float getValueFloat(String key){ return settings.getFloat(key, 1.f); }
    public float getValueFloat(String key, float defaultValue){ return settings.getFloat(key, defaultValue); }
    public boolean getValueBoolean(String key){ return settings.getBoolean(key, false); }
    public boolean getValueBoolean(String key, boolean defaultValue){ return settings.getBoolean(key, defaultValue); }

    public void setValueString(String key, String value)
    {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void setValueInt(String key, int value)
    {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public void setValueFloat(String key, float value)
    {
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    public void setValueBoolean(String key, boolean value)
    {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public void remove(String key)
    {
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(key);
        editor.commit();
    }
}
