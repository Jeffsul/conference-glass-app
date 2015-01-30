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

    private CardScrollView cardScrollView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        orientationManager = OrientationManager.initialize(this);
        userManager = UserManager.getInstance();
        orientationManager.addListener(new OrientationManager.OrientationListener() {
            @Override
            public void onLocationChanged(Location location) {}

            @Override
            public void onOrientationChanged(double bearing) {
                int newIndex = userManager.getIndexByBearing(bearing);
                if (newIndex != cardScrollView.getSelectedItemPosition()) {
                    cardScrollView.animate(newIndex, CardScrollView.Animation.NAVIGATION);
                }
            }
        });
        orientationManager.startTracking();

        cardScrollView = new CardScrollView(this);
        UserCardAdapter adapter = new UserCardAdapter();
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
            public void onUserChange(List<User> users) {

            }
        });
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
        private Map<User, UserCardBuilder> userCardBuilderMap =
                new HashMap<User, UserCardBuilder>();

        @Override
        public int getCount() {
            return userManager.size();
        }

        @Override
        public Object getItem(int i) {
            return userManager.get(i);
        }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            User user = userManager.get(i);
            UserCardBuilder userCardBuilder;
            if (!userCardBuilderMap.containsKey(user)) {
                userCardBuilder = new UserCardBuilder(BrowseActivity.this, user);
            } else {
                userCardBuilder = userCardBuilderMap.get(user);
            }
            return userCardBuilder.getView(convertView, parent);
        }

        @Override
        public int getPosition(Object o) {
            return userManager.indexOf((User) o);
        }
    }
}
