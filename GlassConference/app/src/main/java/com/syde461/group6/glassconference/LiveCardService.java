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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
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

    // When to indicate that location updates have stopped.
    private static final long AGE_CUTOFF = 10000;
    // Stores the time in ms that the currently displayed location found.
    private long lastUpdate;

    // Minimum number of location records to post to the server in one HTTP request.
    private static final int MIN_BATCH_SIZE = 10;
    private static final String SERVER_POST_PATH = "http://jeffsul.com/post";
    private List<Location> locationRecords =
            Collections.synchronizedList(new ArrayList<Location>());

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
        synchronized (locationRecords) {
            locationRecords.add(location);
            if (locationRecords.size() >= MIN_BATCH_SIZE) {
                new SendBatchedLocationsRequest().execute(SERVER_POST_PATH);
            }
        }
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

    private class SendBatchedLocationsRequest extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(uri[0]);

            JSONArray holder = new JSONArray();
            synchronized (locationRecords) {
                for (Location location : locationRecords) {
                    try {
                        JSONObject locationObj = new JSONObject();
                        locationObj.put("latitude", Double.toString(location.getLatitude()));
                        locationObj.put("longitude", Double.toString(location.getLongitude()));
                        locationObj.put("accuracy", Float.toString(location.getAccuracy()));
                        locationObj.put("time", Long.toString(location.getTime()));
                        locationObj.put("provider", location.getProvider());
                        holder.put(locationObj);
                    } catch (JSONException e) {
                        Log.e("glass-conf", "JSON error.", e);
                    }
                }
                locationRecords.clear();
            }
            try {
                httpPost.setEntity(new StringEntity(holder.toString()));
            } catch (UnsupportedEncodingException e) {
                Log.e("glass-conf", "Error setting POST entity.", e);
            }
            // Set request headers so server knows what type of data to handle.
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            try {
                return httpClient.execute(httpPost, new BasicResponseHandler());
            } catch (Exception e) {
                Log.e("glass-conf", "Error executing POST request.", e);
            }
            return null;
        }
    }
}
