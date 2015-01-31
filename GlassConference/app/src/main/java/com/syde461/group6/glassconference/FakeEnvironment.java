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
            new User("Jeff Sullivan", "Google", "0"),
            new User("Anson Ho", "University of Waterloo", "1"),
            new User("Catherine Maritan", "Microsoft", "2"),
            new User("Eric Cheng", "Uber", "3"),
            new User("Jeff Sullivan2", "Google", "4"),
            new User("Anson Ho2", "University of Waterloo", "5"),
            new User("Catherine Maritan2", "Microsoft", "6"),
            new User("Eric Cheng2", "Uber", "7"),
            new User("Jeff Sullivan3", "Google", "8"),
            new User("Anson Ho3", "University of Waterloo", "9"),
            new User("Catherine Maritan3", "Microsoft", "10"),
            new User("Eric Cheng3", "Uber", "11")
    };

    private List<User> fakeUsers = new ArrayList<User>();

    private Handler handler;

    public FakeEnvironment() {
        Collections.addAll(fakeUsers, FAKE_USERS);
        // Set initial bearings.
        for (int i = 0; i < fakeUsers.size(); i++) {
            fakeUsers.get(i).setBearing(25 * i);
        }

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                User mover = fakeUsers.get(0);
                mover.setBearing(mover.getBearing() + 10);
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
