package com.applink.syncmusicplayer;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ford.syncV4.proxy.RPCMessage;

public class RecordMessage {

private String date;
private String msgName;
private RPCMessage message;
	
	public RecordMessage(RPCMessage rpc) {
		message = rpc;
		msgName = rpc.getFunctionName() + " ("
		+ rpc.getMessageType() + ")";
		date = new SimpleDateFormat("hh:mm:ss SSSS").format(new Date(System.currentTimeMillis()));
	
	}
	
	public RecordMessage(String msgName) {
		this.msgName = msgName;
		message = null;
		date = new SimpleDateFormat("hh:mm:ss SSSS").format(new Date(System.currentTimeMillis()));
	
	}

	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	
	public RPCMessage getMessage() {
		return message;
	}
	public void setMessage(RPCMessage message) {
		this.message = message;
	}
	
	public String getMessageName() {
		return msgName;
	}
	public void setMessageName(String msgName) {
		this.msgName = msgName;
	}
}
