package com.applink.syncmusicplayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.Language;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.transport.TransportType;

public class SyncMainActivity extends Activity implements OnCompletionListener,
		SeekBar.OnSeekBarChangeListener {
	private ImageButton btnPlay;
	private ImageButton btnForward;
	private ImageButton btnBackward;
	private ImageButton btnNext;
	private ImageButton btnPrevious;
	private ImageButton btnPlaylist;
	private ImageButton btnRepeat;
	private ImageButton btnShuffle;
	private SeekBar songProgressBar;
	private TextView songTitleLabel;
	private TextView songCurrentDurationLabel;
	private TextView songTotalDurationLabel;
	// Media Player
	public MediaPlayer syncPlayer;
	// Handler to update UI timer, progress bar etc,.
	private Handler mHandler = new Handler();;
	private SongsManager _songManager;
	private Utilities utils;
	private int seekForwardTime = 30000; // 5000 milliseconds
	private int seekBackwardTime = 30000; // 5000 milliseconds
	private int currentSongIndex = 0;
	// private int currentIndex;
	private boolean isShuffle = false;
	private boolean isRepeat = false;
	long totalDuration = 0;
	long currentDuration = 0;
	private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
	private boolean activityOnTop;
	public int trackNumber;
	private int currentPlayingSongIndex;
	Button exit;
	private static final String logTag = "SyncMusicPlayer";
	private static SyncMainActivity _activity;

	/**
	 * In onCreate() specifies if it is the first time the activity is created
	 * during this app launch.
	 */
	private static boolean isFirstActivityRun = true;

	public int getCurrentPlayingSongIndex() {
		return currentPlayingSongIndex;
	}

	public void setCurrentPlayingSongIndex(int currentPlayingSongIndex) {
		this.currentPlayingSongIndex = currentPlayingSongIndex;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		startSyncProxy();
		_activity = this;

		// Set all player buttons
		setAllMusicPlayerButtons();

		isFirstActivityRun = false;

		Bundle b;
		b = getIntent().getExtras();
		try {
			String getExtraFromService = b.getString("startlock");
			if (getExtraFromService != null) {
				if (getExtraFromService.equalsIgnoreCase("lock")) {
					playCurrentSong();
					Intent i = new Intent(SyncMainActivity.this,
							LockScreenActivity.class);
					startActivity(i);
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	private final int PROXY_START = 5;
	private final int MNU_EXIT = 7;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		boolean result = super.onCreateOptionsMenu(menu);
		if (result) {
			menu.add(0, PROXY_START, 0, "Proxy Start");
			menu.add(0, MNU_EXIT, 0, "Exit");

			return true;
		} else {
			return false;
		}
	}

	private boolean getIsMedia() {
		return getSharedPreferences(Const.PREFS_NAME, 0).getBoolean(
				Const.PREFS_KEY_ISMEDIAAPP, Const.PREFS_DEFAULT_ISMEDIAAPP);
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case PROXY_START:
			BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			if (!mBtAdapter.isEnabled())
				mBtAdapter.enable();

			if (ProxyService.getInstance() == null) {
				Intent startIntent = new Intent(this, ProxyService.class);
				startService(startIntent);
			} else {
				ProxyService.getInstance().setCurrentActivity(this);
			}

			if (ProxyService.getInstance().getProxyInstance() != null) {
				try {
					ProxyService.getInstance().getProxyInstance().resetProxy();
				} catch (SyncException e) {
				}
			}

			if (!mBtAdapter.isDiscovering()) {
				Intent discoverableIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.putExtra(
						BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
				startActivity(discoverableIntent);
			}
			return true;
		case MNU_EXIT:
			exitApp();
			break;
		}
		return false;
	}

	/** Closes the activity and stops the proxy service. */
	public void exitApp() {
		stopService(new Intent(this, ProxyService.class));
		finish();
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}, 1000);
	}

	public void setAllMusicPlayerButtons() {
		setContentView(R.layout.player);
		btnPlay = (ImageButton) findViewById(R.id.btnPlay);
		btnForward = (ImageButton) findViewById(R.id.btnForward);
		btnBackward = (ImageButton) findViewById(R.id.btnBackward);
		btnNext = (ImageButton) findViewById(R.id.btnNext);
		btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
		btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist);
		btnRepeat = (ImageButton) findViewById(R.id.btnRepeat);
		btnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
		songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
		songTitleLabel = (TextView) findViewById(R.id.songTitle);
		songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
		songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);
		exit = (Button) findViewById(R.id.button_exit);

		exit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				exitApp();
			}
		});
		// Mediaplayer
		syncPlayer = new MediaPlayer();
		_songManager = new SongsManager();
		utils = new Utilities();

		// Listeners
		songProgressBar.setOnSeekBarChangeListener(this);
		syncPlayer.setOnCompletionListener(this);

		// Getting all songs list
		songsList = _songManager.getPlayList();

		/**
		 * Play button click event plays a song and changes button to pause
		 * image pauses a song and changes button to play image
		 * */
		btnPlay.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				playPauseCurrentPlayingSong();

			}
		});

		/**
		 * Forward button click event Forwards song specified seconds
		 * */
		btnForward.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				seekForwardCurrentPlayingSong();

			}
		});
		/**
		 * Backward button click event Backward song to specified seconds
		 * */
		btnBackward.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				seekBackwardCurrentPlayingSong();
			}
		});
		/**
		 * Next button click event Plays next song by taking currentSongIndex +
		 * 1
		 * */
		btnNext.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				jumpToNextSong();

			}
		});

		/**
		 * Back button click event Plays previous song by currentSongIndex - 1
		 * */
		btnPrevious.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				jumpToPreviousSong();

			}
		});
		/**
		 * Button Click event for Repeat button Enables repeat flag to true
		 * */
		btnRepeat.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (isRepeat) {
					isRepeat = false;
					Toast.makeText(getApplicationContext(), "Repeat is OFF",
							Toast.LENGTH_SHORT).show();
					btnRepeat.setImageResource(R.drawable.btn_repeat);
				} else {
					// make repeat to true
					isRepeat = true;
					Toast.makeText(getApplicationContext(), "Repeat is ON",
							Toast.LENGTH_SHORT).show();
					// make shuffle to false
					isShuffle = false;
					btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
					btnShuffle.setImageResource(R.drawable.btn_shuffle);
				}
				Toast.makeText(_activity, "Option is Disabled for now!",
						Toast.LENGTH_SHORT).show();
			}
		});

		/**
		 * Button Click event for Shuffle button Enables shuffle flag to true
		 * */
		btnShuffle.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (isShuffle) {
					isShuffle = false;
					Toast.makeText(getApplicationContext(), "Shuffle is OFF",
							Toast.LENGTH_SHORT).show();
					btnShuffle.setImageResource(R.drawable.btn_shuffle);
				} else {
					// make repeat to true
					isShuffle = true;
					Toast.makeText(getApplicationContext(), "Shuffle is ON",
							Toast.LENGTH_SHORT).show();
					// make shuffle to false
					isRepeat = false;
					btnShuffle.setImageResource(R.drawable.btn_shuffle_focused);
					btnRepeat.setImageResource(R.drawable.btn_repeat);
				}
				Toast.makeText(_activity, "Option is Disabled for now!",
						Toast.LENGTH_SHORT).show();
			}
		});

	}

	// Go to previous Song from current one
	public void jumpToPreviousSong() {
		currentSongIndex = getCurrentPlayingSongIndex();
		if (currentSongIndex > 0) {
			playCurrentSong(currentSongIndex - 1);
			currentSongIndex = currentSongIndex - 1;
		} else {
			// play last song
			playCurrentSong((songsList.size()) - 1);
			currentSongIndex = ((songsList.size()) - 1);
		}
	}

	// GO to next Song from the current one
	public void jumpToNextSong() {

		// check if next song is there or not
		currentSongIndex = getCurrentPlayingSongIndex();
		if (currentSongIndex < songsList.size()) {
			playCurrentSong(currentSongIndex + 1);
			currentSongIndex = currentSongIndex + 1;
		} else {
			// play first song
			playCurrentSong(0);
			currentSongIndex = 0;
		}
	}

	public void seekBackwardCurrentPlayingSong() {
		// get current song position
		int currentPosition = syncPlayer.getCurrentPosition();
		Log.i("Backward-currentPos", "" + currentPosition);
		// check if seekBackward time is greater than 0 sec
		if (currentPosition - seekBackwardTime >= 0) {
			Log.i("If-Backward ", "Control");
			// Backward song

			syncPlayer.seekTo(currentPosition - seekBackwardTime);
			String songTitle = songsList.get(getCurrentPlayingSongIndex()).get(
					"songTitle");
			Log.i("backward-currentPos", "" + currentPosition + "index "
					+ getCurrentPlayingSongIndex() + " name" + songTitle);

			try {
				ProxyService.getProxyInstance().show(
						"Track No- :" + getCurrentPlayingSongIndex(),
						songTitle, TextAlignment.LEFT_ALIGNED,
						ProxyService.getInstance().nextCorrID());
			} catch (SyncException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Log.i("else-Backward ", "Control");
			// backward to starting position
			syncPlayer.seekTo(0);
		}
	}

	public void seekForwardCurrentPlayingSong() {

		// get current song position
		int currentPosition = syncPlayer.getCurrentPosition();
		// check if seekForward time is lesser than song duration
		if (currentPosition + seekForwardTime <= syncPlayer.getDuration()) {
			Log.i("If-Forward ", "Control");
			// forward song

			syncPlayer.seekTo(currentPosition + seekForwardTime);
			String songTitle = songsList.get(getCurrentPlayingSongIndex()).get(
					"songTitle");
			Log.i("Forwardward-currentPos", "" + currentPosition + "index "
					+ getCurrentPlayingSongIndex() + " name" + songTitle);

			try {
				ProxyService.getProxyInstance().show(
						"Track No- :" + getCurrentPlayingSongIndex(),
						songTitle, TextAlignment.LEFT_ALIGNED,
						ProxyService.getInstance().nextCorrID());
			} catch (SyncException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// forward to end position
			Log.i("Else-Forward ", "Control");
			syncPlayer.seekTo(syncPlayer.getDuration());
		}
	}

	public void playPauseCurrentPlayingSong() {
		if (syncPlayer != null && syncPlayer.isPlaying()) {
			ProxyService.getInstance().playingAudio = false;
			pauseCurrentSong();
		} else {
			ProxyService.getInstance().playingAudio = true;
			playCurrentSong();

		}
	}

	public void playCurrentSong() {
		if (syncPlayer == null) {

			try {
				// Added setDataSource() on 13/3/14 to play song
				Log.i("First Song", "" + (songsList.get(1).get("songPath")));
				String songTitle = songsList.get(1).get("songTitle");
				syncPlayer.setDataSource(songsList.get(1).get("songPath"));
				try {
					ProxyService.getProxyInstance().show("Track No- :" + 1,
							songTitle, TextAlignment.LEFT_ALIGNED,
							ProxyService.getInstance().nextCorrID());
				} catch (SyncException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				syncPlayer.prepare();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			syncPlayer.setLooping(true);
		}
		syncPlayer.start();
	}

	public void pauseCurrentSong() {
		if (syncPlayer != null && syncPlayer.isPlaying()) {
			syncPlayer.pause();
		}
	}

	public void playCurrentSong(int songIndex) {
		// Capturing the current song number
		// setCurrentPlayingSongIndex(songIndex);
		// Play song
		if (songsList.size() > -1) {
			try {
				syncPlayer.reset();
				syncPlayer.setDataSource(songsList.get(songIndex).get(
						"songPath"));
				syncPlayer.prepare();
				syncPlayer.start();
				// Displaying Song title
				String songTitle = songsList.get(songIndex).get("songTitle");
				ProxyService.getProxyInstance().show("Track No- :" + songIndex,
						songTitle, TextAlignment.LEFT_ALIGNED,
						ProxyService.getInstance().nextCorrID());
				// songTitleLabel.setText(songTitle);

				// Changing Button Image to pause image
				btnPlay.setImageResource(R.drawable.btn_pause);

				// set Progress bar values
				songProgressBar.setProgress(0);
				songProgressBar.setMax(100);

				// Updating progress bar
				// updateProgressBar();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();

			} catch (SyncException e) {

			}
		} else {
			AlertDialog alert = new AlertDialog.Builder(SyncMainActivity.this)
					.create();
			alert.setTitle("SD Card Alert");
			alert.setMessage("Your SD Card is Empty. Size of Song List - "
					+ songsList.size());
			alert.setIcon(R.drawable.ic_launcher);
			alert.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							Toast.makeText(getApplicationContext(),
									"Closing the Sync Player",
									Toast.LENGTH_SHORT).show();
							SyncMainActivity.this.finish();
							exitApp();

						}
					});
			alert.show();
		}
		setCurrentPlayingSongIndex(songIndex);
		// currentSongIndex = songIndex;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		// mHandler.removeCallbacks(mUpdateTimeTask);
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		// TODO Auto-generated method stub
		// check for repeat is ON or OFF
		if (isRepeat) {
			// repeat is on play same song again
			playCurrentSong(currentSongIndex);
		} else if (isShuffle) {
			// shuffle is on - play a random song
			Random rand = new Random();
			currentSongIndex = rand.nextInt((songsList.size() - 1) - 0 + 1) + 0;
			playCurrentSong(currentSongIndex);
		} else {
			// no repeat or shuffle ON - play next song
			if (currentSongIndex < (songsList.size() - 1)) {
				playCurrentSong(currentSongIndex + 1);
				currentSongIndex = currentSongIndex + 1;
			} else {
				// play first song
				playCurrentSong(0);
				currentSongIndex = 0;
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		syncPlayer.release();
		endSyncProxyInstance();
		_activity = null;
		ProxyService service = ProxyService.getInstance();
		if (service != null) {
			service.setCurrentActivity(null);
		}
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		totalDuration = 0;
		currentDuration = 0;
		if (ProxyService.getProxyInstance() != null) {
			try {
				// / ProxyService.getProxyInstance().dispose();
				Log.i("Sync Main Servce", " is Running" + isMyServiceRunning());
				if (isMyServiceRunning()) {
					Intent i = new Intent(SyncMainActivity.this,
							ProxyService.class);
					stopService(i);
				}
				// finish();

			} catch (Exception e1) {
				e1.printStackTrace();

			}
		}

		// mHandler.removeCallbacks(mUpdateTimeTask);

	}

	public static SyncMainActivity getInstance() {
		return _activity;
	}

	/** Displays the current protocol properties in the activity's title. */
	private void showPropertiesInTitle() {
		final SharedPreferences prefs = getSharedPreferences(Const.PREFS_NAME,
				0);
		boolean isMedia = prefs.getBoolean(Const.PREFS_KEY_ISMEDIAAPP,
				Const.PREFS_DEFAULT_ISMEDIAAPP);
		String transportType = prefs.getInt(
				Const.Transport.PREFS_KEY_TRANSPORT_TYPE,
				Const.Transport.PREFS_DEFAULT_TRANSPORT_TYPE) == Const.Transport.KEY_TCP ? "WiFi"
				: "BT";
		setTitle(getResources().getString(R.string.app_name) + " ("
				+ (isMedia ? "" : "non-") + "media, " + transportType + ")");
	}

	// upon onDestroy(), dispose current proxy and create a new one to enable
	// auto-start
	// call resetProxy() to do so
	public void endSyncProxyInstance() {
		ProxyService serviceInstance = ProxyService.getInstance();
		if (serviceInstance != null) {
			SyncProxyALM proxyInstance = serviceInstance.getProxyInstance();
			// if proxy exists, reset it
			if (proxyInstance != null) {
				if (proxyInstance.getCurrentTransportType() == TransportType.BLUETOOTH) {
					// serviceInstance.reset();
				} else {
					Log.e(logTag,
							"endSyncProxyInstance. No reset required if transport is TCP");
				}
				// if proxy == null create proxy
			} else {
				serviceInstance.startProxy();
			}
		}
	}

	/**
	 * Shows a dialog where the user can select connection features (protocol
	 * version, media flag, app name, language, HMI language, and transport
	 * settings). Starts the proxy after selecting.
	 */
	private void propertiesUI() {
		Context context = this;
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.properties,
				(ViewGroup) findViewById(R.id.properties_Root));

		final CheckBox mediaCheckBox = (CheckBox) view
				.findViewById(R.id.properties_checkMedia);
		final EditText appNameEditText = (EditText) view
				.findViewById(R.id.properties_appName);
		final RadioGroup transportGroup = (RadioGroup) view
				.findViewById(R.id.properties_radioGroupTransport);
		final EditText ipAddressEditText = (EditText) view
				.findViewById(R.id.properties_ipAddr);
		final EditText tcpPortEditText = (EditText) view
				.findViewById(R.id.properties_tcpPort);
		final CheckBox autoReconnectCheckBox = (CheckBox) view
				.findViewById(R.id.properties_checkAutoReconnect);

		ipAddressEditText.setEnabled(false);
		tcpPortEditText.setEnabled(false);
		autoReconnectCheckBox.setEnabled(false);

		transportGroup
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						boolean transportOptionsEnabled = checkedId == R.id.properties_radioWiFi;
						ipAddressEditText.setEnabled(transportOptionsEnabled);
						tcpPortEditText.setEnabled(transportOptionsEnabled);
						autoReconnectCheckBox
								.setEnabled(transportOptionsEnabled);
					}
				});

		// display current configs
		final SharedPreferences prefs = getSharedPreferences(Const.PREFS_NAME,
				0);
		boolean isMedia = prefs.getBoolean(Const.PREFS_KEY_ISMEDIAAPP,
				Const.PREFS_DEFAULT_ISMEDIAAPP);
		String appName = prefs.getString(Const.PREFS_KEY_APPNAME,
				Const.PREFS_DEFAULT_APPNAME);
		int transportType = prefs.getInt(
				Const.Transport.PREFS_KEY_TRANSPORT_TYPE,
				Const.Transport.PREFS_DEFAULT_TRANSPORT_TYPE);
		String ipAddress = prefs.getString(
				Const.Transport.PREFS_KEY_TRANSPORT_IP,
				Const.Transport.PREFS_DEFAULT_TRANSPORT_IP);
		int tcpPort = prefs.getInt(Const.Transport.PREFS_KEY_TRANSPORT_PORT,
				Const.Transport.PREFS_DEFAULT_TRANSPORT_PORT);
		boolean autoReconnect = prefs.getBoolean(
				Const.Transport.PREFS_KEY_TRANSPORT_RECONNECT,
				Const.Transport.PREFS_DEFAULT_TRANSPORT_RECONNECT_DEFAULT);

		mediaCheckBox.setChecked(isMedia);
		appNameEditText.setText(appName);
		transportGroup
				.check(transportType == Const.Transport.KEY_TCP ? R.id.properties_radioWiFi
						: R.id.properties_radioBT);
		ipAddressEditText.setText(ipAddress);
		tcpPortEditText.setText(String.valueOf(tcpPort));
		autoReconnectCheckBox.setChecked(autoReconnect);

		new AlertDialog.Builder(context).setTitle("Please select properties")
				.setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						String appName = appNameEditText.getText().toString();
						boolean isMedia = mediaCheckBox.isChecked();
						int transportType = transportGroup
								.getCheckedRadioButtonId() == R.id.properties_radioWiFi ? Const.Transport.KEY_TCP
								: Const.Transport.KEY_BLUETOOTH;
						String ipAddress = ipAddressEditText.getText()
								.toString();
						int tcpPort = Integer.parseInt(tcpPortEditText
								.getText().toString());
						boolean autoReconnect = autoReconnectCheckBox
								.isChecked();

						// save the configs
						boolean success = prefs
								.edit()
								.putBoolean(Const.PREFS_KEY_ISMEDIAAPP, isMedia)
								.putString(Const.PREFS_KEY_APPNAME, appName)
								.putInt(Const.Transport.PREFS_KEY_TRANSPORT_TYPE,
										transportType)
								.putString(
										Const.Transport.PREFS_KEY_TRANSPORT_IP,
										ipAddress)
								.putInt(Const.Transport.PREFS_KEY_TRANSPORT_PORT,
										tcpPort)
								.putBoolean(
										Const.Transport.PREFS_KEY_TRANSPORT_RECONNECT,
										autoReconnect).commit();
						if (!success) {
							Log.w(logTag, "Can't save properties");
						}

						showPropertiesInTitle();

						startSyncProxy();
					}
				}).setView(view).show();
	}

	/** Starts the sync proxy at startup after selecting protocol features. */
	// Bug could be here
	private void startSyncProxy() {
		// showPropertiesInTitle();
		boolean isSYNCpaired = false;
		// Get the local Bluetooth adapter
		BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		// BT Adapter exists, is enabled, and there are paired devices with the
		// name SYNC
		// Ideally start service and start proxy if already connected to sync
		// but, there is no way to tell if a device is currently connected (pre
		// OS 4.0)

		if (mBtAdapter != null) {
			if ((mBtAdapter.isEnabled() && mBtAdapter.getBondedDevices()
					.isEmpty() == false)) {
				// Get a set of currently paired devices
				Set<BluetoothDevice> pairedDevices = mBtAdapter
						.getBondedDevices();

				// Check if there is a paired device with the name "SYNC"
				if (pairedDevices.size() > 0) {
					for (BluetoothDevice device : pairedDevices) {
						if (device.getName().toString().contains("SYNC")) {
							isSYNCpaired = true;
							break;
						}
					}
				} else {
					Log.i("TAG", "A No Paired devices with the name sync");
				}

				if (isSYNCpaired == true) {
					if (ProxyService.getInstance() == null) {
						Intent startIntent = new Intent(this,
								ProxyService.class);
						startService(startIntent);
					} else {
						// if the service is already running and proxy is up,
						// set this as current UI activity
						ProxyService serviceInstance = ProxyService
								.getInstance();
						serviceInstance.setCurrentActivity(this);
						SyncProxyALM proxyInstance = ProxyService
								.getProxyInstance();
						if (proxyInstance != null) {
							serviceInstance.reset();
						} else {
							Log.i("TAG", "proxy is null");
							serviceInstance.startProxy();
						}
						Log.i("TAG", " proxyAlive == true success");
					}
				}
			}
		}

	}

	/** Called when a connection to a SYNC device has been closed. */
	public void onProxyClosed() {
		LockScreenActivity.getInstance().finish();
		syncPlayer.reset();
		SyncMainActivity.getInstance().finish();
		Log.i(logTag, "Disconnected");
	}

	public void lockAppsScreen() {

		Intent lockScreen = new Intent(_activity, LockScreenActivity.class);
		startActivity(lockScreen);

	}

	public boolean isActivityonTop() {
		return activityOnTop;
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (ProxyService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
