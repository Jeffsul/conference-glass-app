package com.syde461.group6.glassconference;

/**
 * A model for a user, i.e., another conference-goer.
 */
public class User {
    private final String name;

    public User(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
