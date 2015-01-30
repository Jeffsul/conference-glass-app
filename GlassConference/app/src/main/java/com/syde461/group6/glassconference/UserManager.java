package com.syde461.group6.glassconference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Singleton storage for User model objects.
 */
public class UserManager {

    private List<User> users;

    private List<UserChangeListener> listeners = new ArrayList<UserChangeListener>();

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
            User dummy = new User("User " + i, "Google", Integer.toString(45*i), "");
            dummy.setBearing(45 * i);
            instance.addUser(dummy);
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

    public int getIndexByBearing(double bearing) {
        double[] bearings = new double[users.size()];
        for (int i = 0; i < users.size(); i++) {
            bearings[i] = users.get(i).getBearing();
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

    public static interface UserChangeListener {
        void onUserChange(List<User> users);
    }
}
