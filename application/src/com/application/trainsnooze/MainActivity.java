package com.application.trainsnooze;

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
import android.app.ProgressDialog;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity {

	private ArrayList<String> coordinatesAndNames;
    private Location finalDestination;
    private Location myLocation=null;
    private AutoCompleteTextView mAutoComplete;
    private Button mButton;
    private Button mButtonPrevious;
    private TextView mTextView1;
    private TextView mTextViewStation;
    private TextView mTextView3;
    private TextView mTextViewDistance;
    private TextView mTextView4;
    private TextView mTextViewSpeed;
    private TextView mTextInfo;
    private LocationManager locationManager;
    private Boolean isServiceStarted = Boolean.FALSE;
    private ProgressDialog mProgressDialog;
    @SuppressWarnings("unused")
	private Float distance;
    private Boolean isGPSEnabled = Boolean.FALSE;
    private SharedPreferences prefs;
    private LocationListener locationListener;   
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000*1; // 1 second
    private static final String LOG_TAG = "TrainSnooze";
    private static final String[] KEYS = {"AIzaSyAUvx8iiRdW6f1pqz5zIzdRNFAn5mRtN3I",
    									  "AIzaSyAubMfhG4FU2Wxy3Nv0qj8X0QJ3LItcokA",
    									  "AIzaSyBdzwyhRxZI48c84-IkqNepqS4QQwmHiwg",
    									  "AIzaSyDIUeyPncrOXJg1bLoVr0srUSRofqegPn8"};
    
    private static String API_KEY = "";
    private static final int GPS_SEARCH_TIMEOUT = 32;
    private String countryCode;
    private DataBaseHandler mDataBaseHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG," onCreate " + isServiceStarted);
        
        pickAPIKey();
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        countryCode = prefs.getString("country","se").toLowerCase(Locale.getDefault());

        getWindow().setBackgroundDrawableResource(R.drawable.background);
        
        finalDestination = new Location("Destination");
		mDataBaseHandler = new DataBaseHandler(MainActivity.this);
				
		mAutoComplete = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView1);
        mButton     = (Button) findViewById(R.id.button);
        mButtonPrevious = (Button) findViewById(R.id.button1);
        
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
            	if ( !finalDestination.getProvider().equals("Destination") &&
            		 !TextUtils.isEmpty(mAutoComplete.getText())) {
            		addPreviousLocation();            	
            		startBackgroundService();
            	} else {
            		warningDialog();
            		
            	}
            }
        });
        mButtonPrevious.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
		        previousSearchDialog();
				
			}
        	
        });
        mAutoComplete.setThreshold(4);
        mAutoComplete.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));         
        mAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
		    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				//stationName = (String) adapterView.getItemAtPosition(position);
				finalDestination.setProvider((String) adapterView.getItemAtPosition(position));
		        Double lat = 0.0, lng = 0.0;
		        
                for (String item : coordinatesAndNames) {
                    //if (item.startsWith(stationName)) {
                	if (item.startsWith(finalDestination.getProvider())) {
                        lat = getLatitude(item);
                        lng = getLongitude(item);
                        break;
                    }
                }
                Log.d(LOG_TAG,"Adding finalDestination: " + finalDestination.getProvider());
                finalDestination.setLatitude(lat);
                finalDestination.setLongitude(lng);

                distance = myLocation.distanceTo(finalDestination);
		                    
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
					setTextView(View.INVISIBLE);
				} else {
					setTextView(View.INVISIBLE);
				}
			}
        	
        });

        if ( !isAllSettingsEnabled()) {
        	Log.d(LOG_TAG,"!isAllSettingsEnabled");
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			StringBuilder sb = new StringBuilder();
			
			if (!isDataTrafficEnabled())
				sb.append(this.getString(R.string.enable_data));
			if (isWifiEnabled())
				sb.append(this.getString(R.string.disable_wifi));
			if (!isGPSSettingsEnabled())
				sb.append(this.getString(R.string.enable_gps));
			
			builder.setMessage(sb.toString());
			builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	            	if (!isDataTrafficEnabled())
	            		startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
	            	if (isWifiEnabled())
	            		startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
	            	if (!isGPSSettingsEnabled())
	            		startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
	            	finish();
	            }
	        });
			AlertDialog alert = builder.create();	
			alert.show();
        	
        } else {
        	
        	findGPSPosition();
        	
			mProgressDialog = ProgressDialog.show(MainActivity.this,
	    			this.getString(R.string.search_gps),
	    			this.getString(R.string.trying_gps),
	    			true, false);
	
	        new CountryThread().execute();
        }
    }

    private void pickAPIKey(){
        final Random rand = new Random();
        int chooseAPIKEY = rand.nextInt(KEYS.length);
        API_KEY = KEYS[chooseAPIKEY];
        
        Log.d(LOG_TAG,"Random: " + chooseAPIKEY + " API_KEY " + API_KEY);
    }
    private void warningDialog(){
    	AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                MainActivity.this);
        builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle(this.getString(R.string.valid_station));
        builderSingle.setPositiveButton("Ok, I get it",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                            DialogInterface dialog,
                            int which) {
                        dialog.dismiss();                        
                    }
                });
        builderSingle.show();
    }
    private void startBackgroundService(){
        Intent newIntent = new Intent(MainActivity.this,BackgroundService.class);
        
        stopGPS();
        
        newIntent.putExtra(getResources().getString(R.string.destination_name),finalDestination.getProvider());
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
    private void addPreviousLocation(){
    	Double lat=0.0;
    	Double lng=0.0;
    	
        for (String item : coordinatesAndNames) {
            if (item.startsWith(finalDestination.getProvider())) {
                lat = getLatitude(item);
                lng = getLongitude(item);
                break;
            }
        }             	
        Location l = new Location(finalDestination.getProvider());
        l.setLatitude(lat);
        l.setLongitude(lng);
        Log.d(LOG_TAG,"addLocation: " + finalDestination.getProvider() );
        mDataBaseHandler.addLocation(l);
    }
    private void previousSearchDialog(){
    	AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                MainActivity.this);
        builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle(this.getString(R.string.previous_search));
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
        		MainActivity.this,
                android.R.layout.select_dialog_singlechoice);
        
        ArrayList<Location> locations = mDataBaseHandler.getAllLocations();

        // Adding all previous searched locations to the
        // AlertDialog.
        for (Location l: locations) {
        	arrayAdapter.add(l.getProvider());
        }
        
        builderSingle.setNegativeButton(this.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        
        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String strName = arrayAdapter.getItem(which);
                        AlertDialog.Builder builderInner = new AlertDialog.Builder(
                        		MainActivity.this);
                        
                        //finalDestination = mDataBaseHandler.getLocationFromName(strName);
                        //dialog.dismiss();
                        //startBackgroundService();
                        String set_or_delete = getResources().getString(R.string.Set_or_delete);
                        String go = getResources().getString(R.string.GO);
                        String delete = getResources().getString(R.string.delete);
                        
                        builderInner.setMessage(strName);
                        builderInner.setTitle(set_or_delete);
                        builderInner.setPositiveButton(go,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which) {
                                    	// User has chosen an new final destination
                                        finalDestination = mDataBaseHandler.getLocationFromName(strName);
                                        dialog.dismiss();
                                        startBackgroundService();
                                        
                                    }
                                });
                        
                        builderInner.setNegativeButton(delete, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Log.d(LOG_TAG,"onClick no");
                                mDataBaseHandler.deleteLocation(strName);
                            }
                        });
                        /*
                        builderInner.setNeutralButton("Cancel", new DialogInterface.OnClickListener(){

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								
								dialog.dismiss();
							}
                        	
                        });*/
                        builderInner.show();
                                                
                    }
                });
        builderSingle.show();
    }
    // Filter places that is not of interest
    private Boolean isPlaceAccepted(String place) {
    	String[] places_array = getResources().getStringArray(R.array.places_array);
    	for (String s: places_array){
    		if (place.contains(s))
    			return Boolean.TRUE;
    	}
	
    	return Boolean.FALSE;
    }
	private ArrayList<String> autocomplete(String input) {
	    ArrayList<String> resultList = null;
        coordinatesAndNames = new ArrayList<String>();
		String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
		String TYPE_AUTOCOMPLETE = "/autocomplete";
		String OUT_JSON = "/json";		
		
	    HttpURLConnection conn = null;
	    StringBuilder jsonResults = new StringBuilder();
	    try {
	        StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
	        sb.append("?sensor=false&key=" + API_KEY);	        
        	sb.append("&components=country:" + countryCode);	        
	        sb.append("&input=" + URLEncoder.encode(input, "utf8"));
	        
	        URL url = new URL(sb.toString());
	        conn = (HttpURLConnection) url.openConnection();
	        InputStreamReader in = new InputStreamReader(conn.getInputStream());
	        
	        // Load the results into a StringBuilder
	        int read;
	        char[] buff = new char[1024];
	        while ((read = in.read(buff)) != -1) {
	            jsonResults.append(buff, 0, read);
	        }
	        
	        // Create a JSON object hierarchy from the results
	        JSONObject jsonObj = new JSONObject(jsonResults.toString());
	        JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");
	        
	        // Extract the Place descriptions from the results
	        resultList = new ArrayList<String>(predsJsonArray.length());
	        
	        for (int i = 0; i < predsJsonArray.length(); i++) {
	        	
	        	String s = predsJsonArray.getJSONObject(i).getString("types");
	        	Log.d(LOG_TAG,"City: " + predsJsonArray.getJSONObject(i).getString("description") + " types " + s);
	        	
	        	if ( isPlaceAccepted(s) ){
	        		//https://maps.googleapis.com/maps/api/place/details/json?reference=CjQhAAAAO2dWsIL5-IRUi1cN0V0DLCUPoVJRNR_9xGIv5HMXayEvAba0uvh9EbP3iYDIPOLfEhBCJSVOqPXTkuANitD_1pAJGhQivcKS6O1-nbRRvtwPr1gmMmJ6ew&sensor=true&key=AIzaSyAubMfhG4FU2Wxy3Nv0qj8X0QJ3LItcokA
	        		String reference = predsJsonArray.getJSONObject(i).getString("reference");
	        		String coordinates = getCoordinates(reference);
	        		String station = predsJsonArray.getJSONObject(i).getString("description");
	        		
	        		resultList.add(station);
	        		coordinatesAndNames.add(station + " " + coordinates);
	        	}
        		
	        }	        
	    } catch (MalformedURLException e) {
	        Log.e(LOG_TAG, "Error processing Places API URL", e);
	        return resultList;
	    } catch (IOException e) {
	        Log.e(LOG_TAG, "Error connecting to Places API", e);
	        return resultList;
	    } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
	        if (conn != null) {
	            conn.disconnect();
	        }
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
    private Boolean isAllSettingsEnabled(){
    	return (isGPSSettingsEnabled() && isDataTrafficEnabled() && !isWifiEnabled());
    }
    private Boolean isWifiEnabled(){
    	ConnectivityManager conMan = ((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE));
    	boolean isWifiEnabled = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isAvailable();
    	
    	return isWifiEnabled;
    }
    private Boolean isGPSSettingsEnabled(){
    	Boolean isOn = Boolean.FALSE;

    	String provider = Settings.Secure.getString(getContentResolver(),
    			Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
    	
        if(provider.isEmpty()){
        	isOn = Boolean.FALSE;
        } else
        	isOn = Boolean.TRUE;

        return isOn;
    }
	private Boolean isDataTrafficEnabled(){
		ConnectivityManager conMan = ((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE));
		String reason = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getReason();
		
		if ( reason == null )
			return Boolean.FALSE;			
		
		boolean is3GEnabled = !(conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.DISCONNECTED
                && reason.equals("dataDisabled"));

		return is3GEnabled;
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
        mTextViewStation.setText(finalDestination.getProvider());

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
        
        mTextViewStation.setText(finalDestination.getProvider());
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
            	if ( isAllSettingsEnabled()) {
            		Log.d(LOG_TAG,"mProgressDialog.dismiss");
                	mProgressDialog.dismiss();
            	}
                
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
    public void stopGPS(){
    	//if ( locationManager != null && locationListener != null)
    	//	locationManager.removeUpdates(locationListener);
    	Log.d(LOG_TAG,"Tearing down GPS");
    	isGPSEnabled = Boolean.FALSE;
    	locationManager.removeUpdates(locationListener);
    }
    public String getCountryCode(Double lat, Double lng){
    	//http://api.geonames.org/countryCode?lat=47.03&lng=10.2&username=demo
    	
    	StringBuilder jsonResults = new StringBuilder();
    	HttpURLConnection conn;
    	String country="";
    	StringBuilder sb = new StringBuilder();
    	sb.append("http://ws.geonames.org/countryCode?type=json&");
    	sb.append("lat=" + lat + "&");
    	sb.append("lng=" + lng);

    	try {
	    	URL url = new URL(sb.toString());
	    	
	        conn = (HttpURLConnection) url.openConnection();
	        InputStreamReader in = new InputStreamReader(conn.getInputStream());
	        
	        int read;
	        char[] buff = new char[1024];
	        while ((read = in.read(buff)) != -1) {
	            jsonResults.append(buff, 0, read);
	        }
	        JSONObject jsonObj;
			jsonObj = new JSONObject(jsonResults.toString());
			//JSONObject jsonObject = jsonObj.getJSONObject("countryCode");
			country = jsonObj.getString("countryCode").toLowerCase(Locale.getDefault());
			Log.d(LOG_TAG,"Countrycode: " + country);						 


    	} catch (MalformedURLException e) {
    		Log.d(LOG_TAG,"MalformedURLException");
	    } catch (IOException e) {
	    	Log.d(LOG_TAG,"IOException");
	    } catch (JSONException e) {
			// TODO Auto-generated catch block
	    	Log.d(LOG_TAG,"JSONException");
			e.printStackTrace();
		} 
    	
    	return country;
    }
    class CountryThread extends AsyncTask<Void,Void,Void>{
		private int counter=0;
        @Override
        protected void onPostExecute(Void result) {
        	mProgressDialog.dismiss();
        	
        	// If we have exceeded timeout we should inform
        	// the user that we cannot proceed and application
        	// must be aborted.
        	if ( counter >= GPS_SEARCH_TIMEOUT ) {
	        	AlertDialog.Builder builderSingle = new AlertDialog.Builder(
	                    MainActivity.this);
	            builderSingle.setIcon(R.drawable.ic_launcher);
	            builderSingle.setTitle(getResources().getString(R.string.no_gps_pos));
	            builderSingle.setPositiveButton(getResources().getString(R.string.aborting),
	                    new DialogInterface.OnClickListener() {
	                        @Override
	                        public void onClick(DialogInterface dialog, int which) {
	                            finish();
	                        }
	                    });
	            builderSingle.show();
        	}
        }
    	protected Void doInBackground(Void...params){
 		
            do{//We need to get an position
                try {
                    Thread.sleep(1000);
                    Log.d(LOG_TAG,"looking for GPS");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if ( counter++ > GPS_SEARCH_TIMEOUT) {
                	return null;
                }
            }while(myLocation == null);                      
                        
    		String country = getCountryCode(myLocation.getLatitude(),
    										myLocation.getLongitude());
    		
    		SharedPreferences.Editor editor = prefs.edit();
    		
    		if (!country.isEmpty() && !country.equals(countryCode)){
    			countryCode = country.toLowerCase(Locale.getDefault());
    			Log.d(LOG_TAG,"New country code found: " + countryCode);
    			editor.putString("country",country);    			
    			editor.commit();
    		}

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
            setTextView(View.VISIBLE);

            // Stop the background service when we
            // resume the UI.
            stopService(new Intent(MainActivity.this,
                    BackgroundService.class));

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
