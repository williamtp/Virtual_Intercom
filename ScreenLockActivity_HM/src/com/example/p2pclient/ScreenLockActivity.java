package com.example.p2pclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import com.bairuitech.anychat.AnyChatBaseEvent;
import com.bairuitech.anychat.AnyChatCoreSDK;
import com.bairuitech.anychat.AnyChatDefine;
import com.bairuitech.anychat.AnyChatStateChgEvent;
import com.example.p2pclient.R;
import com.example.p2pclient.R.id;
import com.example.p2pclient.R.layout;
import com.example.p2pclient.TestM.CallEndReason;
import com.example.p2pclient.TestM.ChatInfo;
import com.example.videochatwapper.AnyChatWrapper;
import com.example.videochatwapper.ConfigEntity;
import com.example.videochatwapper.ConfigService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class ScreenLockActivity extends Activity
	implements SlidingTab.OnTriggerListener
{
    /** Called when the activity is first created. */
	private ScreenLocker lockOverlay;
	
	private CommServiceConnection m_conn = new CommServiceConnection();
	
	//show the call loading progress
	private ProgressBar progressBar;
	
	//play the door bell
	private static MediaPlayer m_mp = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	super.onCreate(savedInstanceState);
    	
    	setContentView(R.layout.main);
    	
    	
        lockOverlay = (ScreenLocker) findViewById(R.id.lockerOverlay);
		lockOverlay.setActivity(this, this);
		
		Intent service = new Intent(this,CommService.class);
		bindService(service,m_conn,Context.BIND_AUTO_CREATE);
		
		//vibrate
		Vibrator vib = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		
		long pattern[]={0,800,200,1200,300,2000,400,3000};
		vib.vibrate(pattern,-1);
					
				
		//
		try
		{
			PlayDoorDell();

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
    }
    
    @Override
    protected void onResume()
    {
    	super.onResume();
    	
    	PlayDoorDell();
    }
    
    @Override
	public void onDestroy()
	{
		super.onDestroy();
		unbindService(m_conn);
		
		if(m_mp != null)
		{
			m_mp.release();
		}
		
	}
    

    
    @Override
	public void onTrigger(View v, int whichHandle) {
		
		Vibrator vib = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		
		switch (whichHandle) 
		{
		
		//accept the call
		case LEFT_HANDLE:
		{
			
			//delive message to server
			String[] para = {"Allow"};
			MessageEx msg = MessageEx.buildReqMsg(MessageEx.MessageType.REQ, MessageEx.CommandType.VISITOR_CALL_ANSWER, para );
			
			//todo deal with failure.
			sendMessage(msg);
			
			vib.cancel();
			m_mp.stop();
			m_mp.release();
			
			Intent videoChatIntent = new Intent(this, VideoChatActivity.class);
			startActivity(videoChatIntent);
			
			finish();
			break;
		}
		
		//deny the call
		case RIGHT_HANDLE:
		{
			//delive message to server
			String[] para = {"Deny"};
			MessageEx msg = MessageEx.buildReqMsg(MessageEx.MessageType.REQ, MessageEx.CommandType.VISITOR_CALL_ANSWER, para );
			
			
			//todo deal with failure.
			sendMessage(msg);
			
			m_mp.stop();
			m_mp.release();
			
			vib.cancel();
			finish();
			break;
		}	
		default:
			break;
		}
	}
    
    private boolean sendMessage(MessageEx msg)
    {
    	if( m_conn.getServiceStatus() )
		{
			m_conn.getService().m_commTask.SendMessgage(msg.getMessage());
			return true;
		}
		else
		{
			Log.w(Util.LOG_TAG,"Service is not ready, send message failed. msg = " + msg.getMessage());
			return false;
		}
    }

    
	
	private void PlayDoorDell() 
	{
		try
		{
			m_mp = new MediaPlayer();
			AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(R.raw.test);
			m_mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			m_mp.setAudioStreamType(AudioManager.STREAM_RING);
			afd.close();
			m_mp.prepare();
			
			//play the door bell
			m_mp.seekTo(0);
			m_mp.setVolume(1000, 1000);
			m_mp.start();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			if(m_mp !=null)
			{
				m_mp.stop();
				m_mp.release();
			}
		}
	}
    
}

