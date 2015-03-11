package com.syde461.group6.glassconference;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.syde461.group6.glassconference.util.GpsLiveCardService;

/**
 * The base activity for the app in Browse mode. The user is presented with a view of nearby
 * users, according to relative location.
 */
public class BrowseActivity extends Activity {
    public static final String TAG = "glassconference";

    private static final long MAX_UPDATE_DELAY = 1000;
    private long lastUpdateRequest = Long.MIN_VALUE;

    private BrowseView browseView;

    private GestureDetector gestureDetector;

    private OrientationManager orientationManager;
    private UserManager userManager;

    private Location location;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.browse);
        browseView = (BrowseView) findViewById(R.id.browse_view);

        l("Starting demo!");
        // Initialize the demo with N fake users
        ServerFacade.initializeDemo(OrientationManager.DEFAULT_LOCATION, 16, 0);

        // Stop the display from dimming.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        orientationManager = OrientationManager.initialize(this);
        userManager = UserManager.getInstance();
        ServerFacade.addListener(userManager);
        orientationManager.addListener(new OrientationManager.OrientationListener() {
            @Override
            public void onLocationChanged(Location location) {
                BrowseActivity.this.location = location;
                // TODO(jeffsul): Disassociate this from UI/BrowseActivity.
                makeLocationUpdateRequest();
            }

            @Override
            public void onOrientationChanged(double bearing) {
                if (System.currentTimeMillis() - lastUpdateRequest > MAX_UPDATE_DELAY) {
                    l("Max Degrees before update reached.");
                    makeLocationUpdateRequest();
                }
                browseView.setBearing((float) bearing);
            }
        });
        orientationManager.startTracking();

        browseView.setOrientationManager(orientationManager);
        browseView.setUserManager(userManager);

        gestureDetector = new GestureDetector(this).setBaseListener(
                new GestureDetector.BaseListener() {
                    @Override
                    public boolean onGesture(Gesture gesture) {
                        if (gesture == Gesture.LONG_PRESS) {
                            openOptionsMenu();
                            return true;
                        } else if (gesture == Gesture.SWIPE_LEFT || gesture == Gesture.SWIPE_RIGHT) {
                            Log.e(TAG, "Entering interaction mode.");
                        }
                        return false;
                    }
                });

        userManager.addListener(new UserManager.UserChangeListener() {
            @Override
            public void onUserChange(User[] users) {
                browseView.setNearbyPeople(users);
            }
        });
    }

    private void makeLocationUpdateRequest() {
        if (location != null) {
            //l("Making location update request.");
            ServerFacade.updateLocation(location, orientationManager.getBearing(),
                    browseView.getSelectedUserId());
            lastUpdateRequest = System.currentTimeMillis();
        }
    }

    private static void l(String msg) {
        Log.e(TAG, msg);
    }

    @Override
    protected void onResume() {
        orientationManager.startTracking();
        super.onResume();
    }

    @Override
    protected void onPause() {
        orientationManager.stopTracking();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        orientationManager.stopTracking();
        orientationManager = null;
        super.onDestroy();
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return gestureDetector.onMotionEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Provide menu access to GPS utility.
        getMenuInflater().inflate(R.menu.browse, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_location_utility:
                // Launch GPS utility.
                // TODO(jeffsul): Make this dependent on startup flags.
                startService(new Intent(this, GpsLiveCardService.class));
                return true;
            default:
                return false;
        }
    }
}
