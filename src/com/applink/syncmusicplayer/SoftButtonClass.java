package com.applink.syncmusicplayer;

import java.util.ArrayList;
import java.util.Vector;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.rpc.SoftButton;
import com.ford.syncV4.proxy.rpc.enums.SoftButtonType;
import com.ford.syncV4.proxy.rpc.enums.SystemAction;

import android.content.Context;

public class SoftButtonClass { 
	private Context mContext = null;
	
	public SoftButtonClass(Context ctx) {
		// TODO Auto-generated constructor stub
		mContext = ctx;
	}
	
	public static SoftButtonClass getInstance(Context ctx){
		return new SoftButtonClass(ctx);
		
	}
	
	public void addSoftButton(String buttonName, int id, Vector<SoftButton> buttons){
		try{
			SoftButton softButton = new SoftButton();
			softButton.setText(buttonName);
			softButton.setSoftButtonID(id);
			softButton.setType(SoftButtonType.SBT_TEXT);
			softButton.setSystemAction(SystemAction.DEFAULT_ACTION);
			// Send Show RPC:
			ProxyService.getProxyInstance().show("", "", "", "", null, buttons, null, null, ProxyService.getInstance().nextCorrID());
		}catch(SyncException e){
			
		}
	}
	
	public void addSoftButtons(ArrayList<String> buttonName, ArrayList<Integer> id){
		Vector<SoftButton> vsoftButton=new Vector<SoftButton>();
		SoftButton softButton;
		try{
			for(int i = 0 ; i<buttonName.size();i++){
				softButton = new SoftButton();
				softButton.setText(buttonName.get(i));
				softButton.setSoftButtonID(id.get(i));
				softButton.setType(SoftButtonType.SBT_TEXT);
				softButton.setSystemAction(SystemAction.DEFAULT_ACTION);
				vsoftButton.add(softButton);
				
			}

			ProxyService.getProxyInstance().show("", "", "", "", null, vsoftButton, null, null,
					ProxyService.getInstance().nextCorrID());

		}catch(SyncException e){
			
		}
	}

}
