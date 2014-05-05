package com.applink.syncmusicplayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AppEventsLogger;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.LoginButton;
import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.transport.TransportType;

public class SyncMainActivity extends Activity implements OnCompletionListener,
		SeekBar.OnSeekBarChangeListener, OnPreparedListener, OnBufferingUpdateListener, OnErrorListener {
	private ImageButton btnPlay;
	private ImageButton btnForward;
	private ImageButton btnBackward;
	private ImageButton btnNext;
	private ImageButton btnPrevious;
	private ImageButton btnPlaylist, btnAudioStream, btnStreamStop;
	private ImageButton btnRepeat;
	private ImageButton btnShuffle;
	private SeekBar songProgressBar;
	private TextView songTitleLabel;
	private TextView songCurrentDurationLabel;
	private TextView songTotalDurationLabel;
	// Media Player
	public MediaPlayer syncPlayer, mp;
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
	public static final String logTag = "SyncMusicPlayer";
	private static SyncMainActivity _activity;
	Session.StatusCallback mCallback;
	private LoginButton loginButton;
	private GraphUser muser;
	// Facebook mFacebook;
	private UiLifecycleHelper uiHelper;
	private static final List<String> PERMISSIONS = Arrays
			.asList("publish_actions");

	private final String PENDING_ACTION_BUNDLE_KEY = "com.example.samplefacebookproject:PendingAction";

	public String test_Status;// =
								// "This is a Test Status, Please dont waste your time reading this!!. Thank you";

	public String getTest_Status() {
		return test_Status;
	}

	public void setTest_Status(String test_Status) {
		this.test_Status = test_Status;
	}

	private boolean canPresentShareDialog = false;
	private PendingAction pendingAction = PendingAction.NONE;
	private static final String PERMISSION = "publish_actions";

	private enum PendingAction {
		NONE, POST_PHOTO, POST_STATUS_UPDATE
	}

	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	public int getCurrentPlayingSongIndex() {
		return currentPlayingSongIndex;
	}

	public void setCurrentPlayingSongIndex(int currentPlayingSongIndex) {
		this.currentPlayingSongIndex = currentPlayingSongIndex;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);
		startSyncProxy();
		_activity = this;

		// Set all player buttons
		setAllMusicPlayerButtons();
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
		btnAudioStream = (ImageButton) findViewById(R.id.img_audio_stream);
		btnStreamStop = (ImageButton) findViewById(R.id.img_stop_stream);
		btnRepeat = (ImageButton) findViewById(R.id.btnRepeat);
		btnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
		songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
		songTitleLabel = (TextView) findViewById(R.id.songTitle);
		songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
		songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);

		exit = (Button) findViewById(R.id.button_exit);
		loginButton = (LoginButton) findViewById(R.id.login_button);
		
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
				
				pauseLiveStream();
