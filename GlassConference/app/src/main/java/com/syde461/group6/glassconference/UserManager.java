package com.syde461.group6.glassconference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Singleton storage for User model objects.
 */
public class UserManager implements ServerFacade.UserUpdateListener {

    private User[] users;

    private List<UserChangeListener> listeners = new ArrayList<UserChangeListener>();

    private UserManager() {
        users = new User[0];
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
        this.users = users;
        notifyListeners();
    }

    public static interface UserChangeListener {
        void onUserChange(final User[] users);
    }
}
