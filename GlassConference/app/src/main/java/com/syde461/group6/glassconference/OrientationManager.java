package com.syde461.group6.glassconference;

import android.content.Context;
import android.hardware.SensorManager;
import android.location.LocationManager;

/**
 * Created by Jeff on 17/10/2014.
 * Based on com.google.android.glass.sample.compass.OrientationManager
 * from https://github.com/googleglass/gdk-compass-sample
 */
public class OrientationManager {

    private static OrientationManager instance;

    public static OrientationManager getInstance() {
        if (instance == null) {
            instance = new OrientationManager();
        }
        return instance;
    }

    private OrientationManager() {
        //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }
}
