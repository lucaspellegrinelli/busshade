package com.lucaspellegrinelli.busao;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;

import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback,
        LocationListener {

    public static final String TAG = MapsFragment.class.getSimpleName();

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    double currentLatitude = 0.0;
    double currentLongitude = 0.0;

    boolean hasZoomedIn = false;

    ConfigurationStep configurationStep = ConfigurationStep.NULL;

    LatLng startLocation;
    Marker startLocationMarker;
    LatLng endLocation;
    Marker endLocationMarker;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_maps, container, false);

        setUpMapIfNeeded();

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(AppIndex.API).build();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(1 * 1000);

        return fragmentView;
    }

    public void startLocationRequest(){
        removeEndMarker();
        configurationStep = ConfigurationStep.REQUEST_START_LOCATION;
    }

    public boolean startLocationIsSet(){
        return startLocationMarker != null && startLocationMarker.isVisible() && startLocation != null;
    }

    public void addStartMarker(LatLng position){
        removeStartMarker();

        startLocationMarker = addMarker(position, R.color.startPositionMarker);
    }

    public void removeStartMarker(){
        if(startLocationMarker != null)
            startLocationMarker.remove();
        startLocationMarker = null;
    }

    public void endLocationRequest(){
        configurationStep = ConfigurationStep.REQUEST_END_LOCATION;
    }

    public boolean endLocationIsSet(){
        return endLocationMarker != null && endLocationMarker.isVisible() && endLocation != null;
    }

    public void addEndMarker(LatLng position){
        removeEndMarker();

        endLocationMarker = addMarker(position, R.color.endPositionMarker);
    }

    public void timeRequest(){
        configurationStep = ConfigurationStep.REQUEST_TIME;
    }

    public void resetStartEndLocation(){
        startLocation = null;
        endLocation = null;
    }

    public LatLng getStartLocation(){
        return startLocation;
    }

    public LatLng getEndLocation(){
        return endLocation;
    }

    private void removeEndMarker(){
        if(endLocationMarker != null)
            endLocationMarker.remove();
        endLocationMarker = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            SupportMapFragment fs = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map));
            fs.getMapAsync(this);
        }
    }

    private void setUpMap() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(configurationStep == ConfigurationStep.REQUEST_START_LOCATION){
                    startLocation = latLng;
                    addStartMarker(startLocation);

                    if(startLocationIsSet()){
                        ((MainActivity)getActivity()).setForwardButtonEnabledState(true);
                    }

                }else if(configurationStep == ConfigurationStep.REQUEST_END_LOCATION){
                    endLocation = latLng;
                    addEndMarker(endLocation);

                    if(endLocationIsSet()){
                        ((MainActivity)getActivity()).setForwardButtonEnabledState(true);
                    }
                }
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { } else { }
                return;
            }
        }
    }

    private void handleNewLocation(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        if(!hasZoomedIn) {
            zoomToLocation(latLng);
            hasZoomedIn = true;
        }
    }

    public void zoomToLocation(LatLng location){
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 14.0f));
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location == null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else {
                handleNewLocation(location);
            }
        }catch(SecurityException s){
            System.out.print("Erro: " + s.getMessage());
        }
    }
    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(getActivity(), CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    private GoogleMap.OnMyLocationChangeListener myLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            handleNewLocation(location);
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Maps Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://com.lucaspellegrinelli.busao/http/host/path")
        );

        AppIndex.AppIndexApi.start(mGoogleApiClient, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Maps Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://com.lucaspellegrinelli.busao/http/host/path")
        );

        AppIndex.AppIndexApi.end(mGoogleApiClient, viewAction);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap != null) {
            setUpMap();
            mMap.setOnMyLocationChangeListener(myLocationChangeListener);
        }
    }

    private Marker addMarker(LatLng position, int color){
        Bitmap bitmapIcon = Utilities.getColoredDrawable(getContext(), R.drawable.ic_location_on_white_36dp, color);

        if(position == null)
            return null;

        final Marker m = mMap.addMarker(new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmapIcon)));

        return m;
    }
}