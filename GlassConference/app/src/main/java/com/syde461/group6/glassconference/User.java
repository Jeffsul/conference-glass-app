package com.syde461.group6.glassconference;

/**
 * A model for a user, i.e., another conference-goer.
 */
public class User {
    private static final String DEFAULT_IMAGE = "";

    private final String name;
    private final String employer;
    private final String position;
    private final String image;

    public User(String name) {
        this(name, "", "", DEFAULT_IMAGE);
    }

    public User(String name, String employer, String position, String image) {
        this.name = name;
        this.employer = employer;
        this.position = position;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public String getEmployer() {
        return employer;
    }

    public String getPosition() {
        return position;
    }

    public String getImage() {
        return image;
    }

    public String makeKey() {
        return name + "/" + employer + "/" + position;
    }
}
