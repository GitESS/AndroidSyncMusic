package com.applink.syncmusicplayer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.model.GraphUser;
import com.ford.syncV4.proxy.SyncProxyALM;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class LockScreenActivity extends Activity {
	int itemcmdID = 0;
	int subMenuId = 0;
	private static LockScreenActivity instance = null;
	private static String APP_ID = "1401760796762744"; //App ID
	private Facebook facebook;
	private AsyncFacebookRunner mSyncRunner;
	private SharedPreferences mPrefs;
	

	public static LockScreenActivity getInstance() {
		return instance;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		setContentView(R.layout.lockscreen);

		
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
				LockScreenActivity.getInstance().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					SyncMainActivity.getInstance().onClickPostStatusUpdate();
				}
			});
			}
		});
	}

	// disable back button on lockscreen
	@Override
	public void onBackPressed() {
	}

	public void exit() {
		SyncMainActivity.getInstance().finish();
		if (isMyServiceRunning()) {
			Intent i = new Intent(LockScreenActivity.this, ProxyService.class);
			stopService(i);
		}
		if(SyncMainActivity.getInstance()==null){
			exitApp();
		}
		
		super.finish();
	}

	public void onDestroy() {
		super.onDestroy();
		instance = null;
		
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


}
