package com.syde461.group6.glassconference;

/**
 * A model for a user, i.e., another conference-goer.
 */
public class User {
    private static final String DEFAULT_IMAGE = "";

    private final String name;
    private final String image;

    public User(String name) {
        this(name, DEFAULT_IMAGE);
    }

    public User(String name, String image) {
        this.name = name;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }
}
