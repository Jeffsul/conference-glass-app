package com.syde461.group6.glassconference;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays more details about a person, when requested by user interaction (e.g., item tap).
 */
public class DetailsActivity extends Activity {
    private static final int ITEMS_PER_CARD = 3;

    private User user;
    private List<CardBuilder> cards;
    private List<CardTableData> cardData;

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
                View cardView = cards.get(i).getView(view, viewGroup);
                ViewGroup tableView = (ViewGroup) cardView.findViewById(R.id.detail_table);
                if (tableView != null) {
                    populateTableRows(tableView, cardData.get(i).title, cardData.get(i).rowItems);
                }
                return cardView;
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
        cards = new ArrayList<>();
        cardData = new ArrayList<>();

        Bitmap bmp = UserManager.getInstance().getBitmapFromMemCache(user.makeKey());
        if (bmp == null) {
            bmp = BitmapFactory.decodeResource(getResources(), R.drawable.profile_default);
        }
        // Employer and position card
        cards.add(new CardBuilder(this, CardBuilder.Layout.TEXT)
                .setFootnote(user.getName())
                .setText(user.getEmployer() + "\n" + user.getPosition()));
        cardData.add(new CardTableData());
        // Mutual connections card
        if (user.getConnections().length > 0) {
            CardBuilder card = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                    .setEmbeddedLayout(R.layout.detail_table)
                    .setFootnote(user.getName());
            cards.add(card);
            cardData.add(new CardTableData("Mutual Connections:", user.getConnections()));
        }
        // Papers card
        if (user.getPapers().length > 0) {
            CardBuilder card = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                    .setEmbeddedLayout(R.layout.detail_table)
                    .setFootnote(user.getName());
            cards.add(card);
            cardData.add(new CardTableData("Papers:", user.getPapers()));
        }
        // Interests card
        if (user.getInterests().length > 0) {
            CardBuilder card = new CardBuilder(this, CardBuilder.Layout.EMBED_INSIDE)
                    .setEmbeddedLayout(R.layout.detail_table)
                    .setFootnote(user.getName());
            cards.add(card);
            cardData.add(new CardTableData("Interests:", user.getInterests()));
        }
        // Image card
        cards.add(new CardBuilder(this, CardBuilder.Layout.TITLE)
                .setText(user.getName())
                .addImage(bmp));
    }

    private void populateTableRows(ViewGroup tableView, String title, String[] rowItems) {
        // The first row is the title.
        ViewGroup titleRowView = (ViewGroup) tableView.getChildAt(0);
        TextView titleTextView = (TextView) titleRowView.getChildAt(0);
        titleTextView.setTextColor(getResources().getColor(R.color.muted_text));
        titleTextView.setText(title);

        int endItemIndex = Math.min(ITEMS_PER_CARD, rowItems.length);
        for (int i = 0; i < ITEMS_PER_CARD; i++) {
            ViewGroup rowView = (ViewGroup) tableView.getChildAt(i + 1);

            // The layout contains four fixed rows, so we need to hide the later ones if there are
            // not four items on this card. We need to make sure to update the visibility in both
            // cases though if the card has been recycled.
            if (i < endItemIndex) {
                TextView textView = (TextView) rowView.getChildAt(0);
                textView.setText(rowItems[i]);
                rowView.setVisibility(View.VISIBLE);
            } else {
                rowView.setVisibility(View.INVISIBLE);
            }
        }
    }

    private static class CardTableData {
        public final String title;
        public final String[] rowItems;

        public CardTableData() {
            title = null;
            rowItems = null;
        }

        public CardTableData(String title, String[] rowItems) {
            this.title = title;
            this.rowItems = rowItems;
        }
    }
}
