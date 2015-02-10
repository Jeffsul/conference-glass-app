package com.syde461.group6.glassconference;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Facade concealing details of server requests.
 * Allows responses to be faked for testing.
 */
public class ServerFacade {
    private static final String INIT_DEMO_URL =
            "http://conference-glass.herokuapp.com/web/initialize";
    private static final String UPDATE_LOCATION_URL =
            "http://conference-glass.herokuapp.com/web/distances";

    private static final long FAKE_DELAY = TimeUnit.SECONDS.toMillis(1);

    private static boolean fake = false;
    private static FakeEnvironment fakeEnvironment = new FakeEnvironment();

    private static List<UserUpdateListener> userListeners =
            new ArrayList<UserUpdateListener>();

    // Disallow instantiation.
    private ServerFacade() {}

    public static void initializeDemo(Location location, int n, float bearing) {
        new InitializeDemoTask(location, n, bearing).execute(INIT_DEMO_URL);
    }

    private static class InitializeDemoTask extends AsyncTask<String, String, String> {
        private final Location location;
        private final int n;
        private final float bearing;

        public InitializeDemoTask(Location location, int n, float bearing) {
            this.location = location;
            this.n = n;
            this.bearing = bearing;
        }

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(uri[0]);
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("latitude", location.getLatitude());
                jsonObject.put("longitude", location.getLongitude());
                jsonObject.put("n", n);
                jsonObject.put("direction", bearing);
            } catch (JSONException e) {
                Log.e("glassconference", "Error creating JSON", e);
            }
            try {
                httpPost.setEntity(new StringEntity(jsonObject.toString()));
            } catch (UnsupportedEncodingException e) {
                Log.e("glassconference", "Error setting POST entity.", e);
            }
            // Set request headers so server knows what type of data to handle.
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            try {
                ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(HttpResponse httpResponse) throws IOException {
                        return EntityUtils.toString(httpResponse.getEntity());
                    }
                };
                return httpClient.execute(httpPost, responseHandler);
            } catch (Exception e) {
                Log.e("glassconference", "Error executing POST request.", e);
            }
            return null;
        }
    }

    public static void updateLocation(Location location, double bearing) {
        if (fake) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    User[] users = new User[0];
                    if (fake) {
                        users = fakeEnvironment.getUsers();
                    }
                    notifyUpdatedUserList(users);
                }
            }, FAKE_DELAY);
        } else {
            new UpdateLocationTask(location, bearing).execute(UPDATE_LOCATION_URL);
        }
    }

    private static class UpdateLocationTask extends AsyncTask<String, String, String> {
        private final Location location;
        private final double bearing;

        public UpdateLocationTask(Location location, double bearing) {
            this.location = location;
            this.bearing = bearing;
        }

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(uri[0]);
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("latitude", location.getLatitude());
                jsonObject.put("longitude", location.getLongitude());
                jsonObject.put("direction", bearing);
            } catch (JSONException e) {
                Log.e("glassconference", "Error creating JSON", e);
            }
            try {
                httpPost.setEntity(new StringEntity(jsonObject.toString()));
            } catch (UnsupportedEncodingException e) {
                Log.e("glassconference", "Error setting POST entity.", e);
            }
            // Set request headers so server knows what type of data to handle.
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            try {
                ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(HttpResponse httpResponse) throws IOException {
                        String resp = EntityUtils.toString(httpResponse.getEntity());
                        return resp;
                    }
                };
                return httpClient.execute(httpPost, responseHandler);
            } catch (Exception e) {
                Log.e("glassconference", "Error executing POST request.", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject resp = new JSONObject(result);
                User[] users = new User[10];
                for (int i = 0; i < 10; i++) {
                    JSONObject userObj = resp.getJSONObject(Integer.toString(i));
                    users[i] = new User(userObj.getString("name"), "Test", "Test");
                    users[i].setBearing(userObj.getDouble("bearing"));
                }
                notifyUpdatedUserList(users);
                Log.e("glassconference", resp.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addListener(UserUpdateListener listener) {
        userListeners.add(listener);
    }

    private static void notifyUpdatedUserList(User[] users) {
        for (UserUpdateListener listener : userListeners) {
            listener.onUserListUpdate(users);
        }
    }

    public static interface UserUpdateListener {
        void onUserListUpdate(User[] users);
    }
}
