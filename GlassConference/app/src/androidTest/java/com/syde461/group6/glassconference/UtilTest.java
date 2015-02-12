package com.syde461.group6.glassconference;

import android.test.InstrumentationTestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Test cases for static utility methods.
 */
public class UtilTest extends InstrumentationTestCase {

    private User[] users;

    public void testGetIndexByBearing() {
        assertEquals(BrowseActivity.diff(65, 30), 35);
    }
}
