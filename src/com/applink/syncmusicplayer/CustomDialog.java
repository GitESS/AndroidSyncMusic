package com.applink.syncmusicplayer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomDialog {

	Context context;

	public void getCustomDialogBox(final Activity screen, Context context,
			String title, String msg) {
		// Create custom dialog object

		final Dialog dialog = new Dialog(context);
		// Include dialog.xml file
		dialog.setContentView(R.layout.dialog);
		// Set dialog title
		dialog.setTitle(title);
		// dialog.set
		// set values for custom dialog components - text, image and button
		TextView text = (TextView) dialog.findViewById(R.id.textDialog);
		text.setText(msg);
		// ImageView image = (ImageView) dialog.findViewById(R.id.imageDialog);
		// image.setImageResource(R.drawable.app_icon);

		dialog.show();

		Button declineButton = (Button) dialog.findViewById(R.id.declineButton);
		// if decline button is clicked, close the custom dialog
		declineButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				dialog.dismiss();
			}
		});

	}

	

	public void getCustomDialogWithFinish(final Activity screen, String title,
			String msg) {
		final Dialog dialog = new Dialog(screen);
		dialog.setContentView(R.layout.dialog);
		dialog.setTitle(title);
		TextView text = (TextView) dialog.findViewById(R.id.textDialog);
		text.setText(msg);
		dialog.show();

		Button declineButton = (Button) dialog.findViewById(R.id.declineButton);
		// if decline button is clicked, close the custom dialog
		declineButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				screen.finish();
				dialog.dismiss();
			}
		});

	}

}
