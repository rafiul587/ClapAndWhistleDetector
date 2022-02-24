package com.example.clapandwhistledetector.util;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PreferenceUtil {

    private final Context mContext;

    public PreferenceUtil(Context context) {

        this.mContext = context;

    }

    public Boolean read(String str, Boolean bool) {
        return PreferenceManager.getDefaultSharedPreferences(this.mContext).getBoolean(str, bool);
    }

    public String readString(String str, String str2) {
        return PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(str, str2);
    }

    public void save(String str, Boolean bool) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        edit.putBoolean(str, bool);
        edit.apply();
    }

    public void saveString(String str, String str2) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        edit.putString(str, str2);
        edit.apply();
    }
}
