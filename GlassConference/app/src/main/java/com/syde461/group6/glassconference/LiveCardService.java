package com.syde461.group6.glassconference;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.RemoteViews;

import java.util.List;

/**
 * A {@link Service} that publishes a {@link LiveCard} in the timeline.
 */
public class LiveCardService extends Service {

    private static final String LIVE_CARD_TAG = "GlassConferenceHelperService";

    private final Handler handler = new Handler();
    private final UpdateLiveCardRunnable updateTask = new UpdateLiveCardRunnable();

    private static final long MIN_DELAY = 1000;
    private static final long MIN_DISTANCE = 0;

    private static final long AGE_CUTOFF = 10000;
    private long lastUpdate;

    private LocationManager locationManager;

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lastUpdate = System.currentTimeMillis();
            updateLiveCardView(location);
        }
        @Override
        public void onProviderDisabled(String provider) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

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

            liveCardViews = new RemoteViews(getPackageName(), R.layout.live_card);
            liveCard.setViews(liveCardViews);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, LiveCardMenuActivity.class);
            liveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            liveCard.publish(PublishMode.REVEAL);

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setBearingRequired(false);
            criteria.setSpeedRequired(false);
            List<String> providers = locationManager.getProviders(criteria, true);
            for (String provider : providers) {
                locationManager.requestLocationUpdates(provider,
                        MIN_DELAY, MIN_DISTANCE, locationListener, Looper.getMainLooper());
            }

            handler.post(updateTask);
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
            updateTask.stop();
        }
        super.onDestroy();
    }

    private synchronized void updateLiveCardView(Location location) {
        long age = System.currentTimeMillis() - location.getTime();
        liveCardViews.setTextViewText(R.id.lat, Double.toString(location.getLatitude()));
        liveCardViews.setTextViewText(R.id.lng, Double.toString(location.getLongitude()));
        liveCardViews.setTextViewText(R.id.accuracy, Float.toString(location.getAccuracy()));
        liveCardViews.setTextViewText(R.id.age, Long.toString(age));
        liveCard.setViews(liveCardViews);
    }

    private class UpdateLiveCardRunnable implements Runnable {
        private boolean stopped = false;

        @Override
        public void run() {
            if (stopped) {
                return;
            }
            if (System.currentTimeMillis() - lastUpdate > AGE_CUTOFF) {
                Location location = locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    updateLiveCardView(location);
                }
            }
            handler.postDelayed(this, AGE_CUTOFF);
        }

        public void stop() {
            stopped = true;
        }
    }
}
