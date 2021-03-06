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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

    private static final int[] MALE_PROFILES = {
            R.drawable.m_1, R.drawable.m_2, R.drawable.m_3, R.drawable.m_4, R.drawable.m_5,
            R.drawable.profile_anson,
            R.drawable.profile_eric,
            R.drawable.profile_kyle,
            R.drawable.profile_jeff
    };
    private static final int[] FEMALE_PROFILES = {
            R.drawable.f_1, R.drawable.f_2, R.drawable.f_3, R.drawable.f_4, R.drawable.f_5,
            R.drawable.f_6, R.drawable.f_7,
            R.drawable.profile_catherine
    };

    private static boolean fake = false;
    private static FakeEnvironment fakeEnvironment = new FakeEnvironment();

    private static List<UserUpdateListener> userListeners = new ArrayList<>();

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
        updateLocation(location, bearing, 0);
    }

    public static void updateLocation(Location location, double bearing, int selectedId) {
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
            new UpdateLocationTask(location, bearing, selectedId).execute(UPDATE_LOCATION_URL);
        }
    }

    private static class UpdateLocationTask extends AsyncTask<String, String, String> {
        private final Location location;
        private final double bearing;
        private final int selectedId;

        public UpdateLocationTask(Location location, double bearing, int selectedId) {
            this.location = location;
            this.bearing = bearing;
            this.selectedId = selectedId;
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
                jsonObject.put("selected_id", selectedId);
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

        @Override
        protected void onPostExecute(String result) {
            JSONArray resp;
            try {
                resp = new JSONArray(result);
            } catch (JSONException e) {
                Log.e("glassconference",
                        "Error parsing JSON in updateLocationTask [result = " + result + "]", e);
                return;
            }
            List<User> users = new ArrayList<>();
            for (int i = 0; i < resp.length(); i++) {
                try {
                    JSONObject obj = resp.getJSONObject(i);
                    int id = obj.getInt("id");
                    User.Builder builder = new User.Builder().id(id);
                    if (obj.has("name")) {
                        builder.name(obj.getString("name"));
                    }
                    if (obj.has("first_name")) {
                        builder.firstName(obj.getString("first_name"));
                    }
                    if (obj.has("gender")) {
                        User.Gender gender = obj.getString("gender").equals("Female")
                                ? User.Gender.F : User.Gender.M;
                        builder.gender(gender);
                    }
                    if (obj.has("company")) {
                        builder.employer(obj.getString("company"));
                    }
                    if (obj.has("position")) {
                        builder.position(obj.getString("position"));
                    }
                    if (obj.has("connections")) {
                        builder.connections(parseList(obj.getString("connections")));
                    }
                    if (obj.has("papers")) {
                        builder.papers(parseList(obj.getString("papers")));
                    }
                    if (obj.has("interest")) {
                        builder.interests(parseList(obj.getString("interest")));
                    }
                    if (obj.has("presenter")) {
                        builder.presenter(obj.getBoolean("presenter"));
                    }
                    if (obj.has("picture")) {
                        builder.imageUrl(obj.getString("picture"));
                    }
                    User user = builder.build();
                    if (obj.has("bearing")) {
                        user.setBearing(obj.getDouble("bearing"));
                    }
                    if (obj.has("distance")) {
                        user.setDistance(obj.getDouble("distance"));
                    }
                    users.add(user);
                } catch (JSONException e) {
                    Log.e("glassconference",
                            "Error parsing user JSON in updateLocationTask [i = " + i + "]", e);
                }
            }
            notifyUpdatedUserList(users.toArray(new User[users.size()]));
        }
    }

    public static void addListener(UserUpdateListener listener) {
        userListeners.add(listener);
    }

    private static void notifyUpdatedUserList(User[] users) {
        Arrays.sort(users, new Comparator<User>() {
            @Override
            public int compare(User user1, User user2) {
                return user1.getBearing() >= user2.getBearing() ? 1 : -1;
            }
        });
        for (UserUpdateListener listener : userListeners) {
            listener.onUserListUpdate(users);
        }
    }

    public static interface UserUpdateListener {
        void onUserListUpdate(User[] users);
    }

    private static String[] parseList(String listString) {
        if (listString == null || listString.trim().length() == 0) {
            return new String[0];
        }
        return listString.split(",");
    }
}
