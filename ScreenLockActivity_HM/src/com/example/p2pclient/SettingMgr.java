package com.example.p2pclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingMgr 
{
	public class Preference
	{
		public static final String HOST_NAME = "HostName";
		public static final String COMMUNICATION_PORT = "Port";
		public static final String UNIT_NO = "UnitNO";
		public static final String USER_NAME = "UserName";
		public static final String STREAM_SERVER_IP = "StreamServerIP";
		public static final String STREAM_SERVER_PORT="StreamServerPort";
		public static final String STREAM_FRAME_RATE = "VideoFrameRate";
		public static final String STREAM_BIT_RATET="VideoBitRate";
		public static final String STREAM_QUALITY="VideoQuality";
		public static final String NODENAME = "UN_NODENAME";
	}
	
	public static String getStringValue(Context context, String key)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key,"");
	}
	
	public static int getIntValue(Context context, String key)
	{
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString(key,""));
	}
	
	public static void setStringValue(Context context, String key, String value)
	{
		PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).commit();
	}

	public static void setIntValue(Context context, String key, int value)
	{
		PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, Integer.toString(value)).commit();
	}
	
	

}
