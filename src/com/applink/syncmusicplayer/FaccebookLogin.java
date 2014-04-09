package com.applink.syncmusicplayer;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.android.Facebook;
import com.facebook.model.GraphUser;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class FaccebookLogin extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sync_main);
			
		Session.openActiveSession(FaccebookLogin.this, false, new Session.StatusCallback() {
			
			
			@Override
			public void call(Session session, SessionState state, Exception exception) {
				// TODO Auto-generated method stub
				if(session.isOpened()){
					Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
						
						@Override
						public void onCompleted(GraphUser user, Response response) {
							// TODO Auto-generated method stub
							if(user !=null){
								TextView welcome = (TextView) findViewById(R.id.welcome);
				                welcome.setText("Hello " + user.getName() + "!");
				                Log.i("Login", "Hello "+ user.getName());
				                Toast.makeText(getApplicationContext(), "Hello: "+ user.getName(), Toast.LENGTH_SHORT).show();
							}
						}
					});
				}
			}
		});
	}

}
