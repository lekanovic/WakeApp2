package com.application.trainsnooze;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;


/**
 * Created by Radovan Lekanovic on 2013-07-10.
 */
public class DataBaseHandler extends SQLiteOpenHelper{

    // Path to database in the phone:
    // data/data/com.example.databasetest/databases/stationNames
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "stationNames";
    private static final String TABLE_STATIONS = "stations";

    // Station table name
    private static final String KEY_NAME = "stationName";
    private static final String KEY_LATITUDE = "Lat";
    private static final String KEY_LONGITUDE = "Lng";
    private static final String LOG_TAG = "TrainSnooze";
    
    public DataBaseHandler(Context c){
        super(c,DATABASE_NAME,null,DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_STATION_TABLE =
                "CREATE TABLE " + TABLE_STATIONS + " ("
                        + KEY_NAME + " TEXT,"
                        + KEY_LATITUDE + " FLOAT,"
                        + KEY_LONGITUDE + " FLOAT,"
                        + "PRIMARY KEY (" + KEY_LATITUDE + "," + KEY_LONGITUDE + ")"
                        + ");";

        Log.d(LOG_TAG,"path to database" + sqLiteDatabase.getPath());
        sqLiteDatabase.execSQL(CREATE_STATION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        String DROP_TABLES = "DROP TABLE IF EXISTS " + TABLE_STATIONS;
        // Drop tables
        sqLiteDatabase.execSQL(DROP_TABLES);
        // Create tables again
        onCreate(sqLiteDatabase);
    }

    public void addLocation(Location l){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        //System.out.println("Radde123 addLocation: " + l.getProvider());
        values.put(KEY_NAME,l.getProvider());
        values.put(KEY_LATITUDE,l.getLatitude());
        values.put(KEY_LONGITUDE,l.getLongitude());
        try {
        	db.insertOrThrow(TABLE_STATIONS,null,values);
        } catch (SQLException e){
        	e.printStackTrace();
        }
    }
    public Location getLocationFromName(String name){
    	 
    	 String selectQuery = "SELECT * FROM " + TABLE_STATIONS +
                 " WHERE " + KEY_NAME + " = " + "'" + name + "'";
    	 
         SQLiteDatabase db = this.getWritableDatabase();
         Cursor cursor = db.rawQuery(selectQuery, null);
         cursor.moveToFirst();
         
         Log.d(LOG_TAG,"getLocationFromName: " + 
         cursor.getString(0) + " " +
         cursor.getString(1) + " " +
         cursor.getString(2) + " ");
         
         String stationName = cursor.getString(0);
         Double lat = cursor.getDouble(1);
         Double lng = cursor.getDouble(2);
         
         Location loc = new Location(stationName);
         loc.setLatitude(lat);
         loc.setLongitude(lng);
         
    	 return loc;
    }
    public void deleteLocation(String name){
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_STATIONS, KEY_NAME + "=" + "'" + name + "'", null);
        
        Log.d(LOG_TAG,"Rows removed: " + rows);
        
    }
    public ArrayList<Location> getAllLocations(){
        ArrayList<Location> previousLocations = new ArrayList<Location>();
        String selectQuery = "SELECT * FROM " + TABLE_STATIONS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()){
            do {
                Location tmp = new Location(cursor.getString(0));
                tmp.setLatitude(cursor.getDouble(1));
                tmp.setLongitude(cursor.getDouble(2));
                previousLocations.add(tmp);
            }while (cursor.moveToNext());
        }

        return previousLocations;    	
    }
    public String getDatabaseName(){
        return DATABASE_NAME;
    }
}
