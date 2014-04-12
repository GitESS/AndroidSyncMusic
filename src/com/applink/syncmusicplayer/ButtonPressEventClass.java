package com.applink.syncmusicplayer;

import android.content.Context;
import android.media.AudioManager;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.SubscribeButton;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.ButtonPressMode;

public class ButtonPressEventClass {
	OnButtonPress notification;
	//OnButtonEvent
	SubscribeButton subscribeButton;
	private Context ctx;
	AudioManager audioManager;
	
	public ButtonPressEventClass(Context ctx) {
		// TODO Auto-generated constructor stub
		
	}
	
	public static ButtonPressEventClass getInstance(Context ctx){
		return new ButtonPressEventClass(ctx);
		
	}
	
	public void InitialaizeSubscribableButtons(ButtonName button){
		subscribeButton = new SubscribeButton();
		subscribeButton.setButtonName(button);
		subscribeButton.setCorrelationID(ProxyService.getInstance().nextCorrID());
		
		try{
			ProxyService.getProxyInstance().sendRPCRequest(subscribeButton);
		}catch(SyncException e){
			
		}
	}
	
	
	public void ButtonsBehaviors(ButtonName button){
		if(notification.getCustomButtonName() == null){
			if(notification.getButtonPressMode().equals(ButtonPressMode.SHORT)){
				performShortEventsAction(button);
			} else {
				performLongEventsAction(button);
			}
			
		} else {
			performSoftButtonsEventAction();
		}
	}
	
	private void performShortEventsAction(ButtonName button){
audioManager = (AudioManager) SyncMainActivity.getInstance().getSystemService(Context.AUDIO_SERVICE);
		
		if(button.equals(ButtonName.OK)){
			
		} else if(button.equals(ButtonName.SEEKLEFT)){
			
		} else if(button.equals(ButtonName.SEEKRIGHT)){
			
		} else if(button.equals(ButtonName.TUNEUP)){
			audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
		} else if(button.equals(ButtonName.TUNEDOWN)){
			audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
			
		} else if(button.equals(ButtonName.PRESET_0)){
			SyncMainActivity.getInstance().playCurrentSong(0);
			
		} else if (button.equals(ButtonName.PRESET_1)){
			SyncMainActivity.getInstance().playCurrentSong(1);
		} else if (button.equals(ButtonName.PRESET_2)){
			SyncMainActivity.getInstance().playCurrentSong(2);
		} else if (button.equals(ButtonName.PRESET_3)){
			SyncMainActivity.getInstance().playCurrentSong(3);
		} else if (button.equals(ButtonName.PRESET_4)){
			SyncMainActivity.getInstance().playCurrentSong(4);
		} else if (button.equals(ButtonName.PRESET_5)){
			SyncMainActivity.getInstance().playCurrentSong(5);
		} else if (button.equals(ButtonName.PRESET_6)){
			SyncMainActivity.getInstance().playCurrentSong(6);
		} else if (button.equals(ButtonName.PRESET_7)){
			SyncMainActivity.getInstance().playCurrentSong(7);
		} else if (button.equals(ButtonName.PRESET_8)){
			SyncMainActivity.getInstance().playCurrentSong(8);
		} else if (button.equals(ButtonName.PRESET_9)){
			SyncMainActivity.getInstance().playCurrentSong(9);
		}
	}
	
	private void performLongEventsAction(ButtonName button){
		String msg;
		switch (notification.getButtonName()) {
		case PRESET_1:
			msg = new String("Song_number 10");
			AlertClass.getInstance(ProxyService.getInstance()).getAlert("Long Press", 3000,
					msg);
			//_mainInstance.playCurrentSong(1* 10);
			
			break;
		case PRESET_2:
			msg = new String("Song_number 20");
			AlertClass.getInstance(ProxyService.getInstance()).getAlert("Long Press", 3000,
					msg);
			//_mainInstance.playCurrentSong(2*10);
			
			break;
		case PRESET_3:
			msg = new String("Song_number 30");
			AlertClass.getInstance(ProxyService.getInstance()).getAlert("Long Press", 3000,
					msg);
			//_mainInstance.playCurrentSong(3*10);
			
			break;
		case PRESET_4:
			msg = new String("Song_number 40");
			AlertClass.getInstance(ProxyService.getInstance()).getAlert("Long Press", 3000,
					msg);
			//_mainInstance.playCurrentSong(4*10);
			
			break;
		case PRESET_5:
			msg = new String("Song_number 50");
			AlertClass.getInstance(ProxyService.getInstance()).getAlert("Long Press", 3000,
					msg);
			//_mainInstance.playCurrentSong(5*10);
			
			break;
		case PRESET_6:
			msg = new String("Song_number 60");
			AlertClass.getInstance(ProxyService.getInstance()).getAlert("Long Press", 3000,
					msg);
			//_mainInstance.playCurrentSong(10*6);
			
			break;
		case PRESET_7:
			msg = new String("Song_number 70");
			AlertClass.getInstance(ProxyService.getInstance()).getAlert("Long Press", 3000,
					msg);
			//_mainInstance.playCurrentSong(10*7);
			
			break;
		case PRESET_8:
			msg = new String("Song_number 80");
			AlertClass.getInstance(ProxyService.getInstance()).getAlert("Long Press", 3000,
					msg);
			//_mainInstance.playCurrentSong(10*8);
			
			break;
		case PRESET_9:
			msg = new String("Song_number 90");
			AlertClass.getInstance(ProxyService.getInstance()).getAlert("Long Press", 3000,
					msg);
			//_mainInstance.playCurrentSong(10*9);
			
			break;
		case PRESET_0:
			msg = new String("Song_number 100");
			AlertClass.getInstance(ProxyService.getInstance()).getAlert("Long Press", 3000,
					msg);
			
			//_mainInstance.playCurrentSong(100);
			
			break;

		default:
			break;
		}
	}
	
	private void performSoftButtonsEventAction(){
		
	}

}
