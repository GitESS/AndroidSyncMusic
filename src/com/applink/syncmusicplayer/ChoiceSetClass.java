package com.applink.syncmusicplayer;

import java.util.Arrays;
import java.util.Vector;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.RPCRequest;
import com.ford.syncV4.proxy.RPCRequestFactory;
import com.ford.syncV4.proxy.rpc.Choice;

import android.content.Context;
import android.util.Log;

public class ChoiceSetClass { 
	private Context mContext = null;
	
	public ChoiceSetClass(Context ctx) {
		// TODO Auto-generated constructor stub
		mContext = ctx;
	}
	
	public static ChoiceSetClass getInstance(Context ctx){
		return new ChoiceSetClass(ctx);
		
	}
	
	public void createChoiceSet(int id, String menuName){
		Vector<Choice> choiceVector = new Vector<Choice>();
		try{
			Choice choice = new Choice();
			choice.setChoiceID(id);
			choice.setMenuName(menuName);
			choice.setVrCommands(new Vector<String>(Arrays.asList(new String[] { menuName, "Track " + id })));
			choiceVector.add(choice);
			
			RPCRequest trackMsg;
			//Log.i("InteractionChoiceSet -", "" + nextInteractionChoiceCorrID());
			trackMsg = RPCRequestFactory.buildCreateInteractionChoiceSet(
					choiceVector, ProxyService.getInstance().nextInteractionChoiceCorrID(), ProxyService.getInstance().nextCorrID());

			ProxyService.getProxyInstance().sendRPCRequest(trackMsg);
		}catch(SyncException e){
			
		}
	}

	
	
	//
	public void createChoiceSets(Vector<Choice> choiceVector){
		//Vector<Choice> choiceVector = new Vector<Choice>();
		try{
//			Choice choice = new Choice();
//			choice.setChoiceID(id);
//			choice.setMenuName(menuName);
//			choice.setVrCommands(new Vector<String>(Arrays.asList(new String[] { menuName, "Track " + id })));
//			choiceVector.add(choice);
//			
			RPCRequest trackMsg;
			//Log.i("InteractionChoiceSet -", "" + nextInteractionChoiceCorrID());
			trackMsg = RPCRequestFactory.buildCreateInteractionChoiceSet(
					choiceVector, ProxyService.getInstance().nextInteractionChoiceCorrID(), ProxyService.getInstance().nextCorrID());

			ProxyService.getProxyInstance().sendRPCRequest(trackMsg);
		}catch(SyncException e){
			
		}
	}
}
