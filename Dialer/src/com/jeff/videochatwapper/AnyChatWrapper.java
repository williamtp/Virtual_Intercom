package com.jeff.videochatwapper;

import java.util.Timer;
import java.util.TimerTask;

import com.bairuitech.anychat.AnyChatBaseEvent;
import com.bairuitech.anychat.AnyChatCoreSDK;
import com.bairuitech.anychat.AnyChatDefine;
import com.bairuitech.anychat.AnyChatStateChgEvent;

import android.app.Activity;
import android.content.DialogInterface.OnClickListener;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.jeff.dialer.Util;
import com.jeff.dialer.R;

public class AnyChatWrapper extends Activity implements AnyChatBaseEvent, AnyChatStateChgEvent
{
	class ChatInfo
	{
		public final static String Mobile_User_Name = "VideoChat_Mobile";
		public final static String Mobile_Passw0rd = "Passw0rd";
		
		public final static String Tablet_User_Name = "VideoChat_Tablet";
		public final static String Tablet_Passw0rd = "Passw0rd";
		
		public final static int Room_NO = 1;
		
	}
	
	private ConfigEntity configEntity;
	public AnyChatCoreSDK anychat;
    private boolean bNeedRelease = false;
    private boolean bCallStart = false;
    private boolean bEnterRoom = false;
    private boolean bLogin = false;
    private boolean IsDisConnect = false;
    private boolean bVideoAreaLoaded = false;
    private boolean bSelfVideoOpened = false;
    private boolean bOtherVideoOpened = false;
    private boolean bOnPaused = false;

    private int m_otherUserId;
    private int dwLocalVideoWidth = 0;
	private int dwLocalVideoHeight = 0;
	private int dwRemoteVideoHeight = 0;
	private int dwRemoteVideoWidth = 0;
	
    private SurfaceView m_selfView;
    private SurfaceView m_otherView;
    
    private Timer mTimer = new Timer(true);
	private TimerTask mTimerTask;
	private Handler handler;
	



	
	public enum CallEndReason {CONNECT_FAILURE, PEER_END_CALL, LOGIN_FAILURE, ENTER_ROOM_FAILURE, LINK_CLOSE};
	
