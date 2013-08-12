package com.application.wakeapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.app.ProgressDialog;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
//import android.widget.Filter.FilterResults;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity {

    private Location finalDestination;
    private Location myLocation=null;
    //private SearchView mSearchView;
    private AutoCompleteTextView mAutoComplete;
    //private ListView mListView;
    private ArrayAdapter<String> mAdapter;
    private Button mButton;
    private TextView mTextView1;
    private TextView mTextViewStation;
    private TextView mTextView3;
    private TextView mTextViewDistance;
    private TextView mTextView4;
    private TextView mTextViewSpeed;
    private TextView mTextInfo;
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
    private ProgressDialog progressDialog = null;
    private LocationListener locationListener;   
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000*1; // 1 second
    private static final String LOG_TAG = "WakeApp";
    private static final String DATABASE_NAME = "stationNames";
    private static final String API_KEY = "AIzaSyAubMfhG4FU2Wxy3Nv0qj8X0QJ3LItcokA";
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

        getWindow().setBackgroundDrawableResource(R.drawable.background);
        
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
		
			//executeBackgroundThread();
        
        	findGPSPosition();
		}

        //mSearchView = (SearchView) findViewById(R.id.searchView);
		mAutoComplete = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView1);
        //mListView   = (ListView) findViewById(R.id.listView);
        mButton     = (Button) findViewById(R.id.button);
        
        mTextInfo    = (TextView) findViewById(R.id.textView1); 
        mTextView1   = (TextView) findViewById(R.id.textView);      
        mTextViewStation = (TextView) findViewById(R.id.station);
        mTextView3 = (TextView) findViewById(R.id.textView2);
        mTextViewDistance = (TextView) findViewById(R.id.distance);
        mTextView4 = (TextView) findViewById(R.id.textView4);
        mTextViewSpeed = (TextView) findViewById(R.id.speed);
        
        mTextInfo.setTextColor(Color.rgb(113, 221, 234));
        mTextView1.setTextColor(Color.rgb(113, 221, 234));
        mTextViewStation.setTextColor(Color.WHITE);
        mTextView3.setTextColor(Color.rgb(113, 221, 234));
        mTextViewDistance.setTextColor(Color.WHITE);
        mTextView4.setTextColor(Color.rgb(113, 221, 234));
        mTextViewSpeed.setTextColor(Color.WHITE);

        mButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                Intent newIntent = new Intent(MainActivity.this,BackgroundService.class);
                
                stopGPS();
                
                newIntent.putExtra(getResources().getString(R.string.destination_name),stationName);
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
/*
        mAdapter = 
        		new ArrayAdapter<String> 
        (this,android.R.layout.test_list_item,stationListNameOnly);
		mAutoComplete.setAdapter(mAdapter);
        */
        mAutoComplete.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));         
        mAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
		    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		        String str = (String) adapterView.getItemAtPosition(position);
		        stationName = str;
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
		        
		        mButton.setVisibility(View.VISIBLE);
		        setTextView(View.VISIBLE);
		        hideSoftKeyboard();
		    }
        	
        });
        
        mAutoComplete.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence charsequence, int arg1, int arg2,
					int arg3) {
				String str = charsequence.toString();
				if (TextUtils.isEmpty(str)) {
					mButton.setVisibility(View.INVISIBLE);
					setTextView(View.INVISIBLE);
				} else {
					setTextView(View.INVISIBLE);
				}
				
				
			}
        	
        });
    }
	private ArrayList<String> autocomplete(String input) {
	    ArrayList<String> resultList = null;
		String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
		String TYPE_AUTOCOMPLETE = "/autocomplete";
		String OUT_JSON = "/json";
		
		
	    HttpURLConnection conn = null;
	    StringBuilder jsonResults = new StringBuilder();
	    try {
	        StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
	        sb.append("?sensor=false&key=" + API_KEY);
	        //sb.append("&components=country:SE");
	        sb.append("&input=" + URLEncoder.encode(input, "utf8"));
	        
	        URL url = new URL(sb.toString());
	        conn = (HttpURLConnection) url.openConnection();
	        InputStreamReader in = new InputStreamReader(conn.getInputStream());
	        
	        Log.d(LOG_TAG,sb.toString());
	        // Load the results into a StringBuilder
	        int read;
	        char[] buff = new char[1024];
	        while ((read = in.read(buff)) != -1) {
	            jsonResults.append(buff, 0, read);
	        }
	    } catch (MalformedURLException e) {
	        Log.e(LOG_TAG, "Error processing Places API URL", e);
	        return resultList;
	    } catch (IOException e) {
	        Log.e(LOG_TAG, "Error connecting to Places API", e);
	        return resultList;
	    } finally {
	        if (conn != null) {
	            conn.disconnect();
	        }
	    }

	    try {
	        // Create a JSON object hierarchy from the results
	        JSONObject jsonObj = new JSONObject(jsonResults.toString());
	        JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");
	        
	        // Extract the Place descriptions from the results
	        resultList = new ArrayList<String>(predsJsonArray.length());
	        
	        for (int i = 0; i < predsJsonArray.length(); i++) {
	        	
	        	String s = predsJsonArray.getJSONObject(i).getString("types");
	        	Log.d(LOG_TAG,"City: " + predsJsonArray.getJSONObject(i).getString("description") + " types " + s);
	        	
	        	if ( s.contains("train_station")|| s.contains("bus_station")||
	        		s.contains("subway_station")||s.contains("transit_station")||
	        		s.contains("locality")){
	        		//https://maps.googleapis.com/maps/api/place/details/json?reference=CjQhAAAAO2dWsIL5-IRUi1cN0V0DLCUPoVJRNR_9xGIv5HMXayEvAba0uvh9EbP3iYDIPOLfEhBCJSVOqPXTkuANitD_1pAJGhQivcKS6O1-nbRRvtwPr1gmMmJ6ew&sensor=true&key=AIzaSyAubMfhG4FU2Wxy3Nv0qj8X0QJ3LItcokA
	        		Log.d(LOG_TAG,"Vi har hittat en station");
	        		String reference = predsJsonArray.getJSONObject(i).getString("reference");
	        		String coordinates = getCoordinates(reference);
	        		String stationName = predsJsonArray.getJSONObject(i).getString("description");
	        		Log.d(LOG_TAG,stationName);
	        		Log.d(LOG_TAG,"coordinates: " + coordinates );
	        		
	        		resultList.add(stationName + " " + coordinates);
	        	}
        		
	        }
	    } catch (JSONException e) {
	        Log.e(LOG_TAG, "Cannot process JSON results", e);
	    }
	    
	    return resultList;
	}
	private String getCoordinates(String ref){
		String returnValue="";
	    HttpURLConnection conn = null;
	    StringBuilder jsonResults = new StringBuilder();
		String base = "https://maps.googleapis.com/maps/api/place/details/json?";
		StringBuilder sb = new StringBuilder();
		sb.append(base);
		sb.append("reference=" + ref + "&");	
		sb.append("sensor=true&key=" + API_KEY);
				        
		try {
			URL url = new URL(sb.toString());
			
	        conn = (HttpURLConnection) url.openConnection();
	        InputStreamReader in = new InputStreamReader(conn.getInputStream());
	        
	        Log.d(LOG_TAG,"url: " + sb.toString());		
	        // Load the results into a StringBuilder
	        int read;
	        char[] buff = new char[1024];
	        while ((read = in.read(buff)) != -1) {
	            jsonResults.append(buff, 0, read);
	        }
	        JSONObject jsonObj;
			jsonObj = new JSONObject(jsonResults.toString());
			JSONObject jsonObject = jsonObj.getJSONObject("result")
										   .getJSONObject("geometry")
										   .getJSONObject("location");

			
			returnValue = jsonObject.getString("lat") + " " + jsonObject.getString("lng");
			Log.d(LOG_TAG,"coords: " + returnValue);
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		   
        
		return returnValue;

	}
	private class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
	    private ArrayList<String> resultList;
	    
	    public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
	        super(context, textViewResourceId);
	    }
	    
	    @Override
	    public int getCount() {
	        return resultList.size();
	    }

	    @Override
	    public String getItem(int index) {
	        return resultList.get(index);
	    }
	    @Override
	    public Filter getFilter() {
	        Filter filter = new Filter() {
	            @Override
	            protected FilterResults performFiltering(CharSequence constraint) {
	                FilterResults filterResults = new FilterResults();
	                if (constraint != null) {
	                    // Retrieve the autocomplete results.
	                    resultList = autocomplete(constraint.toString());
	                    
	                    // Assign the data to the FilterResults
	                    filterResults.values = resultList;
	                    filterResults.count = resultList.size();
	                }
	                return filterResults;
	            }

	            @Override
	            protected void publishResults(CharSequence constraint, FilterResults results) {
	                if (results != null && results.count > 0) {
	                    notifyDataSetChanged();
	                }
	                else {
	                    notifyDataSetInvalidated();
	                }
	            }};
	        return filter;
	    }	   
	}
    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    private void executeBackgroundThread(){

    	progressDialog = ProgressDialog.show(this,
    			"Searching nearby stations",
    			"Downloading stations coordinates...",
    			true, false);
    	new Background().execute();
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
                    progressDialog.dismiss();
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

            //executeBackgroundThread();
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
        	//mSearchView.setIconified(true);
            //mListView.setVisibility(View.INVISIBLE);
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
