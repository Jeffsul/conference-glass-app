package com.syde461.group6.glassconference;

import android.content.Context;

import com.google.android.glass.widget.CardBuilder;

/**
 * Represents another person in the Glass UI framework.
 */
public class UserCardBuilder extends CardBuilder {

    public UserCardBuilder(Context context, User user) {
        super(context, Layout.CAPTION);
        setText(user.getName());
        // TODO(jeffsul): Download and add User image asynchronously.
    }
}
