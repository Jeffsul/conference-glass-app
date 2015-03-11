package com.syde461.group6.glassconference.util;

/**
 * Created by Jeff on 10/03/2015.
 */
public class MathUtil {
    public static float diff(float deg1, float deg2) {
//        float d = deg1 - deg2;
//        if (d <= -180) {
//            d += 360;
//        }
//        if (d >= 180) {
//            d -= 360;
//        }
//        return d;
        float distance = Math.abs(deg1 - deg2);
        return Math.min(distance, 360.0f - distance);
    }
}
