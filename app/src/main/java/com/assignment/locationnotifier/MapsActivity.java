package com.assignment.locationnotifier;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.assignment.locationnotifier.databinding.ActivityMapsBinding;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.RuntimeRemoteException;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final int FINE_LOCATION_ACCESS_REQUEST_CODE = 1010;
    private final int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 1011;
    private final String TAG = "Maps Activity";
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private EditText latLongEditText, radiusEditText, addressEditText;
    private LatLng latLng;
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.activityMapRoot);
        latLongEditText = binding.activityMapsCoordinatesEditText;
        radiusEditText = binding.activityMapsRadiusEditText;
        addressEditText = binding.activityMapsAddressEditText;
        binding.activityMapsSubmitButton.setOnClickListener(this::onSubmitClicked);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(checkPermission())
            getUserLocation();
        else
            requestLocationPermissions();
    }

    private void requestLocationPermissions() {
        if (Build.VERSION.SDK_INT < 29)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
        else if (Build.VERSION.SDK_INT == 29)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
        else if (Build.VERSION.SDK_INT > 29)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= 29)
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        else
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Build.VERSION.SDK_INT < 29 && requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkPermission())
                    mMap.setMyLocationEnabled(true);
                Toast.makeText(this, "Permissions Granted\nYou can add geofence...", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onRequestPermissionsResult: " + "background permission granted");
            } else {
                Log.e(TAG, "onRequestPermissionsResult: " + "background permission DENIED");
                Toast.makeText(this, "Permission Denied\nThe app will not work", Toast.LENGTH_SHORT).show();
            }
        } else if (Build.VERSION.SDK_INT == 29 && requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkPermission())
                    mMap.setMyLocationEnabled(true);
                Log.e(TAG, "onRequestPermissionsResult: " + "Android 10 Background permission granted");
                Toast.makeText(this, "Permissions Granted\nYou can add geofences...", Toast.LENGTH_SHORT).show();
                getUserLocation();
            } else {
                Log.e(TAG, "onRequestPermissionsResult: " + "Android 10 Background permission DENIED");
                Toast.makeText(this, "Background location access is necessary for geofences to trigger. \nThe app will not work", Toast.LENGTH_SHORT).show();
            }
        } else if (Build.VERSION.SDK_INT > 29 && requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "onRequestPermissionsResult: " + "Android 11 FINE permission granted");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
            } else {
                Log.e(TAG, "onRequestPermissionsResult: " + "Android 11 fINE permission DENIED");
                Toast.makeText(this, "Permission Denied\nApp will not work unless granted location permissions", Toast.LENGTH_LONG).show();
            }
        } else if (Build.VERSION.SDK_INT > 29 && requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions Granted\nYou can add geofence...", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onRequestPermissionsResult: " + "Android 11 Background permission granted");
                getUserLocation();
            } else {
                Log.e(TAG, "onRequestPermissionsResult: " + "Android 11 Background permission DENIED");
                Toast.makeText(this, "Permissions Denied \nPlease select 'Allow All the time' option\nWe need background permissions to run geofence", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getUserLocation() {
        if (checkPermission()) {
            LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            if (isLocationEnabled(locationManager)) {
                try {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17));
                            addressEditText.setText(String.format("Current Location : %s,%s", userLocation.latitude, userLocation.longitude));
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {

                        }

                        @Override
                        public void onProviderEnabled(String provider) {

                        }

                        @Override
                        public void onProviderDisabled(String provider) {

                        }

                    }, null);
                } catch (SecurityException e) {
                    Log.e(TAG, "getLastLocation: " + e.getMessage());
                    e.printStackTrace();
                } catch (RuntimeRemoteException runtimeRemoteException) {
                    runtimeRemoteException.printStackTrace();
                }
            } else {
                Snackbar.make(binding.activityMapRoot, "Please turn on your location", Snackbar.LENGTH_INDEFINITE)
                        .setAction("TURN ON", v -> {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, FINE_LOCATION_ACCESS_REQUEST_CODE);
                        })
                        .show();
            }
        } else{
            Toast.makeText(this, "Background location access is necessary for geofences to trigger. \nThe app will not work", Toast.LENGTH_LONG).show();
        }
}

    private boolean isLocationEnabled(LocationManager locationManager) {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void onSubmitClicked(View view) {
        String inputCoordinates = latLongEditText.getText().toString();
        String radiusString = radiusEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(inputCoordinates) && inputCoordinates.contains(",") && !TextUtils.isEmpty(radiusString)) {
            String[] latLngString = inputCoordinates.split(",");
            latLng = new LatLng(Double.parseDouble(latLngString[0]), Double.parseDouble(latLngString[1]));
            float radius = Float.parseFloat(radiusString);
            setMarkers(latLng, radius);
            setAddress();
        }
    }

    private void setMarkers(LatLng latLng, float radius) {
        mMap.clear();
        addMarker(latLng);
        addCircle(latLng, radius);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        addGeofence(latLng, radius);
    }

    private void addMarker(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker_icon));
        mMap.addMarker(markerOptions);
    }

    private void addCircle(LatLng latLng, float radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(219, 243, 247, 71));
        circleOptions.fillColor(Color.argb(219, 98, 98, (float) 0.37));
        circleOptions.strokeWidth(10);
        mMap.addCircle(circleOptions);
    }

    private void addGeofence(LatLng latLng, float radius) {
        String GEOFENCE_ID = "IDGJHGHJBNCVC";
        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();
        if (checkPermission())
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(unused -> Log.e(TAG, "onSuccess: Geofence Created"))
                    .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + geofenceHelper.getErrorString(e)));
    }

    private void setAddress() {
        List<Address> addresses;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            String address = addresses.get(0).getAddressLine(0);
            if (!TextUtils.isEmpty(address)) {
                addressEditText.setText(address);
            } else
                addressEditText.setText("No Address found for this Coordinates");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
//28.6937,77.1106