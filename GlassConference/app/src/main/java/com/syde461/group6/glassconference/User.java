package com.syde461.group6.glassconference;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A model for a user, i.e., another conference-goer.
 */
public class User implements Parcelable {
    private static final int DEFAULT_IMAGE = R.drawable.profile_default;

    private final int id;
    private final String name;
    private final String employer;
    private final String position;
    private final int image;

    private double bearing;
    private double distance;

    public User(int id, String name, String employer, String position) {
        this(id, name, employer, position, DEFAULT_IMAGE);
    }

    public User(int id, String name, String employer, String position, int image) {
        this.id = id;
        this.name = name;
        this.employer = employer;
        this.position = position;
        this.image = image;
    }

    public int getId() {
        return id;
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

    public double getBearing() {
        return bearing;
    }

    public void setBearing(double bearing) {
        this.bearing = bearing;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getImage() {
        return image;
    }

    public String makeKey() {
        return id + "/" + name;
    }

    public boolean equals(User user) {
        return makeKey().equals(user.makeKey());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(employer);
        dest.writeString(position);
        dest.writeInt(image);
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel pc) {
            return new User(pc);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public User(Parcel pc) {
        this.id = pc.readInt();
        this.name = pc.readString();
        this.employer = pc.readString();
        this.position = pc.readString();
        this.image = pc.readInt();
    }
}
