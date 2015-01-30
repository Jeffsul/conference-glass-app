package com.syde461.group6.glassconference;

import android.os.Handler;
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

    private static OrientationManager instance;

    public static OrientationManager getInstance() {
        if (instance == null) {
            instance = new OrientationManager();
        }
        return instance;
    }

    private OrientationManager() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bearing = Math.random() * 360;
                Log.e("glassconference", "BEARING: " + bearing);
                notifyListeners();
                handler.postDelayed(this, 5000);
            }
        }, 5000);
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
