package com.google.glass.samples.waveform;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class GlassPreferences extends PreferenceActivity {
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getPreferenceManager().setSharedPreferencesName (
			BioSharedPreferences.PREFS_NAME);
			addPreferencesFromResource(R.xml.prefs);
	}
	
	
}
