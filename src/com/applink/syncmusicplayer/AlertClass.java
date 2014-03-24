package com.applink.syncmusicplayer;

import java.util.Vector;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.Alert;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.enums.SpeechCapabilities;

import android.content.Context;

public class AlertClass {
	private Context mContext = null;
	
	public AlertClass(Context ctx) {
		// TODO Auto-generated constructor stub
	}
	
	public static AlertClass getInstance(Context ctx){
		return new AlertClass(ctx);
		
	}
	
	public void getAlert(String name, int time, String msg){
			Alert alert = new Alert();
			alert.setAlertText1(name);
			alert.setDuration(time);
			alert.setCorrelationID(ProxyService.getInstance().nextCorrID());
			Vector<TTSChunk> ttsChunks = new Vector<TTSChunk>();
			ttsChunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
					msg));
			alert.setTtsChunks(ttsChunks);
		try{
				ProxyService.getProxyInstance().sendRPCRequest(alert);
			
		}catch(SyncException e){
			
		}
		
	}

}
