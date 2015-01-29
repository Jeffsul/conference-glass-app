package com.syde461.group6.glassconference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Singleton storage for User model objects.
 */
public class UserManager {

    private List<User> users;

    private UserManager() {
        users = new ArrayList<User>();
    }

    private static UserManager instance;

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
            prepopulateUsers();
        }
        return instance;
    }

    private static void prepopulateUsers() {
        for (int i = 0; i < 6; i++) {
            instance.addUser(new User("User " + i, "Google", "Developer", ""));
        }
    }

    private void addUser(User user) {
        users.add(user);
    }

    public int size() {
        return users.size();
    }

    public int indexOf(User user) {
        return users.indexOf(user);
    }

    public User get(int index) {
        return users.get(index);
    }
}
