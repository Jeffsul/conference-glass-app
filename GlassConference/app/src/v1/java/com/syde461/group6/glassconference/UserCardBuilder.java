package com.syde461.group6.glassconference;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Represents another person in the Glass UI framework.
 */
public class UserCardBuilder {

    private final Context context;
    private final User user;

    public UserCardBuilder(Context context, User user) {
        this.context = context;
        this.user = user;
    }

    public View getView(View convertView, ViewGroup parent, float rotation) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.user_left_column, parent);
        }

        TextView employerView = (TextView) convertView.findViewById(R.id.user_employer);
        employerView.setText(user.getEmployer());
        TextView nameView = (TextView) convertView.findViewById(R.id.user_name);
        nameView.setText(user.getName());
        TextView positionView = (TextView) convertView.findViewById(R.id.user_position);
        positionView.setText(user.getPosition());

        ImageView profileView = (ImageView) convertView.findViewById(R.id.user_profile);
        Bitmap bmp = UserManager.getInstance().getBitmapFromMemCache(user.makeKey());
        if (bmp == null) {
            profileView.setImageResource(user.getImage());
        } else {
            profileView.setImageBitmap(bmp);
        }

        ImageView arrowView = (ImageView) convertView.findViewById(R.id.timestamp);
        if (rotation < 0) {
            rotation += 360;
        }
        arrowView.setRotation(rotation);

        return convertView;
    }

    public User getUser() {
        return user;
    }
}
