package com.applink.syncmusicplayer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.ford.syncV4.util.DebugTool;

public class SyncReceiver extends BroadcastReceiver {
	static final String TAG = "SyncMusicPlayer";
	ProxyService serviceInstance = ProxyService.getInstance();
	

	public void onReceive(Context context, Intent intent) {
		DebugTool.logInfo("SyncReceiver.onReceive()");
		DebugTool.logInfo("Received Intent with action: " + intent.getAction());
		Log.i(TAG, "Received Intent with action: " + intent.getAction());
		final BluetoothDevice bluetoothDevice = (BluetoothDevice) intent
				.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		BluetoothAdapter mBtAdapter;

		SyncMainActivity mainActivityInstance = SyncMainActivity.getInstance();

		// $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
		// New Implementaion $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$

		if (intent.getAction().compareTo(BluetoothDevice.ACTION_ACL_CONNECTED) == 0) {
			if (bluetoothDevice.getName() != null) {
				if (bluetoothDevice.getName().contains("SYNC")) {
					ProxyService serviceInstance = ProxyService.getInstance();
					if (serviceInstance == null) {
						Intent startIntent = new Intent(context,
								ProxyService.class);
						startIntent.putExtras(intent);
						context.startService(startIntent);
					}
				}
			}

			// if SYNC is disconnected from phone or BT disabled, stop service
			// (and thus the proxy)
		} else if (intent.getAction().equals(
				BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
			if (bluetoothDevice.getName().contains("SYNC")) {
				ProxyService serviceInstance = ProxyService.getInstance();
				if (serviceInstance != null) {
					Intent stopIntent = new Intent(context, ProxyService.class);
					stopIntent.putExtras(intent);
					context.stopService(stopIntent);
				}
				try{
				LockScreenActivity.getInstance().finish();
				SyncMainActivity.getInstance().syncPlayer.release();
				ProxyService.getProxyInstance().dispose();
				}catch(Exception e){
					Log.i("SyncReceiver", e.toString());
				}
			}

			// Listen for STATE_CHANGED as double-check when BT turned off & not
			// connected to BT
		} else if (intent.getAction().equals(
				BluetoothAdapter.ACTION_STATE_CHANGED)) {
			if ((intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == (BluetoothAdapter.STATE_TURNING_OFF))) {
				if (serviceInstance != null) {
					Intent stopIntent = new Intent(context, ProxyService.class);
					stopIntent.putExtras(intent);
					context.stopService(stopIntent);
					SyncMainActivity.getInstance().syncPlayer.release();
				}
			} else if((intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == (BluetoothAdapter.STATE_TURNING_ON))){
				 mBtAdapter = BluetoothAdapter.getDefaultAdapter();
				if(serviceInstance != null){	
				 if (mBtAdapter != null) {
						if (mBtAdapter.isEnabled()) {
							Intent startIntent = new Intent(context, ProxyService.class);
							startIntent.putExtras(intent);
							context.startService(startIntent);
						}
					}
				}
			}
			} else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBtAdapter != null) {
				if (mBtAdapter.isEnabled()) {
					Intent startIntent = new Intent(context, ProxyService.class);
					startIntent.putExtras(intent);
					context.startService(startIntent);
				}
			}
		} else if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			// signal your service to stop playback
			try{
				if (mainActivityInstance != null && mainActivityInstance.syncPlayer.isPlaying()) {
					
						SyncMainActivity.getInstance().pauseCurrentSong();
					
				
		}}catch(Exception e){
			Log.i("SyncReceiver", ""+e.toString());
		}
		}

		
	//################################################################## END CODE#####################################################################	
		
		
	/*	
		// open proxy when BT is on. Dispose and shutdown service when BT is off
		if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
			if ((intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == (BluetoothAdapter.STATE_TURNING_OFF))) {
				if (serviceInstance != null) {
					Log.i(TAG, "Bt off stop service");
					Intent stopIntent = new Intent(context, ProxyService.class);
					stopIntent.putExtras(intent);
					// SyncMainActivity.getInstance().syncPlayer.release();
					context.stopService(stopIntent);
				}
			} else if ((intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == (BluetoothAdapter.STATE_TURNING_ON))) {
				Log.i(TAG, "Bt on");
				if (serviceInstance == null) {
					Log.i(TAG, "Bt on start service");
					Intent startIntent = new Intent(context, ProxyService.class);
					startIntent.putExtras(intent);
					context.startService(startIntent);
				}
				// if the service was already running when BT turned back on
				else {
					serviceInstance.reset();
				}
			}
			// Listen for phone reboot and start service
		} else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBtAdapter != null) {
				if (mBtAdapter.isEnabled()) {
					Intent startIntent = new Intent(context, ProxyService.class);
					startIntent.putExtras(intent);
					context.startService(startIntent);
				}
			}
		}

		if (intent.getAction().compareTo(Intent.ACTION_MEDIA_BUTTON) == 0) {
			KeyEvent event = (KeyEvent) intent
					.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
				abortBroadcast();
			}

		}

		if (intent.getAction().equals(
				android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			// signal your service to stop playback
			if (mainActivityInstance != null) {
				mainActivityInstance.syncPlayer.pause();
			}
		}*/
	}
}
