package com.application.wakeapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

/**
 * Created by Radovan Lekanovic on 2013-07-07.
 */
public class BackgroundService extends Service {
    private int setradius;

    private Float currentSpeed;
    private LocationListener mLocationListener;
    private NotificationManager mNotificationManager;
    private Location finalDestination;
    private LocationManager locationManager;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
    private TextToSpeech tts;
    private SharedPreferences prefs;
    private static final String LOG_TAG = "WakeApp";
    private Boolean hasRestartedGPS;
    private String destination_message;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG,"Service: onStartCommand");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification not = new Notification(R.drawable.ic_launcher, "WakeApp", System.currentTimeMillis());
        PendingIntent contentIntent =
                PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                        Notification.FLAG_ONGOING_EVENT);

        not.flags = Notification.FLAG_ONGOING_EVENT;
        not.setLatestEventInfo(this, "WakeApp", "Running in background", contentIntent);
        mNotificationManager.notify(1, not);

        prefs =  PreferenceManager.getDefaultSharedPreferences(this);
        setradius = Integer.parseInt(prefs.getString("setradius","600"));

        finalDestination = new Location("Destination");
        finalDestination.setLongitude(intent.getExtras().getDouble("lng"));
        finalDestination.setLatitude(intent.getExtras().getDouble("lat"));
        
        hasRestartedGPS=Boolean.FALSE;
        
        destination_message = getResources().getString(R.string.arrive_message);
        
        mLocationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                int distance;
                currentSpeed = location.getSpeed();
                distance = Math.round(location.distanceTo(finalDestination));
                Log.d(LOG_TAG,"onLocationChanged " + location.getProvider()
                                    + " Speed: " + currentSpeed + " " +
                		" Distance " + distance + " hasRestartedGPS " + hasRestartedGPS);
                
                // When we are closing in to our destination we should increase the
                // GPS update freq so that we do not miss the station.
                // Why 1800 meters?
                // vehicle travels at avarage 10 - 30 meter-per-second.
                // We check GPS pos every 60seconds. Meaning in worst case we can travel 60*30=1800meters
                // before we check GPS and this will make us to miss our final destination.
                if ( distance > setradius && distance < (1800 - setradius) && !hasRestartedGPS) {
                	Log.d(LOG_TAG,"restarting GPS with high freq mode");
                	restartGPS(1);
                	hasRestartedGPS=Boolean.TRUE;
                }
                
                if ( distance < setradius ){
                   Toast.makeText(getApplicationContext(),destination_message,Toast.LENGTH_LONG).show();
                   fireAlarm();
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        startGPS(MIN_TIME_BW_UPDATES);

        return Service.START_NOT_STICKY;
    }
    public void startGPS(long minTime){
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTime,MIN_DISTANCE_CHANGE_FOR_UPDATES,
                mLocationListener);

        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                minTime,MIN_DISTANCE_CHANGE_FOR_UPDATES,
                mLocationListener);
    }
    private void fireAlarm(){
    	Log.d(LOG_TAG,"fireAlarm");
    	Intent intent = new Intent(BackgroundService.this,AlarmReceiverActivity.class);
    	intent.setAction(Intent.ACTION_MAIN);
    	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);    	
    	startActivity(intent);
        stopGPS();
        stopSelf();
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
                        AudioManager amanager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);

                        //If we do not have headset just vibrate
                        if (!amanager.isWiredHeadsetOn()){
                            Log.d(LOG_TAG,"isWiredHeadsetOn");
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

                            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            v.vibrate(pattern,-1);

                        } else if (amanager.isWiredHeadsetOn() && amanager.isMusicActive()){
                            Log.d(LOG_TAG,"music active");
                            //Turn off music
                            Intent intent = new Intent("com.android.music.musicservicecommand.togglepause");
                            getApplicationContext().sendBroadcast(intent);

                            amanager.setStreamVolume(AudioManager.STREAM_MUSIC,12,0);
                            
                            tts.setPitch(0.8f);                           
                            result = tts.speak(destination_message,
                                    TextToSpeech.QUEUE_FLUSH, null);

                            if (result == TextToSpeech.ERROR)
                                Log.e(LOG_TAG,"speach failed");

                        } else {
                            amanager.setStreamVolume(AudioManager.STREAM_MUSIC,12,0);
                            
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
        stopGPS();
        stopSelf();
    }
    public void restartGPS(long time){
    	stopGPS();
    	startGPS(1);  	
    }
    
    public void stopGPS(){
        locationManager.removeUpdates(mLocationListener);
    }
    @Override
    public void onDestroy(){
        mNotificationManager.cancelAll();
        stopGPS();
        Log.d(LOG_TAG,"Service: onDestroy");
    }
    public IBinder onBind(Intent intent) {
        return null;
    }
}
