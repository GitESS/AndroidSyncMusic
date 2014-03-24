package com.applink.syncmusicplayer;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.rpc.ScrollableMessage;

import android.content.Context;

public class ScrollableMessageClass {
	private Context mContext = null;
	
	public ScrollableMessageClass(Context ctx) {
		// TODO Auto-generated constructor stub
		mContext = ctx;
	}

	public static ScrollableMessageClass getInstance(Context ctx){
		return new ScrollableMessageClass(ctx);
		
	}
	
	public void getScrollableMessage(String msg){
		try{
			ScrollableMessage scrMsg = new ScrollableMessage();
			scrMsg.setCorrelationID(ProxyService.getInstance().nextCorrID());
			scrMsg.setTimeout(3000);
			scrMsg.setScrollableMessageBody(msg);
			
			ProxyService.getProxyInstance().sendRPCRequest(scrMsg);
			
		}catch(SyncException e){
			
		}
	}
}
