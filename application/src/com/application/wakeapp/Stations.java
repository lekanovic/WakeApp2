package com.application.wakeapp;


import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by Radovan Lekanovic on 2013-07-05.
 * Information is fetched from: http://console.apihq.com/tagtider
 */
public class Stations {
    private NearbyStations nearbyStations;
    private StationList stationList;

    public Stations(Double x, Double y,Integer r){

        nearbyStations = new NearbyStations(x,y,r);
        stationList = new StationList();

    }
    public class NearbyStations {
        private String url = "https://api.trafiklab.se/samtrafiken/resrobot/StationsInZone.json?";
        private JSONParser jParser;
        private JSONObject json;
        private JSONArray items;

        public NearbyStations(Double x, Double y, Integer r){
            StringBuilder sb = new StringBuilder();
            sb.append(url);
            sb.append("apiVersion=2.1&");
            sb.append("centerX=" + x.toString() + "&");
            sb.append("centerY=" + y.toString() + "&");
            sb.append("radius=" + r.toString() + "&" );
            sb.append("coordSys=WGS84&");
            sb.append("key=da562e01a1a10ae57652788f1d5dd642");
            url = sb.toString();

            System.out.println("Radde123" + url);
            jParser = new JSONParser();
            json = jParser.getJSONFromUrl(url,Boolean.FALSE);

        }
        public JSONArray getItems(){
            try {
                JSONObject c1 = json.getJSONObject("stationsinzoneresult");
                items = c1.getJSONArray("location");
                // for(int i = 0; i < items.length(); i++){
                //     JSONObject c = items.getJSONObject(i);
                //     System.out.println("Radde123" + c.getString("name"));
                // }
                //System.out.println("Radde123 " + items.length());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return items;
        }
    }

    public class StationList {
        private String url = "http://api.tagtider.net/v1/stations.json";
        private JSONParser jParser;
        private JSONObject json;
        private JSONArray items;

        public StationList(){
            jParser = new JSONParser();
            json = jParser.getJSONFromUrl(url,Boolean.TRUE);
        }

        public JSONArray getItems(){
            try {
                JSONObject c1 = json.getJSONObject("stations");
                items = c1.getJSONArray("station");
                //for(int i = 0; i < items.length(); i++){
                //    JSONObject c = items.getJSONObject(i);
                //    System.out.println("Radde123" + c.getString("name"));
                //}
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return items;
        }
    }

    public ArrayList<String> getAllStations(Boolean useDebug){
        ArrayList<String> locationList = new ArrayList<String>();

        if (useDebug){
            locationList.add("Lund Thulehemsvägen 55.703083 13.230134");
            locationList.add("Lund Tekniska Högskolan 55.711681 13.211743");
            locationList.add("Lund Sakförarevägen 55.727991 13.209706");
            locationList.add("Lund Fagottgränden 55.708201 13.229956");
            locationList.add("Lund Gambro 55.720581 13.216247");
            locationList.add("Lund Scheeleparken 55.715836 13.217038");
            locationList.add("Arboga 59.3971 15.8405");
            locationList.add("Arlanda C 59.6486 17.9287");
            locationList.add("Avesta centrum 60.1473 16.1706");
            locationList.add("Aneby 57.8373 14.8117");

        }else {
            locationList = getNearbyStations();
            locationList.addAll(getTrainStations());
        }
        return locationList;
    }
    public ArrayList<String> getNearbyStations(){
        JSONArray jsonArray;
        ArrayList<String> locationList = new ArrayList<String>();

        jsonArray = nearbyStations.getItems();

        for (int i=0;i<jsonArray.length();i++){
            try {
                JSONObject c = jsonArray.getJSONObject(i);

                // We get null as coordinates this should be removed
                if (!c.getString("@y").equals("null")){
                    locationList.add(c.getString("name") + " " +
                            c.getString("@y") + " " +
                            c.getString("@x"));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return locationList;
    }
    public ArrayList<String> getTrainStations(){
        JSONArray jsonArray;
        ArrayList<String> locationList = new ArrayList<String>();

        jsonArray = stationList.getItems();

        for (int i=0;i<jsonArray.length();i++){
            try {
                JSONObject c = jsonArray.getJSONObject(i);

                // If we get null as coordinates we should remove them
                if ( !c.getString("lat").equals("null") ){
                    locationList.add(c.getString("name") + " " +
                            c.getString("lat") + " " +
                            c.getString("lng"));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return locationList;
    }
}