	//sub class need to overried this method.
	protected void onVideoCallEnded(CallEndReason reason){}
	
	
	public void startVideoChat()
	{
		configEntity = ConfigService.LoadConfig(this);
		
		InitialSDK();

		//connect
		anychat.Connect(configEntity.ip, configEntity.port);
		
		//login
		anychat.Login( ChatInfo.Tablet_User_Name, ChatInfo.Tablet_Passw0rd );
		
		//check video status
		mTimerTask = new TimerTask() {
			public void run() {
				if(handler == null)
					return;
				Message mesasge = new Message();
				handler.sendMessage(mesasge);
			}
		};

		mTimer.schedule(mTimerTask, 1000, 100);

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				CheckVideoStatus();
				SetVolum();
				super.handleMessage(msg);
			}
		};
		
	}
	
	private void SetVolum() {
		anychat.GetUserSpeakVolume(m_otherUserId);
		anychat.GetUserSpeakVolume(-1);
	}
	
	
	
	protected void onRestart() {
		super.onRestart();
		
		// 如果是采用Java视频显示，则需要设置Surface的CallBack
		if(AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) == AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
			int index = anychat.mVideoHelper.bindVideo(m_selfView.getHolder());
			anychat.mVideoHelper.SetVideoUser(index, m_otherUserId);
		}
		
		refreshAV();
		bOnPaused = false;
	}
	
	@Override
	protected void onDestroy()
	{
		if(bNeedRelease)
		{
			anychat.mSensorHelper.DestroySensor();
			anychat.Release(); 
			
			/*
			if(!IsDisConnect)
	    	{
	    		android.os.Process.killProcess(android.os.Process.myPid());
	    	}
	    	*/
		}
		
		if( mTimer != null)
		{
			mTimer.cancel();
		}
		
		
		
		super.onDestroy();
	}
	
	
	private void InitialSDK() {
		if (anychat == null) {
			anychat = new AnyChatCoreSDK();
			
			if (configEntity.useARMv6Lib != 0)
				AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_CORESDK_USEARMV6LIB, 1);
			anychat.InitSDK(android.os.Build.VERSION.SDK_INT, 0);
			
			anychat.mSensorHelper.InitSensor(this);
			
			anychat.SetBaseEvent(this);
			anychat.SetStateChgEvent(this);
			
			ApplyVideoConfig();
			
			bNeedRelease = true;
		}
	}
	
	private void InitialLayout() {
		m_selfView = (SurfaceView) findViewById(R.id.selfView);
		m_otherView = (SurfaceView) findViewById(R.id.mobileView);
		
		// 如果是采用Java视频采集，则需要设置Surface的CallBack
		if(AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER) == AnyChatDefine.VIDEOCAP_DRIVER_JAVA) {
			m_selfView.getHolder().addCallback(AnyChatCoreSDK.mCameraHelper);			
		}
		
		// 如果是采用Java视频显示，则需要设置Surface的CallBack
		if(AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) == AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
			int index = anychat.mVideoHelper.bindVideo(m_otherView.getHolder());
			anychat.mVideoHelper.SetVideoUser(index, m_otherUserId);
		}
		
		m_otherView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			
			@Override
			public void onGlobalLayout() {
				// TODO Auto-generated method stub
				if(!bVideoAreaLoaded)
				{
					adjuestVideoSize(m_otherView.getWidth(), m_otherView.getHeight());
					bVideoAreaLoaded=true;
				}
			}
		});

		SurfaceHolder holder = m_otherView.getHolder();
		holder.setKeepScreenOn(true);
		anychat.UserCameraControl(m_otherUserId, 1);
		anychat.UserSpeakControl(m_otherUserId, 1);
		//m_selfView.setOnClickListener(this);
		ConfigEntity configEntity = ConfigService.LoadConfig(this);		
		if (configEntity.videoOverlay != 0) {
			m_selfView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		// 判断是否显示本地摄像头切换图标
		if(AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER) == AnyChatDefine.VIDEOCAP_DRIVER_JAVA) {
			if(AnyChatCoreSDK.mCameraHelper.GetCameraNumber() > 1) {
				AnyChatCoreSDK.mCameraHelper.SelectVideoCapture(AnyChatCoreSDK.mCameraHelper.CAMERA_FACING_FRONT);
			}
		}else {
			String[] strVideoCaptures = anychat.EnumVideoCapture();
			if (strVideoCaptures != null && strVideoCaptures.length > 1) {
				// 默认打开前置摄像头
				for(int i=0;i<strVideoCaptures.length;i++)
				{
					String strDevices=strVideoCaptures[i];
					if(strDevices.indexOf("Front")>=0) {
						anychat.SelectVideoCapture(strDevices);
						break;
					}
				}
			}
		}
		// 打开本地音频、视频设备	
		anychat.UserCameraControl(-1, 1);
		anychat.UserSpeakControl(-1, 1);	
		
		anychat.UserCameraControl(m_otherUserId, 1);
		anychat.UserSpeakControl(m_otherUserId, 1);
	}
	

	@Override
	public void OnAnyChatConnectMessage(boolean bSuccess) {
		
		//connect successful
		if (bSuccess)
		{
			Log.w(Util.LOG_TAG, "Connect to Chat server succesfully.");
			bLogin = true;
		}
		else
		{
			Log.w(Util.LOG_TAG, "Fail to connect to server.");
			
			onVideoCallEnded( CallEndReason.CONNECT_FAILURE );
			
		}
		
	}

	@Override
	public void OnAnyChatLoginMessage(int dwUserId, int dwErrorCode) {
		if(dwErrorCode == 0)
		{
			Log.w(Util.LOG_TAG, "Log in Chat server successfully.");
			
			//enter room
			anychat.EnterRoom(ChatInfo.Room_NO, "");	
			
			bEnterRoom = true;
			
		}
		else
		{
			Log.w(Util.LOG_TAG, "Fail to log in to Chat server");
			
			onVideoCallEnded( CallEndReason.LOGIN_FAILURE );
		}
		
	}

	@Override
	public void OnAnyChatEnterRoomMessage(int dwRoomId, int dwErrorCode) {
		
		if(dwErrorCode == 0)
		{
			Log.w(Util.LOG_TAG, "Enter chat room successfully.");
		}
		else
		{
			Log.w(Util.LOG_TAG, "Fail to enter chat room.");
			
			onVideoCallEnded( CallEndReason.ENTER_ROOM_FAILURE );
		}
		
	}

	@Override
	public void OnAnyChatOnlineUserMessage(int dwUserNum, int dwRoomId)
	{
		Log.w(Util.LOG_TAG, "There are " + dwUserNum + " online users.");
		
		if(dwUserNum > 0)
		{
			//get online user
			int[] userID = anychat.GetOnlineUser();
	    	for(int i=0; i<userID.length ; i++)
	    	{
	    		String user_name = anychat.GetUserName(userID[i]);
	    		
	    		//mobile is online
	    		if (user_name.compareTo(ChatInfo.Mobile_User_Name) == 0 )
	    		{
	    			callUser(userID[i]);
	    			
	    			break;
	    		}
	    	}
		}
	}

	@Override
	public void OnAnyChatUserAtRoomMessage(int dwUserId, boolean bEnter) 
	{
		Log.w(Util.LOG_TAG, String.format("User movement,  user id = %1$d, action = %2$b", dwUserId, bEnter) );
		
		if ( anychat.GetUserName(dwUserId) == ChatInfo.Mobile_User_Name)
		{
			if( bEnter )
			{
				callUser(dwUserId);
				
			}
			else
			{
				//when tablet leave the connection, call shall be stopped.
				callStop();
			}
		}
	}
	
	public void onClick(View v) {}

	@Override
	public void OnAnyChatLinkCloseMessage(int dwErrorCode) 
	{
		Log.w(Util.LOG_TAG, "Chat link close.");
		
		if(bOtherVideoOpened)
		{
			anychat.UserCameraControl(m_otherUserId, 0);
			anychat.UserSpeakControl(m_otherUserId, 0);
			bOtherVideoOpened = false;
		}
		if(bSelfVideoOpened)
		{
			anychat.UserCameraControl(-1, 0);
			anychat.UserSpeakControl(-1, 0);
			bSelfVideoOpened = false;
		}
		
		
		IsDisConnect = true;
		
		onVideoCallEnded( CallEndReason.LINK_CLOSE );
		
	}
	
	private void callUser(int userID)
	{
		Log.w(Util.LOG_TAG, "Start call user, userId = " + Integer.toString(userID));
		
		m_otherUserId = userID;
		
		if(bCallStart) return;
				
		bCallStart = true;
		
		bOtherVideoOpened = true;
		
		InitialLayout();
		
		
	}
	
	private void refreshAV() 
	{
		anychat.UserCameraControl(m_otherUserId, 1);
		anychat.UserSpeakControl(m_otherUserId, 1);
		anychat.UserCameraControl(-1, 1);
		anychat.UserSpeakControl(-1, 1);
		bOtherVideoOpened = false;
	}
	
	private void CheckVideoStatus() {
		if(bOnPaused)
			return;
		if (!bOtherVideoOpened) 
		{

			String device_name = anychat.GetCurVideoCapture();
			
			//Log.w(Util.LOG_TAG, "current video device " + device_name);
			
			int camera_state = anychat.GetCameraState(m_otherUserId);
			int video_width = anychat.GetUserVideoWidth(m_otherUserId);
			
			if ( camera_state == 2 && video_width != 0) {
				
				Log.w(Util.LOG_TAG, "display other audio and video view");
				
				SurfaceHolder holder = m_otherView.getHolder();
				// 如果是采用内核视频显示（非Java驱动），则需要设置Surface的参数
				if(AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) != AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
					holder.setFormat(PixelFormat.RGB_565);
					holder.setFixedSize(anychat.GetUserVideoWidth(m_otherUserId), anychat.GetUserVideoHeight(m_otherUserId));
				}
				Surface s = holder.getSurface();
				anychat.SetVideoPos(m_otherUserId, s, 0, 0, 0, 0);
				bOtherVideoOpened = true;
			}
		}
		if (!bSelfVideoOpened) 
		{
			int camera_state = anychat.GetCameraState(-1);
			int video_width = anychat.GetUserVideoWidth(-1);
	
			if (camera_state == 2 && video_width != 0) {
				
				Log.w(Util.LOG_TAG, "display self audio and video view");
				
				SurfaceHolder holder = m_selfView.getHolder();
				// 如果是采用内核视频显示（非Java驱动），则需要设置Surface的参数
				if(AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) != AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
					holder.setFormat(PixelFormat.RGB_565);
					holder.setFixedSize(anychat.GetUserVideoWidth(-1), anychat.GetUserVideoHeight(-1));
				}
				Surface s = holder.getSurface();
				anychat.SetVideoPos(-1, s, 0, 0, 0, 0);
				bSelfVideoOpened = true;
			}
		}
	}
	
	private void adjuestVideoSize(int width, int height) {

		if (3 * width > 4 * height) {

			dwRemoteVideoHeight = height;
			dwRemoteVideoWidth = (int) (4.0 / 3.0 * dwRemoteVideoHeight);
		} else {
			dwRemoteVideoWidth = width;
			dwRemoteVideoHeight = (int) (3.0 / 4.0 * dwRemoteVideoWidth);
		}
		dwLocalVideoWidth = dwRemoteVideoWidth;
		dwLocalVideoHeight = dwRemoteVideoHeight;
		//FrameLayout.LayoutParams layoutParamSelf=new FrameLayout.LayoutParams(dwLocalVideoWidth, dwLocalVideoHeight);
		//m_selfView.setLayoutParams(layoutParamSelf);
		//LinearLayout.LayoutParams layoutPramOther=new LinearLayout.LayoutParams(dwLocalVideoWidth, dwLocalVideoHeight);
		//m_otherView.setLayoutParams(layoutPramOther);
	}
	
	private void callStop()
	{
		Log.w(Util.LOG_TAG, "call stoped");
		if(bOtherVideoOpened)
		{
			//request user's video and audio
			anychat.UserCameraControl(m_otherUserId, 0);
			anychat.UserSpeakControl(m_otherUserId, 0);
			
			bOtherVideoOpened = false;
		}
		
		if(bEnterRoom)
		{
			anychat.LeaveRoom(ChatInfo.Room_NO);
		}
		
		if(bLogin)
		{
			anychat.Logout();
		}
		
		
	}
	
	
	private void ApplyVideoConfig()
	{
		ConfigEntity configEntity = ConfigService.LoadConfig(this);
		if(configEntity.configMode == 1)		// 自定义视频参数配置
		{
			// 设置本地视频编码的码率（如果码率为0，则表示使用质量优先模式）
			AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_BITRATECTRL, configEntity.videoBitrate);
			if(configEntity.videoBitrate==0)
			{
				// 设置本地视频编码的质量
				AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_QUALITYCTRL, configEntity.videoQuality);
			}
			// 设置本地视频编码的帧率
			AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_FPSCTRL, configEntity.videoFps);
			// 设置本地视频编码的关键帧间隔
			AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_GOPCTRL, configEntity.videoFps*4);
			// 设置本地视频采集分辨率
			AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_WIDTHCTRL, configEntity.resolution_width);
			AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_HEIGHTCTRL, configEntity.resolution_height);
			// 设置视频编码预设参数（值越大，编码质量越高，占用CPU资源也会越高）
			AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_PRESETCTRL, configEntity.videoPreset);
		}
		// 让视频参数生效
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_APPLYPARAM, configEntity.configMode);
		// P2P设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_NETWORK_P2PPOLITIC, configEntity.enableP2P);
		// 本地视频Overlay模式设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_OVERLAY, configEntity.videoOverlay);
		// 回音消除设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_AUDIO_ECHOCTRL, configEntity.enableAEC);
		// 平台硬件编码设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_CORESDK_USEHWCODEC, configEntity.useHWCodec);
		// 视频旋转模式设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_ROTATECTRL, configEntity.videorotatemode);
		// 视频采集驱动设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER, configEntity.videoCapDriver);
		// 本地视频采集偏色修正设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_FIXCOLORDEVIA, configEntity.fixcolordeviation);
		// 视频显示驱动设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL, configEntity.videoShowDriver);
		// 音频播放驱动设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_AUDIO_PLAYDRVCTRL, configEntity.audioPlayDriver);
		// 音频采集驱动设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_AUDIO_RECORDDRVCTRL, configEntity.audioRecordDriver);
		// 视频GPU渲染设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_GPUDIRECTRENDER, configEntity.videoShowGPURender);
		// 本地视频自动旋转设置
		AnyChatCoreSDK.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_AUTOROTATION, configEntity.videoAutoRotation);
	}


	@Override
	public void OnAnyChatMicStateChgMessage(int dwUserId, boolean bOpenMic) {
		// TODO Auto-generated method stub
		Log.w(Util.LOG_TAG, String.format("Camera state changed,  userId = %1$d, camera state = %2$b", dwUserId,bOpenMic ));
		
	}


	@Override
	public void OnAnyChatCameraStateChgMessage(int dwUserId, int dwState) {
		// TODO Auto-generated method stub
		Log.w(Util.LOG_TAG, String.format("Camera state changed,  userId = %1$d, camera state = %2$d", dwUserId,dwState ));
		
	}


	@Override
	public void OnAnyChatChatModeChgMessage(int dwUserId, boolean bPublicChat) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void OnAnyChatActiveStateChgMessage(int dwUserId, int dwState) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void OnAnyChatP2PConnectStateMessage(int dwUserId, int dwState) {
		// TODO Auto-generated method stub
		
	}
	
}
