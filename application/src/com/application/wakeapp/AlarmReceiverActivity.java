package com.application.wakeapp;

import java.util.Locale;

import com.application.wakeapp.MainActivity.Background;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class AlarmReceiverActivity extends Activity{
	private MediaPlayer mPlayer;
	private Button mButton;
	private TextToSpeech tts;
	private String destination_message;
	private Vibrator vibrator;
	private AudioManager audioManager;
	private Background backgroundThread;
	private static final String LOG_TAG = "WakeApp";
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG,"AlarmReceiverActivity onCreate");
		setContentView(R.layout.activity_alarm);
		destination_message = getResources().getString(R.string.arrive_message);
		
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.xperia_z_alarm);
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mButton = (Button) findViewById(R.id.button1);
		mButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
            	Intent goToMainActivity = new Intent(getApplicationContext(),MainActivity.class);
            	
            	notifyUserDestinationReached();
            	
            	backgroundThread.cancel(true);
            	vibrator.cancel();
            	mPlayer.stop();
            	          	  
            	goToMainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            	startActivity(goToMainActivity);
            	
            	finish();
            }
        });
		
		backgroundThread = new Background();
		backgroundThread.execute();
		
		
	}
	private void playAlarm(int volume){
        int dot = 200;      // Length of a Morse Code "dot" in milliseconds
        int dash = 500;     // Length of a Morse Code "dash" in milliseconds
        int short_gap = 200;    // Length of Gap Between dots/dashes
        int medium_gap = 500;   // Length of Gap Between Letters
        int long_gap = 1000;    // Length of Gap Between Words
        long[] pattern = {
                0,  // Start immediately
                dot, short_gap, dot, short_gap, dot,    // s
                medium_gap,
                dash, short_gap, dash, short_gap, dash, // o
                medium_gap,
                dot, short_gap, dot, short_gap, dot,    // s
                long_gap
        };
        Log.d(LOG_TAG,"playAlarm volume: " + volume);
        //Turn off music
        Intent intent = new Intent("com.android.music.musicservicecommand.togglepause");
        getApplicationContext().sendBroadcast(intent);
        
		vibrator.vibrate(pattern,1);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,0);
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.start();		
	}
	
	private void notifyUserDestinationReached(){
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Log.d(LOG_TAG,"engine: " + tts.getDefaultEngine());

                    int result = tts.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(LOG_TAG,"This Language is not supported");
                    } else {
                    	if (audioManager.isWiredHeadsetOn()){
	                        Log.d(LOG_TAG,"music active");
	                        //Turn off music
	                        Intent intent = new Intent("com.android.music.musicservicecommand.togglepause");
	                        getApplicationContext().sendBroadcast(intent);
	
	                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,12,0);
	                        
	                        tts.setPitch(0.8f);                           
	                        result = tts.speak(destination_message,
	                                TextToSpeech.QUEUE_FLUSH, null);
	
	                        if (result == TextToSpeech.ERROR)
	                            Log.e(LOG_TAG,"speach failed");
                    	}

                    }
                } else {
                    Log.e(LOG_TAG,"Initilization Failed!");
                }
            }
        });

    }
    
    protected void onRestart(){
    	super.onRestart();
    	Log.d(LOG_TAG,"AlarmReceiverActivity onRestart");
    }

    protected void onResume(){
    	super.onResume();
    	Log.d(LOG_TAG,"AlarmReceiverActivity onResume");
    }

    protected void onPause(){
    	super.onPause();
    	Log.d(LOG_TAG,"AlarmReceiverActivity onPause");
    	
    }

    protected void onStop(){
    	super.onStop();
    	Log.d(LOG_TAG,"AlarmReceiverActivity onStop");
    }

    protected void onDestroy(){
    	super.onDestroy();
    	Log.d(LOG_TAG,"AlarmReceiverActivity onDestroy");
    	
    }
    class Background extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... arg0) {
			// TODO Auto-generated method stub
			int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			int duration = mPlayer.getDuration();
			Log.d(LOG_TAG,"doInBackground maxvol: " + maxVol + " duration: " + duration);
			
			// increase volume slowly
			for(int i=1;i<maxVol;){
				if (isCancelled()) return null;
				
				Log.d(LOG_TAG," loop");
				if (!mPlayer.isPlaying()){
					playAlarm(i);
					i+=5;
				}
				
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
			// Now put alarm sound on max volume and
			// play it for an long time.
			for (int i=0;i<1000;i++){
				if (isCancelled()) return null;
				
				if (!mPlayer.isPlaying())
					playAlarm(maxVol);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
			
			return null;
		}
    	
    }
   
}
