package com.developers.meraki.projectw;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;

    // shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "projectx";

    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setIntValue(String level, int point) {
        editor.putInt(level, point);
        editor.commit();
    }

    public int getIntValue(String prefName) {
        return pref.getInt(prefName, 0);
    }

    public void setStringValue(String prefName, String quote) {
        editor.putString(prefName, quote);
        editor.commit();
    }

    public String getStringValue(String prefName) {
        return pref.getString(prefName, "");
    }


}
