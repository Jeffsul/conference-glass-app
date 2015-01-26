package com.syde461.group6.glassconference;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.syde461.group6.glassconference.util.GpsLiveCardService;

import java.util.ArrayList;
import java.util.List;

/**
 * The base activity for the app in Browse mode. The user is presented with a view of nearby
 * users, according to relative location.
 */
public class BrowseActivity extends Activity {

    private List<UserCardBuilder> userCards;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUserCards();

        CardScrollView cardScrollView = new CardScrollView(this);
        UserCardAdapter adapter = new UserCardAdapter();
        cardScrollView.setAdapter(adapter);
        cardScrollView.activate();
        setContentView(cardScrollView);
    }

    private void initUserCards() {
        userCards = new ArrayList<UserCardBuilder>();

        for (int i = 1; i <= 10; i++) {
            userCards.add(new UserCardBuilder(this, new User("User " + i)));
        }
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
            return userCards.size();
        }

        @Override
        public Object getItem(int i) {
            return userCards.get(i);
        }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            return userCards.get(i).getView(convertView, parent);
        }

        @Override
        public int getPosition(Object o) {
            return userCards.indexOf(o);
        }
    }
}
