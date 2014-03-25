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
			PerformInteraction pi = new PerformInteraction();
			Vector<TTSChunk> initialPrompt = TTSChunkFactory.createSimpleTTSChunks(initChunks);
			Vector<TTSChunk> helpPrompt = TTSChunkFactory.createSimpleTTSChunks(helpChunks);
			Vector<TTSChunk> tymoutPrompt = TTSChunkFactory.createSimpleTTSChunks(tymoutChunks);
			Vector<Integer> interactionChoiceSetIdList = new Vector<Integer>();
			interactionChoiceSetIdList.add(id);
			
			RPCRequest req;
			req = RPCRequestFactory.buildPerformInteraction(initialPrompt, displayable, interactionChoiceSetIdList, helpPrompt, tymoutPrompt, InteractionMode.VR_ONLY, 1000, 
					ProxyService.getInstance().nextCorrID());
			
				ProxyService.getProxyInstance().sendRPCRequest(req);
			
			
		}catch(SyncException e){
			
		}
	}

}
