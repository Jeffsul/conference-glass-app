package com.syde461.group6.glassconference;

import android.location.Location;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Facade concealing details of server requests.
 * Allows responses to be faked for testing.
 */
public class ServerFacade {
    private static final long FAKE_DELAY = TimeUnit.SECONDS.toMillis(3);

    private static boolean fake = true;
    private static FakeEnvironment fakeEnvironment = new FakeEnvironment();

    private static List<UserUpdateListener> userListeners =
            new ArrayList<UserUpdateListener>();

    // Disallow instantiation.
    private ServerFacade() {}

    public static void updateLocation(Location location) {
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
