package com.jeff.dialer;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

class MsgProcessor extends Handler 
{
	private Context m_context;
	private TcpServer m_tcpServer;
	
	public Context getContext()
	{
		return m_context;
	}
	
	public TcpServer getTcpServer()
	{
		return m_tcpServer;
	}
	
	public MsgProcessor(Context context, TcpServer server)
	{
		m_context = context;
		m_tcpServer = server;
	}
	
	private static boolean m_lastfireState = false;
	
	/*
	class GetDoorStatus extends Thread
	{
		private MessageEx m_msg;
		public GetDoorStatus(MessageEx msg)
		{
			m_msg = msg;
		}
		
		public void run()
		{
			
			//UN has performance issue when doing http request, which will cause the door status refresh very slow
			//interact with ACX first here.
			
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet getOper = new HttpGet("http://192.168.1.90/pe/DoorStatus");
			getOper.addHeader("Content-Type", "application/x-www-form-urlencoded");
			
			int door_status = -1;
			
			HttpResponse rsp1;
			try 
			{
				rsp1 = httpClient.execute(getOper);
				String status = EntityUtils.toString(rsp1.getEntity()) ;
				
				door_status = Integer.parseInt(status);
			 
			} 
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			int status = ObixProxy.GetDoorStatus();
			
			String unitNO = m_msg.getParas()[0];
			
			String[] rsp_para = { Integer.toString(status) };
			MessageEx rsp = MessageEx.buildRspMsg(m_msg, rsp_para);
			MsgProcessor.this.m_server.SendMessgage(unitNO, rsp.getMessage() );
		}
	}
	*/
	
	protected void releaseDoor()
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				ObixProxy.Release(m_context);
			}
		}).start();
	}
	
	protected void writeLog(String userName, String UnitNo, String Description)
	{
		class LogWriter implements Runnable
		{
			private String m_userName;
			private String m_unitNo;
			private String m_Desc;
			public LogWriter(String userName, String UnitNo, String Description)
			{
				m_userName = userName;
				m_unitNo = UnitNo;
				m_Desc = Description;
			}
			
			public void run()
			{
				ObixProxy.WriteLog(m_context, m_userName, m_unitNo, m_Desc);
			}
		}
		new Thread(new LogWriter(userName, UnitNo, Description)).start();
	}
	
	@Override
	public void handleMessage(Message msg)
	{
		MessageEx msgEx = (MessageEx) msg.obj;
		msgEx.setContext(m_context);
		
		//connection establish
		Message new_msg = new Message();
		new_msg.what = msg.what;
		new_msg.obj = msg.obj;
		
		switch (msgEx.getCommandType())
		{
		case DOOR_CTRL_RESIDENT:
			releaseDoor();
			writeLog(msgEx.getParas()[2], msgEx.getParas()[1], "Remote access");
			break;
		
		case REGISTER:
		{
			Log.w(Util.LOG_TAG, "Receive register message " + msgEx.getMessage());
			String[] paras = msgEx.getParas();
			String unitNO = paras[0];
			//todo password verification
			
			m_tcpServer.AppResiger(unitNO, msgEx.getSocket());
		
			m_tcpServer.SendMessage(unitNO, msgEx.getMessageID() + ":RSP:REGISTER:OK" );
			
			String UnitNO = msgEx.getParas()[0];
			Toast.makeText(m_context, "User " + UnitNO + " is online." , Toast.LENGTH_SHORT).show();
			
			break;
		}
		
			
		case ECHO:
		{
			Log.w(Util.LOG_TAG, "Receive echo message from client " + msgEx.getMessage());
			String unitNO = msgEx.getParas()[0];
			MessageEx rsp = MessageEx.buildRspMsg(msgEx, new String[0]);
			m_tcpServer.SendMessage(unitNO, rsp.getMessage() );
			break;
		}
		
		case FIRE_ALARM:
		{
			
			//get fireware signal
			
			signalFireAlarm();
		}
		
		
		default:
			//update UI
			super.handleMessage(msg);
			break;
		}

	}

	private void signalFireAlarm() {
		new Thread(new Runnable()
		{
			public void run()
			{
				boolean alarm = ObixProxy.GetFireAlarmSignal(m_context);
				if(alarm && !m_lastfireState)
				{
					Log.w(Util.LOG_TAG, "Detect fire alarm signal ");
					
					//send fire alarm to app clients 
					MessageEx rsp = MessageEx.buildReqMsg(MessageEx.CommandType.FIRE_ALARM, new String[0]);
					m_tcpServer.SendMessage(rsp.getMessage() );
					
					
				}
				
				m_lastfireState = alarm;

				
			}
			
		}).start();
		
		
	}
}

