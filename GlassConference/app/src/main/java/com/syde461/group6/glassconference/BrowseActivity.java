package com.syde461.group6.glassconference;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

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

    private CardScrollView cardScrollView;

    private double userBearing;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                userBearing = bearing;
                int newIndex = userManager.getIndexByBearing(bearing);
                if (newIndex != cardScrollView.getSelectedItemPosition()) {
                    cardScrollView.animate(newIndex, CardScrollView.Animation.NAVIGATION);
                }
            }
        });
        orientationManager.startTracking();

        cardScrollView = new CardScrollView(this);
        final UserCardAdapter adapter = new UserCardAdapter();
        cardScrollView.setAdapter(adapter);
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
                int index = cardScrollView.getSelectedItemPosition();
                if (index >= users.length) {
                    cardScrollView.animate(users.length - 1, CardScrollView.Animation.DELETION);
                }
                User selectedUser = userCards.length > 0 ? userCards[index].getUser() : null;
                userCards = new UserCardBuilder[users.length];
                for (int i = 0; i < userCards.length; i++) {
                    userCards[i] = new UserCardBuilder(BrowseActivity.this, users[i]);
                }
                int newIndex = userManager.getIndexByBearing(userBearing);
                if (selectedUser != null && userCards.length > 0
                        && !users[newIndex].equals(selectedUser)) {
                    cardScrollView.animate(index, CardScrollView.Animation.DELETION);
                } else if (newIndex != index) {
                    cardScrollView.animate(newIndex, CardScrollView.Animation.NAVIGATION);
                } else {
                    adapter.notifyDataSetChanged();
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
            return userCards.length;
        }

        @Override
        public Object getItem(int i) {
            return userCards[i];
        }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            return userCards[i].getView(convertView, parent);
        }

        @Override
        public int getPosition(Object o) {
            return userManager.indexOf((User) o);
        }
    }
}
