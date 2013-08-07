package com.application.wakeapp;

import java.util.HashMap;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

@SuppressLint("NewApi")
public class AlarmReceiverActivity extends Activity implements OnInitListener{
	private MediaPlayer mPlayer;
	private Button mButton;
	private TextToSpeech tts;
	private String destination_message;
	private Vibrator vibrator;
	private AudioManager audioManager;
	private Background backgroundThread;
	private SharedPreferences prefs;
	private Uri alarmURI;
	private static final String LOG_TAG = "WakeApp";
	private UtteranceProgressListener progressListener;
	private HashMap<String, String> params = new HashMap<String, String>();
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG,"AlarmReceiverActivity onCreate");
		setContentView(R.layout.activity_alarm);

        prefs =  PreferenceManager.getDefaultSharedPreferences(this);
        alarmURI = Uri.parse(prefs.getString("ringtone","default ringtone"));
        
        // If we do not have any default ringtones in preferencemanager
        // we have to add DEFAULT_RINGTONE otherwise we crash.
        if ( alarmURI.toString().equals("default ringtone"))
        	alarmURI = Settings.System.DEFAULT_RINGTONE_URI;

        final Animation animAccelerateDecelerate = AnimationUtils.loadAnimation(this, R.anim.animation);
        final ImageView image = (ImageView)findViewById(R.id.imageView1);
        image.startAnimation(animAccelerateDecelerate);
        
        destination_message = getResources().getString(R.string.arrive_message);
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		mPlayer = MediaPlayer.create(getApplicationContext(), alarmURI);
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mButton = (Button) findViewById(R.id.button1);
		tts = new TextToSpeech(getApplicationContext(), AlarmReceiverActivity.this);
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"1");
		progressListener = new UtteranceProgressListener() {

			@Override
			public void onDone(String utteranceId) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG,"Speech is done!!!!");
				returnToMainActicity();
			}

			@Override
			public void onError(String utteranceId) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStart(String utteranceId) {
				// TODO Auto-generated method stub
				
			}
			
		};
		mButton.setOnClickListener(new View.OnClickListener(){		
		
            @Override
            public void onClick(View view) {
            	
            	mButton.setEnabled(false);
            	notifyUserDestinationReached();
           	
            	backgroundThread.cancel(true);
            	vibrator.cancel();
            	mPlayer.stop();

            }
        });
		
		backgroundThread = new Background();
		backgroundThread.execute();
		
		
	}
	private void returnToMainActicity(){
		Intent goToMainActivity = new Intent(getApplicationContext(),MainActivity.class);
    	goToMainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	goToMainActivity.putExtra("AlarmActivity", "PingByAlarm");
    	startActivity(goToMainActivity);            	
    	finish();
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

        //Turn off music if it's playing
        if (audioManager.isMusicActive()) {
        	Intent intent = new Intent("com.android.music.musicservicecommand.pause");
        	getApplicationContext().sendBroadcast(intent);
        }
        
		vibrator.vibrate(pattern,1);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,0);
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setLooping(true);
		mPlayer.start();
	}
	private void notifyUserDestinationReached(){
		// If we have headset use use text-to-speech and read
		// the message. When message is done exit activity and
		// return to mainactivity. Otherwise just exit activit
		// and return to mainactivity.
       	if (audioManager.isWiredHeadsetOn()){
            Log.d(LOG_TAG,"music active");
            
            int result;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,12,0);
            
            tts.setPitch(0.8f);       
            result = tts.speak(destination_message,
                    TextToSpeech.QUEUE_FLUSH, params);

            if (result == TextToSpeech.ERROR)
                Log.e(LOG_TAG,"speach failed");
    	} else {
    		returnToMainActicity();
    	}
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
	if ( tts != null){
    	    tts.stop();
            tts.shutdown();
    	}
    	mPlayer.release();
    	super.onDestroy();
    	Log.d(LOG_TAG,"AlarmReceiverActivity onDestroy");
    	
    }
    class Background extends AsyncTask<String, Integer, String> {
    	
    	private void sleep(int msec){
			try {
				Thread.sleep(msec);
			} catch (InterruptedException e) {
				 Thread.currentThread().interrupt(); // restore interrupted status
			}		
    	}
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

				playAlarm(i);
				i+=5;

				sleep(duration);
				
			}
			// Now put alarm sound on max volume and
			// play it for an long time.
			for (int i=0;i<1000;i++){
				if (isCancelled()) return null;
								
				playAlarm(maxVol);
								
				sleep(duration);
				
			}
			
			return null;
		}
    	
    }
	@Override
	public void onInit(int status) {		
		if (status == TextToSpeech.SUCCESS) {
            Log.d(LOG_TAG,"engine: " + tts.getDefaultEngine());
            tts.setOnUtteranceProgressListener(progressListener);
            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(LOG_TAG,"This Language is not supported");
            }
		
		}
	}
   
}
