package com.applink.syncmusicplayer;

import java.util.Timer;
import java.util.TimerTask;

import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.UiLifecycleHelper;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.FacebookDialog.PendingCall;
import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.SyncProxyALM;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class LockScreenActivity extends Activity {
	int itemcmdID = 0;
	int subMenuId = 0;
	private static LockScreenActivity instance = null;
	UiLifecycleHelper uiHelper;
	private static String APP_ID = "1401760796762744"; //App ID
	 private Facebook facebook;
	    private AsyncFacebookRunner mAsyncRunner;
	    String FILENAME = "AndroidSSO_data";
	    SharedPreferences mPrefs;
	    
	Session.StatusCallback statusCallback = new Session.StatusCallback() {
		
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			// TODO Auto-generated method stub
			onSessionStateChange(session, state, exception);
		}
	}; 
	
	public LockScreenActivity() {
		// TODO Auto-generated constructor stub
		Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
	}

	public static LockScreenActivity getInstance() {
		return instance;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		setContentView(R.layout.lockscreen);
		
		facebook = new Facebook(APP_ID);
        mAsyncRunner = new AsyncFacebookRunner(facebook);
		
		
		uiHelper = new UiLifecycleHelper(LockScreenActivity.this, statusCallback);
		uiHelper.onCreate(savedInstanceState);
		
		FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
        .setLink("https://developers.facebook.com/android")
        
        .build();
           uiHelper.trackPendingDialogCall(shareDialog.present());

		final Button resetSYNCButton = (Button) findViewById(R.id.lockreset);
		resetSYNCButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// if not already started, show main activity and end lock
				// screen activity
				if (SyncMainActivity.getInstance() == null) {
					Intent i = new Intent(getBaseContext(),
							SyncMainActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getApplication().startActivity(i);
				}

				// reset proxy; do not shut down service
				ProxyService serviceInstance = ProxyService.getInstance();
				if (serviceInstance != null) {
					SyncProxyALM proxyInstance = ProxyService
							.getProxyInstance();
					if (proxyInstance != null) {
						serviceInstance.reset();
					} else {
						serviceInstance.startProxy();
					}
				}

				exit();
			}
		});
		
		final Button shareContent = (Button) findViewById(R.id.share);
		shareContent.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				publishContents();
			}
		});
	}

	// disable back button on lockscreen
	@Override
	public void onBackPressed() {
	}

	public void exit() {
//		SyncMainActivity.getInstance().finish();
//		if (isMyServiceRunning()) {
//			Intent i = new Intent(LockScreenActivity.this, ProxyService.class);
//			stopService(i);
//		}
		if(SyncMainActivity.getInstance()==null){
			exitApp();
		}
		
		super.finish();
	}

	public void onDestroy() {
		super.onDestroy();
		instance = null;
		uiHelper.onDestroy();
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(resultCode, resultCode, data, new FacebookDialog.Callback() {
			
			@Override
			public void onError(PendingCall pendingCall, Exception error, Bundle data) {
				// TODO Auto-generated method stub
				 Log.e("Activity", String.format("Error: %s", error.toString()));
			}
			
			@Override
			public void onComplete(PendingCall pendingCall, Bundle data) {
				// TODO Auto-generated method stub
				Log.i("Activity", "Success!");
			}
		});
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    uiHelper.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    uiHelper.onSaveInstanceState(outState);
	}

	@Override
	public void onPause() {
	    super.onPause();
	    uiHelper.onPause();
	}

	@SuppressWarnings("deprecation")
	private void onSessionStateChange(Session session, SessionState state, Exception exception){
		if (session != null && session.isOpened()) {
    		Log.d("DEBUG", "facebook session is open ");
    		// make request to the /me API
            Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
                // callback after Graph API response with user object
                @Override
                public void onCompleted(GraphUser user, Response response) {
                	if (user != null) {
                		Log.d("DEBUG", "email: " + user.asMap().get("email").toString());
                	}
                }

            });
    	}
	}
	
	
	
	@SuppressWarnings("deprecation")
	private void publishContents(){
		mPrefs = getPreferences(MODE_PRIVATE);
		String access_token = mPrefs.getString("access_token", null);
		long expires = mPrefs.getLong("access_expires", 0);
		if (access_token != null) {
	        facebook.setAccessToken(access_token);
	    }
	 
	    if (expires != 0) {
	        facebook.setAccessExpires(expires);
	    }
	    if(!facebook.isSessionValid()){
	    	facebook.authorize(LockScreenActivity.this, new String[] {"email", "Publish Stream"}, new DialogListener() {
				
				@Override
				public void onFacebookError(FacebookError e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onError(DialogError e) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onComplete(Bundle values) {
					// TODO Auto-generated method stub
					// Function to handle complete event
                    // Edit Preferences and update facebook acess_token
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString("access_token",
                            facebook.getAccessToken());
                    editor.putLong("access_expires",
                            facebook.getAccessExpires());
                    editor.commit();
				}
				
				@Override
				public void onCancel() {
					// TODO Auto-generated method stub
					
				}
			});
	    }
	}
}
