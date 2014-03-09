package com.jeff.dialer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class CommService extends Service
{
	private TcpServer m_tcpServer = null;
	private Handler m_msgHandler;
	
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
		Log.w(Util.LOG_TAG, "onStartCommand...");
		return START_STICKY;
	}
	
	
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		Log.w(Util.LOG_TAG, "onCreate...");
			
		initService();
		
	}
	
	
	@Override
	public void onDestroy()
	{
		stopService(new Intent(this,CommService.class));
		super.onDestroy();
		Log.w(Util.LOG_TAG, "stop service");
		
		
	}
	
	public TcpServer getTcpServer()
	{
		return m_tcpServer;
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
	
	
	public Handler getHandler() {return m_msgHandler;}
	
	public String getUnitNO() {return "1234";}
	
	private void initService()
	{
		m_tcpServer = new TcpServer();
		m_msgHandler = new MsgProcessor(this,m_tcpServer);
		m_tcpServer.setServiceHandler(m_msgHandler);
		new Thread(m_tcpServer).start();
		
		HttpServer httpServer = new HttpServer(m_msgHandler);
		httpServer.start();
		
		((MyApp)getApplicationContext()).setTcpServer(m_tcpServer);
	}
	
}

class SrvConnection implements ServiceConnection
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

//Temporally wrien for fire alarm demo
class HttpServer extends Thread
{
	private Handler m_handler = null;
	public HttpServer(Handler handler)
	{
		m_handler = handler;
	}
	
	
	public void run()
	{
		ServerSocket server_socket = null;
		try 
		{
			server_socket = new ServerSocket(6001);
		} catch (IOException e) 
		{
			e.printStackTrace();
			return;
		}
		
		while ( !Thread.currentThread().isInterrupted() )
		{
			try
			{
				Log.w(Util.LOG_TAG, "start accept connection ...");
				Socket socket = server_socket.accept();
				
				//trigger fire_alarm
				MessageEx msgEx = MessageEx.buildReqMsg(MessageEx.CommandType.FIRE_ALARM, new String[]{});
				Message msg = new Message();
				msg.obj = msgEx;
				
				m_handler.sendMessage(msg);
				
				socket.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}	
		}
		
		if(server_socket != null)
		{
			try
			{
				server_socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}


class TcpServer implements Runnable
{
	public static final int SERVER_PORT=6000;
	private final ReentrantLock lock = new ReentrantLock();

	private Handler m_msgHandler = null;
	private Handler m_srvBaseHandler = null;
	
	public Handler getHandler(){return m_msgHandler;}
	
	private Timer m_fireAlarmTimer;
	
	public void setServiceHandler(Handler handler)
	{
		m_srvBaseHandler = handler;
		m_msgHandler = handler;
	}

	
	public Handler resetHandler( Handler handler )
	{
		Handler tmp = m_msgHandler;
		m_msgHandler = handler;
		return tmp;
	}
	
	public void resetHandler( )
	{
		m_msgHandler = m_srvBaseHandler;
	}

	private ServerSocket m_serverSocket = null;
	
	public ReentrantLock getLock() { return lock; }
	
	//public interface
	public MessageEx SendSynMsg(String UnitNO, MessageEx reqMsg, int timeout)
	{
		
		Log.i(Util.LOG_TAG, "Sending syn message," + "Unit NO = " + UnitNO + ", message = " + reqMsg.getMessage());
		
		Socket socket = this.mClientConnectionMap.get(UnitNO);
		if ( socket == null)
		{
			Log.w(Util.LOG_TAG, "Resident is not online," + "Unit NO = " + UnitNO);
			
			return new MessageEx("Resident is not online.");
		}
		
		lock.lock();
		
		Condition msglock = lock.newCondition();
		
		try
		{
			GetMsgMap().put(reqMsg.getMessageID(), msglock);
			
			SendMessage(UnitNO,reqMsg.getMessage());
			
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
			
			Log.i(Util.LOG_TAG, "Receive syn message," + "Unit NO = " + UnitNO + ", message = " + rsp.getMessage());
			
			
			return rsp;
		}
		else
		{
			return new MessageEx("No response from the resident side.");
		}
	}
	
	public void SendMessage(String message)
	{
		Set<String> units = this.mClientConnectionMap.keySet();
		for(Iterator it=units.iterator(); it.hasNext(); )
		{
			String unit = (String)it.next();
			SendMessage( unit, message );
		}
	}
	
	public Boolean SendMessage(String UnitNO, String message)
	{
		Log.i(Util.LOG_TAG, "Sending syn message," + "Unit NO = " + UnitNO + ", message = " + message);
		
		Socket socket = this.mClientConnectionMap.get(UnitNO);
		if ( socket == null)
		{
			Log.w(Util.LOG_TAG, "Resident is not online," + "Unit NO = " + UnitNO);
			return false;
		}
		
		try
		{
			m_sendTask.SendMessage(socket, message);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return true;
	}
	
	

	
	public void AppResiger(String unitNO, Socket socket)
	{
		this.mClientConnectionMap.put(unitNO, socket);
	}
	
	
	
	public TcpServer()
	{
	}
	
	//Mapping N.O. <-->  Clinet Socket
	private Map<String,Socket> mClientConnectionMap = Collections.synchronizedMap(new HashMap<String, Socket>());
	
	
	//multi-thread issue?
	private Map<String, Object> m_msgMap = Collections.synchronizedMap(new HashMap<String, Object>());
	
	public Map<String, Object> GetMsgMap()
	{
		return m_msgMap;
	}
	
	private SendTask m_sendTask;
	
	public void run()
	{
		boolean serverStart = false;
		int count = 0;
		while (count < 5)
		{
			try
			{
				m_serverSocket = new ServerSocket(SERVER_PORT);
				
				Log.w(Util.LOG_TAG, "create socket srever on port 6000");
				
				break;
			}
			catch(IOException e)
			{
				e.printStackTrace();
				try
				{
					Thread.sleep(30*1000);
					count++;
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		
		if( count == 5)
		{
			Log.w(Util.LOG_TAG, "app start failed!");
			return;
		}
	
		
		while ( !Thread.currentThread().isInterrupted() )
		{
			try
			{
				Log.w(Util.LOG_TAG, "start accept connection ...");
				Socket socket = m_serverSocket.accept();
				
				Log.w(Util.LOG_TAG, "Establish a new conneciton");
				new Thread(new ReceiveTask(socket, this)).start();
				
				//start send task thread
				m_sendTask = new SendTask(this);
				m_sendTask.start();
				
				//start firealarm detect timer
				this.m_fireAlarmTimer = new Timer();
				m_fireAlarmTimer.schedule(new TimerTask()
				{
					public void run()
					{
						MessageEx msgEx = MessageEx.buildReqMsg(MessageEx.CommandType.FIRE_ALARM, new String[]{});
						Message msg = new Message();
						msg.obj = msgEx;
						m_msgHandler.sendMessage(msg);
					}
				}, 2000, 5000);
				
				
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}	
		}
	}
	
	public void stop()
	{
		try 
		{
			m_serverSocket.close();
			Log.w(Util.LOG_TAG, "connection closed");
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class SendTask extends Thread
{
	
	class Msg2Send
	{
		public Socket m_socket;
		public String m_string;
		public Msg2Send(Socket socket, String str)
		{
			m_socket = socket;
			m_string = str;
		}
	}
	
	public Handler getHandler() {return m_msgHandler; }
	public void SendMessage(Socket socket, String str)
	{
		if( m_msgHandler != null)
		{
			Msg2Send obj = new Msg2Send(socket,str);
			Message msg = new Message();
			msg.obj = obj;
			m_msgHandler.sendMessage(msg);
		}
		else
		{
			Log.e(Util.LOG_TAG,"m_msgHandler is null, message is lost , msg = " + str);
		}
		
	}
	
	private Handler m_msgHandler;

	private TcpServer m_tcpServer;
	
	public SendTask(TcpServer server)
	{
		m_tcpServer = server;

	}
	
	
	public void run()
	{
		Looper.prepare(); 
		
		m_msgHandler = new Handler() { 
            public void handleMessage(Message msg)
            { 
            	Msg2Send msg_to_send = (Msg2Send)msg.obj;
            	try
        		{
        			PrintWriter out = new PrintWriter( 
        					new BufferedWriter( 
        							new OutputStreamWriter(msg_to_send.m_socket.getOutputStream())), true);
        			
        			out.println(msg_to_send.m_string);
        			Log.i(Util.LOG_TAG, "send out message: " + msg_to_send.m_string );
        		}
        		catch(Exception e)
        		{
        			e.printStackTrace();
        		}
            } 
         }; 
         Looper.loop(); 
	}
}

class ReceiveTask implements Runnable
{
	private Socket m_socket;
	private TcpServer m_tcpServer;
	
	public ReceiveTask(Socket socket, TcpServer tcpServer)
	{
		m_socket = socket;
		m_tcpServer = tcpServer;
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
				
				MessageEx msg = new MessageEx(str);
				msg.setSocket(m_socket);
				
				Log.i(Util.LOG_TAG, "receive message: " + str );
				
				if( msg.getMessgeType() == MessageEx.MessageType.RSP)
				{
					Map<String, Object> msgMap = m_tcpServer.GetMsgMap();
					
					Condition msgLock = (Condition) msgMap.get(msg.getMessageID());
					if( msgLock == null)
					{
						msgMap.remove(msg.getMessageID());
						continue;
					}
					
					
					msgMap.put(msg.getMessageID(), msg);
					
					try
					{
						m_tcpServer.getLock().lock();
						msgLock.signalAll();
					}
					finally
					{
						m_tcpServer.getLock().unlock();
					}
					
				}
				else
				{
					Message m = new Message();
					m.obj = msg;
					m_tcpServer.getHandler().sendMessage(m);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}


