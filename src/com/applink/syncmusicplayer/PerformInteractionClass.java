package com.applink.syncmusicplayer;

import java.util.Vector;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.RPCRequest;
import com.ford.syncV4.proxy.RPCRequestFactory;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.PerformInteraction;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;

import android.content.Context;
import android.util.Log;

public class PerformInteractionClass {
	private Context mContext = null;
	
	public PerformInteractionClass(Context ctx) {
		// TODO Auto-generated constructor stub
		mContext = ctx;
	}
	
	public static PerformInteractionClass getInstance(Context ctx){
		return new PerformInteractionClass(ctx);
		
	}
	
	public void performInteraction(String initChunks, String helpChunks, String tymoutChunks, String displayable, int id){
	
		try{
			//Log.i("PIClass", ""+initChunks+ "/"+helpChunks+"/"+tymoutChunks+"/"+displayable+"/"+id);
			PerformInteraction pi = new PerformInteraction();
			pi.setCorrelationID(ProxyService.getInstance().nextCorrID());
			Vector<TTSChunk> initialPrompt = TTSChunkFactory.createSimpleTTSChunks(initChunks);
			pi.setInitialPrompt(initialPrompt);
			Vector<TTSChunk> helpPrompt = TTSChunkFactory.createSimpleTTSChunks(helpChunks);
			pi.setHelpPrompt(helpPrompt);
			Vector<TTSChunk> tymoutPrompt = TTSChunkFactory.createSimpleTTSChunks(tymoutChunks);
			pi.setTimeoutPrompt(tymoutPrompt);
			Vector<Integer> interactionChoiceSetIdList = new Vector<Integer>();
			Log.i("PIClass", ""+id);
			interactionChoiceSetIdList.add(id);
			pi.setInteractionChoiceSetIDList(interactionChoiceSetIdList);
			pi.setTimeout(10000);
		
			
			RPCRequest req;
			req = RPCRequestFactory.buildPerformInteraction(pi.getInitialPrompt(), displayable, pi.getInteractionChoiceSetIDList(), pi.getHelpPrompt(), pi.getTimeoutPrompt(), InteractionMode.VR_ONLY, pi.getTimeout(), 
					ProxyService.getInstance().nextCorrID());
			ProxyService.getProxyInstance().sendRPCRequest(req);
			
			
		}catch(SyncException e){
			
		}
	}

}
