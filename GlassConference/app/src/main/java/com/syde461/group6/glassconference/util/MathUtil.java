package com.syde461.group6.glassconference.util;

/**
 * Created by Jeff on 10/03/2015.
 */
public class MathUtil {
    public static float diff(float deg1, float deg2) {
        float distance = Math.abs(deg1 - deg2);
        return Math.min(distance, 360.0f - distance);
    }

    /**
     * From: https://github.com/googleglass/gdk-compass-sample/blob/master/app/src/main/java/com/
     *              google/android/glass/sample/compass/util/MathUtils.java
     *
     * Calculates {@code a mod b} in a way that respects negative values (for example,
     * {@code mod(-1, 5) == 4}, rather than {@code -1}).
     *
     * @param a the dividend
     * @param b the divisor
     * @return {@code a mod b}
     */
    public static float mod(float a, float b) {
        return (a % b + b) % b;
    }
}
