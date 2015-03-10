package com.syde461.group6.glassconference;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.syde461.group6.glassconference.util.GpsLiveCardService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The base activity for the app in Browse mode. The user is presented with a view of nearby
 * users, according to relative location.
 */
public class BrowseActivity extends Activity {
    public static final String TAG = "glassconference";

    private static final long EXIT_INTERACTION_MODE_DELAY = TimeUnit.SECONDS.toMillis(10);

    private static final long MAX_UPDATE_DELAY = TimeUnit.SECONDS.toMillis(8);

    private static final int OFFSET = 200;

    private static final double MAX_DEGREES_BEFORE_UPDATE = 10;

    private long lastUpdateRequest = Long.MAX_VALUE;

    private GestureDetector gestureDetector;

    private OrientationManager orientationManager;
    private UserManager userManager;
    private UserCardBuilder[] userCards = new UserCardBuilder[0];

    private CardScrollView cardScrollView;
    private UserCardAdapter adapter;

    private double updateBearing;
    private double userBearing;
    private Location location;

    private Object lock = new Object();
    private int nextIndex;

    private boolean interactionMode;

    private User[] newUsers;

    private Handler handler;
    private Runnable exitInteractionModeRunnable = new Runnable() {
        @Override
        public void run() {
            interactionMode = false;
            Log.e(TAG, "Exiting interaction mode.");
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        l("Starting demo!");
        // Initialize the demo with N fake users
        ServerFacade.initializeDemo(OrientationManager.DEFAULT_LOCATION, 8, 0);

        // Stop the display from dimming.
        // TODO(jeffsul): Implement timeout?
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        orientationManager = OrientationManager.initialize(this);
        userManager = UserManager.getInstance();
        ServerFacade.addListener(userManager);
        orientationManager.addListener(new OrientationManager.OrientationListener() {
            @Override
            public void onLocationChanged(Location location) {
                BrowseActivity.this.location = location;
                updateBearing = BrowseActivity.this.userBearing;
                // TODO(jeffsul): Disassociate this from UI/BrowseActivity.
                makeLocationUpdateRequest();
            }

            @Override
            public void onOrientationChanged(double bearing) {
                if (Math.abs(bearing - updateBearing) > MAX_DEGREES_BEFORE_UPDATE) {
                    l("Max Degrees before update reached.");
                    updateBearing = bearing;
                    makeLocationUpdateRequest();
                }
                if (interactionMode && Math.abs(bearing - userBearing) < 45) {
                    l("Not updating orientation: Interaction Mode.");
                    return;
                }
                synchronized (lock) {
                    int newIndex = getIndexByBearing(bearing, userCards);
                    int oldIndex = mod(cardScrollView.getSelectedItemPosition(), userCards.length);
                    l(String.format("Old: %d to New: %d (bearing=" + Math.round(bearing) + ")", oldIndex, newIndex));
                    if (newIndex != oldIndex) {
                        nextIndex = newIndex;
                        int animateTo = cardScrollView.getSelectedItemPosition()
                                + (newIndex - oldIndex);
                        cardScrollView.animate(animateTo, CardScrollView.Animation.NAVIGATION);
                        l(String.format("Orientation change: navigating from %d to %d (animate %d)",
                                oldIndex, newIndex, animateTo));
                    }
                    userBearing = bearing;
                }
            }
        });
        orientationManager.startTracking();

        cardScrollView = new CardScrollView(this);
        adapter = new UserCardAdapter();
        cardScrollView.setAdapter(adapter);
        cardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(BrowseActivity.this, DetailsActivity.class);
                intent.putExtra("user",
                        userCards[mod(cardScrollView.getSelectedItemPosition(), userCards.length)]
                                .getUser());
                startActivity(intent);
            }
        });
        cardScrollView.activate();
        setContentView(cardScrollView);

        handler = new Handler();
        gestureDetector = new GestureDetector(this).setBaseListener(
                new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.LONG_PRESS) {
                    openOptionsMenu();
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT || gesture == Gesture.SWIPE_RIGHT) {
                    Log.e(TAG, "Entering interaction mode.");
                    interactionMode = true;
                    handler.removeCallbacks(exitInteractionModeRunnable);
                    handler.postDelayed(exitInteractionModeRunnable, EXIT_INTERACTION_MODE_DELAY);
                }
                return false;
            }
        });

        userManager.addListener(new UserManager.UserChangeListener() {
            @Override
            public void onUserChange(User[] users) {
                synchronized (lock) {
                    newUsers = users;
                    handler.removeCallbacks(updateUsersRunnable);
                    if (!updateUsers()) {
                        handler.postDelayed(updateUsersRunnable, 1000);
                    }
                }
            }
        });
    }

    private Runnable updateUsersRunnable = new Runnable() {
        @Override
        public void run() {
            if (!updateUsers()) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void makeLocationUpdateRequest() {
        if (location != null) {
            int selectedId = 0;
            if (userCards.length > 0) {
                selectedId = userCards[getIndexByBearing(updateBearing, userCards)].getUser().getId();
            }
            l("Making location update request: " + updateBearing);
            ServerFacade.updateLocation(location, updateBearing, selectedId);
        }
    }

    private static void l(String msg) {
        Log.e(TAG, msg);
    }

    private synchronized boolean updateUsers() {
        int position = mod(cardScrollView.getSelectedItemPosition(), userCards.length);
        if (position != nextIndex) {
            l(String.format("updateUsers: does not match: %d %d", position, nextIndex));
            return false;
        }
        boolean isEmpty = userCards.length == 0;
        User selectedUser = !isEmpty ? userCards[nextIndex].getUser() : newUsers[0];
        l("Selected User: " + selectedUser.getName());
        l("Next Index: " + nextIndex);
        userCards = new UserCardBuilder[newUsers.length];
        int index = -1;
        for (int i = 0; i < newUsers.length; i++) {
            if (newUsers[i].equals(selectedUser)) {
                index = i;
                break;
            }
        }
        l("Found selected user: " + index);
        // TODO(jeffsul): What if number of users changes?
        String logMessage = "";
        for (int i = 0; i < userCards.length; i++) {
            userCards[mod(i + nextIndex, newUsers.length)] = new UserCardBuilder(
                    BrowseActivity.this,
                    newUsers[mod(i + index, newUsers.length)]);
        }
        for (int i = 0; i < userCards.length; i++) {
            logMessage += i + " " + newUsers[i].getName() + " " + Math.round(newUsers[i].getBearing()) + ", ";
        }
        l("Updating adapter: " + logMessage);
        adapter.notifyDataSetChanged();
        if (isEmpty) {
            cardScrollView.setSelection(OFFSET);
        }
        return true;
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
        handler.removeCallbacks(updateUsersRunnable);
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

    private class UserCardAdapter extends CardScrollAdapter {
        @Override
        public int getCount() {
            return userCards.length > 0 ? Integer.MAX_VALUE : 0;
        }

        @Override
        public Object getItem(int i) {
            return userCards[mod(i, userCards.length)];
        }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            UserCardBuilder userCardBuilder = userCards[mod(i, userCards.length)];
            return userCards[mod(i, userCards.length)].getView(convertView, parent,
                    90 + (float)diff(userCardBuilder.getUser().getBearing(), userBearing));
        }

        @Override
        public int getPosition(Object o) {
            return userManager.indexOf((User) o);
        }
    }

    /**
     * From: https://github.com/googleglass/gdk-compass-sample/blob/master/app/src/main/java/com/
     *              google/android/glass/sample/compass/util/MathUtils.java
     *
     * Calculates {@code a mod b} in a way that respects negative values (for example,
     * {@code mod(-1, 5) == 4}, rather than {@code -1}).
     *
     * @param a the dividend
     * @param b the divisor
     * @return {@code a mod b}
     */
    private static int mod(int a, int b) {
        return b == 0 ? 0 : (a % b + b) % b;
    }

    private static final double SCOPE = 10;

    public static int getIndexByBearing(double bearing, UserCardBuilder[] userCards) {
        User[] users = new User[userCards.length];
        for (int i = 0; i < userCards.length; i++) {
            users[i] = userCards[i].getUser();
        }
        return getIndexByBearing(bearing, users);
    }

    public static int getIndexByBearing(double bearing, User[] users) {
        int l = users.length;
        if (l == 0) {
            return 0;
        }
        int index = -1;
        double[] bearings = new double[l];
        for (int i = 0; i < l; i++) {
            bearings[i] = users[i].getBearing();
        }
        for (int i = 0; i < l; i++) {
            int a = i;
            int b = (i + 1) % l;
            double diffA = diff(bearing, bearings[a]);
            double diffB = diff(bearings[b], bearing);
            if (diffA >= 0 && diffB >= 0) {
                l("Found interval: " + users[a].getName() + " (" + diffA + ") " + users[b].getName() + " (" + diffB + ")");
                if (diffA < SCOPE && diffB < SCOPE) {
                    return users[a].getDistance() < users[b].getDistance() ? a : b;
                } else {
                    return diffA < diffB ? a : b;
                }
            }
        }
        return index;
    }

    public static double diff(double deg1, double deg2) {
        double d = deg1 - deg2;
        if (d <= -180) {
            d += 360;
        }
        if (d >= 180) {
            d -= 360;
        }
        return d;
    }
}
