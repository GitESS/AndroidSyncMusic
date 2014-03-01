package com.applink.syncmusicplayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.proxy.RPCMessage;
import com.ford.syncV4.proxy.RPCRequest;
import com.ford.syncV4.proxy.RPCRequestFactory;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.AddCommandResponse;
import com.ford.syncV4.proxy.rpc.AddSubMenuResponse;
import com.ford.syncV4.proxy.rpc.Alert;
import com.ford.syncV4.proxy.rpc.AlertResponse;
import com.ford.syncV4.proxy.rpc.ChangeRegistrationResponse;
import com.ford.syncV4.proxy.rpc.Choice;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteCommandResponse;
import com.ford.syncV4.proxy.rpc.DeleteFileResponse;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteSubMenuResponse;
import com.ford.syncV4.proxy.rpc.DialNumberResponse;
import com.ford.syncV4.proxy.rpc.EndAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.GenericResponse;
import com.ford.syncV4.proxy.rpc.GetDTCsResponse;
import com.ford.syncV4.proxy.rpc.GetVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.ListFilesResponse;
import com.ford.syncV4.proxy.rpc.OnAudioPassThru;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.OnDriverDistraction;
import com.ford.syncV4.proxy.rpc.OnHMIStatus;
import com.ford.syncV4.proxy.rpc.OnLanguageChange;
import com.ford.syncV4.proxy.rpc.OnPermissionsChange;
import com.ford.syncV4.proxy.rpc.OnVehicleData;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThruResponse;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.PutFileResponse;
import com.ford.syncV4.proxy.rpc.ReadDIDResponse;
import com.ford.syncV4.proxy.rpc.ResetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.ScrollableMessage;
import com.ford.syncV4.proxy.rpc.ScrollableMessageResponse;
import com.ford.syncV4.proxy.rpc.SetAppIconResponse;
import com.ford.syncV4.proxy.rpc.SetDisplayLayoutResponse;
import com.ford.syncV4.proxy.rpc.SetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetMediaClockTimerResponse;
import com.ford.syncV4.proxy.rpc.ShowResponse;
import com.ford.syncV4.proxy.rpc.Slider;
import com.ford.syncV4.proxy.rpc.SliderResponse;
import com.ford.syncV4.proxy.rpc.SoftButton;
import com.ford.syncV4.proxy.rpc.Speak;
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.SubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;
import com.ford.syncV4.proxy.rpc.enums.Language;
import com.ford.syncV4.proxy.rpc.enums.Result;
import com.ford.syncV4.proxy.rpc.enums.SoftButtonType;
import com.ford.syncV4.proxy.rpc.enums.SpeechCapabilities;
import com.ford.syncV4.proxy.rpc.enums.SystemAction;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.transport.TCPTransportConfig;

public class ProxyService extends Service implements IProxyListenerALM {
	static final String TAG = "SyncMusciPlayer";
	private Integer autoIncCorrId = 1;
	AudioManager audioManager;
	private static ProxyService _instance;
	private static SyncMainActivity _mainInstance;
	private static SyncProxyALM _syncProxy;
	private BluetoothAdapter mBtAdapter;
	public Boolean playingAudio = false;
	protected SyncReceiver mediaButtonReceiver;
	//variable to contain the current state of the lockscreen
	private boolean lockscreenUP = false;
	private boolean firstHMIStatusChange = true;
	private static boolean waitingForResponse = false;
	
	public int trackNumber = 1;
	//Voice cmd implementation
	private Integer autoIncCNDCorrId = 1001;
	private Integer choiceId = 1010;
	private Integer choiceSetId = 1020;
	private Integer interactionChoiceSetID = 1030;
	private int lastIndexOfSongChoiceId;
	private SoftButton next, previous, appInfo, applinkInfo,	cmdInfo, scrollableMsg, APTHCheck, vehicleData;
	
	public int getLastIndexOfSongChoiceId() {
		return lastIndexOfSongChoiceId;
	}

	public void setLastIndexOfSongChoiceId(int lastIndexOfSongChoiceId) {
		this.lastIndexOfSongChoiceId = lastIndexOfSongChoiceId;
	}

	private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();

	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		Toast.makeText(getApplicationContext(), "Control is on OnCreate if Service is not created, Create it", Toast.LENGTH_SHORT).show();
		IntentFilter mediaIntentFilter = new IntentFilter();
		mediaIntentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
		
		mediaButtonReceiver = new SyncReceiver();
		registerReceiver(mediaButtonReceiver, mediaIntentFilter);
	
