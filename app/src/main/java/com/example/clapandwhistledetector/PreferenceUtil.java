package com.example.clapandwhistledetector;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

class PreferenceUtil {

    private final Context mContext;

    public PreferenceUtil(Context context) {

        this.mContext = context;

    }

    public Boolean read(String str, Boolean bool) {
        return PreferenceManager.getDefaultSharedPreferences(this.mContext).getBoolean(str, bool);
    }

    public void save(String str, Boolean bool) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        edit.putBoolean(str, bool);
        edit.apply();
    }
}
