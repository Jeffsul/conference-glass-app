package com.syde461.group6.glassconference;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The base activity for the app in Browse mode. The user is presented with a view of nearby
 * users, according to relative location.
 */
public class BrowseActivity extends Activity {

    private GestureDetector gestureDetector;

    private OrientationManager orientationManager;
    private UserManager userManager;
    private UserCardBuilder[] userCards = new UserCardBuilder[0];
    private int rootIndex = 0;

    private CardScrollView cardScrollView;

    private double userBearing = 125;

    private Object lock = new Object();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Stop the display from dimming.d
        // TODO(jeffsul): Implement timeout?
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        orientationManager = OrientationManager.initialize(this);
        userManager = UserManager.getInstance();
        ServerFacade.addListener(userManager);
        orientationManager.addListener(new OrientationManager.OrientationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // TODO(jeffsul): Disassociate this from UI/BrowseActivity.
                ServerFacade.updateLocation(location);
            }

            @Override
            public void onOrientationChanged(double bearing) {
                synchronized (lock) {
                    if (Math.abs(bearing - userBearing) < 10) {
                        return;
                    }
                    Log.e("glassconference", "BEARING: " + bearing);
                    //userBearing = bearing;
                    int newIndex = userManager.getIndexByBearing(userBearing) + rootIndex - mod(rootIndex, userCards.length);
                    int oldIndex = cardScrollView.getSelectedItemPosition();
                    if (newIndex != oldIndex) {
                        cardScrollView.animate(newIndex, CardScrollView.Animation.NAVIGATION);
                        Log.e("glassconference", "Orientation change: navigating from " + oldIndex + " to " + newIndex);
                    }
                }
            }
        });
        orientationManager.startTracking();

        cardScrollView = new CardScrollView(this);
        final UserCardAdapter adapter = new UserCardAdapter();
        cardScrollView.setAdapter(adapter);
        cardScrollView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(BrowseActivity.this, DetailsActivity.class);
                intent.putExtra("user",
                        userCards[cardScrollView.getSelectedItemPosition()].getUser());
                startActivity(intent);
            }
        });
        cardScrollView.activate();
        setContentView(cardScrollView);

        gestureDetector = new GestureDetector(this).setBaseListener(
                new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.LONG_PRESS) {
                    openOptionsMenu();
                    return true;
                }
                return false;
            }
        });

        userManager.addListener(new UserManager.UserChangeListener() {
            @Override
            public void onUserChange(User[] users) {
                synchronized (lock) {
                    int posn = cardScrollView.getSelectedItemPosition();
                    int index = rootIndex == 0 ? 0 : mod(posn, rootIndex);
                    rootIndex = posn;
                    Log.e("glassconference", "Updating root index: " + rootIndex);
                    User selectedUser = userCards.length > 0 ? userCards[index].getUser() : users[0];
                    userCards = new UserCardBuilder[users.length];
                    int newIndex = userManager.getIndexByBearing(userBearing);
                    Log.e("glassconference", "New index: " + newIndex + " " + users[newIndex].getName());
                    for (int i = 0; i < userCards.length; i++) {
                        userCards[i] = new UserCardBuilder(BrowseActivity.this,
                                users[mod(i + newIndex, users.length)]);
                    }
                    if (selectedUser != null && userCards.length > 0
                            && !users[newIndex].equals(selectedUser)) {
                        cardScrollView.animate(rootIndex, CardScrollView.Animation.DELETION);
                        Log.e("glassconference", "Deletion animation: " + rootIndex);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        });
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

    private class UserCardAdapter extends CardScrollAdapter {
        @Override
        public int getCount() {
            return userCards.length > 0 ? Integer.MAX_VALUE : 0;
        }

        @Override
        public Object getItem(int i) {
            return userCards[mod(i - rootIndex, userCards.length)];
        }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            return userCards[mod(i - rootIndex, userCards.length)].getView(convertView, parent);
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
}
