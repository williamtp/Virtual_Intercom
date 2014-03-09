package com.example.p2pclient;

import android.app.Application;
import android.content.Intent;
import android.preference.PreferenceManager;

public class MyApp extends Application {
	
	@Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, CommService.class));
        
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
    }

}
