package com.syde461.group6.glassconference;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;

/**
 * A {@link Service} that publishes a {@link LiveCard} in the timeline.
 */
public class BrowseService extends Service {

    private static final String LIVE_CARD_TAG = "GlassConferenceBrowseService";

    private LiveCard liveCard;
    private RemoteViews liveCardViews;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (liveCard == null) {
            liveCard = new LiveCard(this, LIVE_CARD_TAG);
            liveCardViews = new RemoteViews(getPackageName(), R.layout.browse);
            liveCard.setViews(liveCardViews);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, BrowseMenuActivity.class);
            liveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            liveCard.publish(LiveCard.PublishMode.REVEAL);
        } else {
            liveCard.navigate();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (liveCard != null && liveCard.isPublished()) {
            liveCard.unpublish();
            liveCard = null;
        }
        super.onDestroy();
    }
}
