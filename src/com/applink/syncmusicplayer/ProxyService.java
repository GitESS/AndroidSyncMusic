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
import android.graphics.YuvImage;
import android.media.AudioManager;
import android.net.Proxy;
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
import com.ford.syncV4.proxy.rpc.AlertManeuverResponse;
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
import com.ford.syncV4.proxy.rpc.OnTBTClientState;
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
import com.ford.syncV4.proxy.rpc.ShowConstantTBTResponse;
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
import com.ford.syncV4.proxy.rpc.UpdateTurnListResponse;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.DriverDistractionState;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;
import com.ford.syncV4.proxy.rpc.enums.Language;
import com.ford.syncV4.proxy.rpc.enums.Result;
import com.ford.syncV4.proxy.rpc.enums.SoftButtonType;
import com.ford.syncV4.proxy.rpc.enums.SpeechCapabilities;
import com.ford.syncV4.proxy.rpc.enums.SystemAction;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.transport.TCPTransportConfig;
import com.ford.syncV4.util.DebugTool;

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
	// variable to contain the current state of the lockscreen
	private boolean lockscreenUP = false;
	/*
	 * private boolean firstHMIStatusChange = true; private static boolean
	 * waitingForResponse = false;
	 */
	private boolean driverdistrationNotif = false;
	public int trackNumber = 1;
	private boolean isFullCalled = false;
	// Voice cmd implementation
	private Integer autoIncCNDCorrId = 1001;
	private Integer choiceId = 1010;
	private Integer choiceSetId = 1020;
	private Integer interactionChoiceSetID = 1030;
	private int lastIndexOfSongChoiceId;
	private String initChunks, helpChunks, tymoutChunks, displayable;
	

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
		Toast.makeText(getApplicationContext(),
				"Control is on OnCreate if Service is not created, Create it",
				Toast.LENGTH_SHORT).show();

		Log.i(TAG, "ProxyService.onCreate()");
		_instance = this;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "ProxyService.onStartCommand()");
		Toast.makeText(getApplicationContext(), "Control is on OnStartCommand",
				Toast.LENGTH_SHORT).show();
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBtAdapter != null) {
			if (mBtAdapter.isEnabled()) {
				startProxy();
			}
		}

		if (SyncMainActivity.getInstance() != null) {
			setCurrentActivity(SyncMainActivity.getInstance());
		}

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

	public void startProxy() {
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

				if (transportType == Const.Transport.KEY_BLUETOOTH) {
					// _syncProxy = new SyncProxyALM(this, appName, isMediaApp);
					_syncProxy = new SyncProxyALM(this, appName, isMediaApp,
							Language.EN_US, Language.EN_US, "584421907");

				}
			} catch (SyncException e) {
				e.printStackTrace();
				// error creating proxy, returned proxy = null
				if (_syncProxy == null) {
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
		if (_mainInstance.syncPlayer != null)
			_mainInstance.syncPlayer.release();
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

	public static ProxyService getInstance() {
		return _instance;
	}

	public void setCurrentActivity(SyncMainActivity currentActivity) {
		if (this._mainInstance != null) {
			this._mainInstance.finish();
			this._mainInstance = null;
		}

		this._mainInstance = currentActivity;

	}

	public static SyncProxyALM getProxyInstance() {
		return _syncProxy;
	}

	// public static void waiting(boolean waiting) {
	// waitingForResponse = waiting;
	// }

	private void initializeTheApp() {

		playingAudio = true;

		// ButtonSubscriptions
		initializeButtonsToBeSubscribed();
		// softButtons Implementation
		showSoftButtonsOnScreen();

		initializeVoiceCommand();

		Log.i("Indide the", "Init method4");
		// ChoiceSet
		createInteractionChoiceSet();
		showLockScreen();

	}

	private void initializeVoiceCommand() {
		try {
			Log.i("Play Song", "" + nextCMDCorrID());
			_syncProxy.addCommand(
					1002,
					"Play Song",
					new Vector<String>(Arrays.asList(new String[] {
							"Play Song", "Play" })), nextCorrID());
			Log.i("Pause Song", "" + nextCMDCorrID());
			_syncProxy.addCommand(
					1003,
					"Pause Song",
					new Vector<String>(Arrays.asList(new String[] {
							"Pause Song", "Pause" })), nextCorrID());
			Log.i("Next Song", "" + nextCMDCorrID());
			_syncProxy.addCommand(
					1004,
					"Next Song",
					new Vector<String>(Arrays.asList(new String[] {
							"Next Song", "Next" })), nextCorrID());
			Log.i("Previous Song", "" + nextCMDCorrID());
			_syncProxy.addCommand(
					1005,
					"Previous Song",
					new Vector<String>(Arrays.asList(new String[] {
							"Previous Song", "Previous" })), nextCorrID());
			Log.i("Backward Song", "" + nextCMDCorrID());
			_syncProxy.addCommand(
					1006,
					"Backward Song",
					new Vector<String>(Arrays.asList(new String[] {
							"Backward Song", "Backward", "bekward", "bekword",
							"Seek Backward" })), nextCorrID());
			Log.i("Forward Song", "" + nextCMDCorrID());
			_syncProxy.addCommand(
					1007,
					"Forward Song",
					new Vector<String>(Arrays.asList(new String[] {
							"Forward Song", "Forward", "forward", "forwod",
							"Seek Forward" })), nextCorrID());
			Log.i("Play Song", "" + nextCMDCorrID());
			_syncProxy.addCommand(1008, "Select Song", new Vector<String>(
					Arrays.asList(new String[] { "Select Song" })),
					nextCorrID());
			Log.i("Play Song", "" + nextCMDCorrID());
			_syncProxy.addCommand(1009, "info",
					new Vector<String>(Arrays.asList(new String[] { "info" })),
					nextCorrID());
		} catch (SyncException e) {
			Log.e(TAG, "Error adding AddCommands", e);
		}
	}

	private void initializeButtonsToBeSubscribed() {
		try {
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
		} catch (SyncException e) {
			e.printStackTrace();
			Log.e(TAG, e.toString());
		}
	}

	@Override
	public void onOnHMIStatus(OnHMIStatus notification) {
		Log.i(TAG, "" + notification);

		switch (notification.getSystemContext()) {
		case SYSCTXT_MAIN:
			break;
		case SYSCTXT_VRSESSION:
			try {
				initializeTheApp();
			} catch (Exception e) {
				Log.i("SyncProxy", "VRSESSION");
			}
			break;
		case SYSCTXT_MENU:
			try {
				initializeTheApp();
			} catch (Exception e) {
				Log.i("SyncProxy", "MENU");
			}
			break;
		default:
			return;
		}
		switch (notification.getAudioStreamingState()) {
		case AUDIBLE:
			if (playingAudio)
				_mainInstance.playCurrentSong();
			break;
		case NOT_AUDIBLE:
			SyncMainActivity.getInstance().pauseCurrentSong();
			break;
		default:
			return;
		}

		switch (notification.getHmiLevel()) {
		// Checking bluetooth connectivity here to terminate the app in case
		// bluetooth is not on.
		case HMI_FULL:
			Log.i("HMI", "FULL");
			isFullCalled = true;

			if (_syncProxy.getAppInterfaceRegistered()) {
				if (notification.getFirstRun()) {
					try {
						_syncProxy.show("Welcome", "Sync Music Player",
								TextAlignment.CENTERED, nextCorrID());
						Log.i("InFull", "Before Calling initializeTheApp()");
						initializeTheApp();
					} catch (SyncException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			} else {
				try {
					_syncProxy.show("SyncProxy", "Alive",
							TextAlignment.CENTERED, nextCorrID());
					initializeTheApp();
				} catch (SyncException e) {
					DebugTool.logError("Not Able to send Show", e);
				}
			}

			break;
		case HMI_LIMITED:
			Log.i("HMI_LIMITED", "HMI_LIMITED");
			// initializeTheApp();
			break;
		case HMI_BACKGROUND:
			Log.i("HMI_BACKGROUND", "HMI_BACKGROUND");
			// initializeTheApp();
			break;
		case HMI_NONE:
			if (isFullCalled) {
				clearlockscreen();
				SyncMainActivity.getInstance().finish();
				stopSelf();
			}
			Log.i("SyncProxy", "HMI_NONE");
			//
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
		Log.e(TAG, "onProxyClosed: TDK-EXIT" + arg0, e);

		clearlockscreen();
		if ((((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.SYNC_PROXY_CYCLED)) {
			if (((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.BLUETOOTH_DISABLED) {
				Log.v(TAG, "reset proxy in onproxy closed");
				// reset();
			}
		}

	}

	public void reset() {

		/*
		 * if (_syncProxy != null) { try { _syncProxy.resetProxy();
		 * SyncMainActivity.getInstance().finish(); } catch (SyncException e1) {
		 * e1.printStackTrace(); // // something goes wrong, & the proxy returns
		 * as null, stop the // // service. // // do not want a running service
		 * with a null proxy if (_syncProxy == null) { stopSelf(); } } } else {
		 * startProxy(); }
		 */

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
	public void onAlertResponse(AlertResponse alertNotif) {
		// TODO Auto-generated method stub
		Log.i("" + alertNotif.getFunctionName(), "" + alertNotif);
	}

	@Override
	public void onCreateInteractionChoiceSetResponse(
			CreateInteractionChoiceSetResponse interactionChoiceSetResponse) {
		// TODO Auto-generated method stub
		Log.i("" + interactionChoiceSetResponse.getFunctionName(), ""
				+ interactionChoiceSetResponse);
	}

	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse delCmdResponse) {
		// TODO Auto-generated method stub
		Log.i(""+delCmdResponse.getFunctionName(), ""+delCmdResponse.getInfo()+"-"+delCmdResponse);
	}

	@Override
	public void onDeleteInteractionChoiceSetResponse(
			DeleteInteractionChoiceSetResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse delSubMenuResponse) {
		// TODO Auto-generated method stub
		Log.i(""+delSubMenuResponse.getFunctionName()+"-"+delSubMenuResponse.getResultCode(), ""+delSubMenuResponse.getInfo()+"-"+delSubMenuResponse);
	}

	@Override
	public void onGenericResponse(GenericResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnButtonEvent(OnButtonEvent notification) {
		// TODO Auto-generated method stub
		Log.i(TAG, "" + notification.getButtonName());

	}

	@Override
	public void onOnButtonPress(OnButtonPress notification) {
		// TODO Auto-generated method stub
		Log.i(TAG, "" + notification);

		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		switch (notification.getButtonName()) {
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
			audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_RAISE, AudioManager.FLAG_VIBRATE);
			break;
		case TUNEDOWN:
			audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
					AudioManager.ADJUST_LOWER, AudioManager.FLAG_VIBRATE);
			break;
		case PRESET_0:
			// SubscribeVehicleDataClass.getInstance(ProxyService.this,
			// 0).getVehicleData();
			_mainInstance.playCurrentSong(0);
			break;
		case PRESET_1:
			// SubscribeVehicleDataClass.getInstance(ProxyService.this,
			// 1).getVehicleData();
			_mainInstance.playCurrentSong(1);
			break;
		case PRESET_2:
			// SubscribeVehicleDataClass.getInstance(ProxyService.this,
			// 2).getVehicleData();
			_mainInstance.playCurrentSong(2);
			break;
		case PRESET_3:
			_mainInstance.playCurrentSong(3);
			break;
		case PRESET_4:
			_mainInstance.playCurrentSong(4);
			break;
		case PRESET_5:
			_mainInstance.playCurrentSong(5);
			break;
		case PRESET_6:
			_mainInstance.playCurrentSong(6);
			break;
		case PRESET_7:
			_mainInstance.playCurrentSong(7);
			break;
		case PRESET_8:
			_mainInstance.playCurrentSong(8);
			break;
		case PRESET_9:
			_mainInstance.playCurrentSong(9);
			break;

		default:
			break;
		}

		// Handling softButtons notifications-- 6 softbuttons cmd are albumList,
		// SongList, Song info, app info, applink info, command info

		if (notification.getCustomButtonName().equals(100)) {
			String msg = "Next";
			AlertClass.getInstance(ProxyService.this).getAlert("Next", 3000, msg);
			SyncMainActivity.getInstance().jumpToNextSong();

		} else if (notification.getCustomButtonName().equals(101)) {
			String msg = "Previous";
			AlertClass.getInstance(ProxyService.this).getAlert("Previous", 3000, msg);
			SyncMainActivity.getInstance().jumpToPreviousSong();
		} else if (notification.getCustomButtonName().equals(102)) {
			String msg = "This is a Applink enabled music player application, designed to run on Ford's Sync. All commands of this application are Voice based.";
			AlertClass.getInstance(ProxyService.this).getAlert("Appinfo",3000,  msg);
			// SyncMainActivity.getInstance().seekBackwardCurrentPlayingSong();
		} else if (notification.getCustomButtonName().equals(103)) {
			String msg = "Applink is a Ford's API which is used to make Android or iOS application Applink enabled.";
			AlertClass.getInstance(ProxyService.this).getAlert("Appinfo", 3000, msg);

		} else if (notification.getCustomButtonName().equals(104)) {
			String msg = "Four Main Commands are available in this Music application. Apart from Play and pause commands. +"
											+ "There are Next, Previous, Seek forward and Seek Backward commands are vailable as well. These commmands are also available in Voice recognition and Sub Menu CMD forms";
			AlertClass.getInstance(ProxyService.this).getAlert("Appinfo", 3000, msg);
			
		} else if (notification.getCustomButtonName().equals(105)) {
			String scrollableMessageBody = new String(
					"This is Applink enabled Application. This Player has Voice command. User can give voice command to operate this player. Available Voice commands are Play, Pause, Next, Previous, Backward and forwards. This Player has Voice command. User can give voice command to operate this player. Available Voice commands are Play, Pause, Next, Previous, Backward and forwards");
			ScrollableMessageClass.getInstance(ProxyService.this)
					.getScrollableMessage(scrollableMessageBody);

		} else if (notification.getCustomButtonName().equals(106)) {
			PerformAudioPassThruClass.getInstance(ProxyService.this).show();
		} else if (notification.getCustomButtonName().equals(107)) {
			SubscribeVehicleDataClass.getInstance(ProxyService.this, 2).getVehicleData();
		

		}

	}

	@Override
	public void onOnCommand(OnCommand notification) {
		// TODO Auto-generated method stub
		Log.i("notification.getCmdID()", "" + notification.getCmdID());
		switch (notification.getCmdID()) {
		case 1002:
			_mainInstance.syncPlayer.start();
			break;
		case 1003:
			_mainInstance.pauseCurrentSong();
			break;
		case 1004:
			Log.i("notification.getCmdID()", "" + notification.getCmdID());
			_mainInstance.jumpToNextSong();
			break;
		case 1005:
			Log.i("notification.getCmdID()", "" + notification.getCmdID());
			_mainInstance.jumpToPreviousSong();
			break;
		case 1006:
			Log.i("notification.getCmdID()", "" + notification.getCmdID());
			_mainInstance.seekBackwardCurrentPlayingSong();
			break;
		case 1007:
			Log.i("notification.getCmdID()", "" + notification.getCmdID());
			_mainInstance.seekForwardCurrentPlayingSong();
			break;
		case 1008: // for Choice set
			PerformInteraction();
//			initChunks = new String("Say track number or Song title");
//			helpChunks = new String("Please select a song");
//			tymoutChunks = new String("Time's up! Try again!");
//			displayable = new String("Available Tracks");
//			PerformInteractionClass.getInstance(ProxyService.this).performInteraction(initChunks, helpChunks, tymoutChunks, displayable, 1032);
			break;
		case 1009: // for text to speech
			PerformTTsInteraction();
//			initChunks = new String("Available voice commands under info are, Features, Application, and, Applink");
//			helpChunks = new String("Please select your option");
//			tymoutChunks = new String("Time's up! Try again!");
//			displayable = new String("Get Informations");
//			PerformInteractionClass.getInstance(ProxyService.this).performInteraction(initChunks, helpChunks, tymoutChunks, displayable, 1033);
			break;
		default:
			break;
		}
	}

	@Override
	public void onOnPermissionsChange(OnPermissionsChange arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPerformInteractionResponse(
			PerformInteractionResponse interactionResponse) {
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

		// _syncProxy.resetGlobalProperties(properties, nextCorrID());

	}

	@Override
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onShowResponse(ShowResponse showNotif) {
		// TODO Auto-generated method stub
		Log.i("" + showNotif.getFunctionName(), "" + showNotif.getInfo()
				+ " - " + showNotif);
	}

	@Override
	public void onSpeakResponse(SpeakResponse speakResNotif) {
		// TODO Auto-generated method stub
		Log.i("" + speakResNotif.getFunctionName(),
				"" + speakResNotif.getInfo() + " - " + speakResNotif);
	}

	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
		// TODO Auto-generated method stub
		Log.i(TAG, "" + response);
	}

	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
		// TODO Auto-generated method stub
		Log.i(TAG, "UnsubscribeButtonResponse" + response);
	}

	@Override
	public void onOnDriverDistraction(OnDriverDistraction notification) {
		// TODO Auto-generated method stub
		driverdistrationNotif = true;
		// Log.i(TAG, "dd: " + notification.getStringState());
		if (notification.getState() == DriverDistractionState.DD_OFF) {
			Log.i(TAG, "clear lock, DD_OFF");
			clearlockscreen();
		} else {
			Log.i(TAG, "show lockscreen, DD_ON");
			showLockScreen();
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return new Binder();
	}

	private void createInteractionChoiceSet() {
		int i;
		Vector<Choice> choiceVector = new Vector<Choice>();

		SongsManager mgr = new SongsManager();
		songsList = mgr.getPlayList();
		for (i = 0; i < songsList.size(); i++) {
			Choice choice1 = new Choice();
			choice1.setChoiceID(i);
			// Displaying song title as Interaction choice set displayable
			choice1.setMenuName(songsList.get(i).get("songTitle"));
			choice1.setVrCommands(new Vector<String>(Arrays
					.asList(new String[] { songsList.get(i).get("songTitle"),
							"Track " + i })));
			choiceVector.addElement(choice1);
		}
		//ChoiceSetClass.getInstance(ProxyService.this).createChoiceSets(choiceVector);
		
		lastIndexOfSongChoiceId = i;
		setLastIndexOfSongChoiceId(lastIndexOfSongChoiceId);
		RPCRequest trackMsg;
		Log.i("InteractionChoiceSet -", "" + nextInteractionChoiceCorrID());
		trackMsg = RPCRequestFactory.buildCreateInteractionChoiceSet(
				choiceVector, nextInteractionChoiceCorrID(), nextCorrID());

		try {
			_syncProxy.sendRPCRequest(trackMsg);

		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Choice set for Info to be used TTS and TTs chunk
//		ChoiceSetClass.getInstance(ProxyService.this).createChoiceSet(lastIndexOfSongChoiceId + 1, "Application");
//		ChoiceSetClass.getInstance(ProxyService.this).createChoiceSet(lastIndexOfSongChoiceId + 2, "Features");
//		ChoiceSetClass.getInstance(ProxyService.this).createChoiceSet(lastIndexOfSongChoiceId + 3, "Applink");
		Vector<Choice> ttsVector = new Vector<Choice>();
		Choice choice1 = new Choice();
		choice1.setChoiceID(lastIndexOfSongChoiceId + 1);
		choice1.setMenuName("Application");
		choice1.setVrCommands(new Vector<String>(Arrays
				.asList(new String[] { "Application" })));
		ttsVector.addElement(choice1);

		Choice choice2 = new Choice();
		choice2.setChoiceID(lastIndexOfSongChoiceId + 2);
		choice2.setMenuName("Features");
		choice2.setVrCommands(new Vector<String>(Arrays
				.asList(new String[] { "Features" })));
		ttsVector.addElement(choice2);

		Choice choice3 = new Choice();
		choice3.setChoiceID(lastIndexOfSongChoiceId + 3);
		choice3.setMenuName("Applink");
		choice3.setVrCommands(new Vector<String>(Arrays
				.asList(new String[] { "Applink" })));
		ttsVector.addElement(choice3);

		RPCRequest infoMsg;
		infoMsg = RPCRequestFactory.buildCreateInteractionChoiceSet(ttsVector,
				nextInteractionChoiceCorrID(), nextCorrID());
		try {
			_syncProxy.sendRPCRequest(infoMsg);

		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void PerformInteraction() {
		Vector<TTSChunk> initChunks = TTSChunkFactory
				.createSimpleTTSChunks("Say track number, or, song title");
		Vector<TTSChunk> helpChunks = TTSChunkFactory
				.createSimpleTTSChunks("Please Select a song");
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory
				.createSimpleTTSChunks("Time's up! Try Again!");
		Vector<Integer> interactionChoiceSetIdList = new Vector<Integer>();
		interactionChoiceSetIdList.addElement(1032);
		RPCRequest req;
		req = RPCRequestFactory.buildPerformInteraction(initChunks,
				"Available Tracks", interactionChoiceSetIdList, helpChunks,
				timeoutChunks, InteractionMode.VR_ONLY, 10000, nextCorrID());
		try {
			_syncProxy.sendRPCRequest(req);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void PerformTTsInteraction() {
		Vector<TTSChunk> initChunks = TTSChunkFactory
				.createSimpleTTSChunks("Available voice commands under info are, Features, Application, and, Applink");
		Vector<TTSChunk> helpChunks = TTSChunkFactory
				.createSimpleTTSChunks("Please Select your option");
		Vector<TTSChunk> timeoutChunks = TTSChunkFactory
				.createSimpleTTSChunks("Time's up! Try Again!");
		Vector<Integer> interactionChoiceSetIdList = new Vector<Integer>();
		interactionChoiceSetIdList.addElement(1033);
		RPCRequest req;
		req = RPCRequestFactory.buildPerformInteraction(initChunks,
				"Get Information", interactionChoiceSetIdList, helpChunks,
				timeoutChunks, InteractionMode.VR_ONLY, 10000, nextCorrID());
		try {
			_syncProxy.sendRPCRequest(req);
		} catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void perfomChoiceSetSelection(PerformInteractionResponse interactionResponse) {
		Speak msg = new Speak();
		int choice = interactionResponse.getChoiceID();
		//System.out.println("Choice ID - "+choice);
		Vector<TTSChunk> chunks = new Vector<TTSChunk>();
		if (choice == getLastIndexOfSongChoiceId() + 1) {
			String applicationInfo = "This is a media Application";
			if (applicationInfo.length() > 0) {
				chunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,	applicationInfo));

			}

		} else if (choice == getLastIndexOfSongChoiceId() + 2) {
			String features = "This app has voice and Buttons press capabilities";
			if (features.length() > 0) {
				chunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
						features));

			}

		} else if (choice == getLastIndexOfSongChoiceId() + 3) {
			String appLink = "Applink is a ford Api to develope appLink enabled App";
			if (appLink.length() > 0) {
				// chunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
				// appLink));
				chunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT,
						appLink));
			}
		} else {
			Log.i("proxy service", " In choice mOd" + choice);

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
	/*
	 * public void onAlertManeuverResponse(AlertManeuverResponse arg0) { // TODO
	 * Auto-generated method stub
	 * 
	 * }
	 */
	// @Override
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
		Log.i("OnAudioPassThruNotif", "-" + notification.toString());

		final byte[] aptData = notification.getAPTData();
		SyncMainActivity.getInstance().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				RecordingAudio.getInstance().audioPassThru(aptData);
			}
		});

	}

	@Override
	public void onPerformAudioPassThruResponse(
			PerformAudioPassThruResponse response) {
		// TODO Auto-generated method stub
		Log.i("PerformAudioPassThru", "-" + response);

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

		Log.i("EndAudioPassThru", "-" + response.toString());

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
	public void onScrollableMessageResponse(
			ScrollableMessageResponse scrollResponse) {
		// TODO Auto-generated method stub
		Log.i("Scroll Response", "" + scrollResponse.toString());
	}

	@Override
	public void onSetAppIconResponse(SetAppIconResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse arg0) {
		// TODO Auto-generated method stub

	}

	/*
	 * @Override public void onShowConstantTBTResponse(ShowConstantTBTResponse
	 * arg0) { //TODO Auto-generated method stub
	 * 
	 * }
	 */

	@Override
	public void onSliderResponse(SliderResponse sliderRes) {
		// TODO Auto-generated method stub
		Log.i("Slider", "" + sliderRes);
	}

	@Override
	public void onSubscribeVehicleDataResponse(
			SubscribeVehicleDataResponse response) {
		// TODO Auto-generated method stub
		Log.i("onSubscribeVehicledata", response.toString());

	}

	@Override
	public void onUnsubscribeVehicleDataResponse(
			UnsubscribeVehicleDataResponse response) {
		// TODO Auto-generated method stub
		Log.i("onUnSubscribeVehicledata", response.toString());

	}

	/*
	 * @Override public void onUpdateTurnListResponse(UpdateTurnListResponse
	 * arg0) { // TODO Auto-generated method stub
	 * 
	 * }
	 */

	private void showSoftButtonsOnScreen() {

		//Add Soft button name 
		ArrayList<String> SoftButtonName = new ArrayList<String>();
		SoftButtonName.add("Next");
		SoftButtonName.add("Prev");
		SoftButtonName.add("AppInfo");
		SoftButtonName.add("Applink");
		SoftButtonName.add("CmdInfo");
		SoftButtonName.add("Help");
		SoftButtonName.add("Record");
		SoftButtonName.add("Vehicle");
		
		//Add Soft buttonID
		ArrayList<Integer> SoftButtonId = new ArrayList<Integer>();
		SoftButtonId.add(100);
		SoftButtonId.add(101);
		SoftButtonId.add(102);
		SoftButtonId.add(103);
		SoftButtonId.add(104);
		SoftButtonId.add(105);
		SoftButtonId.add(106);
		SoftButtonId.add(107);
		

		SoftButtonClass.getInstance(ProxyService.this).addSoftButtons(
				SoftButtonName, SoftButtonId);

			}

	public void showLockScreen() {
		// only throw up lockscreen if main activity is currently on top
		// else, wait until onResume() to throw lockscreen so it doesn't
		// pop-up while a user is using another app on the phone
		Log.i("In ShowLockScreen", "control is here");
		if (SyncMainActivity.getInstance() == null) {
			Intent i = new Intent(ProxyService.this, SyncMainActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			i.putExtra("startlock", "lock");
			startActivity(i);
		} else {
			_mainInstance.playCurrentSong();

			Intent i = new Intent(this, LockScreenActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
			startActivity(i);
		}

		lockscreenUP = true;
	}

	private void clearlockscreen() {
		if (LockScreenActivity.getInstance() != null) {
			// LockScreenActivity.getInstance().exit();
			Log.i("LOckScreen", "Calling");
			LockScreenActivity.getInstance().finish();
		}
		lockscreenUP = false;
	}

	public boolean getLockScreenStatus() {
		return lockscreenUP;
	}

	@Override
	public void onAlertManeuverResponse(AlertManeuverResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOnTBTClientState(OnTBTClientState arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onShowConstantTBTResponse(ShowConstantTBTResponse arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUpdateTurnListResponse(UpdateTurnListResponse arg0) {
		// TODO Auto-generated method stub

	}

}
