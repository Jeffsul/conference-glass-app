package com.syde461.group6.glassconference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.syde461.group6.glassconference.util.GpsLiveCardService;

/**
 * The base activity for the app in Browse mode. The user is presented with a view of nearby
 * users, according to relative location.
 */
public class BrowseActivity extends Activity {
    public static final String TAG = "glassconference";

    public static final int VERSION = 2;

    private static final long MAX_UPDATE_DELAY = 500;
    private long lastUpdateRequest = 0L;

    private BrowseView browseView;

    private GestureDetector gestureDetector;
    private AudioManager audioManager;

    private OrientationManager orientationManager;
    private UserManager userManager;

    private Location location = OrientationManager.DEFAULT_LOCATION;

    private Handler handler;
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            makeLocationUpdateRequest();
            handler.postDelayed(this, MAX_UPDATE_DELAY);
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.browse);
        browseView = (BrowseView) findViewById(R.id.browse_view);
        browseView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BrowseActivity.this, DetailsActivity.class);
                intent.putExtra("user", browseView.getSelectedUser());
                startActivity(intent);
            }
        });

        l("Starting demo!");
        // Initialize the demo with N fake users
        ServerFacade.initializeDemo(location, 12, 0);

        // Stop the display from dimming.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        orientationManager = OrientationManager.initialize(this);
        userManager = UserManager.getInstance();
        ServerFacade.addListener(userManager);
        orientationManager.addListener(new OrientationManager.OrientationListener() {
            @Override
            public void onLocationChanged(Location location) {
                BrowseActivity.this.location = location;
                //makeLocationUpdateRequest();
            }

            @Override
            public void onOrientationChanged(double bearing) {
//                if (System.currentTimeMillis() - lastUpdateRequest > MAX_UPDATE_DELAY) {
//                    makeLocationUpdateRequest();
//                }
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
                        if (gesture == Gesture.SWIPE_DOWN) {
                            audioManager.playSoundEffect(Sounds.DISMISSED);
                            openOptionsMenu();
                            return true;
                        } else if (gesture == Gesture.SWIPE_LEFT || gesture == Gesture.SWIPE_RIGHT) {
                            Log.e(TAG, "Entering interaction mode.");
                            audioManager.playSoundEffect(Sounds.SELECTED);
                            browseView.switchSelection(gesture == Gesture.SWIPE_LEFT ? -1 : 1);
                            return true;
                        } else if (gesture == Gesture.TAP) {
                            audioManager.playSoundEffect(Sounds.TAP);
                            Intent intent = new Intent(BrowseActivity.this, DetailsActivity.class);
                            intent.putExtra("user", browseView.getSelectedUser());
                            startActivity(intent);
                            return true;
                        }
                        return false;
                    }
                });
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        userManager.addListener(new UserManager.UserChangeListener() {
            @Override
            public void onUserChange(User[] users) {
                browseView.setNearbyPeople(users);
            }
        });

        handler = new Handler();
        handler.post(updateRunnable);
    }

    private void makeLocationUpdateRequest() {
        lastUpdateRequest = System.currentTimeMillis();
        if (location != null && orientationManager != null) {
            l("Making location update request.");
            ServerFacade.updateLocation(location, orientationManager.getBearing(),
                    browseView.getSelectedUserId());
        }
    }

    private static void l(String msg) {
        Log.e(TAG, msg);
    }

    @Override
    protected void onResume() {
        // Start/stopping tracking seems to cause issues after leaving Details activity.
        // Orientation is reset, or something.
        //orientationManager.startTracking();
        super.onResume();
    }

    @Override
    protected void onPause() {
        //orientationManager.stopTracking();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        orientationManager.stopTracking();
        orientationManager = null;
        handler.removeCallbacks(updateRunnable);
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
            case R.id.action_stop:
                finish();
                return true;
        }
        return false;
    }
}
