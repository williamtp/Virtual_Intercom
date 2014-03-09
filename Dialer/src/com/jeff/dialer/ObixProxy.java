package com.jeff.dialer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.util.Log;

class ObixProxy 
{
	
	static DefaultHttpClient getHttpClient()
	{
		DefaultHttpClient httpClient = new DefaultHttpClient();
		httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY) 
		, new UsernamePasswordCredentials("admin","admin") );
		
		return httpClient;
	}
	
	static public String GetUNNodeName(Context context)
	{
		String NodeName = SettingMgr.getStringValue(context, SettingMgr.Preference.NODENAME);
		if ( NodeName == "")
		{
			String Uri = String.format("http://%1$s/obix/network", 
					SettingMgr.getStringValue(context, SettingMgr.Preference.UNADDR)  );
			
			HttpGet getOper = new HttpGet(Uri);
			
			HttpResponse rsp1;
			try 
			{
				rsp1 = getHttpClient().execute(getOper);
				
				
				String rsp_str = EntityUtils.toString(rsp1.getEntity()) ;
				
				int index = rsp_str.indexOf("name=");
				int i = index + 6 ;
				
				while (i < rsp_str.length()-1 && rsp_str.charAt(i) != '\"' )
				{
					NodeName += rsp_str.charAt(i);
					++i;
				}
				
				SettingMgr.setStringValue(context, SettingMgr.Preference.NODENAME,NodeName );
				
				return NodeName;
			
			} 
			catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return NodeName;
		
	}
	
	static private String GetNodeURI(Context context)
	{
		String URI_TPL = "http://%1$s/obix/network/%2$s/DEV100/";
		String uri = String.format( URI_TPL, SettingMgr.getStringValue(context, SettingMgr.Preference.UNADDR), GetUNNodeName(context) );
		
		return uri;
	}
	
	static void doorControl(Context context, boolean unlock)
	{
		String uri = GetNodeURI(context) + "BO1/Present_Value/";
		HttpPut putOper = new HttpPut(uri);
		
		putOper.addHeader("Content-Type", "text/xml");
		String relsDoorXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<bool name=\"Present_Value\" href=\"Present_Value/\" val=\"" + Boolean.toString(unlock) 
		+ "\" writable=\"true\"/>";
		
		try {
			
			putOper.setEntity(new StringEntity(relsDoorXML));
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		}
		
		HttpResponse rsp1;
		try 
		{
			Log.w(Util.LOG_TAG,"Obix send to UN: " + relsDoorXML);
			rsp1 = getHttpClient().execute(putOper);
			Log.w(Util.LOG_TAG,"Obix response from UN: " + rsp1.getStatusLine());
		} 
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	static public void Release(final Context context)
	{
		doorControl(context,true);
		
		new Timer().schedule(new TimerTask()
		{
			public void run()
			{
				doorControl(context, false);
			}
		}, 10*1000);
	}
	
	//1 open; 0 close; -1 unkonw
	static public int GetDoorStatus()
	{
		DefaultHttpClient httpClient = new DefaultHttpClient();
		
		//UN has performance issue when doing http request, which will cause the door status refresh very slow
		//interact with ACX first here.
		HttpGet getOper = new HttpGet("http://192.168.1.90/pe/DoorStatus");
		getOper.addHeader("Content-Type", "application/x-www-form-urlencoded");
		
		int door_status = -1;
		
		HttpResponse rsp1;
		try 
		{
			rsp1 = httpClient.execute(getOper);
			
			
			String status = EntityUtils.toString(rsp1.getEntity()) ;
			
			door_status = Integer.parseInt(status.trim().replaceAll("\r\n", ""));
		
		} 
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return door_status;
	}

	static public void WriteLog( Context context, String userName, String UnitNo, String Descrition )
	{
		
		String uri = GetNodeURI(context) + "AV1/Description/";
		
		HttpPut putOper = new HttpPut(uri);
		
		java.util.Date current=new java.util.Date();
        java.text.SimpleDateFormat sdf=new java.text.SimpleDateFormat("MMMMMM d yyyy HH:mm:ss"); 
        String date_str =sdf.format(current);
		
		putOper.addHeader("Content-Type", "text/xml");
		String log_full_tpl = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<str name=\"Description\" href=\"Description/\" val=\"%1$s,%2$s,%3$s,%4$s,\" writable=\"true\" max=\"255\"/>";
		
		String log_str = String.format(log_full_tpl, date_str,  UnitNo , userName,Descrition );
		
		Log.w("TabletApp", log_str);
		
		try {
			
			putOper.setEntity(new StringEntity(log_str));
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		}
		
		HttpResponse rsp1;
		try 
		{
			rsp1 = getHttpClient().execute(putOper);
			Log.w(Util.LOG_TAG,"Obix response from UN: " + rsp1.getStatusLine());
		} 
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static public boolean GetFireAlarmSignal(Context context)
	{
		String uri = GetNodeURI(context) + "BI1/Present_Value/";
			
		HttpGet getOper = new HttpGet(uri);
		
		
		
		HttpResponse rsp1;
		try 
		{
			String value="";
			
			rsp1 = getHttpClient().execute(getOper);
			
			
			String rsp_str = EntityUtils.toString(rsp1.getEntity()) ;
			
			int index = rsp_str.indexOf("val=");
			int i = index + 5 ;
			
			while (i < rsp_str.length()-1 && rsp_str.charAt(i) != '\"' )
			{
				value += rsp_str.charAt(i);
				++i;
			}
			
			return Boolean.parseBoolean(value);
		
		} 
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
		
	}
}
