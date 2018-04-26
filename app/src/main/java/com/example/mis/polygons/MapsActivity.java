package com.example.mis.polygons;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private final int MY_PERMISSIONS_REQUEST_ACESS_LOCATION = 0;
    Location currentLocation;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    private List<Marker> markers;
    Button btnClearMap;
    Button btnStartPoly;
    boolean isPolyDrawn = false;
    Polygon poly;
    Marker centroidMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        btnClearMap = (Button) findViewById(R.id.clear);
        btnClearMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //Clear the map and markers list
                btnStartPoly.setText("Start Polygon");
                mMap.clear();
                markers.clear();
            }
        });

        btnStartPoly = (Button) findViewById(R.id.butt);
        btnStartPoly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPolyDrawn && markers.size() >= 3) {
                    isPolyDrawn = true;
                    btnStartPoly.setText("End Polygon"); //Change text to End polygon
                    PolygonOptions polygonOptions = new PolygonOptions();
                    for (Marker marker : markers) {
                        polygonOptions.add(marker.getPosition());
                    }
                    LatLng centroid = getCentroid(polygonOptions.getPoints());//Reference (1) in "references" file
                    poly = mMap.addPolygon(polygonOptions.fillColor(Color.argb(20, 50, 0, 255)));
                    double preciseArea = SphericalUtil.computeArea(polygonOptions.getPoints());// Reference(2) in "references" file
                    double roundedArea;
                    String unit;
                    if (preciseArea > 1000000) {
                        roundedArea = ((double) Math.round(preciseArea / 10000)) / 100; //Area with two decimals of precision
                        unit = " km²";
                    } else {
                        roundedArea = ((double) Math.round(preciseArea * 100)) / 100.0; //Area with two decimals of precision
                        unit = " m²";
                    }

                    centroidMarker = mMap.addMarker(
                            new MarkerOptions().position(centroid)
                                               .title(String.valueOf(roundedArea) + unit)
                                               .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));//Puts the area value in the centroid marker
                } else if(markers.size() < 3) {
                    Toast.makeText(getApplicationContext(), "At least 3 markers are needed to draw a polygon!", Toast.LENGTH_LONG).show();
                } else if(isPolyDrawn){//Removing polygon and everything related
                    isPolyDrawn = false;
                    btnStartPoly.setText("Start Polygon");
                    poly.remove();
                    centroidMarker.remove();
                }
            }

        });
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);//Initialize Shared Preferences
        if(markers == null){
            markers = new ArrayList<Marker>();//Initialize the markerList
        }

        buildGoogleApiClient();//Create GoogleAPIClient.
        mGoogleApiClient.connect();//Connect mGoogleApiClient

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map); // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) { //Manipulates the map once available. This callback is triggered when the map is ready to be used. This is where we can add markers or lines, add listeners or move the camera.
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACESS_LOCATION);
        //References (3) (4) in "references" file
        loadMarkers(); //load the markers list
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_LONG).show();
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACESS_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){// Considering that if request is cancelled, the result arrays are empty.
                        currentLocation = initializeCurrentLocation(currentLocation);
                    if (currentLocation == null)
                        Toast.makeText(this, "No GPS signal. Try again later.", Toast.LENGTH_LONG).show();
                    else {
                        mMap.setOnMapLongClickListener(this);
                    }
                } else {
                    this.finish();
                    System.exit(0);
                }
            }
        }
    }

    public Location initializeCurrentLocation(Location location) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
        }
        location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            LatLng myPosition = new LatLng(location.getLatitude(), location.getLongitude());
            Marker localPosMarker = mMap.addMarker(new MarkerOptions().position(myPosition).title("My position"));
            markers.add(localPosMarker);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 6));
            return location;
        }
        else {
            return null;
        }
    }

    public void onMapLongClick(LatLng point) {//Reference (5) in "references" file
        EditText editText = (EditText) findViewById(R.id.editText);
        String text = String.valueOf(editText.getText());
        Marker marker = mMap.addMarker(new MarkerOptions().position(point).title(text).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))); //creates markers
        markers.add(marker); //add the marker to the markers list
        editText.setText(null);   //clear the editText
    }

    private void SavePreferences(){//Reference (6) in "references" file
        editor = sharedPref.edit();
        editor.putInt("listSize", markers.size());
        for(int i = 0; i < markers.size(); i++){
            editor.putFloat("lat"+i, (float) markers.get(i).getPosition().latitude);
            editor.putFloat("long"+i, (float) markers.get(i).getPosition().longitude);
            editor.putString("title"+i, markers.get(i).getTitle());
        }
        editor.apply(); //or commit()
    }

    private void loadMarkers() {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        int size = sharedPreferences.getInt("listSize", 0);
        if(size > 0) {
            for (int i = 0; i < size; i++) {
                double latitude = (double) sharedPreferences.getFloat("lat" + i, 0);
                double longitude = (double) sharedPreferences.getFloat("long" + i, 0);
                String title = sharedPreferences.getString("title" + i, "NULL");
                markers.add(mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(title)));
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        SavePreferences();
    }

    private LatLng getCentroid(List<LatLng> positions) {//References (7) (8) in "references" file
        double centerX = 0;
        double centerY = 0;
        for (LatLng position : positions) {
            centerX += position.latitude;
            centerY += position.longitude;
        }
        LatLng centroid = new LatLng(centerX / positions.size(), centerY / positions.size());
        Toast.makeText(this, "Centroid Lat: "+centroid.latitude+" Long: "+centroid.longitude, Toast.LENGTH_LONG).show();
        return centroid;
    }
}