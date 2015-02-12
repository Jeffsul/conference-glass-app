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
            new User.Builder().id(0).name("Jeff Sullivan").employer("Google").image(R.drawable.profile_jeff).build(),
            new User.Builder().id(1).name("Anson Ho").employer("University of Waterloo").image(R.drawable.profile_anson).build(),
            new User.Builder().id(2).name("Catherine Maritan").employer("Microsoft").image(R.drawable.profile_catherine).build(),
            new User.Builder().id(3).name("Eric Cheng").employer("Uber").image(R.drawable.profile_eric).build(),
            new User.Builder().id(4).name("Kyle Koerth").employer("Cars").image(R.drawable.profile_kyle).build()
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
