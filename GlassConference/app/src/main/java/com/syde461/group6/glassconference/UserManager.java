package com.syde461.group6.glassconference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;

import com.syde461.group6.glassconference.util.ImageUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Singleton storage for User model objects.
 */
public class UserManager implements ServerFacade.UserUpdateListener {

    private User[] users;
    private User[] getLastUsers;

    private List<UserChangeListener> listeners = new ArrayList<>();

    private LruCache<String, Bitmap> memoryCache;

    private UserManager() {
        users = new User[0];

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }

    private class DownloadImageTask extends AsyncTask<String, String, Bitmap> {
        private final User user;

        public DownloadImageTask(User user) {
            this.user = user;
        }

        @Override
        protected Bitmap doInBackground(String... imageUrl) {
            Bitmap bmp = null;
            try {
                URL url = new URL(imageUrl[0]);
                bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (Exception e) {
                Log.e("confv2", "Error loading image: " + imageUrl[0], e);
            }
            if (bmp != null && BrowseActivity.VERSION == 2) {
                bmp = ImageUtil.getRoundedCornerBitmap(bmp);
            }
            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                addBitmapToMemoryCache(user.makeKey(), result);
            }
        }
    }

    private static UserManager instance;

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public int size() {
        return users.length;
    }

    public int indexOf(User user) {
        for (int i = 0; i < users.length; i++) {
            if (user.equals(users[i])) {
                return i;
            }
        }
        return -1;
    }

    public User get(int index) {
        return users[index];
    }

    public int getIndexByBearing(double bearing) {
        if (users.length == 0) {
            return 0;
        }
        double[] bearings = new double[users.length];
        for (int i = 0; i < users.length; i++) {
            bearings[i] = users[i].getBearing();
        }
        int index = -Arrays.binarySearch(bearings, bearing) - 1;
        if (index == bearings.length) {
            // Special case: wrap-around from 360 to 0.
            return bearing - bearings[index - 1] <= bearings[0] + 360 - bearing ? index - 1 : 0;
        } else if (index == 0) {
            // Special case: wrap-around from 0 to 360.
            return bearings[0] - bearing <= bearing - bearings[bearings.length - 1] + 360 ?
                    0 : bearings.length - 1;
        }
        return bearings[index] - bearing <= bearing - bearings[index - 1] ? index : index - 1;
    }

    private void notifyListeners() {
        for (UserChangeListener listener : listeners) {
            listener.onUserChange(users);
        }
    }

    public void addListener(UserChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onUserListUpdate(User[] users) {
        getLastUsers = this.users;
        this.users = users;
        for (User user : users) {
            if (getBitmapFromMemCache(user.makeKey()) == null) {
                new DownloadImageTask(user).execute(user.getImageUrl());
            }
        }
        notifyListeners();
    }

    public User[] getLastUsers() {
        return getLastUsers;
    }

    public static interface UserChangeListener {
        void onUserChange(final User[] users);
    }
}
