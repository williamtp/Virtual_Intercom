package com.example.p2pclient;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

public class CommService extends Service
{
	private static final int MSG_CONN_OK = 1;
	private static final int RECEIVER_MSG = 2;
	public static final String LOG_TAG = "SSL_CLIENT";
	private static boolean m_serviceRunning = false;
	
	public boolean getRunningStatus() { return m_serviceRunning; }
	
	private boolean m_registed = false;
	public boolean getRigisterStatus() { return m_registed; }
	public void setRegisterStatus(boolean registed) { m_registed = registed;}
	
	
	public class startUpReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Intent service = new Intent(context, CommService.class);
			context.startService(service);
			Log.w("ServiceLearn", "service start up ...");
		}
	}
	
	public static final String TAG="commService";
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.w(CommService.TAG, "onStartCommand...");
		return START_STICKY;
	}
	
	
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		if(!getRunningStatus())
		{
		
			Log.w(CommService.TAG, "onCreate...");
			
			
			m_serviceRunning = true;
			
			initService();
		}
		
	}
	

	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		
		Log.w(CommService.TAG, "stop service");
		
		m_serviceRunning = false;
		
	}
	
	IBinder m_binder = new MyBinder();
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return m_binder;
	}
	
	class MyBinder extends Binder
	{
		CommService getService() {
	            // Return this instance of LocalService so clients can call public methods
	            return CommService.this;
	    }
		
		
	}
	
	private Handler m_msgHandler = new ServiceMsgHandler();;
	public CommTask m_commTask = null;
	
	
	public Handler getHandler() {return m_msgHandler;}
	
	public String getUnitNO() {return SettingMgr.getStringValue(this, SettingMgr.Preference.UNIT_NO) ;}
	
	private void initService()
	{
		//todo multi-thread issue
		m_commTask = new CommTask(this);
		Handler handler = new ServiceMsgHandler();
		m_commTask.RegisterReqMsgHandler(handler);
		
		new Thread(m_commTask).start();
	}
	
	
	
	class ServiceMsgHandler extends Handler
	{
		public void handleMessage(Message msg)
		{
			MessageEx msgEx = (MessageEx) msg.obj;
			
			switch ( msgEx.getCommandType() )
			{
			case VISITOR_CALL:
				
				
				Log.w(CommService.LOG_TAG, "Receive visitor call ...");
				
				//send response
				MessageEx rsp = MessageEx.buildRspMsg(msgEx, new String[0]);
				CommService.this.m_commTask.SendMessgage(rsp.getMessage());
				
				
				Intent incomingCall = new Intent( CommService.this ,ScreenLockActivity.class);
				incomingCall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				CommService.this.startActivity(incomingCall);
				
				
				
				break;
			case REGISTER:
				//indicator connection is OK in the UI
				setRegisterStatus(true);
				
				
				break;
				
			case FIRE_ALARM:
				processFireAlarm();
				break;
				
			default:
			}
			
			super.handleMessage(msg);
		}

		private void endCall() {
			// TODO Auto-generated method stub
			
		}

		private void processFireAlarm() 
		{
			//add a message in notification bar
			NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
			Notification n = new Notification(R.drawable.stat_notify_alarm, "Fire Alarm in your building", System.currentTimeMillis()); 
			n.flags = Notification.FLAG_AUTO_CANCEL;
			
			Intent fireActivity = new Intent(CommService.this, FireAlarmActivity.class);
			fireActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);  
			
			//PendingIntent
			PendingIntent contentIntent = PendingIntent.getActivity(
					CommService.this, 
			        R.string.app_name, 
			        fireActivity, 
			        PendingIntent.FLAG_UPDATE_CURRENT);
			                 
			n.setLatestEventInfo(
					CommService.this,
			        "Fire Alarm Notification", 
			        "Dear William, your building in on fire, run ..", 
			        contentIntent);
			
			n.defaults = Notification.DEFAULT_SOUND;

			long[] vibrate = {0,100,200,300};

			n.vibrate = vibrate;
			
			nm.notify(R.string.app_name, n);
			
			
		}
	}
}

