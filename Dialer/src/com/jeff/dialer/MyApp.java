package com.jeff.dialer;


import android.app.Application;
import android.content.Intent;
import android.preference.PreferenceManager;

public class MyApp extends Application
{
	private  TcpServer m_tcpServer = null;
	
	public TcpServer  getTcpServer() {return m_tcpServer;}
	public void setTcpServer(TcpServer tcpServer)
	{
		m_tcpServer = tcpServer;
	}
	
	
	@Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, CommService.class));
        
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
    }
	
}