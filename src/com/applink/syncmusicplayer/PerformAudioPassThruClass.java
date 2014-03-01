package com.applink.syncmusicplayer;

import java.util.Vector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.PerformAudioPassThru;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.enums.AudioType;
import com.ford.syncV4.proxy.rpc.enums.BitsPerSample;
import com.ford.syncV4.proxy.rpc.enums.SamplingRate;

public class PerformAudioPassThruClass {
	
	private Context mContext = null;
	LayoutInflater inflater = null;
	
	public PerformAudioPassThruClass() {
		// TODO Auto-generated constructor stub
	}

	PerformAudioPassThruClass(Context ctx) {
		mContext = ctx;
		inflater = (LayoutInflater) mContext
				.getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE);
	}

	public static PerformAudioPassThruClass getInstance(Context ctx) {

		return new PerformAudioPassThruClass(ctx);
	}

	
	public void show() {
				
				Vector<TTSChunk> initChunks = TTSChunkFactory
						.createSimpleTTSChunks("Initial prompt");
				try {
					PerformAudioPassThru msg = new PerformAudioPassThru();
					msg.setInitialPrompt(initChunks);
					msg.setAudioPassThruDisplayText1("Display text1");
					msg.setAudioPassThruDisplayText2("Display text2");
					//msg.setSamplingRate(samplingRate)
					msg.setSamplingRate(SamplingRate._8KHZ);
					msg.setMaxDuration(Integer.parseInt("10000"));
					msg.setBitsPerSample(BitsPerSample._8_BIT);
					msg.setAudioType(AudioType.PCM);
					msg.setCorrelationID(ProxyService.getInstance().nextCorrID());
					msg.setMuteAudio(false);
					RecordingAudio.getInstance().latestPerformAudioPassThruMsg = msg;
					RecordingAudio.getInstance().mySampleRate = 8000;
					RecordingAudio.getInstance().myBitsPerSample = 8;
				

					Log.i("PerformAudioPClass", msg.toString());
					
					ProxyService.getInstance().getProxyInstance().sendRPCRequest(msg);
				}catch (SyncException e) {
				}
	}
}
	


