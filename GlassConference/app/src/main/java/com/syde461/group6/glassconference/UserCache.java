package com.syde461.group6.glassconference;

import java.util.HashMap;

/**
 * Singleton storage for User model objects.
 */
public class UserCache {

    private HashMap<String, User> users;

    private UserCache() {
        users = new HashMap<String, User>();
    }

    private static UserCache instance;

    public static UserCache getInstance() {
        if (instance == null) {
            instance = new UserCache();
            prepopulateUsers();
        }
        return instance;
    }

    private static void prepopulateUsers() {
        for (int i = 0; i < 6; i++) {
            instance.addUser(new User("User " + i, "Google", "Developer", ""));
        }
    }

    public void addUser(User user) {
        users.put(user.makeKey(), user);
    }

    public User[] getUsers() {
        // TODO(jeffsul): Consider concurrency issues.
        User[] allUsers = new User[users.size()];
        users.values().toArray(allUsers);
        return allUsers;
    }
}
