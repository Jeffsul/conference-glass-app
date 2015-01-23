package com.syde461.group6.glassconference;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeff on 23/01/2015.
 */
public class BrowseActivity extends Activity {

    private List<CardBuilder> userCards;

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
        userCards = new ArrayList<CardBuilder>();

        for (int i = 1; i <= 10; i++) {
            userCards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                    .setText("User " + i));
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
