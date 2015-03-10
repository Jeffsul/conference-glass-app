package com.syde461.group6.glassconference;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Created by Jeff on 10/03/2015.
 */
public class BrowseView extends View {

    private static final float DIRECTION_TEXT_HEIGHT = 84.0f;
    private static final float PLACE_TEXT_HEIGHT = 22.0f;

    private static final float MIN_DISTANCE_TO_ANIMATE = 15.0f;

    /**
     * Copied:
     * Represents the heading that is currently being displayed when the view is drawn. This is
     * used during animations, to keep track of the heading that should be drawn on the current
     * frame, which may be different than the desired end point.
     */
    private float animatedHeading;

    private float bearing;

    private final ValueAnimator animator;

    private Paint paint;
    private TextPaint userNamePaint;

    private OrientationManager orientationManager;

    private User[] users = new User[0];

    public BrowseView(Context context) {
        this(context, null, 0);
    }

    public BrowseView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrowseView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setTextSize(DIRECTION_TEXT_HEIGHT);
        paint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));

        userNamePaint = new TextPaint();
        userNamePaint.setStyle(Paint.Style.FILL);
        userNamePaint.setAntiAlias(true);
        userNamePaint.setColor(Color.WHITE);
        userNamePaint.setTextSize(PLACE_TEXT_HEIGHT);
        userNamePaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        animatedHeading = Float.NaN;

        animator = new ValueAnimator();
        setupAnimator();
    }

    public void setOrientationManager(OrientationManager orientationManager) {
        this.orientationManager = orientationManager;
    }

    public void setNearbyPeople(User[] nearbyPeople) {
        this.users = nearbyPeople;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // The view displays 90 degrees across its width so that one 90 degree head rotation is
        // equal to one full view cycle.
        float pixelsPerDegree = getWidth() / 90.0f;
        float centerX = getWidth() / 2.0f;
        float centerY = getHeight() / 2.0f;

        canvas.save();
        float canvasX = -animatedHeading * pixelsPerDegree + centerX;
        l("Drawing canvas, X: " + canvasX);
        canvas.translate(canvasX, centerY);

        for (int i = -1; i <= 1; i++) {
            drawNearbyPeople(canvas, pixelsPerDegree, i * pixelsPerDegree * 360);
        }

        canvas.restore();
    }

    private void drawNearbyPeople(Canvas canvas, float pixelsPerDegree, float offset) {
        l("Drawing people: " + users.length);
        synchronized (users) {
            double myBearing = orientationManager.getBearing();
            for (User user : users) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.outWidth = 50;
                opts.outHeight = 50;
                Bitmap bmp = BitmapFactory.decodeResource(getContext().getResources(), user.getImage(), opts);

                double relativeBearing = user.getBearing();

                double distance = user.getDistance();
                Rect textBounds = new Rect();
                String text = user.getName();
                userNamePaint.getTextBounds(text, 0, text.length(), textBounds);
                textBounds.offsetTo((int)(offset + relativeBearing * pixelsPerDegree), 30);

                float drawX = (float) (offset + relativeBearing * pixelsPerDegree - 25);
                l("Drawing user: " + user.getName() + ", " + relativeBearing + ", " + drawX);
                canvas.drawBitmap(bmp, drawX, -25, paint);
                canvas.drawText(text, (float)(offset + relativeBearing * pixelsPerDegree),
                        textBounds.top + PLACE_TEXT_HEIGHT, userNamePaint);
            }
        }
    }

    public void setBearing(float bearing) {
        this.bearing = mod(bearing, 360.0f);
        animateTo(bearing);
    }

    private void setupAnimator() {
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(250);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                animatedHeading = mod((Float) animator.getAnimatedValue(), 360.0f);
                invalidate();
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                l("Finished animating, new goal: " + bearing);
                animateTo(bearing);
            }
        });
    }

    private void animateTo(float end) {
        if (animator.isRunning()) {
            return;
        }
        float start = animatedHeading;
        float distance = Math.abs(end - start);
        float reverseDistance = 360.0f - distance;
        float shortest = Math.min(distance, reverseDistance);

        if (Float.isNaN(animatedHeading) || shortest < MIN_DISTANCE_TO_ANIMATE) {
            // If the distance to the destination angle is small enough (or if this is the
            // first time the compass is being displayed), it will be more fluid to just redraw
            // immediately instead of doing an animation.
            animatedHeading = end;
            invalidate();
            l("Jumping to: " + animatedHeading);
        } else {
            // For larger distances (i.e., if the compass "jumps" because of sensor calibration
            // issues), we animate the effect to provide a more fluid user experience. The
            // calculation below finds the shortest distance between the two angles, which may
            // involve crossing 0/360 degrees.
            float goal;

            if (distance < reverseDistance) {
                goal = end;
            } else if (end < start) {
                goal = end + 360.0f;
            } else {
                goal = end - 360.0f;
            }

            animator.setFloatValues(start, goal);
            animator.start();
            l("Staring animator: " + start + " to " + goal);
        }
    }

    /**
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

    private static void l(String msg) {
        //Log.e("glassconfv2", msg);
    }
}
