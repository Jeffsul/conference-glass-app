package com.syde461.group6.glassconference;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A model for a user, i.e., another conference-goer.
 * Uses the Builder construction design pattern.
 */
public final class User implements Parcelable {
    public static final int DEFAULT_IMAGE = R.drawable.profile_default;

    private double bearing;
    private double distance;

    private final int id;
    private final String name;
    private final String employer;
    private final String position;
    private final int image;

    public static final class Builder {
        private int id;
        private String name;
        private String employer;
        private String position;
        private int image;

        public Builder id(int id) {
            this.id = id;
            return this;
        }
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        public Builder employer(String employer) {
            this.employer = employer;
            return this;
        }
        public Builder position(String position) {
            this.position = position;
            return this;
        }
        public Builder image(int image) {
            this.image = image;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }

    private User(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.employer = builder.employer;
        this.position = builder.position;
        this.image = builder.image;
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
