package com.application.wakeapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.google.ads.AdRequest;
import com.google.ads.AdView;

public class MainActivity extends Activity {

    private Location finalDestination;
    private Location myLocation=null;
    private SearchView mSearchView;
    private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private Button mButton;
    private TextView mTextView1;
    private TextView mTextViewStation;
    private TextView mTextView3;
    private TextView mTextViewDistance;
    private TextView mTextView4;
    private TextView mTextViewSpeed;
    private ArrayList<String> stationList;
    private ArrayList<String> stationListNameOnly;
    private LocationManager locationManager;
    private Boolean isServiceStarted = Boolean.FALSE;
    private String stationName="none";
    @SuppressWarnings("unused")
	private Float distance;
    private DataBaseHandler mDataBaseHandler;
    private Boolean isThereAnDatabase = Boolean.FALSE;
    private Boolean isGPSEnabled = Boolean.FALSE;
    private SharedPreferences prefs;
    private int searchRadius;
    private int outsidethreshold;
    private Boolean usedatabase;
    private LocationListener locationListener;   
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000*1; // 1 second
    private static final String LOG_TAG = "WakeApp";
    private static final String DATABASE_NAME = "stationNames";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG," onCreate " + isServiceStarted);

        prefs =  PreferenceManager.getDefaultSharedPreferences(this);
        outsidethreshold = Integer.parseInt(prefs.getString("outsidethreshold","500"));
        searchRadius = Integer.parseInt(prefs.getString("searchradius","5000"));

        usedatabase = prefs.getBoolean("usedatabase",Boolean.TRUE);
        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                Log.d(LOG_TAG,"onSharedPreferenceChanged: " + s);
                // If the user have changed searchradius we need to discard the database
                // and re-fetch the station list from server.
                if ( s.equals("searchradius") ){
                    getApplicationContext().deleteDatabase(mDataBaseHandler.getDatabaseName());
                }

            }
        });

        
        AdView adView = (AdView)this.findViewById(R.id.adView);
        adView.loadAd(new AdRequest());
                
        if ( checkDataBase()){
            Log.d(LOG_TAG,"Database exists");
            isThereAnDatabase = Boolean.TRUE;
        }

        finalDestination = new Location("Destination");
        stationList = new ArrayList<String>();
        stationListNameOnly = new ArrayList<String>();
        mDataBaseHandler = new DataBaseHandler(MainActivity.this);

        // User should enable GPS
        if (!isGPSSettingsEnabled()){
        	Log.d(LOG_TAG,"GPS sensor is not enabled");
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("You must enable GPS sensor!");
			builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
	            	finish();
	            }
	        });
			AlertDialog alert = builder.create();
			alert.show();
        }
        // We must make sure that the user has enabled 3g traffic
        // before we can proceed. If it's not enabled we should call
        // finish() and open settings menu
		if (!isDataTrafficEnabled()){
			Log.d(LOG_TAG,"Data traffic is not enabled");

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("You must enable data traffic!");
			builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
	            	finish();
	            }
	        });
			AlertDialog alert = builder.create();
			alert.show();
		} else {
		
			new Background().execute();
        
        	findGPSPosition();
		}

        mSearchView = (SearchView) findViewById(R.id.searchView);
        mListView   = (ListView) findViewById(R.id.listView);
        mButton     = (Button) findViewById(R.id.button);
        
        mTextView1   = (TextView) findViewById(R.id.textView);      
        mTextViewStation = (TextView) findViewById(R.id.station);
        mTextView3 = (TextView) findViewById(R.id.textView2);
        mTextViewDistance = (TextView) findViewById(R.id.distance);
        mTextView4 = (TextView) findViewById(R.id.textView4);
        mTextViewSpeed = (TextView) findViewById(R.id.speed);
                
        mListView.setAdapter(mAdapter = new ArrayAdapter<String>(
                            this,android.R.layout.test_list_item,
                    stationListNameOnly));

        mButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent newIntent = new Intent(MainActivity.this,BackgroundService.class);
                
                stopGPS();

                newIntent.putExtra("lng",finalDestination.getLongitude());
                newIntent.putExtra("lat",finalDestination.getLatitude());
                startService(newIntent);

                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);

                isServiceStarted = Boolean.TRUE;
                Log.d(LOG_TAG, finalDestination.getLongitude() + " " + isServiceStarted);
            }
        });
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                stationName = ((TextView) view).getText().toString();
                Double lat = 0.0, lng = 0.0;

                for (String item : stationList) {
                    if (item.startsWith(stationName)) {
                        lat = getLatitude(item);
                        lng = getLongitude(item);
                        break;
                    }
                }
                finalDestination.setLatitude(lat);
                finalDestination.setLongitude(lng);

                distance = myLocation.distanceTo(finalDestination);

                mListView.setVisibility(View.INVISIBLE);
                mButton.setVisibility(View.VISIBLE);
                               
                setTextView(View.VISIBLE);

                hideSoftKeyboard();
            }

            protected void hideSoftKeyboard() {
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }
        });
        mListView.setTextFilterEnabled(true);
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    mListView.clearTextFilter();
                    mListView.setVisibility(View.INVISIBLE);
                    mButton.setVisibility(View.INVISIBLE);                    
                    setTextView(View.INVISIBLE);
                } else {
                    mListView.setFilterText(newText.toString());
                    mListView.setVisibility(View.VISIBLE);
                    setTextView(View.INVISIBLE);
                }
                return true;
            }
        });

    }
    private Boolean isGPSSettingsEnabled(){
    	Boolean isOn = Boolean.FALSE;

    	String provider = Settings.Secure.getString(getContentResolver(),
    			Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if(provider.equals("")){
        	isOn = Boolean.FALSE;
        } else
        	isOn = Boolean.TRUE;

        return isOn;
    }    
	private Boolean isDataTrafficEnabled(){
		NetworkInfo networkInfo = null;
        ConnectivityManager connectivityManager = 
        		(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null){
        	networkInfo = connectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		    if (!networkInfo.isAvailable()) {
		    	networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		    	
		    }
        }
        
		return networkInfo == null ? false : networkInfo.isConnected();
	}
    public void updateText(){
        String dist;
        Float currentDistance=0.0f;
        String speed = String.format(Locale.getDefault(),"%.3g km/h", myLocation.getSpeed()*(3.6));
        Log.d(LOG_TAG,"updateText");
        
        if ( myLocation != null)
        	currentDistance = myLocation.distanceTo(finalDestination);

        if (currentDistance > 1000)
            dist = String.format(Locale.getDefault(),"%.3g km",currentDistance/1000);
        else
            dist = String.format(Locale.getDefault(),"%.3g meter",currentDistance);
        
        mTextViewSpeed.setText(speed);       
        mTextViewDistance.setText(dist);        
        mTextViewStation.setText(stationName);

    }
    public void setTextView(int visible){
        String dist;
        Float currentDistance=0.0f;
        String speed = "";
        Log.d(LOG_TAG,"setTextView visibility: " + visible);
        
        if ( myLocation != null) {
        	currentDistance = myLocation.distanceTo(finalDestination);
        	speed = String.format(Locale.getDefault(),"%.3g km/h", myLocation.getSpeed()*(3.6));
        }

        if (currentDistance > 1000)
        	dist = String.format(Locale.getDefault(),"%.3g km",currentDistance/1000);
        else
        	dist = String.format(Locale.getDefault(),"%.3g meter",currentDistance);

        mTextView1.setVisibility(visible);
        mTextView3.setVisibility(visible);
        mTextView4.setVisibility(visible);
        
        mTextViewSpeed.setText(speed);
        mTextViewSpeed.setVisibility(visible);
        
        mTextViewDistance.setText(dist);
        mTextViewDistance.setVisibility(visible);
        
        mTextViewStation.setText(stationName);
        mTextViewStation.setVisibility(visible);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    private Double getLatitude(String s){
        String[] tmp = s.split(" ");
        return Double.parseDouble(tmp[tmp.length-2]);
    }
    private Double getLongitude(String s){
        String[] tmp = s.split(" ");
        return Double.parseDouble(tmp[tmp.length-1]);
    }
    @SuppressWarnings("unused")
	private Boolean tryGetQuickGPSFix(){
        Boolean ret = Boolean.FALSE;

        Criteria criteria = new Criteria();
        criteria.setSpeedAccuracy(Criteria.ACCURACY_LOW);

        String name = locationManager.getBestProvider(criteria,false);

        Location tmp = locationManager.getLastKnownLocation(name);

        if (tmp == null)
            return Boolean.FALSE;

        long delta = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - tmp.getTime());

        // If the location is older then 3minutes
        // then we should consider it out-dated
        if ( (int)delta < (60*3) ){
            Log.d(LOG_TAG,"We got Quick GPS fix: " + delta);
            myLocation = tmp;
            ret = Boolean.TRUE;
        }

        return ret;
    }
    private void findGPSPosition(){
    	
    	if (isGPSEnabled){
    		Log.d(LOG_TAG,"GPS already up");
    		return;
    	} else
    		Log.d(LOG_TAG,"Setting up GPS");


        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        // If there is an position that is not out-dated
        // we should use it to get fast GPS coordinates.
        // Otherwise we need to use the GPS which takes
        // more time and consumes more power.
        //if (tryGetQuickGPSFix())
        //    return;

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            	Log.d(LOG_TAG,"onLocationChanged");
                myLocation = location;
                updateText();
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

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener);
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener);
        locationManager.requestLocationUpdates(
                LocationManager.PASSIVE_PROVIDER,
                MIN_TIME_BW_UPDATES,MIN_DISTANCE_CHANGE_FOR_UPDATES,
                locationListener);

        isGPSEnabled = Boolean.TRUE;

    }
    // Check if database exits
    private boolean checkDataBase() {
        File dbFile=getApplicationContext().getDatabasePath(DATABASE_NAME);
        return dbFile.exists();
    }
    public void stopGPS(){
    	//if ( locationManager != null && locationListener != null)
    	//	locationManager.removeUpdates(locationListener);
    	Log.d(LOG_TAG,"Tearing down GPS");
    	isGPSEnabled = Boolean.FALSE;
    	locationManager.removeUpdates(locationListener);
    }
    
    class Background extends AsyncTask<String, Integer, String> {

        private void addPreviousLocation() {
            Log.d(LOG_TAG,"addPreviousLocation");
            Location l = new Location("previousSearch");
            l.setLatitude(myLocation.getLatitude());
            l.setLongitude(myLocation.getLongitude());

            mDataBaseHandler.addLocation(l);
        }
        private void populateDatabase(){
            Log.d(LOG_TAG,"populateDatabase");
            Stations stations =
                    new Stations(myLocation.getLongitude(),
                            myLocation.getLatitude(),
                            searchRadius);

            stationList = stations.getAllStations(Boolean.FALSE);

            stationListNameOnly = removeCoordinates(stationList);

            // Very ugly way to parse the location string that
            // looks like this
            // "Lund cental station 53.213 15.235"
            // "Aroboga 56.542 17.34456"
            // when we have extracted name and coordinates we
            // add it to the database
            for (String item : stationList){
                StringBuilder sb = new StringBuilder();
                String[] tmp = item.split(" ");
                int items = tmp.length;
                Double lat = Double.parseDouble(tmp[(items-2)]);
                Double lng = Double.parseDouble(tmp[(items-1)]);
                for ( int i=0;i<items-2;i++){
                    sb.append(tmp[i] + " ");
                }
                String name = sb.toString();

                Location l = new Location(name);
                l.setLatitude(lat);
                l.setLongitude(lng);
                
                mDataBaseHandler.addLocation(l);
            }
        }
        private Boolean haveWeBeenHereBefore(){
            Boolean ret = Boolean.FALSE;
            Float distanceTo=0f;
            ArrayList<Location> locations = mDataBaseHandler.getOnlyPreviousSearchesLocation();

            for (Location l : locations){
                    if ( myLocation.distanceTo(l) < outsidethreshold){
                        distanceTo = myLocation.distanceTo(l);
                        ret = Boolean.TRUE;
                        break;
                    }

            }
            Log.d(LOG_TAG,"haveWeBeenHereBefore " + ret + " distance: "
                    + distanceTo + "meters");
            return ret;
        }
        @SuppressWarnings("unused")
		private void fetchFromServer(){
            Log.d(LOG_TAG,"fetchFromServer");
            Stations stations =
                    new Stations(myLocation.getLongitude(),
                            myLocation.getLatitude(),
                            searchRadius);

            stationList = stations.getAllStations(Boolean.FALSE);

            stationListNameOnly = removeCoordinates(stationList);
        }
        private void fetchFromCache(){
            Log.d(LOG_TAG,"fetchFromCache");
            stationList = mDataBaseHandler.getAllButPreviousString();
            stationListNameOnly = removeCoordinates(stationList);
        }

        private ArrayList<String> removeCoordinates(ArrayList<String> l){
            ArrayList<String> newList = new ArrayList<String>();

            for ( int i=0;i<l.size();i++){
                StringBuilder sb = new StringBuilder();
                String tmp = l.get(i);
                String[] temp = tmp.split(" ");
                for ( int j=0;j<temp.length-2;j++){
                    sb.append(temp[j] + " ");
                }
                newList.add(sb.toString());
            }

            return  newList;
        }
        protected String doInBackground(String... urls) {
            long startTime = System.currentTimeMillis();
            do{//We need to get an position
                try {
                    Thread.sleep(1000);
                    Log.d(LOG_TAG,"looking for GPS");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }while(myLocation == null);
            Log.d(LOG_TAG,"Time to get GPS; " +
                    (System.currentTimeMillis() - startTime)/1000 + " sec");

            // First start-up we don't have an database.
            // Download station list from server and
            // populate database.
            // else we have the data locally so no need to
            // fetch from server.
            if ( !isThereAnDatabase || !haveWeBeenHereBefore() || !usedatabase)
                populateDatabase();
            else
                fetchFromCache();

            if (!haveWeBeenHereBefore())
            	addPreviousLocation();

            Log.d(LOG_TAG,"Pos found: lat: " +
                    myLocation.getLatitude() + " lng: " +
                    myLocation.getLongitude());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.addAll(stationListNameOnly);
                    mAdapter.notifyDataSetChanged();
                    Log.d(LOG_TAG,"notifyDataSetChanged len: " + stationListNameOnly.size());
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Station list updated",
                            Toast.LENGTH_LONG);
                    t.setGravity(Gravity.CENTER ,0, 0);
                    t.show();
                }
            });
            return null;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG,"onResume");
		// Find our new position if we have moved
		findGPSPosition();
	
        if (isServiceStarted){            
            mButton.setVisibility(View.VISIBLE);
            setTextView(View.VISIBLE);

            // Stop the background service when we
            // resume the UI.
            stopService(new Intent(MainActivity.this,
                    BackgroundService.class));

            new Background().execute();
        }
        isServiceStarted = Boolean.FALSE;
    }
    @Override
    protected void onNewIntent(Intent intent){
    	super.onNewIntent(intent);
    	setIntent(intent);
    	String msg;
    	    	
    	msg = intent.getStringExtra("AlarmActivity");
    	
    	Log.d(LOG_TAG,"MainActivity onNewIntent " + msg);
    	
    	// If we get an intent from the AlarmActivity that means
    	// we should exit application and set it to on-first-start-mode
        if ( msg != null && msg.equals("PingByAlarm")){
        	mSearchView.setIconified(true);
            mListView.setVisibility(View.INVISIBLE);
            mButton.setVisibility(View.INVISIBLE);
            //mTextView.setVisibility(View.INVISIBLE);
            setTextView(View.INVISIBLE);
            finish();
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        
        Log.d(LOG_TAG,"onDestroy");
        stopGPS();
        
        stopService(new Intent(MainActivity.this,
                BackgroundService.class));
        isServiceStarted = Boolean.FALSE;

    }
    @Override
    protected void onPause(){
    	super.onPause();
      	stopGPS();
    	Log.d(LOG_TAG,"onPause");
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getTitle().equals("Settings")){
            Intent i = new Intent(MainActivity.this,WakeAppPreferences.class);
            startActivity(i);
        }

        return true;
    }
}
