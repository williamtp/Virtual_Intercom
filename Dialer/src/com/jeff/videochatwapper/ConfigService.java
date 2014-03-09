package com.jeff.videochatwapper;

import com.bairuitech.anychat.AnyChatDefine;
import com.jeff.dialer.SettingMgr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

@SuppressLint("WorldWriteableFiles")
public class ConfigService {

	public static ConfigEntity LoadConfig(Context context)
	{
		ConfigEntity configEntity = new ConfigEntity();
    	SharedPreferences  share = context.getSharedPreferences("perference", Context.MODE_WORLD_WRITEABLE);  

        configEntity.name = share.getString("name", "");
        configEntity.password = share.getString("password", "");
        configEntity.IsSaveNameAndPw = share.getString("IsSaveNameAndPw", "").equals("1") ? true : false;
      
        //configEntity.ip = share.getString("ip", "demo.anychat.cn");
        configEntity.ip = SettingMgr.getStringValue(context, SettingMgr.Preference.STREAM_SERVER_IP);
        configEntity.port = SettingMgr.getIntValue(context, SettingMgr.Preference.STREAM_SERVER_PORT);
        
        configEntity.configMode = share.getInt("configMode", ConfigEntity.VIDEO_MODE_CUSTOMCONFIG);
        configEntity.resolution_width = share.getInt("resolution_width", 320);
        configEntity.resolution_height = share.getInt("resolution_height", 240);
        configEntity.videoBitrate = SettingMgr.getIntValue(context, SettingMgr.Preference.STREAM_BIT_RATET);
        configEntity.videoFps = SettingMgr.getIntValue(context, SettingMgr.Preference.STREAM_FRAME_RATE);
        configEntity.videoQuality = SettingMgr.getIntValue(context, SettingMgr.Preference.STREAM_QUALITY);
        configEntity.videoPreset = share.getInt("videoPreset", 3);
        configEntity.videoOverlay = share.getInt("videoOverlay", 1);
        configEntity.videorotatemode = share.getInt("VideoRotateMode", 0);
        configEntity.videoCapDriver = share.getInt("VideoCapDriver", AnyChatDefine.VIDEOCAP_DRIVER_JAVA);
        configEntity.fixcolordeviation = share.getInt("FixColorDeviation", 0);
        configEntity.videoShowGPURender = share.getInt("videoShowGPURender", 0);
        configEntity.videoAutoRotation = share.getInt("videoAutoRotation", 0);

        configEntity.enableP2P = share.getInt("enableP2P", 1);
        configEntity.useARMv6Lib = share.getInt("useARMv6Lib", 0);
        configEntity.enableAEC = share.getInt("enableAEC", 1);
        configEntity.useHWCodec = share.getInt("useHWCodec", 0);
        configEntity.videoShowDriver = share.getInt("videoShowDriver", AnyChatDefine.VIDEOSHOW_DRIVER_JAVA);
        configEntity.audioPlayDriver = share.getInt("audioPlayDriver", AnyChatDefine.AUDIOPLAY_DRIVER_JAVA);   
        configEntity.audioRecordDriver = share.getInt("audioRecordDriver", AnyChatDefine.AUDIOREC_DRIVER_JAVA);
		return configEntity;
	}
	
	public static void SaveConfig(Context context,ConfigEntity configEntity)
	{
    	SharedPreferences  share = context.getSharedPreferences("perference",  Context.MODE_WORLD_WRITEABLE);  
        Editor editor = share.edit();//取得编辑�?        
        editor.putString("name", configEntity.name);
        editor.putString("password", configEntity.password);
        editor.putString("IsSaveNameAndPw", configEntity.IsSaveNameAndPw ? "1" : "0");
        
        editor.putString("ip", configEntity.ip);
        editor.putInt("port", configEntity.port);
        
        editor.putInt("configMode", configEntity.configMode);
        editor.putInt("resolution_width", configEntity.resolution_width);
        editor.putInt("resolution_height", configEntity.resolution_height);

        editor.putInt("videoBitrate", configEntity.videoBitrate);
        editor.putInt("videoFps", configEntity.videoFps);
        editor.putInt("videoQuality", configEntity.videoQuality);
        editor.putInt("videoPreset", configEntity.videoPreset);
        editor.putInt("videoOverlay", configEntity.videoOverlay);
        editor.putInt("VideoRotateMode", configEntity.videorotatemode);
        editor.putInt("VideoCapDriver", configEntity.videoCapDriver);
        editor.putInt("FixColorDeviation", configEntity.fixcolordeviation);
        editor.putInt("videoShowGPURender", configEntity.videoShowGPURender);
        editor.putInt("videoAutoRotation", configEntity.videoAutoRotation);
        
        editor.putInt("enableP2P", configEntity.enableP2P);
        editor.putInt("useARMv6Lib", configEntity.useARMv6Lib);
        editor.putInt("enableAEC", configEntity.enableAEC);
        editor.putInt("useHWCodec", configEntity.useHWCodec);
        editor.putInt("videoShowDriver", configEntity.videoShowDriver);
        editor.putInt("audioPlayDriver", configEntity.audioPlayDriver); 
        editor.putInt("audioRecordDriver", configEntity.audioRecordDriver);         
    	editor.commit();
		
	}
	
}
