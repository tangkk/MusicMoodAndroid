package com.example.musicmood;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MusicMood extends ActionBarActivity {
    public final String LOG_TAG = "MusicMood";
    private static final String SAMPLE0 = Environment.getExternalStorageDirectory() + "/Music/MusicMood/haoting.mp3";
    private static final String SAMPLE1 = Environment.getExternalStorageDirectory() + "/Music/MusicMood/1984.mp3";
    private static final String SAMPLE2 = Environment.getExternalStorageDirectory() + "/Music/MusicMood/Here Comes The Sun.mp3";
    private static final String SAMPLE3 = Environment.getExternalStorageDirectory() + "/Music/MusicMood/Manhattan.mp3";
    private static final String SAMPLE4 = Environment.getExternalStorageDirectory() + "/Music/MusicMood/individuallytwisted.mp3";
    private static final String SAMPLE5 = Environment.getExternalStorageDirectory() + "/Music/MusicMood/farewellandromeda.mp3";
    private static final String SAMPLE6 = Environment.getExternalStorageDirectory() + "/Music/MusicMood/iloveyouso.mp3";
    private static final String SAMPLE7 = Environment.getExternalStorageDirectory() + "/Music/MusicMood/inloveagain.mp3";
    private static final String SAMPLE8 = Environment.getExternalStorageDirectory() + "/Music/MusicMood/Love Never Felt So Good.mp3";
    private static final String SAMPLE9 = Environment.getExternalStorageDirectory() + "/Music/MusicMood/Paris In Your Eyes.mp3";
    private static final String SAMPLE10 = Environment.getExternalStorageDirectory() + "/Music/MusicMood/Wanna Be Startin Somethin.mp3";
    private static final String[] SAMPLE = {SAMPLE0, SAMPLE1, SAMPLE2, SAMPLE3, SAMPLE4, SAMPLE5, SAMPLE6,
            SAMPLE7, SAMPLE8, SAMPLE9, SAMPLE10};
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_mood);
        FeatureExtraction fe = 	new FeatureExtraction(SAMPLE);
        try {
            fe.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void startAnalysis() {
    	ArrayList<String>filePaths = new ArrayList<String>(10);
    	String path = Environment.getExternalStorageDirectory()+"/Music/MusicMood";
    	Log.i("Files", "Path: " + path);
    	File f = new File(path);        
    	File file[] = f.listFiles();
    	Log.d("Files", "Size: "+ file.length);
    	for (int i=0; i < file.length; i++)
    	{
    	    String filename = file[i].getName();
    	    Log.i("Files", "FileName:" + path+"/"+filename);
    	    filePaths.add(path+"/"+filename);
    	}
    	Object[] mStringArray = filePaths.toArray();
    	String[] fileStringPath = new String[mStringArray.length];

    	for(int i = 0; i < mStringArray.length ; i++){
    	    Log.d("string is",(String)mStringArray[i]);
    	    fileStringPath[i] = (String)mStringArray[i];
    	}
    	FeatureExtraction fe = 	new FeatureExtraction(fileStringPath);
    	try {
            fe.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.music_mood, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
