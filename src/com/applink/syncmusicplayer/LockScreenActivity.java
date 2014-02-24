package com.applink.syncmusicplayer;

import java.util.Timer;
import java.util.TimerTask;

import com.ford.syncV4.proxy.SyncProxyALM;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class LockScreenActivity extends Activity {
	/*private static SyncMainActivity _mainInstance;
	ImageView image;
	Button unlock;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lockscreen);
		addLockScreen();
	}

	public void addLockScreen(){
		image = (ImageView) findViewById(R.id.lockImageView1);
		 
		unlock = (Button) findViewById(R.id.btnUnlockScreen);
		unlock.setOnClickListener(new OnClickListener() {
 
			@Override
			public void onClick(View arg0) {
				finish();
	
			}
 
		});
	}
	
	public void setCurrentActivity(SyncMainActivity currentActivity) {
		if (this._mainInstance != null) {
			this._mainInstance.finish();
			this._mainInstance = null;
		}
		
		this._mainInstance = currentActivity;
		
	}*/
	int itemcmdID = 0;
	int subMenuId = 0;
	private static LockScreenActivity instance = null;
	
	public static LockScreenActivity getInstance() {
		return instance;
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.lockscreen);
		
		final Button resetSYNCButton = (Button)findViewById(R.id.lockreset);
		resetSYNCButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//if not already started, show main activity and end lock screen activity
				if(SyncMainActivity.getInstance() == null) {
					Intent i = new Intent(getBaseContext(), SyncMainActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getApplication().startActivity(i);
				}
				
				//reset proxy; do not shut down service
				ProxyService serviceInstance = ProxyService.getInstance();
				if (serviceInstance != null){
					SyncProxyALM proxyInstance = serviceInstance.getProxyInstance();
					if(proxyInstance != null){
						serviceInstance.reset();
					} else {
						serviceInstance.startProxy();
					}
				}
				
				exit();
			}
		});
    }
    
    //disable back button on lockscreen
    @Override
    public void onBackPressed() {
    }
    
    public void exit() {
    	super.finish();
    }
    
    public void onDestroy(){
    	super.onDestroy();
    	instance = null;
    }
}
