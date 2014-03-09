package com.example.p2pclient;

import java.util.Timer;
import java.util.TimerTask;

import com.example.p2pclient.SlidingTab.OnTriggerListener;
import com.example.p2pclient.R;
import com.example.p2pclient.R.drawable;
import com.example.p2pclient.R.string;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;

public class ScreenLocker extends RelativeLayout implements OnTouchListener{

	//private static final String THIS_FILE = "ScreenLocker";
	private Timer lockTimer;
	private Activity activity;
	private SlidingTab stab;

	public static final int WAIT_BEFORE_LOCK_LONG = 10000;
	public static final int WAIT_BEFORE_LOCK_START = 5000;

	private final static int SHOW_LOCKER = 0;
	private final static int HIDE_LOCKER = 1;
	
	
	
	public ScreenLocker(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnTouchListener(this);
		
		
		stab = new SlidingTab(getContext());
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		//lp.setMargins(0, 286, 0, 0);
		stab.setLayoutParams(lp);
		/*
		stab.setLeftHintText(R.string.answerCall);
		stab.setLeftTabResources(R.drawable.ic_jog_dial_answer, R.drawable.jog_tab_target_green, R.drawable.jog_tab_bar_left_answer, R.drawable.jog_tab_left_answer);
		stab.setRightHintText(R.string.clear_call);
		*/
		addView(stab);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		
		final int parentWidth = r - l;
		final int parentHeight = b - t;
		final int top = parentHeight * 3/5 - stab.getHeight()/2;
		final int bottom = parentHeight * 3/5 + stab.getHeight() / 2;
		stab.layout(0, top, parentWidth, bottom);
		
	}

	public void setActivity(Activity anActivity, OnTriggerListener l) {
		activity = anActivity;
		stab.setOnTriggerListener(l);
	}
	
	public void reset() {
		stab.resetView();
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}
	

	private class LockTimerTask extends TimerTask{
		@Override
		public void run() {
			handler.sendMessage(handler.obtainMessage(SHOW_LOCKER));
		}
	};
	

	class MsgProcessor extends Handler 
	{
		private CommService m_UIContext;
		private CommTask m_commTask;
		public MsgProcessor(CommService uiContext, CommTask commTask)
		{
			m_UIContext = uiContext;
			m_commTask = commTask;
		}
		
		@Override
		public void handleMessage(Message msg)
		{
			MessageEx msgEx = (MessageEx) msg.obj;
			msgEx.setContext(m_UIContext);
			
			
			switch (msgEx.getCommandType())
			{
			//connection establish
			
			//call from the visitor
			case VISITOR_CALL:
				//rsp
				String[] paras = new String[0];
				this.m_commTask.SendMessgage(MessageEx.buildRspMsg(msgEx, paras).getMessage());
				
				//need to do UI integration , post to UI thread to do it.
				
				//seems message couldn't be reused in this situation.
				Message new_msg = new Message();
				new_msg.what = msg.what;
				new_msg.obj = msg.obj;
				m_UIContext.getHandler().sendMessage(new_msg);
				
				break;
			
			case REGISTER:
				/*
				
				Log.w("AndroidTablet", "Resident register message " + msgEx.getMessage());
				String[] paras = msgEx.getParas();
				String unitNO = paras[0];
				//todo password verification
				
				m_server.AppResiger(unitNO, msgEx.getSocket());
			
				m_server.SendMessgage(unitNO, msgEx.getMessageID() + ":RSP:REGISTER:OK" );
				
				*/
				
				break;
			
			default:
				break;
			}
			
			super.handleMessage(msg);
		}
	}


	public void delayedLock(int time) {
		if(lockTimer != null) {
			lockTimer.cancel();
			lockTimer.purge();
			lockTimer = null;
		}
		
		lockTimer = new Timer();
		
		lockTimer.schedule(new LockTimerTask(), time);
	}
	
	
	public void show() {
		setVisibility(VISIBLE);
		if(activity != null) {
			Window win = activity.getWindow();
			win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	        win.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
		
	}
	
	public void hide() {
		setVisibility(GONE);
		if(activity != null) {
			Window win = activity.getWindow();
			win.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
	        win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}
	
	public void tearDown() {
		if(lockTimer != null) {
			lockTimer.cancel();
			lockTimer.purge();
			lockTimer = null;
		}
	}
	
	
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOW_LOCKER:
				show();
				break;
			case HIDE_LOCKER:
				hide();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};

}
