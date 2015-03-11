package com.syde461.group6.glassconference;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays more details about a person, when requested by user interaction (e.g., item tap).
 */
public class DetailsActivity extends Activity {

    private User user;
    private List<CardBuilder> cards;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user = getIntent().getParcelableExtra("user");

        createCards();

        CardScrollView cardScrollView = new CardScrollView(this);
        cardScrollView.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return cards.size();
            }

            @Override
            public Object getItem(int i) {
                return cards.get(i);
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                return cards.get(i).getView(view, viewGroup);
            }

            @Override
            public int getPosition(Object o) {
                return cards.indexOf(o);
            }
        });
        cardScrollView.activate();
        setContentView(cardScrollView);
    }

    private void createCards() {
        cards = new ArrayList<CardBuilder>();

        Bitmap bmp = UserManager.getInstance().getBitmapFromMemCache(user.makeKey());
        if (bmp == null) {
            bmp = BitmapFactory.decodeResource(getResources(), R.drawable.profile_default);
        }
        cards.add(new CardBuilder(this, CardBuilder.Layout.AUTHOR)
                .setHeading(user.getName())
                .setSubheading(user.getPosition())
                .setIcon(bmp)
                .setText(user.getEmployer()));
        if (user.getConnections() != null && user.getConnections().trim().length() > 0) {
            cards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                    .setText("Mutual connections: " + user.getConnections())
                    .setFootnote(user.getName()));
        }
        cards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setText("Papers: " + user.getPapers())
                .setFootnote(user.getName()));
        cards.add(new CardBuilder(this, CardBuilder.Layout.TITLE)
                .setText(user.getName())
                .addImage(bmp));
    }
}
