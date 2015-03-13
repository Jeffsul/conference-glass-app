package com.syde461.group6.glassconference;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.syde461.group6.glassconference.util.MathUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks device location and orientation.
 *
 * Based on com.google.android.glass.sample.compass.OrientationManager
 * from https://github.com/googleglass/gdk-compass-sample
 */
public class OrientationManager {
    /**
     * The minimum distance desired between location notifications.
     */
    private static final long METERS_BETWEEN_LOCATIONS = 0;
    /**
     * The minimum elapsed time desired between location notifications.
     */
    private static final long MILLIS_BETWEEN_LOCATIONS = 1500;

    /**
     * The sensors used by the compass are mounted in the movable arm on Glass. Depending on how
     * this arm is rotated, it may produce a displacement ranging anywhere from 0 to about 12
     * degrees. Since there is no way to know exactly how far the arm is rotated, we just split the
     * difference.
     */
    private static final int ARM_DISPLACEMENT_DEGREES = 6;

    public static final Location DEFAULT_LOCATION = new Location("");
    static {
        DEFAULT_LOCATION.setLatitude(43.483126983087026);
        DEFAULT_LOCATION.setLongitude(-80.53417685449173);
        DEFAULT_LOCATION.setAltitude(300);
        DEFAULT_LOCATION.setTime(System.currentTimeMillis());
    }
    private static boolean fake = true;

    private Handler handler;

    private List<OrientationListener> listeners = new ArrayList<OrientationListener>();

    private double bearing;
    private final LocationManager locationManager;
    private final SensorManager sensorManager;
    private final float[] rotationMatrix;
    private final float[] orientation;
    private Location location = DEFAULT_LOCATION;
    private GeomagneticField geomagneticField;
    private float pitch;

    private boolean isTracking;

    private static OrientationManager instance;

    public static OrientationManager initialize(Context context) {
        if (instance == null) {
            LocationManager lm = (LocationManager)
                    context.getSystemService(Context.LOCATION_SERVICE);
            SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            instance = new OrientationManager(lm, sm);
        } else {
            Log.e("glassconference", "OrientationManager initialized twice!");
        }
        return instance;
    }

    public static OrientationManager getInstance() {
        return instance;
    }

    private OrientationManager(LocationManager locationManager, SensorManager sensorManager) {
        this.locationManager = locationManager;
        this.sensorManager = sensorManager;
        this.rotationMatrix = new float[16];
        this.orientation = new float[9];
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            OrientationManager.this.location = location;
            updateGeomagneticField();

            notifyLocationChange();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {}

        @Override
        public void onProviderEnabled(String s) {}

        @Override
        public void onProviderDisabled(String s) {}
    };

    private void registerLocationListener() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);

        List<String> providers = locationManager.getProviders(criteria, true);
        for (String provider : providers) {
            locationManager.requestLocationUpdates(provider, MILLIS_BETWEEN_LOCATIONS,
                    METERS_BETWEEN_LOCATIONS, locationListener, Looper.getMainLooper());
        }
    }

    private SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                // Get the current heading, then notify listeners.
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X,
                        SensorManager.AXIS_Z, rotationMatrix);
                SensorManager.getOrientation(rotationMatrix, orientation);

                // Store the pitch (used to display a message indicating that the user's head
                // angle is too steep to produce reliable results.
                pitch = (float) Math.toDegrees(orientation[1]);

                // Convert heading from magnetic to true north.
                float magneticHeading = (float) Math.toDegrees(orientation[0]);
                bearing = MathUtil.mod(computeTrueNorth(magneticHeading), 360.0f)
                        - ARM_DISPLACEMENT_DEGREES;

                notifyOrientationChange();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            // TODO(jeffsul): Notify about accuracy change.
        }
    };

    private void registerSensorListener() {
        sensorManager.registerListener(sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
    }

    private Runnable fakeUpdater = new Runnable() {
        @Override
        public void run() {
            notifyLocationChange();
            handler.postDelayed(this, MILLIS_BETWEEN_LOCATIONS);
        }
    };

    public void startTracking() {
        if (!isTracking) {
            registerSensorListener();
            if (!fake) {
                registerLocationListener();
            } else {
                handler = new Handler();
                handler.post(fakeUpdater);
                updateGeomagneticField();
            }

            // TODO(jeffsul): Check last known location on start.

            isTracking = true;
        }
    }

    public void stopTracking() {
        if (isTracking) {
            sensorManager.unregisterListener(sensorListener);
            if (!fake) {
                locationManager.removeUpdates(locationListener);
            } else {
                handler.removeCallbacks(fakeUpdater);
            }
            isTracking = false;
        }
    }

    public void addListener(OrientationListener listener) {
        listeners.add(listener);
    }

    private void notifyOrientationChange() {
        for (OrientationListener listener : listeners) {
            listener.onOrientationChanged(bearing);
        }
    }

    private void notifyLocationChange() {
        for (OrientationListener listener : listeners) {
            listener.onLocationChanged(location);
        }
    }

    public double getBearing() {
        return bearing;
    }

    public static interface OrientationListener {
        void onLocationChanged(Location location);
        void onOrientationChanged(double bearing);
    }

    private void updateGeomagneticField() {
        geomagneticField = new GeomagneticField((float) location.getLatitude(),
                (float) location.getLongitude(), (float) location.getAltitude(),
                location.getTime());
    }

    private float computeTrueNorth(float heading) {
        if (geomagneticField != null) {
            return heading + geomagneticField.getDeclination();
        }
        return heading;
    }
}
