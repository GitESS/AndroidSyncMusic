package com.applink.syncmusicplayer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class SongsManager {
	final String MEDIA_PATH = Environment.getExternalStorageDirectory()
	        .getPath() + "/";
	private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
	private String mp3Pattern = ".mp3";
	private String wavPattern = ".wav";

	// Constructor
	public SongsManager() {

	}

	/**
	 * Function to read all mp3 files and store the details in
	 * ArrayList
	 * */
	public ArrayList<HashMap<String, String>> getPlayList() {
	    //System.out.println(MEDIA_PATH);
	    if (MEDIA_PATH != null) {
	        File home = new File(MEDIA_PATH);
	        File[] listFiles = home.listFiles();
	        if (listFiles != null && listFiles.length > 0) {
	            for (File file : listFiles) {
	                //System.out.println(file.getAbsolutePath());
	                if (file.isDirectory()) {
	                    scanDirectory(file);
	                } else {
	                    addSongToList(file);
	                }
	            }
	        }
	    }
	    // return songs list array
	    return songsList;
	}

	private void scanDirectory(File directory) {
	    if (directory != null) {
	        File[] listFiles = directory.listFiles();
	        if (listFiles != null && listFiles.length > 0) {
	            for (File file : listFiles) {
	                if (file.isDirectory()) {
	                    scanDirectory(file);
	                } else {
	                    addSongToList(file);
	                }
	                
	            }
	        }
	    }
	}

	private void addSongToList(File song) {
	    if (song.getName().endsWith(mp3Pattern) || song.getName().endsWith(wavPattern)) {
	        HashMap<String, String> songMap = new HashMap<String, String>();
	        songMap.put("songTitle", song.getName().substring(0, (song.getName().length() - 4)));
	        songMap.put("songPath", song.getPath());

	        //Adding each song to SongList
	        songsList.add(songMap);
	    }
	}


}
