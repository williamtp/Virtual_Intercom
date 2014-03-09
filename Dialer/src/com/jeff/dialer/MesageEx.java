package com.jeff.dialer;

import java.net.Socket;
import java.util.Random;

import android.content.Context;

import com.jeff.dialer.MessageEx.CommandType;
import com.jeff.dialer.MessageEx.MessageType;


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
	public enum CommandType {REGISTER,VISITOR_CALL,DOOR_CTRL,VISITOR_CALL_ANSWER,DOOR_CTRL_RESIDENT,ECHO,GET_DOOR_STATUS,END_CALL,FIRE_ALARM};
	
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
	
	public static MessageEx buildReqMsg(CommandType cmdType, String[] paras)
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