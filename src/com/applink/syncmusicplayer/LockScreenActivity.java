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
		
		facebook = new Facebook(APP_ID);
		mSyncRunner = new AsyncFacebookRunner(facebook);

//		try {
//			PackageInfo info = getPackageManager().getPackageInfo("com.facebook.samples.hellofacebook", PackageManager.GET_SIGNATURES);
//			for (Signature signature : info.signatures) {
//	            MessageDigest md = MessageDigest.getInstance("SHA");
//	            md.update(signature.toByteArray());
//	            Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
//	            }
//		} catch (NameNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}catch (NoSuchAlgorithmException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
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
				//publishContents();
				shareOnFB();
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


	public void shareOnFB(){
		mPrefs = getPreferences(MODE_PRIVATE);
	    String access_token = mPrefs.getString("access_token", null);
	    long expires = mPrefs.getLong("access_expires", 0);
	    if(access_token != null){
	    	facebook.setAccessToken(access_token);
	    }
	    if (expires != 0) {
	        facebook.setAccessExpires(expires);
	    }
	    
	    if(!facebook.isSessionValid()){
	    	facebook.authorize(LockScreenActivity.this, new String[] { "email", "publish_stream" }, new DialogListener() {
				
				public void onFacebookError(FacebookError e) {
					// TODO Auto-generated method stub
					
				}
				
				public void onError(DialogError e) {
					// TODO Auto-generated method stub
					
				}
				
				public void onComplete(Bundle values) {
					// TODO Auto-generated method stub
					SharedPreferences.Editor  editor = mPrefs.edit();
					editor.putString("access_token",
                            facebook.getAccessToken());
                    editor.putLong("access_expires",
                            facebook.getAccessExpires());
                    editor.commit();
                    
                    postToWall();
                    
				}
				
				public void onCancel() {
					// TODO Auto-generated method stub
					
				}
			});
	    }
	    
	}
	
	public void postToWall(){
		facebook.dialog(this, "feed", new DialogListener() {
			 
	        @Override
	        public void onFacebookError(FacebookError e) {
	        }
	 
	        @Override
	        public void onError(DialogError e) {
	        }
	 
	        @Override
	        public void onComplete(Bundle values) {
	        }
	 
	        @Override
	        public void onCancel() {
	        }
	    });
	}
	
//	public void publishContents(){
//		Session.openActiveSession(LockScreenActivity.this, true, new Session.StatusCallback() {
//			
//			@SuppressWarnings("deprecation")
//			@Override
//			public void call(Session session, SessionState state, Exception exception) {
//				// TODO Auto-generated method stub
//				if(session.isOpened()){
//					Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
//						
//						@Override
//						public void onCompleted(GraphUser user, Response response) {
//							// TODO Auto-generated method stub
//							if(user !=null){
//								TextView welcome = (TextView) findViewById(R.id.welcome);
//				                welcome.setText("Hello " + user.getName() + "!");
//							}
//						}
//					});
//				}
//			}
//		});
//	}
//	
//	 @Override
//	  public void onActivityResult(int requestCode, int resultCode, Intent data) {
//	      super.onActivityResult(requestCode, resultCode, data);
//	      Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
//	  }
}
