package com.hkucs.flavor;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class PlacesTask extends AsyncTask<String, Integer, String> {

    String data = null;
    private HashMap<String, Boolean> markerList;
    private Context context;
    private GoogleMap mMap;
    private LatLng latLng;

    public PlacesTask(Context context, GoogleMap mMap, LatLng latLng, HashMap<String, Boolean> markerList){
        this.context = context;
        this.mMap = mMap;
        this.latLng = latLng;
        this.markerList = markerList;
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            data = downloadUrl(strings[0]);
            Log.d("doInBackground", "doInBackground: "+data);
        } catch (Exception e) {
            Log.d("Background Task", e.toString());
        }
        return data;
    }

    @Override
    protected void onPostExecute(String result) {

        ParserTask parserTask = new ParserTask(context, mMap, latLng, markerList);

        // Start parsing the Google places in JSON format
        // Invokes the "doInBackground()" method of the class ParserTask
        parserTask.execute(result);
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}