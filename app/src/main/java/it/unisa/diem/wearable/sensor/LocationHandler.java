package it.unisa.diem.wearable.sensor;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

public class LocationHandler implements SensorHandler {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Activity activity;
    private static final int MIN_TIME_MS = 30000;
    private static final int MIN_DISTANCE_M = 20;

    public LocationHandler(LocationManager locationManager, LocationListener locationListener, Activity activity) {
        this.locationManager = locationManager;
        this.locationListener = locationListener;
        this.activity = activity;
    }

    /**
     * This method checks the permission for location and,
     * if it is present, the relative listener is registered.
     *
     * @return True if the permission for location is granted, false otherwise
     */
    @Override
    public Boolean registerListener() {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_MS,
                    MIN_DISTANCE_M, locationListener);
            return true;
        } else
            return false;
    }

    /**
     * This method unregister the listeners for all location providers.
     */
    @Override
    public void unregisterListener() {
        //if (locationManager!=null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            locationManager.removeUpdates(locationListener);
    }
}
