package com.google.glass.samples.waveform;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class BioSharedPreferences {
	public final static String PREFS_NAME = "bio_prefs";
	
	public static boolean getBackgroundUpdateFlag(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		
	    return prefs.getBoolean(
                context.getString(R.string.pref_key_flag_background_update),
                false);
    }
 
    public static void setBackgroundUpdateFlag(Context context, boolean newValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean(
                context.getString(R.string.pref_key_flag_background_update),
                newValue);
        prefsEditor.commit();
    }
}
