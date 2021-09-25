package com.assignment.locationnotifier;

import android.Manifest;
import android.app.Activity;
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
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.Objects;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final int FINE_LOCATION_ACCESS_REQUEST_CODE = 1010;
    private final int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 1011;
    private final String TAG = "Maps Activity";
    private final MarkerOptions markerOptions = new MarkerOptions();
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private EditText latLongEditText, radiusEditText, addressEditText;
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;
    private ActivityResultLauncher<Intent> locationSettingsResultLauncher;
    private SharedPreferencesConfig preferencesConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.activityMapRoot);
        preferencesConfig = new SharedPreferencesConfig(this);
        latLongEditText = binding.activityMapsCoordinatesEditText;
        radiusEditText = binding.activityMapsRadiusEditText;
        addressEditText = binding.activityMapsAddressEditText;
        binding.activityMapsSubmitButton.setOnClickListener(this::onSubmitClicked);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(this);

        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);
        locationSettingsResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_CANCELED)
                        getUserLocation();
                });
        radiusEditText.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE))
                binding.activityMapsSubmitButton.performClick();
            return false;
        });
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                if (!Character.isDigit(source.charAt(i)) && source.charAt(i) != '-' && source.charAt(i) != '.' && source.charAt(i) != ',' && source.charAt(i) != ' ') {
                    Log.e(TAG, "onCreate: filter " + source + "\t" + start + "\t" + end);
                    return source.toString().replace("" + source.charAt(i), "");
                }
            }
            return null;
        };
        latLongEditText.setFilters(new InputFilter[]{filter});

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
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (checkPermission()) {
            if (preferencesConfig.readLocation() != null && preferencesConfig.readRadius() != null) {
                latLongEditText.setText(preferencesConfig.readLocation());
                radiusEditText.setText(preferencesConfig.readRadius());
                binding.activityMapsSubmitButton.performClick();
            } else
                getUserLocation();
        } else
            requestLocationPermissions();
    }

    private void requestLocationPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
        else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
        else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        else
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) || (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE)) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkPermission())
                    mMap.setMyLocationEnabled(true);
                Toast.makeText(this, "Permissions Granted\nYou can add geofence...", Toast.LENGTH_SHORT).show();
                getUserLocation();
                Log.e(TAG, "onRequestPermissionsResult: " + "background permission granted");
            } else {
                Log.e(TAG, "onRequestPermissionsResult: " + "background permission DENIED");
                Toast.makeText(this, "Permission Denied\nThe app will not work", Toast.LENGTH_SHORT).show();
            }
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "onRequestPermissionsResult: " + "Android 11 FINE permission granted");
                //For Android 11 we have to perform incremental request to get background location.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
            } else {
                Log.e(TAG, "onRequestPermissionsResult: " + "Android 11 fINE permission DENIED");
                Toast.makeText(this, "Permission Denied\nApp will not work unless granted location permissions", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getUserLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (isLocationEnabled(locationManager)) {
            try {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17));
                        addMarker(userLocation);
                        setAddress(userLocation);
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
                        locationSettingsResultLauncher.launch(intent);
                    })
                    .show();
        }
    }

    private boolean isLocationEnabled(LocationManager locationManager) {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void onSubmitClicked(View view) {
        String inputCoordinates = latLongEditText.getText().toString();
        String twoDoublesRegularExpression = "-?[1-9][0-9]?(\\.[0-9]+)?,\\s*-?[1-9][0-9][0-9]?(\\.[0-9]+)?";
        String radiusString = radiusEditText.getText().toString().trim();

        if (!TextUtils.isEmpty(inputCoordinates) && inputCoordinates.matches(twoDoublesRegularExpression))
            if (!TextUtils.isEmpty(radiusString)) {
                String[] latLngString = inputCoordinates.split(",");
                LatLng coordinates = new LatLng(Double.parseDouble(latLngString[0]), Double.parseDouble(latLngString[1].trim()));
                float radius = Float.parseFloat(radiusString);
                preferencesConfig.writeLocation(inputCoordinates);
                preferencesConfig.writeRadius(radiusString);
                setMarkers(coordinates, radius);
                setAddress(coordinates);
            } else
                Toast.makeText(this, "Radius can't be blank\nPlease enter radius", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "Incorrect coordinates detected\nPlease enter valid coordinates", Toast.LENGTH_LONG).show();
    }

    private void setMarkers(LatLng latLng, float radius) {
        mMap.clear();
        addMarker(latLng);
        addCircle(latLng, radius);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        addGeofence(latLng, radius);
    }

    private void addMarker(LatLng latLng) {
        markerOptions.position(latLng)
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
        final String GEOFENCE_ID = "IDGJHGHJBNCVC";
        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();
        if (checkPermission())
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(unused -> Log.e(TAG, "onSuccess: Geofence Created"))
                    .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + geofenceHelper.getErrorString(e)));
    }

    private void setAddress(LatLng coordinates) {
        List<Address> addresses;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1);
            String address = addresses.get(0).getAddressLine(0);
            if (!TextUtils.isEmpty(address)) {
                addressEditText.setText(address);
            } else
                addressEditText.setText(R.string.address_error);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            Toast.makeText(this, R.string.address_error, Toast.LENGTH_SHORT).show();
            addressEditText.setText(String.format("%s, %s", coordinates.latitude, coordinates.longitude));
        }
    }
}
//28.6937,77.1106 for testing purpose only