//				if (isRepeat) {
//					isRepeat = false;
//					Toast.makeText(getApplicationContext(), "Repeat is OFF",
//							Toast.LENGTH_SHORT).show();
//					btnRepeat.setImageResource(R.drawable.btn_repeat);
//				} else {
//					// make repeat to true
//					isRepeat = true;
//					Toast.makeText(getApplicationContext(), "Repeat is ON",
//							Toast.LENGTH_SHORT).show();
//					// make shuffle to false
//					isShuffle = false;
//					btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
//					btnShuffle.setImageResource(R.drawable.btn_shuffle);
//				}
//				Toast.makeText(_activity, "Option is Disabled for now!",
//						Toast.LENGTH_SHORT).show();
			}
		});

		/**
		 * Button Click event for Shuffle button Enables shuffle flag to true
		 * */
		btnShuffle.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// shareOnWall();
				
				
			}
		});
		
		btnStreamStop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				stopLiveStream();
			}
		});
		
		btnAudioStream.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				playLiveStream();
			}
		});

		btnPlaylist.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// loginOnFB();
				callPlayList();
			}
		});

		loginButton.setPublishPermissions(Arrays.asList("basic_info",
				"publish_actions"));
		
		loginButton
				.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
					@Override
					public void onUserInfoFetched(GraphUser user) {

						muser = user;
						updateUI();
						// It's possible that we were waiting for this.user to
						// be populated in order to post a
						// status update.
						handlePendingAction();
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
		// Log.i("Backward-currentPos", "" + currentPosition);
		// check if seekBackward time is greater than 0 sec
		if (currentPosition - seekBackwardTime >= 0) {
			// Log.i("If-Backward ", "Control");
			// Backward song

			syncPlayer.seekTo(currentPosition - seekBackwardTime);
			String songTitle = songsList.get(getCurrentPlayingSongIndex()).get(
					"songTitle");
			// Log.i("backward-currentPos", "" + currentPosition + "index " +
			// getCurrentPlayingSongIndex() + " name" + songTitle);

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
			// Log.i("else-Backward ", "Control");
			// backward to starting position
			syncPlayer.seekTo(0);
		}
	}

	public void seekForwardCurrentPlayingSong() {

		// get current song position
		int currentPosition = syncPlayer.getCurrentPosition();
		// check if seekForward time is lesser than song duration
		if (currentPosition + seekForwardTime <= syncPlayer.getDuration()) {
			// Log.i("If-Forward ", "Control");
			// forward song

			syncPlayer.seekTo(currentPosition + seekForwardTime);
			String songTitle = songsList.get(getCurrentPlayingSongIndex()).get(
					"songTitle");
			// Log.i("Forwardward-currentPos", "" + currentPosition + "index " +
			// getCurrentPlayingSongIndex() + " name" + songTitle);

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
			// Log.i("Else-Forward ", "Control");
			syncPlayer.seekTo(syncPlayer.getDuration());
		}
	}

	public void playPauseCurrentPlayingSong() {
		if (syncPlayer != null && syncPlayer.isPlaying()) {
			ProxyService.getInstance().playingAudio = false;
			pauseCurrentSong();
		} else {
			try{
			ProxyService.getInstance().playingAudio = true;
			playCurrentSong();
			}catch(Exception e){
				
			}
		}
	}

	public void playCurrentSong() {
		if (mp != null && mp.isPlaying()) {
			Log.i("SoundCloud", "" + "Stopping");
			mp.stop();
			mp.reset();
			///syncPlayer = null;
		}
		
		if (syncPlayer == null) {

			try {
				String songTitle = songsList.get(1).get("songTitle");
				this.setTest_Status(songTitle);
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
				_activity.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						btnPlay.setImageResource(R.drawable.btn_pause);
					}
				});
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			syncPlayer.setLooping(true);
		}
		// Log.i("Music Player Start", "After perform Interaction");
		syncPlayer.start();
	}
	
	public void playCurrentSong4Cloud(String mStreamURL) {
		if (syncPlayer != null && syncPlayer.isPlaying()) {
			Log.i("SoundCloud", "" + "Stopping");
			syncPlayer.stop();
			syncPlayer.reset();
			///syncPlayer = null;
		}
		Log.i("SoundCloud", "" + "Reset");
		//if(syncPlayer == null){

		  try {
			  mp.setDataSource(this, Uri.parse(mStreamURL));
			  mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			  mp.prepare(); // don't use prepareAsync for mp3 playback
			  
			  ProxyService.getProxyInstance().show("Tracks", "4m Clouds", TextAlignment.CENTERED, ProxyService.getInstance().nextCorrID());
		  } catch (IOException e) {
		   // TODO Auto-generated catch block
		   e.printStackTrace();
		  } catch (SyncException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		  syncPlayer.start();
		
		Log.i("SoundCloud", "EOC");
		//}
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
		if (mp != null && mp.isPlaying()) {
			Log.i("Internet radio", "" + "Stopping");
			mp.stop();
			mp.reset();
		}
		
		if (songsList.size() > -1) {
			try {
				syncPlayer.reset();
				syncPlayer.setDataSource(songsList.get(songIndex).get(
						"songPath"));
				syncPlayer.prepare();
				syncPlayer.start();
				// Displaying Song title
				String songTitle = songsList.get(songIndex).get("songTitle");
				this.setTest_Status("Song Title : " + songTitle);
				ProxyService.getProxyInstance().show("Track No- :" + songIndex,
						songTitle, TextAlignment.LEFT_ALIGNED,
						ProxyService.getInstance().nextCorrID());
				
				_activity.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						btnPlay.setImageResource(R.drawable.btn_pause);
					}
				});
				songProgressBar.setProgress(0);
				songProgressBar.setMax(100);
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
		uiHelper.onDestroy();
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
					// Log.e(logTag,
					// "endSyncProxyInstance. No reset required if transport is TCP");
				}
				// if proxy == null create proxy
			} else {
				serviceInstance.startProxy();
			}
		}
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
					// Log.i("TAG", "A No Paired devices with the name sync");
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
							// Log.i("TAG", "proxy is null");
							serviceInstance.startProxy();
						}
						// Log.i("TAG", " proxyAlive == true success");
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
		// Log.i(logTag, "Disconnected");
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


	/**
	 * 
	 * This function performs Posting status without opening share dialog.
	 **/
	public void onClickPostStatusUpdate() {
		performPublish(PendingAction.POST_STATUS_UPDATE, canPresentShareDialog);
	}

	/**
	 * This Method performPublish Checks for the permission and if not available
	 * then asks for the same
	 * 
	 **/
	private void performPublish(PendingAction action, boolean allowNoSession) {
		Session session = Session.getActiveSession();

		if (session != null) {
			pendingAction = action;
			if (hasPublishPermission()) {
				// We can do the action right away.
				handlePendingAction();
				return;
			} else if (session.isOpened()) {
				// We need to get new permissions, then complete the action when
				// we get called back.
				session.requestNewPublishPermissions(new Session.NewPermissionsRequest(
						this, PERMISSION));
				return;
			}
		}
		if (allowNoSession) {
			pendingAction = action;
			handlePendingAction();
		}

	}

	/**
	 * Handles Action for the sake of posting :D *
	 * 
	 * */

	@SuppressWarnings("incomplete-switch")
	private void handlePendingAction() {
		PendingAction previouslyPendingAction = pendingAction;
		// These actions may re-set pendingAction if they are still pending, but
		// we assume they
		// will succeed.
		pendingAction = PendingAction.NONE;

		switch (previouslyPendingAction) {
		case POST_PHOTO:
			// postPhoto();
			break;
		case POST_STATUS_UPDATE:
			postStatusUpdate();
			break;
		}
	}

	/**
	 * Checks it has permission or not ,
	 * */
	private boolean hasPublishPermission() {
		Session session = Session.getActiveSession();
		return session != null
				&& session.getPermissions().contains("publish_actions");
	}

	/*
	 * Posts the status
	 */
	private void postStatusUpdate() {
		if (canPresentShareDialog) {
			FacebookDialog shareDialog = createShareDialogBuilderForLink()
					.build();
			uiHelper.trackPendingDialogCall(shareDialog.present());
		} else if (muser != null && hasPublishPermission()) {
			final String message =this
					.getTest_Status();
			Bundle params = new Bundle();
			params.putString("name", "Ford Sync Music Player");
			params.putString("caption", "SYNC Owners: Stay current.");
			params.putString("description", message);
			params.putString("link", "http://www.ford.com/");
			params.putString(
					"picture",
					"http://www.brandsoftheworld.com/sites/default/files/styles/logo-thumbnail/public/0008/5841/brand.gif");
			new Request(Session.getActiveSession(),
					"/me/feed", params, HttpMethod.POST,
					new Request.Callback() {
						public void onCompleted(Response response) {
							/* handle the result */
						}
					}).executeAsync();

			// request.executeAsync();
		} else {
			pendingAction = PendingAction.POST_STATUS_UPDATE;
		}
	}

	private FacebookDialog.ShareDialogBuilder createShareDialogBuilderForLink() {
		return new FacebookDialog.ShareDialogBuilder(this)
				.setName("Hello Facebook")
				.setDescription(
						"The 'Hello Facebook' sample application showcases simple Facebook integration")
				.setLink("http://developers.facebook.com/android");
	}

	private void onSessionStateChange(Session session, SessionState state,
			Exception exception) {
		if (pendingAction != PendingAction.NONE
				&& (exception instanceof FacebookOperationCanceledException || exception instanceof FacebookAuthorizationException)) {
			new AlertDialog.Builder(SyncMainActivity.this).setTitle("Hemant")
					.setMessage("Permission Granted")
					.setPositiveButton("OK", null).show();
			pendingAction = PendingAction.NONE;
		} else if (state == SessionState.OPENED_TOKEN_UPDATED) {
			handlePendingAction();
		}
		updateUI();
	}

	private FacebookDialog.Callback dialogCallback = new FacebookDialog.Callback() {
		@Override
		public void onError(FacebookDialog.PendingCall pendingCall,
				Exception error, Bundle data) {
			}

		@Override
		public void onComplete(FacebookDialog.PendingCall pendingCall,
				Bundle data) {
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		uiHelper.onResume();

		// Call the 'activateApp' method to log an app event for use in
		// analytics and advertising reporting. Do so in
		// the onResume methods of the primary Activities that an app may be
		// launched into.
		AppEventsLogger.activateApp(this);

		updateUI();
	}

	private void updateUI() {
		Session.getActiveSession();
		if (muser != null) {
			// welcome.setText("Hello " + muser.getName() + "!");
			Toast.makeText(getApplicationContext(),
					"Hello " + muser.getName() + "!", Toast.LENGTH_SHORT)
					.show();

		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);

		outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data, dialogCallback);
		if(resultCode == 100){
            currentSongIndex = data.getExtras().getInt("songIndex");
            // play selected song
            playCurrentSong(currentSongIndex);
       }
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	public void playLiveStream(){
		if (syncPlayer != null && syncPlayer.isPlaying()) {
			Log.i("SoundCloud", "" + "Stopping");
			syncPlayer.stop();
			syncPlayer.reset();
			///syncPlayer = null;
		}
		
		
		Uri myUri = Uri.parse("http://fr3.ah.fm:9000/");
		try {
			if (mp == null) {
				this.mp = new MediaPlayer();
			} else {
				mp.stop();
				mp.reset();
			}
			mp.setDataSource(this, myUri); // Go to Initialized state
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mp.setOnPreparedListener(this);
			mp.setOnBufferingUpdateListener(this);

			mp.setOnErrorListener(this);
			mp.prepareAsync();
			
			ProxyService.getProxyInstance().show("Afterhours", "Internet Radio", TextAlignment.CENTERED, ProxyService.getInstance().nextCorrID());

			Log.d("SyncMusicPlayer", "LoadClip Done");
		} catch (Throwable t) {
			Log.d("SyncMusicPlayer", t.toString());
		}
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder();
		sb.append("Media Player Error: ");
		switch (what) {
		case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			sb.append("Not Valid for Progressive Playback");
			break;
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			sb.append("Server Died");
			break;
		case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			sb.append("Unknown");
			break;
		default:
			sb.append(" Non standard (");
			sb.append(what);
			sb.append(")");
		}
		sb.append(" (" + what + ") ");
		sb.append(extra);
		Log.e("SyncMusicPlayer", sb.toString());
		return true;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		Log.d("SyncMusicPlayer", "PlayerService onBufferingUpdate : " + percent + "%");
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// TODO Auto-generated method stub
		Log.d("SyncMusicPlayer", "Stream is prepared");
		mp.start();
	}
	
	public void stopLiveStream(){
		
		if(mp != null && mp.isPlaying()){
			try{
			mp.stop();
			mp.release();
			}catch(Exception e){
				
			}
		} else{
		Toast.makeText(_activity, "No Streaming", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void pauseLiveStream(){
		mp.pause();
	}
	
	public void callPlayList(){
		Intent i = new Intent(getApplicationContext(), PlayListActivity.class);
        startActivityForResult(i, 100);
	}
}
