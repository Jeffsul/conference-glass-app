package com.syde461.group6.glassconference;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Based on com.google.android.glass.sample.compass.OrientationManager
 * from https://github.com/googleglass/gdk-compass-sample
 */
public class OrientationManager {

    private List<OrientationListener> listeners = new ArrayList<OrientationListener>();

    private double bearing;
    private final SensorManager sensorManager;
    private final float[] rotationMatrix;
    private final float[] orientation;

    private float pitch;

    private static OrientationManager instance;

    public static OrientationManager initialize(SensorManager sensorManager) {
        if (instance == null) {
            instance = new OrientationManager(sensorManager);
        } else {
            Log.e("glassconference", "OrientationManager initialized twice!");
        }
        return instance;
    }

    public static OrientationManager getInstance() {
        return instance;
    }

    private OrientationManager(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        this.rotationMatrix = new float[16];
        this.orientation = new float[9];
        registerSensorListener();
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
                bearing = magneticHeading + 180;
                notifyListeners();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

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

    public void addListener(OrientationListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (OrientationListener listener : listeners) {
            listener.onOrientationChanged(bearing);
        }
    }

    public static interface OrientationListener {
        void onOrientationChanged(double bearing);
    }
}
