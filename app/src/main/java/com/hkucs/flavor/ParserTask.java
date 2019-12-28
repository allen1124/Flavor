package com.hkucs.flavor;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ParserTask extends AsyncTask<String, Integer, List<HashMap<String, String>>> {

    private static final String TAG = "ParserTask";
    private static final String BOOKMARK_PREF = "BOOKMARK_PREF";
    JSONObject jObject;
    private HashMap<String, Boolean> markerList;
    private GoogleMap mMap;
    private LatLng latLng;
    private Context context;
    SharedPreferences bookmarkPref;
    boolean bookmarked = false;

    public ParserTask(Context context, GoogleMap mMap, LatLng latLng, HashMap<String, Boolean> markerList) {
        this.context = context;
        this.mMap = mMap;
        this.latLng = latLng;
        this.markerList = markerList;
    }

    @Override
    protected List<HashMap<String, String>> doInBackground(String... jsonData) {

        List<HashMap<String, String>> places = null;
        Place_JSON placeJson = new Place_JSON();

        try {
            jObject = new JSONObject(jsonData[0]);
            places = placeJson.parse(jObject);
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        }
        return places;
    }

    @Override
    protected void onPostExecute(List<HashMap<String, String>> list) {

        if(list == null){
            Log.d(TAG, "onPostExecute: List is empty");
            return;
        }
        Log.d("Map", "list size: " + list.size());
        for (int i = 0; i < list.size(); i++) {
            MarkerOptions markerOptions = new MarkerOptions();
            HashMap<String, String> hmPlace = list.get(i);
            if(markerList.get(hmPlace.get("place_id")) != null && markerList.get(hmPlace.get("place_id")) == true){
                break;
            }else{
                markerList.put(hmPlace.get("place_id"), true);
            }
            Place restaurant = new Place();
            restaurant.setLatitude(hmPlace.get("lat"));
            double lat = Double.parseDouble(hmPlace.get("lat"));
            restaurant.setLongitude(hmPlace.get("lng"));
            double lng = Double.parseDouble(hmPlace.get("lng"));
            String name = hmPlace.get("place_name");
            restaurant.setName(name);
            Log.d("Map", "place: " + name);
            String vicinity = hmPlace.get("vicinity");
            restaurant.setVicinity(vicinity);
            LatLng latLng = new LatLng(lat, lng);
            restaurant.setPlaceId(hmPlace.get("place_id"));
            restaurant.setOpenNow(hmPlace.get("open_now"));
            restaurant.setPhotoReference(hmPlace.get("photo_reference"));
            markerOptions.position(latLng);
            markerOptions.title(name);
            bookmarkPref = context.getSharedPreferences(BOOKMARK_PREF, Context.MODE_PRIVATE);
            bookmarked = bookmarkPref.getBoolean(hmPlace.get("place_id"), false);
            if(bookmarked){
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            }else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            }
            Marker m = mMap.addMarker(markerOptions);
            m.setTag(restaurant);
        }
    }
}