class CommServiceConnection implements ServiceConnection
{
	
	@Override
	public void onServiceConnected(ComponentName compName, IBinder binder)
	{
		CommService.MyBinder localbinder = (CommService.MyBinder)binder;
		m_service = localbinder.getService();
		
		m_bound = true;
		
	}
	
	@Override
	public void onServiceDisconnected(ComponentName comp)
	{
		m_bound = false;
	}
	
	public CommService getService() {return m_service;}
	public boolean getServiceStatus() {return m_bound;}
	
	private CommService m_service;
	private boolean m_bound = false;
	
};


class CommTask implements Runnable
{
	//private static final String SERVER_IP = "sslacsdemo.wicp.net";
	private static final int RE_CONNECTION_INTERVAL =  10;
	private static final int SOCKET_RW_TIMEOUT = 5;
	
	private final ReentrantLock lock = new ReentrantLock();
	
	private String getHostName()
	{

		return SettingMgr.getStringValue(m_context, SettingMgr.Preference.HOST_NAME);
	}
	
	public void stopAll()
	{
		try
		{
			m_receiveThread.stop();
			this.stop();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private int getPort()
	{
		return 	SettingMgr.getIntValue(m_context, SettingMgr.Preference.COMMUNICATION_PORT);
	}

	private Handler m_msgHandler = null;
	private Handler m_uiHandler = null;
	private CommService m_context;
	private Socket m_socket = null;
	private Timer m_heartTimer = null;
	private Thread m_receiveThread = null;

	//multi-thread issue?
	private Map<String, Object> m_msgMap = Collections.synchronizedMap(new HashMap<String, Object>());
	
	public ReentrantLock getLock(){return lock;}
	
	public CommService getContext(){return m_context;}
	
	//public interface
	public MessageEx SendSynMsg(MessageEx reqMsg, int timeout)
	{	
		Log.w(CommService.LOG_TAG, "SendSynMsg , msg = " + reqMsg.getMessage() );
		
		lock.lock();
		Condition msglock = lock.newCondition();
		try
		{
			GetMsgMap().put(reqMsg.getMessageID(), msglock);
			
			SendMessgage(reqMsg.getMessage());
			
			msglock.await(timeout, TimeUnit.SECONDS);
		}
		catch(Exception e)
		{
			return new MessageEx("Exception occured when waiting for the answer from resident side.");
		}
		finally
		{
			lock.unlock();
		}
		
		Object rsp_o = GetMsgMap().remove(reqMsg.getMessageID());
		if(rsp_o!=null &&(rsp_o != msglock)) 
		{
			MessageEx rsp = (MessageEx) rsp_o;
			
			Log.w(CommService.LOG_TAG, "Receive syn message, message = " + rsp.getMessage());
			
			
			return rsp;
		}
		else
		{
			return new MessageEx("No response from the resident side.");
		}
	}
	
	public Boolean SendMessgage(String message)
	{
		try
		{
			Thread thread = new Thread(new SendTask(this, getSocket() ,message) );
			thread.start();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return true;
	}
	
	
	public void RegisterReqMsgHandler(Handler handler)
	{
		m_msgHandler = handler;
	}
	
	public void RegisterUIHandler(Handler handler)
	{
		m_uiHandler = handler;
	}
	
	public Socket getSocket()
	{
		return m_socket;
	}
	
	
	public CommTask(CommService context)
	{
		m_context = context;
	}
	
	
	
	public Map<String, Object> GetMsgMap()
	{
		return m_msgMap;
	}
	
	public Handler getHandler(){return m_msgHandler;}
	
	
	public Handler getUIHandler(){return m_uiHandler;}
	
	public void Notify(Message msg)
	{
		getHandler().sendMessage(msg);
		
		if (getUIHandler() != null)
		{
			getUIHandler().sendMessage(Message.obtain(msg));
		}
	}
	
	private void startHeartbeatCheck()
	{
		m_heartTimer = new Timer();
		int counter = 0;
		//start a timer, which will send echo message to server and wait for response.
		//if server fail to answer the echo,client to re-connected to the server.
		m_heartTimer.schedule( new TimerTask()
		{
			public void run()
			{
				int counter = 0;
				while( true)
				{
					String[] paras = {m_context.getUnitNO()};
					MessageEx echoMsg = MessageEx.buildReqMsg(MessageEx.MessageType.REQ, MessageEx.CommandType.ECHO,paras );
					MessageEx rsp = SendSynMsg(echoMsg, 3);
					if( !rsp.getResult() )
					{
						counter ++;
					}
					else
					{
						counter = 0;
						Log.w(CommService.LOG_TAG,"Heart beat detection succesful");
						break;
					}
					
					if ( counter >= 3)
					{
						Log.w(CommService.LOG_TAG, "Connection with server is broken, trying to reconnect ...");
						
						
						 CommTask.this.m_context.setRegisterStatus(false);
						
						//stop the timer
						
						m_heartTimer.cancel();
						
						//re-establish the connection.
						reConnect();
						
						break;
					}
				}
				
				
			}
		} , 1000 * 10, 1000*10 ) ;
	}
	
	private void reConnect()
	{
		new Thread( new Runnable()
		{
			public void run()
			{
				if (null != m_heartTimer) m_heartTimer.cancel();
				if (null != m_receiveThread) m_receiveThread.interrupt();
				if (null != m_socket)
				{
					try {
						m_socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				initConnection();
			}
		}).start();
	}
	
	
	private void initConnection()
	{
		Boolean connection_ok = false;
		while( !connection_ok )
		{
			try
			{
				InetAddress addr = InetAddress.getByName(getHostName());
				m_socket = new Socket (addr,getPort());
				connection_ok = true;
				Log.w(CommService.LOG_TAG, "connect to server " + getHostName() + ":" + Integer.toString(getPort()));
				
				m_receiveThread = new Thread(new ReceiveTask(m_socket, this));
				m_receiveThread.start();
				
				//Register APP
				String[] paras = {m_context.getUnitNO()};

				MessageEx regMsg = MessageEx.buildReqMsg(MessageEx.MessageType.REQ, MessageEx.CommandType.REGISTER, paras);
				SendMessgage(regMsg.getMessage());
				
				
				
				
				startHeartbeatCheck();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				
				try 
				{
					Thread.sleep(RE_CONNECTION_INTERVAL * 1000);
				} 
				catch (InterruptedException e1)
				{
					e1.printStackTrace();
				}
			}
			
			
		}
		
		
	}
	
	public void run()
	{
		initConnection();
	}
	
	public void stop()
	{
		try 
		{
			m_socket.close();
			Log.w("AndroidTablet", "connection closed");
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}


class SendTask implements Runnable
{
	private Socket m_socket;
	private String m_message;
	private CommTask m_commTask;
	
	public SendTask(CommTask commTask,Socket socket, String msg_to_send)
	{
		m_commTask = commTask;
		m_socket = socket;
		m_message = msg_to_send;
	}
	
	public void run()
	{
		try
		{
			PrintWriter out = new PrintWriter( 
					new BufferedWriter( 
							new OutputStreamWriter(m_socket.getOutputStream())), true);
			
			out.println(m_message);
			Log.w(CommService.LOG_TAG, "send out message: " + m_message );
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}

class ReceiveTask implements Runnable
{
	private Socket m_socket;
	private CommTask m_commTask;
	
	public ReceiveTask(Socket socket, CommTask commTask)
	{
		m_socket = socket;
		m_commTask = commTask;
	}
	
	public void run()
	{
		try
		{
			while (!Thread.currentThread().isInterrupted())
			{
				BufferedReader input = new BufferedReader( new InputStreamReader( m_socket.getInputStream() ) );
				
				String str = input.readLine();
				if (str == null)
				{
					continue;
				}
				
				Log.w(CommService.LOG_TAG,"ReceiveTask Receive messge, msg =  " + str);
				
				MessageEx msg = new MessageEx(str);
				msg.setSocket(m_socket);
				
				
				if( msg.getMessgeType() == MessageEx.MessageType.RSP)
				{
					//to refactor
					if(msg.getCommandType() == MessageEx.CommandType.REGISTER)
					{
						m_commTask.getContext().setRegisterStatus(true);
					}
					
					Map<String, Object> msgMap = m_commTask.GetMsgMap();
					
					Condition msgLock = (Condition) msgMap.get(msg.getMessageID());
					if( msgLock == null)
					{
						msgMap.remove(msg.getMessageID());
						continue;
					}
					
					
					msgMap.put(msg.getMessageID(), msg);
					
					try
					{
						m_commTask.getLock().lock();
						msgLock.signalAll();
					}
					finally
					{
						m_commTask.getLock().unlock();
					}
					
				}
				else
				{
					Message m = new Message();
					m.obj = msg;
					m_commTask.Notify(m);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}

class MessageEx 
{
	//define MESSAGE TYPE, MESSAGE FORMAT
	
	//MESSAGE TYPE   :   REQ /  RSP
	//MESSAGE ID     :   8 byte string
	//MESSAG COMMAND :   REGISTER/VISITOR_CALL/DOOR_CTRL
	//COMMAND PARA   :   STR1,STR2,STR3...
	//MESSAGE FORMAT :   $ID:$TYPE:CMD:PARA
	private String m_msg;
	private MessageType m_msgType;
	private CommandType m_cmdType;
	private String m_msgId;
	private String[] m_paras;
	private Boolean m_result;
	private Socket m_socket;
	private Context m_context;
	
	public enum MessageType {REQ,RSP};
	public enum CommandType {REGISTER,VISITOR_CALL,VISITOR_CALL_ANSWER,DOOR_CTRL,DOOR_CTRL_RESIDENT,ECHO,GET_DOOR_STATUS,END_CALL,FIRE_ALARM};
	
	public MessageEx(String msg)
	{
		m_msg = msg;
		
		
		String[] strs = m_msg.split(":");
		if( strs.length < 3)
		{
			m_msgId="-1";
			m_result = false;
		}
		else
		{
			m_result = true;
			m_msgId= strs[0];
			m_msgType = MessageType.valueOf(strs[1]);
			m_cmdType = CommandType.valueOf(strs[2]);
			if ( strs.length > 3)
			{
				m_paras = strs[3].split(",");
			}
		}
	}
	
	public static String generateMsgID()
	{
		Random rand = new Random();
		return Integer.toString(rand.nextInt(0xFFFFFF));
	}
	
	public static MessageEx genMessage(String messageId, MessageType msgType,CommandType cmdType, String[] paras)
	{
		String paraStr = "";
		for (int i=0; i< paras.length; ++i)
		{
			paraStr = paraStr + paras[i] + ",";
		}
		
		String command = messageId + ":" + msgType.toString() + ":" + cmdType.toString() + ":" + paraStr;
		
		return new MessageEx(command);
	}
	
	public static MessageEx buildReqMsg(MessageType msgType,CommandType cmdType, String[] paras)
	{
		String id = generateMsgID();
		
		return genMessage(id, MessageType.REQ, cmdType, paras);
	}
	
	public static MessageEx buildRspMsg(MessageEx reqMsg, String[] paras)
	{
		return genMessage(reqMsg.getMessageID(), MessageType.RSP, reqMsg.getCommandType(),paras);
	}
	
	public Boolean getResult()
	{
		return m_result;
	}
	
	public void setSocket(Socket socket)
	{
		m_socket = socket;
	}
	
	public Socket getSocket()
	{
		return m_socket;
	}
	
	public String getMessage()
	{
		return m_msg;
	}
	
	public String getMessageID()
	{
		return m_msgId;
	}
	
	public MessageType getMessgeType()
	{
		return m_msgType;
	}
	
	public CommandType getCommandType()
	{
		return m_cmdType;
	}
	
	public String[] getParas()
	{
		return m_paras;
	}
	
	public void setContext(Context context)
	{
		m_context = context;
	}
	
	public Context getContext()
	{
		return m_context;
	}
	
}