		Log.i(TAG, "ProxyService.onCreate()");
		_instance = this;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "ProxyService.onStartCommand()");
		Toast.makeText(getApplicationContext(), "Control is on OnStartCommand", Toast.LENGTH_SHORT).show();
		startProxyIfNetworkConnected();
		
        setCurrentActivity(SyncMainActivity.getInstance());
			
        return START_STICKY;
		
	}
	
	protected int nextCorrID() {
		autoIncCorrId++;
		return autoIncCorrId;
	}
	
	protected int nextCMDCorrID() {
		autoIncCNDCorrId++;
		return autoIncCNDCorrId;
	}
	
	protected int nextChoiceCorrID() {
		choiceId++;
		return choiceId;
	}
	
	protected int nextChoiceSetCorrID() {
		choiceSetId++;
		return choiceSetId;
	}
	
	protected int nextInteractionChoiceCorrID() {
		interactionChoiceSetID++;
		return interactionChoiceSetID;
	}
	
	private void startProxyIfNetworkConnected(){
		Log.i(TAG, "startProxyIfNetworkConnected()");
		final SharedPreferences prefs = getSharedPreferences(Const.PREFS_NAME,
				MODE_PRIVATE);
		final int transportType = prefs.getInt(
				Const.Transport.PREFS_KEY_TRANSPORT_TYPE,
				Const.Transport.PREFS_DEFAULT_TRANSPORT_TYPE);

		if (transportType == Const.Transport.KEY_BLUETOOTH) {
			Log.d(TAG, "ProxyService. onStartCommand(). Transport = Bluetooth.");
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBtAdapter != null) {
				if (mBtAdapter.isEnabled()) {
					startProxy();
				}
			}
		} else {
			startProxy();
		}
	}
	
	public void startProxy(){
		Log.i(TAG, "ProxyService.startProxy()");
		
		if (_syncProxy == null) {
			try {
				SharedPreferences settings = getSharedPreferences(
						Const.PREFS_NAME, 0);
				boolean isMediaApp = settings.getBoolean(
						Const.PREFS_KEY_ISMEDIAAPP,
						Const.PREFS_DEFAULT_ISMEDIAAPP);
				String appName = settings.getString(Const.PREFS_KEY_APPNAME,
						Const.PREFS_DEFAULT_APPNAME);
				int transportType = settings.getInt(
						Const.Transport.PREFS_KEY_TRANSPORT_TYPE,
						Const.Transport.PREFS_DEFAULT_TRANSPORT_TYPE);
				String ipAddress = settings.getString(
						Const.Transport.PREFS_KEY_TRANSPORT_IP,
						Const.Transport.PREFS_DEFAULT_TRANSPORT_IP);
				int tcpPort = settings.getInt(
						Const.Transport.PREFS_KEY_TRANSPORT_PORT,
						Const.Transport.PREFS_DEFAULT_TRANSPORT_PORT);
				boolean autoReconnect = settings
						.getBoolean(
								Const.Transport.PREFS_KEY_TRANSPORT_RECONNECT,
								Const.Transport.PREFS_DEFAULT_TRANSPORT_RECONNECT_DEFAULT);

				if (transportType == Const.Transport.KEY_BLUETOOTH) {
					//_syncProxy = new SyncProxyALM(this, appName, isMediaApp);
					_syncProxy = new SyncProxyALM(this, appName, isMediaApp, Language.EN_US, Language.EN_US, "584421907");
					
				} else {
					//_syncProxy = new SyncProxyALM(this, appName, isMediaApp, new TCPTransportConfig(tcpPort, ipAddress, autoReconnect));
					_syncProxy = new SyncProxyALM(this, appName, isMediaApp, Language.EN_US, Language.EN_US, "584421907", new TCPTransportConfig(tcpPort, ipAddress, autoReconnect));
				}
			} catch (SyncException e) {
				e.printStackTrace();
				//error creating proxy, returned proxy = null
				if (_syncProxy == null){
					stopSelf();
				}
			}
		}
		Log.i(TAG, "ProxyService.startProxy() returning");
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.i(TAG, "ProxyService.onDestroy()");
		
		disposeSyncProxy();
		clearlockscreen();
		_instance = null;
		if (_mainInstance.syncPlayer != null) _mainInstance.syncPlayer.release();		
		unregisterReceiver(mediaButtonReceiver);	
		super.onDestroy();
		
	}
	
	public void disposeSyncProxy() {
		Log.i(TAG, "ProxyService.disposeSyncProxy()");
		
		if (_syncProxy != null) {
			try {
				_syncProxy.dispose();
			} catch (SyncException e) {
				e.printStackTrace();
			}
			_syncProxy = null;
			clearlockscreen();
		}
	}
	public static ProxyService getInstance(){
		return _instance;
	}
	public void setCurrentActivity(SyncMainActivity currentActivity) {
		if (this._mainInstance != null) {
			this._mainInstance.finish();
			this._mainInstance = null;
		}
		
		this._mainInstance = currentActivity;
		
	}
	public static SyncProxyALM getProxyInstance(){
		return _syncProxy;
	}
	
	public static void waiting(boolean waiting) {
		waitingForResponse = waiting;
	}
	
	private void initializeTheApp(){
		playingAudio = true;
		_mainInstance.lockAppsScreen();
		_mainInstance.playPauseCurrentPlayingSong();
		
		//ButtonSubscriptions
		initializeButtonsToBeSubscribed();
		
		// softButtons Implementation
		showSoftButtonsOnScreen();
		
		//Menu Command
		initializeSubMenuCommandForSyncPlayer();
		
		//Voice Command
		initializeVoiceCommand();
		
		//ChoiceSet
		createInteractionChoiceSet();
	}
	
	private void initializeVoiceCommand(){
		try {
			_syncProxy.addCommand(nextCMDCorrID(), "Play", new Vector<String>(Arrays.asList(new String[] {"Play", "Play Song"})), nextCorrID());
			_syncProxy.addCommand(nextCMDCorrID(), "Pause", new Vector<String>(Arrays.asList(new String[] {"Pause", "Pause Song"})), nextCorrID());
			_syncProxy.addCommand(nextCMDCorrID(), "Next", new Vector<String>(Arrays.asList(new String[] {"Next", "Next Song"})), nextCorrID());
			_syncProxy.addCommand(nextCMDCorrID(), "Previous", new Vector<String>(Arrays.asList(new String[] {"Previous", "Previous Song"})), nextCorrID());
			_syncProxy.addCommand(nextCMDCorrID(), "Backward", new Vector<String>(Arrays.asList(new String[] {"Backward", "Seek Backward"})), nextCorrID());
			_syncProxy.addCommand(nextCMDCorrID(), "Forward", new Vector<String>(Arrays.asList(new String[] {"Forward", "Seek Forward"})), nextCorrID());
			_syncProxy.addCommand(nextCMDCorrID(), "Select Song", new Vector<String>(Arrays.asList(new String[] {"Select Song"})), nextCorrID());
			_syncProxy.addCommand(nextCMDCorrID(), "info", new Vector<String>(Arrays.asList(new String[] {"info"})), nextCorrID());
		} catch (SyncException e) {
			 Log.e(TAG, "Error adding AddCommands", e);
		}
	}
	
	private void initializeButtonsToBeSubscribed(){
		try{
			_syncProxy.subscribeButton(ButtonName.OK, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.TUNEUP, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.TUNEDOWN, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.SEEKLEFT, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.SEEKRIGHT, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.PRESET_0, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.PRESET_1, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.PRESET_2, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.PRESET_3, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.PRESET_4, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.PRESET_5, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.PRESET_6, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.PRESET_7, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.PRESET_8, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.PRESET_9, nextCorrID());
		}catch(SyncException e){
			e.printStackTrace();
			Log.e(TAG, e.toString());
		}
	}
	
	@Override
	public void onOnHMIStatus(OnHMIStatus notification) {
		// TODO Auto-generated method stub
		Log.i(TAG, "" + notification);
		switch(notification.getSystemContext()) {
		case SYSCTXT_MAIN:
			break;
		case SYSCTXT_VRSESSION:
			break;
		case SYSCTXT_MENU:
			break;
		default:
			return;
	}
		switch(notification.getAudioStreamingState()) {
		case AUDIBLE:
			if (playingAudio) 
				_mainInstance.playCurrentSong(0);
			break;
		case NOT_AUDIBLE:
			_mainInstance.pauseCurrentSong();
			break;
		default:
			return;
	}
		
		switch(notification.getHmiLevel()) {
		case HMI_FULL:
			if(_syncProxy.getAppInterfaceRegistered()){
			if (notification.getFirstRun()) {
				initializeTheApp();
				
			} 
		}
//			if (_syncProxy.getAppInterfaceRegistered()) {
//				// if (hmiFull) {
//				if (notification.getFirstRun()) {
//
//					try {
//						_syncProxy.show("Sync Music Player", "", TextAlignment.CENTERED, nextCorrID());
//						// MainActivity.getInstance().playPauseCurrentPlayingSong();
//						Log.d(TAG, "First run");
//						initializeTheApp();
//					} catch (SyncException e) {
//					}
//
//				} else {
//					try {
//						if (!mbWaitingForResponse) {
//							_syncProxy.show("Sync Music Player", "", TextAlignment.CENTERED, nextCorrID());
//						}
//					} catch (SyncException e) {
//
//					}
//				}
//				// }
//
//			}
			
			break;
		case HMI_LIMITED:
			break;
		case HMI_BACKGROUND:
			break;
		case HMI_NONE:
			break;
		default:
			return;
	}
		
	}

	@Override
	public void onAddCommandResponse(AddCommandResponse addCmdResponse) {
		// TODO Auto-generated method stub
		Log.i(TAG, addCmdResponse.toString());
		
	}
	
	@Override
	public void onProxyClosed(String arg0, Exception e) {
		// TODO Auto-generated method stub
		Log.e(TAG, "onProxyClosed: " + arg0, e);
		
		boolean wasConnected = !firstHMIStatusChange;
		firstHMIStatusChange = true;
		if (wasConnected) {
			final SyncMainActivity mainActivity = SyncMainActivity.getInstance();
			if (mainActivity != null) {
				mainActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mainActivity.onProxyClosed();
					}
				});
			} else {
				Log.w(TAG, "mainActivity not found");
			}
		}
		
		if(((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.SYNC_PROXY_CYCLED
				&& ((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.BLUETOOTH_DISABLED) {
			reset();
		}
			
	}
	
	public void reset(){
		   try {
			   if (_syncProxy != null) _syncProxy.resetProxy();
	           else startProxyIfNetworkConnected();
			} catch (SyncException e1) {
				e1.printStackTrace();
				//something goes wrong, & the proxy returns as null, stop the service.
				//do not want a running service with a null proxy
				if (_syncProxy == null){
					stopSelf();
				}
			}
		}
	
	/**
	 * Restarting SyncProxyALM. For example after changing transport type
	 */
	public void restart() {
		Log.i(TAG, "ProxyService.Restart SyncProxyALM.");
		disposeSyncProxy();
		startProxyIfNetworkConnected();
	}
	
	@Override
	public void onError(String info, Exception e) {
		// TODO Auto-generated method stub
		Log.e(TAG, "******onProxyError******");
		Log.e(TAG, "ERROR: " + info, e);
	}

	@Override
	public void onAddSubMenuResponse(AddSubMenuResponse subMenuResponse) {
		// TODO Auto-generated method stub
		Log.i(TAG, subMenuResponse.toString());
		
	}

	@Override
	public void onAlertResponse(AlertResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCreateInteractionChoiceSetResponse(
			CreateInteractionChoiceSetResponse interactionChoiceSetResponse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleteInteractionChoiceSetResponse(
			DeleteInteractionChoiceSetResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	/*@Override
	public void onEncodedSyncPDataResponse(EncodedSyncPDataResponse arg0) {
		// TODO Auto-generated method stub
		
	}*/

	

	@Override
	public void onGenericResponse(GenericResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnButtonEvent(OnButtonEvent notification) {
		// TODO Auto-generated method stub
		Log.i(TAG, "" + notification);
	}

	@Override
	public void onOnButtonPress(OnButtonPress notification) {
		// TODO Auto-generated method stub
		Log.i(TAG, "" + notification);
		
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			
		switch(notification.getButtonName())
		{
			case OK:
				_mainInstance.playPauseCurrentPlayingSong();
				break;
			case SEEKLEFT:
				_mainInstance.seekBackwardCurrentPlayingSong();
				break;
			case SEEKRIGHT:
				_mainInstance.seekForwardCurrentPlayingSong();
				break;
			case TUNEUP:
				audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
				break;
			case TUNEDOWN:
				audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
				break;
			case PRESET_0:
				SubscribeVehicleDataClass.getInstance(ProxyService.this, 0).getVehicleData();
				//_mainInstance.playTrackNumber(0);
				break;
			case PRESET_1:
				SubscribeVehicleDataClass.getInstance(ProxyService.this, 1).getVehicleData();
				//_mainInstance.playTrackNumber(1);
				break;
			case PRESET_2:
				SubscribeVehicleDataClass.getInstance(ProxyService.this, 2).getVehicleData();
				//_mainInstance.playTrackNumber(2);
				break;
			case PRESET_3:
				_mainInstance.playTrackNumber(3);
				break;
			case PRESET_4:
				_mainInstance.playTrackNumber(4);
				break;
			case PRESET_5:
				_mainInstance.playTrackNumber(5);
				break;
			case PRESET_6:
				_mainInstance.playTrackNumber(6);
				break;
			case PRESET_7:
				_mainInstance.playTrackNumber(7);
				break;
			case PRESET_8:
				_mainInstance.playTrackNumber(8);
				break;
			case PRESET_9:
				_mainInstance.playTrackNumber(9);
				break;
			
			default:
				break;
		}
		
		// Handling softButtons notifications-- 6 softbuttons cmd are albumList,
				// SongList, Song info, app info, applink info, command info

				if (notification.getCustomButtonName().equals(100)) {
					SyncMainActivity.getInstance().jumpToNextSong();
					Alert next = new Alert();
					next.setAlertText1("Next");
					next.setDuration(1000);
					next.setCorrelationID(nextCorrID());
					Vector<TTSChunk> ttsChunks = new Vector<TTSChunk>();
					ttsChunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
							"Next"));
					next.setTtsChunks(ttsChunks);
					try {
						_syncProxy.sendRPCRequest(next);
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} else if (notification.getCustomButtonName().equals(101)) {
					SyncMainActivity.getInstance().jumpToPreviousSong();
					Alert previous = new Alert();
					previous.setAlertText1("Previous");
					previous.setDuration(1000);
					previous.setCorrelationID(nextCorrID());
					Vector<TTSChunk> ttsChunks = new Vector<TTSChunk>();
					ttsChunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
							"Previous"));
					previous.setTtsChunks(ttsChunks);
					try {
						_syncProxy.sendRPCRequest(previous);
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} /*else if (notification.getCustomButtonName().equals(102)) {
					SyncMainActivity.getInstance().seekForwardCurrentPlayingSong();
					Alert forward = new Alert();
					forward.setAlertText1("Forward");
					forward.setDuration(1000);
					forward.setCorrelationID(nextCorrID());
					Vector<TTSChunk> ttsChunks = new Vector<TTSChunk>();
					ttsChunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
							"Seek Forward"));
					forward.setTtsChunks(ttsChunks);
					try {
						_syncProxy.sendRPCRequest(forward);
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (notification.getCustomButtonName().equals(103)) {
					SyncMainActivity.getInstance().seekBackwardCurrentPlayingSong();
					Alert backward = new Alert();
					backward.setAlertText1("Backward");
					backward.setDuration(1000);
					backward.setCorrelationID(nextCorrID());
					Vector<TTSChunk> ttsChunks = new Vector<TTSChunk>();
					ttsChunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
							"Seek Backward"));
					backward.setTtsChunks(ttsChunks);
					try {
						_syncProxy.sendRPCRequest(backward);
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}*/ else if (notification.getCustomButtonName().equals(104)) {
					Alert appInfo = new Alert();
					appInfo.setAlertText1("Application Info");
					appInfo.setDuration(3000);
					appInfo.setCorrelationID(nextCorrID());
					Vector<TTSChunk> ttsChunks = new Vector<TTSChunk>();
					ttsChunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
							"This is a Applink enabled Music player App, Designed specifically for Ford's SYNC."));
					appInfo.setTtsChunks(ttsChunks);
					try {
						_syncProxy.sendRPCRequest(appInfo);
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (notification.getCustomButtonName().equals(105)) {
					Alert applinkinfo = new Alert();
					applinkinfo.setAlertText1("AppLink Information");
					applinkinfo.setDuration(3000);
					applinkinfo.setCorrelationID(nextCorrID());
					Vector<TTSChunk> ttsChunks = new Vector<TTSChunk>();
					ttsChunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
							"AppLink is a Ford's sdk, Designed to develop AppLink enabled Application for Ford's car!"));
					applinkinfo.setTtsChunks(ttsChunks);
					try {
						_syncProxy.sendRPCRequest(applinkinfo);
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					/*try {
						
								_syncProxy.show("AppLink info",
										"AppLink is a Ford's sdk, Designed to develop AppLink enabled Application for Ford's car!",
										TextAlignment.LEFT_ALIGNED, nextCorrID());
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
				} else if (notification.getCustomButtonName().equals(106)) {
					Alert alert = new Alert();
					alert.setAlertText1("Command Information");
					alert.setAlertText2("");
					alert.setDuration(3000);
					alert.setCorrelationID(nextCorrID());
					Vector<TTSChunk> ttsChunks = new Vector<TTSChunk>();
					ttsChunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
							"Four Main Commands are available in this Music application. Apart from Play and pause commands. +" +
							"There are Next, Previous, Seek forward and Seek Backward commands are vailable as well. These commmands are also available in Voice recognition and Sub Menu CMD forms"));
					alert.setTtsChunks(ttsChunks);
					try {
						_syncProxy.sendRPCRequest(alert);
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if(notification.getCustomButtonName().equals(107)){
					String scrollableMessageBody = new String("This is a scrollable Message. Obiviously this Text is going to be long enough so that a user can experience the scroll in the SYNC Display. It's request to the user to not read this message!!!");
					ScrollableMessage scrllMsg = new ScrollableMessage();
					scrllMsg.setCorrelationID(nextCorrID());
					scrllMsg.setTimeout(30000);
					scrllMsg.setScrollableMessageBody(scrollableMessageBody);
					
					try {
						_syncProxy.sendRPCRequest(scrllMsg);
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if(notification.getCustomButtonName().equals(108)){
					//PerformVoiceRecordingInteraction();
					//new PerformAudioPassThruClass();
					PerformAudioPassThruClass.getInstance(ProxyService.this).show();
				} else if(notification.getCustomButtonName().equals(109)){
					Vector<String> str = new Vector<String>();
					str.add("Footer");
					Slider slider = new Slider();
					slider.setCorrelationID(nextCorrID());
					slider.setNumTicks(3);
					slider.setPosition(3);
					slider.setSliderHeader("SLider Test");
					slider.setSliderFooter(str);
					slider.setTimeout(5000);
					
					try {
						_syncProxy.sendRPCRequest(slider);
						_syncProxy.show("Slider", "Coming Soon", TextAlignment.CENTERED, nextCorrID());
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}

	}

	@Override
	public void onOnCommand(OnCommand notification) {
		// TODO Auto-generated method stub
		//Handling Menu Events
		handlingMenuCommandEvents(notification);
		
		//Handling voice command
		handlingVoiceCommandForSync(notification);
	
	}

	@Override
	public void onOnPermissionsChange(OnPermissionsChange arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse interactionResponse) {
		// TODO Auto-generated method stub
		perfomChoiceSetSelection(interactionResponse);
	}

	

	@Override
	public void onResetGlobalPropertiesResponse(
			ResetGlobalPropertiesResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse arg0) {
		// TODO Auto-generated method stub
		SetGlobalPropertiesResponse sgpd = new SetGlobalPropertiesResponse();
		
		//_syncProxy.resetGlobalProperties(properties, nextCorrID());
		
	}

	@Override
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onShowResponse(ShowResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSpeakResponse(SpeakResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
		// TODO Auto-generated method stub
		 Log.i(TAG, "" + response);
	}

	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
		// TODO Auto-generated method stub
		 Log.i(TAG, "" + response);
	}

	@Override
	public void onOnDriverDistraction(OnDriverDistraction arg0) {
		// TODO Auto-generated method stub
		
	}

	/*@Override
	public void onOnEncodedSyncPData(OnEncodedSyncPData arg0) {
		// TODO Auto-generated method stub
		
	}*/

	/*@Override
	public void onOnTBTClientState(OnTBTClientState arg0) {
		// TODO Auto-generated method stub
		
	}*/
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return new Binder();
	}
	
	private void initializeSubMenuCommandForSyncPlayer(){
		try{
			String mnPlayCmd = new String("Play Song");
			_syncProxy.addCommand(100, mnPlayCmd, nextCorrID());
			String mnPauseCmd = new String("Pause Song");
			_syncProxy.addCommand(101, mnPauseCmd, nextCorrID());
			String mnNextCmd = new String("Next Song");
			_syncProxy.addCommand(102, mnNextCmd, nextCorrID());
			String mnPreviousCmd = new String("Previous Song");
			_syncProxy.addCommand(103, mnPreviousCmd, nextCorrID());
			String mnSeekBackwardCmd = new String("Seek Backward");
			_syncProxy.addCommand(104, mnSeekBackwardCmd, nextCorrID());
			String mnSeekForwardCmd = new String("Seek Forward");
			_syncProxy.addCommand(105, mnSeekForwardCmd, nextCorrID());
			
		}catch(SyncException e){
			Log.e(TAG+"Error in initializing MenuCMD", e.toString());
		}

	}
	private void handlingMenuCommandEvents(OnCommand notification){
		
		switch (notification.getCmdID()) {	
		case 100:
			_mainInstance.syncPlayer.start();
			break;
		case 101:
			_mainInstance.pauseCurrentSong();
			break;
		case 102:
			_mainInstance.jumpToNextSong();
			break;
		case 103:
			_mainInstance.jumpToPreviousSong();
			break;
		case 104:
			_mainInstance.seekBackwardCurrentPlayingSong();
			break;
		case 105:
			_mainInstance.seekForwardCurrentPlayingSong();
			break;

		default:
			break;
		}
	}
	
	private void handlingVoiceCommandForSync(OnCommand notification){
		switch (notification.getCmdID()) {
		case 1002:
			_mainInstance.syncPlayer.start();
			break;
		case 1003:
			_mainInstance.pauseCurrentSong();
			break;
		case 1004:
			_mainInstance.jumpToNextSong();
			break;
		case 1005:
			_mainInstance.jumpToPreviousSong();
			break;
		case 1006:
			_mainInstance.seekBackwardCurrentPlayingSong();
			break;
		case 1007:
			_mainInstance.seekForwardCurrentPlayingSong();
			break;
		case 1008:   // for Choice set
			PerformInteraction();
			break;
		case 1009:   // for text to speech
			PerformTTsInteraction();
			break;
		default:
			break;
		}
	}
	
	private void createInteractionChoiceSet(){
		int i;
		Vector<Choice> choiceVector = new Vector<Choice>();
			
		SongsManager mgr = new SongsManager();
		songsList = mgr.getPlayList();
		for(i = 0; i <songsList.size(); i++){
		
			Choice choice1 = new Choice();
			choice1.setChoiceID(i);
			//choice1.setMenuName("Track Number" +i);
			//Displaying song title as Interaction choice set displayable
			choice1.setMenuName(songsList.get(i).get("songTitle"));
			choice1.setVrCommands(new Vector<String>(Arrays.asList(new String[]{songsList.get(i).get("songTitle"),"Track "+i})));
			choiceVector.addElement(choice1);
			
		}
		lastIndexOfSongChoiceId = i;
		setLastIndexOfSongChoiceId(lastIndexOfSongChoiceId);
		RPCMessage trackMsg;
		trackMsg = RPCRequestFactory.buildCreateInteractionChoiceSet(choiceVector, nextInteractionChoiceCorrID(), nextCorrID());
		
		try {
			_syncProxy.sendRPCRequest((RPCRequest) trackMsg);
			
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Choice set for Info to be used TTS and TTs chunk
		Vector<Choice> ttsVector = new Vector<Choice>();
			Choice choice1 = new Choice();
			choice1.setChoiceID(lastIndexOfSongChoiceId+1);
			choice1.setMenuName("Info1");
			choice1.setVrCommands(new Vector<String>(Arrays.asList(new String[]{"Application"})));
			ttsVector.addElement(choice1);
			

			Choice choice2 = new Choice();
			choice2.setChoiceID(lastIndexOfSongChoiceId+2);
			choice2.setMenuName("Info2");
			choice2.setVrCommands(new Vector<String>(Arrays.asList(new String[]{"Features"})));
			ttsVector.addElement(choice2);
			

			Choice choice3 = new Choice();
			choice3.setChoiceID(lastIndexOfSongChoiceId+3);
			choice3.setMenuName("Info3");
			choice3.setVrCommands(new Vector<String>(Arrays.asList(new String[]{"Applink"})));
			ttsVector.addElement(choice3);
			
			RPCMessage infoMsg;
			infoMsg = RPCRequestFactory.buildCreateInteractionChoiceSet(ttsVector, nextInteractionChoiceCorrID(), nextCorrID());
			try {
				_syncProxy.sendRPCRequest((RPCRequest) infoMsg);
				
			} catch (SyncException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
	
	private void PerformInteraction(){
		Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks("Say track number, or, song title");
		Vector<TTSChunk> helpChunks = TTSChunkFactory
				.createSimpleTTSChunks("Please Select a song");
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory
				.createSimpleTTSChunks("Time's up! Try Again!");
		Vector<Integer> interactionChoiceSetIdList = new Vector<Integer>();
		interactionChoiceSetIdList.addElement(1031);
		RPCMessage req;
		req = RPCRequestFactory.buildPerformInteraction(initChunks, "Available Tracks", interactionChoiceSetIdList, helpChunks, timeoutChunks, InteractionMode.VR_ONLY, 10000, nextCorrID());
		try {
			_syncProxy.sendRPCRequest((RPCRequest) req);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void PerformTTsInteraction(){
		Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks("Available voice commands under info are, Features, Application, and, Applink");
		Vector<TTSChunk> helpChunks = TTSChunkFactory
				.createSimpleTTSChunks("Please Select your option");
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory
				.createSimpleTTSChunks("Time's up! Try Again!");
		Vector<Integer> interactionChoiceSetIdList = new Vector<Integer>();
		interactionChoiceSetIdList.addElement(1032);
		RPCMessage req;
		req = RPCRequestFactory.buildPerformInteraction(initChunks, "Get Information", interactionChoiceSetIdList, helpChunks, timeoutChunks, InteractionMode.VR_ONLY, 10000, nextCorrID());
		try {
			_syncProxy.sendRPCRequest((RPCRequest) req);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void perfomChoiceSetSelection(PerformInteractionResponse interactionResponse){
		Speak msg = new Speak();
		int choice = interactionResponse.getChoiceID();
		Vector<TTSChunk> chunks = new Vector<TTSChunk>();
		if(choice == getLastIndexOfSongChoiceId()+1){
			String applicationInfo = "This is a media Application";
			if(applicationInfo.length() > 0){
				chunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT, applicationInfo));
				
			}
			
		}else if(choice == getLastIndexOfSongChoiceId()+2){
			String features = "This app has voice and Buttons press capabilities";
			if(features.length() > 0){
				chunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT, features));
				
			}
			
			
		}else if(choice == getLastIndexOfSongChoiceId()+3 ){
			String appLink = "Applink is a ford Api to develope appLink enabled App";
			if(appLink.length() > 0){
				//chunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT, appLink));
				chunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT, appLink));
			}
		} else {
			_mainInstance.playCurrentSong(choice);
		}
		
		msg.setTtsChunks(chunks);
		msg.setCorrelationID(nextCorrID());
		try {
			_syncProxy.sendRPCRequest(msg);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, e.toString());
		}
			
	}

	@Override
	/*public void onAlertManeuverResponse(AlertManeuverResponse arg0) {
		// TODO Auto-generated method stub
		
	}*/

	//@Override
	public void onChangeRegistrationResponse(ChangeRegistrationResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleteFileResponse(DeleteFileResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDialNumberResponse(DialNumberResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	

	@Override
	public void onGetDTCsResponse(GetDTCsResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGetVehicleDataResponse(GetVehicleDataResponse response) {
		// TODO Auto-generated method stub
		Log.i("onGetVehicleData", response.toString());
	}

	@Override
	public void onListFilesResponse(ListFilesResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnAudioPassThru(OnAudioPassThru notification) {
		// TODO Auto-generated method stub
		Log.i("OnAudioPassThruNotif", "-"+notification.toString());

		final byte[] aptData = notification.getAPTData();
		SyncMainActivity.getInstance().runOnUiThread(new Runnable() {
			@Override
		public void run() {
		RecordingAudio.getInstance().audioPassThru(aptData);
			}
		});
		
	}
	
	@Override
	public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse response) {
		// TODO Auto-generated method stub
		Log.i("PerformAudioPassThru", "-"+response);

		final Result result = response.getResultCode();
		SyncMainActivity.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				RecordingAudio.getInstance().performAudioPassThruResponse(
						result);
			}
		});
		
	}
	
	@Override
	public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {
		// TODO Auto-generated method stub
		
		Log.i("EndAudioPassThru", "-"+response.toString());

		final SyncMainActivity mainActivity = SyncMainActivity.getInstance();
		final Result result = response.getResultCode();
		mainActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				RecordingAudio.getInstance().endAudioPassThruResponse(result);
			}
		});
		
	}

	@Override
	public void onOnLanguageChange(OnLanguageChange arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnVehicleData(OnVehicleData arg0) {
		// TODO Auto-generated method stub
		
	}

	

	@Override
	public void onPutFileResponse(PutFileResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReadDIDResponse(ReadDIDResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onScrollableMessageResponse(ScrollableMessageResponse scrollResponse) {
		// TODO Auto-generated method stub
		Log.i("Scroll Response", ""+scrollResponse.toString());
	}

	@Override
	public void onSetAppIconResponse(SetAppIconResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	/*public void onShowConstantTBTResponse(ShowConstantTBTResponse arg0) {
		// TODO Auto-generated method stub
		
	}*/

	@Override
	public void onSliderResponse(SliderResponse sliderRes) {
		// TODO Auto-generated method stub
		Log.i("Slider", ""+sliderRes);
	}

	@Override
	public void onSubscribeVehicleDataResponse(SubscribeVehicleDataResponse response) {
		// TODO Auto-generated method stub
		Log.i("onSubscribeVehicledata", response.toString());
		
	}

	@Override
	public void onUnsubscribeVehicleDataResponse(
			UnsubscribeVehicleDataResponse response) {
		// TODO Auto-generated method stub
		Log.i("onUnSubscribeVehicledata", response.toString());
		
	}

	/*@Override
	public void onUpdateTurnListResponse(UpdateTurnListResponse arg0) {
		// TODO Auto-generated method stub
		
	}*/

	private void showSoftButtonsOnScreen(){
		/*Image playimage = new Image();
		playimage.setValue("0xCF");
		playimage.setImageType(ImageType.STATIC);*/

//		Image pauseimage = new Image();
//		pauseimage.setValue("0xD0");
//		pauseimage.setImageType(ImageType.STATIC);

		next = new SoftButton();
		next.setText("Next");
		next.setSoftButtonID(100);
		next.setType(SoftButtonType.SBT_TEXT);
		next.setSystemAction(SystemAction.DEFAULT_ACTION);
		
		previous = new SoftButton();
		previous.setText("Previous");
		previous.setSoftButtonID(101);
		previous.setType(SoftButtonType.SBT_TEXT);
		previous.setSystemAction(SystemAction.DEFAULT_ACTION);
		
		/*forward = new SoftButton();
		forward.setText("Forward");
		forward.setSoftButtonID(102);
		forward.setType(SoftButtonType.SBT_TEXT);
		forward.setSystemAction(SystemAction.DEFAULT_ACTION);
		
		backward = new SoftButton();
		backward.setText("Backward");
		backward.setSoftButtonID(103);
		backward.setType(SoftButtonType.SBT_TEXT);
		backward.setSystemAction(SystemAction.DEFAULT_ACTION);*/
		
		//AppInfo
		appInfo = new SoftButton();
		appInfo.setText("AppInfo");
		appInfo.setSoftButtonID(104);
		appInfo.setType(SoftButtonType.SBT_TEXT);
		appInfo.setSystemAction(SystemAction.DEFAULT_ACTION);
		
		applinkInfo = new SoftButton();
		applinkInfo.setText("ApplinkInfo");
		applinkInfo.setSoftButtonID(105);
		applinkInfo.setType(SoftButtonType.SBT_TEXT);
		applinkInfo.setSystemAction(SystemAction.DEFAULT_ACTION);
		
		cmdInfo = new SoftButton();
		cmdInfo.setText("CommandInfo");
		cmdInfo.setSoftButtonID(106);
		cmdInfo.setType(SoftButtonType.SBT_TEXT);
		cmdInfo.setSystemAction(SystemAction.DEFAULT_ACTION);
		
		scrollableMsg = new SoftButton();
		scrollableMsg.setText("ScrollMsg");
		scrollableMsg.setSoftButtonID(107);
		scrollableMsg.setType(SoftButtonType.SBT_TEXT);
		scrollableMsg.setSystemAction(SystemAction.DEFAULT_ACTION);
		
		APTHCheck = new SoftButton();
		APTHCheck.setText("Record");
		APTHCheck.setSoftButtonID(108);
		APTHCheck.setType(SoftButtonType.SBT_TEXT);
		APTHCheck.setSystemAction(SystemAction.DEFAULT_ACTION);
		
		vehicleData = new SoftButton();
		vehicleData.setText("Vehicle");
		vehicleData.setSoftButtonID(109);
		vehicleData.setType(SoftButtonType.SBT_TEXT);
		vehicleData.setSystemAction(SystemAction.DEFAULT_ACTION);
		
		
		
		
		//Send Show RPC:
		      Vector<SoftButton> buttons = new Vector<SoftButton>();
				buttons.add(next);
				buttons.add(previous);
				//buttons.add(forward);
				//buttons.add(backward);
				buttons.add(appInfo);
				buttons.add(applinkInfo);
				buttons.add(cmdInfo);
				buttons.add(scrollableMsg);
				buttons.add(APTHCheck);
				buttons.add(vehicleData);
				try {
					_syncProxy.show("", "",
							"", "", null, buttons, null,
							null, nextCorrID());
				} catch (SyncException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}
	
	public void showLockScreen() {
		//only throw up lockscreen if main activity is currently on top
		//else, wait until onResume() to throw lockscreen so it doesn't 
		//pop-up while a user is using another app on the phone
		if(_mainInstance != null) {
			if(_mainInstance.isActivityonTop() == true){
				if(LockScreenActivity.getInstance() == null) {
					Intent i = new Intent(this, LockScreenActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
					startActivity(i);
				}
			}
		}
		lockscreenUP = true;		
	}

	private void clearlockscreen() {
		if(LockScreenActivity.getInstance() != null) {  
			LockScreenActivity.getInstance().exit();
		}
		lockscreenUP = false;
	}
	
	private void PerformVoiceRecordingInteraction(){
		Alert alert = new Alert();
		alert.setAlertText1("Voice Recording");
		alert.setAlertText2("Start in 3 seconds");
		alert.setDuration(3000);
		alert.setCorrelationID(nextCorrID());
		Vector<TTSChunk> ttsChunks = new Vector<TTSChunk>();
		ttsChunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
				"Speak to SYNC microphone to record! "));
		alert.setTtsChunks(ttsChunks);
		try {
			_syncProxy.sendRPCRequest(alert);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
