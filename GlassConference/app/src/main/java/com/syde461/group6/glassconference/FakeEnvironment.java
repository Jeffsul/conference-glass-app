package com.syde461.group6.glassconference;

import android.os.Handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fake representation of reality for testing.
 */
public class FakeEnvironment {

    private static final long UPDATE_DELAY = TimeUnit.SECONDS.toMillis(2);

    private static final User[] FAKE_USERS = {
            new User("Jeff Sullivan", "Google", "0", R.drawable.profile_jeff),
            new User("Anson Ho", "University of Waterloo", "1", R.drawable.profile_anson),
            new User("Catherine Maritan", "Microsoft", "2", R.drawable.profile_catherine),
            new User("Eric Cheng", "Uber", "3", R.drawable.profile_eric),
            new User("Kyle Koerth", "Cars", "4", R.drawable.profile_kyle)
    };

    private List<User> fakeUsers = new ArrayList<User>();

    private Handler handler;

    public FakeEnvironment() {
        Collections.addAll(fakeUsers, FAKE_USERS);
        // Set initial bearings.
        for (int i = 0; i < fakeUsers.size(); i++) {
            fakeUsers.get(i).setBearing(40 * i);
        }

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                User mover = fakeUsers.get(0);
                mover.setBearing(mover.getBearing() + 13);
                handler.postDelayed(this, UPDATE_DELAY);
            }
        }, UPDATE_DELAY);
    }

    public User[] getUsers() {
        User[] users = new User[fakeUsers.size()];
        fakeUsers.toArray(users);
        Arrays.sort(users, new Comparator<User>() {
            @Override
            public int compare(User user1, User user2) {
                return user1.getBearing() >= user2.getBearing() ? 1 : -1;
            }
        });
        return users;
    }
}
