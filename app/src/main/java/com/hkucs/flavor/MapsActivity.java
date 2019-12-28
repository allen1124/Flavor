package com.hkucs.flavor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnCameraIdleListener {

    private static final String TAG = "MapsActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String BOOKMARK_PREF = "BOOKMARK_PREF";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float DEFAULT_ZOOM = 17.5f;
    private GoogleMap mMap;
    private Boolean locationPermissionGranted = false;
    private FusedLocationProviderClient fusedLocationClient;
    private AutocompleteSupportFragment autocompleteFragment;
    private ImageView gps = null;
    private CardView restaurantCard;
    private ImageView restaurantPhoto;
    private TextView restaurantName;
    private TextView restaurantVicinity;
    private TextView restaurantOpenNow;
    private Button restaurantDetail;
    private TextView restaurantLikeCount;
    private TextView restaurantDislikeCount;
    private RatingBar restaurantRating;
    private FloatingActionButton fabDirection;
    private LatLng lastSearchLatLng;
    private com.hkucs.flavor.Place restaurant;
    private PlacesClient placesClient;
    private Marker markerClicked;
    private boolean bookmarked = false;
    private HashMap<String, Boolean> markerList = new HashMap<>();

    SharedPreferences bookmarkPref;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    DatabaseReference dbRef = database.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        setContentView(R.layout.activity_maps);
        Places.initialize(getApplicationContext(),  getResources().getString(R.string.google_api_key));
        PlacesClient placesClient = Places.createClient(this);
        restaurantCard = (CardView) findViewById(R.id.restaurant_card);
        restaurantCard.setVisibility(View.GONE);
        restaurantPhoto = (ImageView) findViewById(R.id.photo_imageView);
        restaurantName = (TextView) findViewById(R.id.restaurant_name);
        restaurantVicinity = (TextView) findViewById(R.id.restaurant_vicinity);
        restaurantOpenNow = (TextView) findViewById(R.id.restaurant_open_now);
        restaurantDetail = (Button) findViewById(R.id.button_details);
        restaurantLikeCount = (TextView) findViewById(R.id.like_count_textView);
        restaurantDislikeCount = (TextView) findViewById(R.id.dislike_count_textView);
        restaurantRating = (RatingBar) findViewById(R.id.restaurant_ratingBar);
        fabDirection = (FloatingActionButton) findViewById(R.id.fab_direction);
        restaurantDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        fabDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(restaurant == null){
                    return;
                }
                Log.d(TAG, "onClick: Direct to: "+restaurant.getName());
                Intent intent = null;
                try {
                    intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+ URLEncoder.encode(restaurant.getName(), "utf-8")+"&destination_place_id="+restaurant.getPlaceId()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                startActivity(intent);
            }
        });
        autocompleteFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteFragment.setCountry("HK");
        autocompleteFragment.setHint(getResources().getString(R.string.query_hint));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
                mMap.clear();
                moveCamera(place.getLatLng(), DEFAULT_ZOOM);
                getNearbyRestaurants(place.getLatLng());
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });
        gps = (ImageView) findViewById(R.id.ic_gps);
        gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    getDeviceLocation();
                }else{
                    requestLocationEnabled();
                }
            }
        });
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        getLocationPermission();
        if(locationPermissionGranted == true)
            requestLocationEnabled();
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume: Back to MapsActivity");
        bookmarkPref = getSharedPreferences(BOOKMARK_PREF, Context.MODE_PRIVATE);
        if(markerClicked == null){
            return;
        }
        com.hkucs.flavor.Place restaurantClicked = (com.hkucs.flavor.Place) markerClicked.getTag();
        if(restaurantClicked == null){
            return;
        }
        bookmarked = bookmarkPref.getBoolean(restaurantClicked.getPlaceId(), false);
        if(bookmarked){
            markerClicked.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
        }else {
            markerClicked.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        }
    }

    private void requestLocationEnabled(){
        Log.d(TAG, "requestLocationEnabled: called, check GPS on/off");
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        final Activity activity = this;
        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    Log.d(TAG, "requestLocationEnabled: GPS is on");
                    getDeviceLocation();
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                resolvable.startResolutionForResult(activity,
                                        LocationRequest.PRIORITY_HIGH_ACCURACY);
                            } catch (IntentSender.SendIntentException e) {
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            break;
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "onActivityResult: GPS Enabled by user");
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                getDeviceLocation();
                                mMap.setMyLocationEnabled(true);
                            }
                        }, 2000);
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "onActivityResult: User rejected GPS request");
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;
        LatLng hongkong = new LatLng(22.28552, 114.15769);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hongkong, DEFAULT_ZOOM));
        mMap.setOnMarkerClickListener(this);
        mMap.setOnCameraIdleListener(this);
        mMap.setOnMapClickListener(this);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(locationPermissionGranted && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            getDeviceLocation();
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting device location");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Log.d(TAG, "getDeviceLocation: locationPermissionGranted: "+locationPermissionGranted.toString());
        if(locationPermissionGranted){
            fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if(task.isSuccessful() && task.getResult() != null){
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        Log.d(TAG, "onComplete: location get");
                        Location currentLocation = (Location) task.getResult();
                        Log.d(TAG, "onComplete: currentLocation "+currentLocation);
                        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        moveCamera(currentLatLng, DEFAULT_ZOOM);
                        Log.d(TAG, "onComplete: currentLatLng: "+Math.round(currentLatLng.latitude));
                        getNearbyRestaurants(currentLatLng);
                    }else{
                        Log.d(TAG, "onComplete: location is null");
                        Toast.makeText(MapsActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    
    private void moveCamera(LatLng latLng, float zoom){
        Log.d(TAG, "moveCamera: moving the camera to: ("+latLng.latitude+", "+latLng.longitude+")");
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    public void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permission");
        String[] permissions = {FINE_LOCATION};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "getLocationPermission: permission granted");
            locationPermissionGranted = true;
        }else{
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationPermissionGranted = false;
        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: permission failed");
                    return;
                }
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    locationPermissionGranted = true;
                }
            }
        }
    }

    private void getNearbyRestaurants(LatLng latLng){
        if(lastSearchLatLng == null){
            Log.d(TAG, "getNearbyRestaurants: latLng: "+latLng.toString());
            lastSearchLatLng = latLng;
            StringBuilder sbValue = new StringBuilder(nearbyUrlBuilder(latLng));
            PlacesTask placesTask = new PlacesTask(this, mMap, latLng, markerList);
            placesTask.execute(sbValue.toString());
        }else {
            if (Math.round(lastSearchLatLng.latitude*1000) != Math.round(latLng.latitude*1000) ||
                    Math.round(lastSearchLatLng.longitude*1000) != Math.round(latLng.longitude*1000)) {
                Log.d(TAG, "getNearbyRestaurants: latLng: "+latLng.toString());
                lastSearchLatLng = latLng;
                StringBuilder sbValue = new StringBuilder(nearbyUrlBuilder(latLng));
                PlacesTask placesTask = new PlacesTask(this, mMap, latLng, markerList);
                placesTask.execute(sbValue.toString());
            }
        }
    }

    public StringBuilder nearbyUrlBuilder(LatLng location){
        StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        sb.append("location=" + location.latitude + "," + location.longitude);
        sb.append("&radius=400");
        sb.append("&types=" + "restaurant");
        sb.append("&sensor=true");
        sb.append("&key="+getString(R.string.firebase_server_key));
        return sb;
    }

    public StringBuilder photoUrlBuilder(String reference){
        StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/photo?");
        sb.append("maxwidth="+1000);
        sb.append("&photoreference="+reference);
        sb.append("&key="+getString(R.string.firebase_server_key));
        return sb;
    }

    private Bitmap downloadPhoto(String strUrl) throws IOException {
        Bitmap bitmap=null;
        InputStream iStream = null;
        try{
            URL url = new URL(strUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            bitmap = BitmapFactory.decodeStream(iStream);
        }catch(Exception e){
            Log.d("Exception", e.toString());
        }finally{
            iStream.close();
        }
        return bitmap;
    }

    private class PhotoDownloadTask extends AsyncTask<String, Integer, Bitmap> {
        Bitmap bitmap = null;
        @Override
        protected Bitmap doInBackground(String... url) {
            try{
                bitmap = downloadPhoto(url[0]);
                try {
                    String filename = restaurant.getPlaceId()+".png";
                    FileOutputStream stream = openFileOutput(filename, Context.MODE_PRIVATE);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    stream.flush();
                    stream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            restaurantPhoto.setImageBitmap(result);
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        Log.d(TAG, "onMarkerClick: Marker clicked");
        markerClicked = marker;
        restaurant = (com.hkucs.flavor.Place)marker.getTag();
        restaurant.setRating(0.0);
        restaurantCard.setVisibility(View.VISIBLE);
        restaurantName.setText(marker.getTitle());
        restaurantRating.setRating(0f);
        restaurantVicinity.setText(restaurant.getVicinity());
        if(!restaurant.getPhotoReference().matches("")){
            Bitmap bmp = null;
            try {
                FileInputStream is = openFileInput(restaurant.getPlaceId()+".png");
                bmp = BitmapFactory.decodeStream(is);
                restaurantPhoto.setVisibility(View.VISIBLE);

                restaurantPhoto.setImageBitmap(bmp);
                Log.d(TAG, "onMarkerClick: load cached photo");
                is.close();
            } catch (Exception e) {
                Log.d(TAG, "onMarkerClick: No cached photo");
            }
            if(bmp == null) {
                restaurantPhoto.setImageResource(R.drawable.searchbar_bg);
                restaurantPhoto.setVisibility(View.VISIBLE);
                StringBuilder sbValue = new StringBuilder(photoUrlBuilder(restaurant.getPhotoReference()));
                PhotoDownloadTask photoDownloadTask = new PhotoDownloadTask();
                photoDownloadTask.execute(sbValue.toString());
            }
        }else{
            restaurantPhoto.setVisibility(View.GONE);
        }
        Log.d(TAG, "onMarkerClick: OpenNow: "+restaurant.getOpenNow());
        if(restaurant.getOpenNow() == null){
            restaurantOpenNow.setVisibility(View.INVISIBLE);
        }else{
            restaurantOpenNow.setVisibility(View.VISIBLE);
            if(restaurant.getOpenNow().matches("true")) {
                restaurantOpenNow.setText(R.string.open_now);
                restaurantOpenNow.setTextColor(getResources().getColor(R.color.open));
            }else {
                restaurantOpenNow.setText(R.string.closed_now);
                restaurantOpenNow.setTextColor(getResources().getColor(R.color.closed));
            }
        }

        // Read from the database
        dbRef.child("places").child(restaurant.getPlaceId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                com.hkucs.flavor.Place dbRestaurant = dataSnapshot.getValue(com.hkucs.flavor.Place.class);
                if(dbRestaurant == null){
                    Log.d(TAG, "get Restaurant DB: restaurant not found in db");
                    restaurant.setRating(0.0);
                    restaurant.setLikeCount(0);
                    restaurant.setDislikeCount(0);
                    writeNewRestaurant(restaurant);
                    return;
                }
                Log.d(TAG, "get Restaurant DB: " + dbRestaurant.getPlaceId());
                restaurantLikeCount.setText(String.valueOf(dbRestaurant.getLikeCount()));
                restaurant.setLikeCount(dbRestaurant.getLikeCount());
                restaurantDislikeCount.setText(String.valueOf(dbRestaurant.getDislikeCount()));
                restaurant.setDislikeCount(dbRestaurant.getDislikeCount());
                if(dbRestaurant.getRating() != null) {
                    Log.d(TAG, "get Restaurant DB: rating: "+dbRestaurant.getRating().toString());
                    restaurantRating.setRating(dbRestaurant.getRating().floatValue());
                    restaurant.setRating(dbRestaurant.getRating());
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

        restaurantDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: go place detail :"+restaurant.getPlaceId());
                Intent myIntent = new Intent(MapsActivity.this, RestaurantActivity.class);
                myIntent.putExtra("place", (Serializable) restaurant);
                if(!restaurant.getPhotoReference().matches("")){
                    myIntent.putExtra("photo", restaurant.getPlaceId());
                }
                startActivity(myIntent);
                MapsActivity.this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
        return false;
    }

    private void writeNewRestaurant(com.hkucs.flavor.Place restaurant){
        String key = restaurant.getPlaceId();
        Map<String, Object> postValues = restaurant.toMap();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/places/" + key, postValues);
        dbRef.updateChildren(childUpdates);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick: Map clicked");
        restaurantCard.setVisibility(View.GONE);
    }

    @Override
    public void onCameraIdle() {
        LatLng cameraPosition = mMap.getCameraPosition().target;
        getNearbyRestaurants(cameraPosition);
    }

}
