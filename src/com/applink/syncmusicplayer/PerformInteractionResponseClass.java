package com.applink.syncmusicplayer;

import java.util.Vector;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.Speak;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.enums.SpeechCapabilities;

import android.content.Context;

public class PerformInteractionResponseClass { 
	private Context mContext;
	
	public PerformInteractionResponseClass(Context ctx) {
		// TODO Auto-generated constructor stub
		this.mContext = ctx;
	}
	
	public static PerformInteractionResponseClass getInstance(Context ctx){
		return new PerformInteractionResponseClass(ctx);
		
	}
	
	public void getPerformInteractionResponse(/*Vector<TTSChunk> chunks, */String txt){
		
		Vector<TTSChunk> chunks = new Vector<TTSChunk>();
		try{
		Speak msg = new Speak();
		msg.setTtsChunks(chunks);
		msg.setCorrelationID(ProxyService.getInstance().nextCorrID());
		chunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT, txt));
		
		
		ProxyService.getProxyInstance().sendRPCRequest(msg);
		}catch(SyncException e){
			
		}
	}

}
