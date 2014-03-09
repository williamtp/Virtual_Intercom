package com.example.p2pclient;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.example.p2pclient.MessageEx.CommandType;
import com.example.p2pclient.MessageEx.MessageType;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends Activity {
	
	private static final int MSG_CONN_OK = 1;
	private static final int RECEIVER_MSG = 2;
	public static final String LOG_TAG = "SSL_CLIENT";

	
	private CommServiceConnection m_conn = new CommServiceConnection();
	
	Timer m_statusTimer;
	
	private static final int CMD_ONLINE = 1;
	private static final int CMD_OFFLINE = 2;
	private static final int CMD_OPEN = 3;
	private static final int CMD_CLOSE = 4;
	private Handler m_uiHandler;
	
	ImageView m_imgDoorStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		
		Log.w(MainActivity.LOG_TAG, "Start P2PClient ...");
		
	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		m_imgDoorStatus = (ImageView) findViewById(R.id.doorStatus);
		
		Intent service = new Intent(this,CommService.class);
		
		bindService(service,m_conn,Context.BIND_AUTO_CREATE);
		
		((TextView)findViewById(R.id.callInfo)).setText("Unit N.O. " + SettingMgr.getStringValue(this, SettingMgr.Preference.UNIT_NO));
		
		findViewById(R.id.btnUnlock).setOnClickListener( new OnClickListener()
		{
			public void onClick(View view)
			{
				switch(view.getId())
				{
				case R.id.btnUnlock:
					String unit_no = SettingMgr.getStringValue(MainActivity.this, SettingMgr.Preference.UNIT_NO);
					String user_name = SettingMgr.getStringValue(MainActivity.this, SettingMgr.Preference.USER_NAME);
					//delive message to server
					String[] para = {"UnLock", unit_no, user_name};
					MessageEx msg = MessageEx.buildReqMsg(MessageEx.MessageType.REQ, MessageEx.CommandType.DOOR_CTRL_RESIDENT, para );
					sendMessage(msg);
					
					refreshDoorStatus();
					break;
				}
			}
		});
		
		
		m_uiHandler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				ActionBar actionBar = getActionBar(); // || getActionBar()
				
				switch(msg.what)
				{
				case CMD_ONLINE:
					actionBar.setIcon(R.drawable.online );
					break;
				case CMD_OFFLINE:
					actionBar.setIcon(R.drawable.offline);
					break;
				case CMD_OPEN:
					m_imgDoorStatus.setImageResource(R.drawable.door_open);
					break;
				case CMD_CLOSE:
					m_imgDoorStatus.setImageResource(R.drawable.door_close);
					break;
				}
			}
		};
		
	}
	

	
	private void refreshDoorStatus()
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				Log.w(LOG_TAG, "refresh the door status");
				
				String UnitNO = SettingMgr.getStringValue(MainActivity.this, SettingMgr.Preference.UNIT_NO);
				//get door status
				MessageEx doorStatusMgr = MessageEx.buildReqMsg(MessageType.REQ,CommandType.GET_DOOR_STATUS, new String[]{UnitNO});
				MessageEx rsp = sendSynMessage(doorStatusMgr);
				if(rsp.getResult())
				{
					String[] paras = rsp.getParas();
					
					Message msg = new Message();
					msg.what= Integer.parseInt(paras[0]) == 1 ? CMD_OPEN :CMD_OFFLINE;
					m_uiHandler.sendMessage(msg);
				}
			}
		}).start();
		
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		m_statusTimer.cancel();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		if( m_conn.getService() != null )
		{
			getActionBar().setIcon(m_conn.getService().getRigisterStatus() ? R.drawable.online : R.drawable.offline );
		}
		else
		{
			getActionBar().setIcon(R.drawable.offline);
		}
		
		m_statusTimer = new Timer();
		m_statusTimer.schedule( new TimerTask()
		{
			public void run()
			{
				
				
				if( MainActivity.this.m_conn.getService() != null )
				{
					Message msg = new Message();
					msg.what= m_conn.getService().getRigisterStatus() ? CMD_ONLINE :CMD_OFFLINE;
					m_uiHandler.sendMessage(msg);
				}
				
			}
		}, 2 * 1000,5 * 1000);
		
		refreshDoorStatus();
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
			Log.w(LOG_TAG,"Service is not ready, send message failed. msg = " + msg.getMessage());
			return false;
		}
    }
	
	private MessageEx sendSynMessage(MessageEx msg)
    {
		MessageEx rsp;
    	if( m_conn.getServiceStatus() )
		{
    		rsp = m_conn.getService().m_commTask.SendSynMsg(msg, 3);
		}
		else
		{
			rsp = new MessageEx("Service is not ready, send message failed.");
			Log.w(LOG_TAG,"Service is not ready, send message failed. msg = " + msg.getMessage());
		}
    	
    	return rsp;
    }
    
	

	
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.p2_pclient, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	            openSettings();
	            return true;
	        case R.id.action_logOut:
	        	logOut();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void logOut() {
		// TODO Auto-generated method stub
		android.os.Process.killProcess(android.os.Process.myPid());
	}



	void openSettings()
	{
		Intent setting = new Intent(this,SettingsActivity.class);
		startActivity(setting);
	}
	
	
	@Override
	public void onDestroy()
	{
		m_statusTimer.cancel();
		super.onStart();
		unbindService(m_conn);
		
		
		
	}
}	







