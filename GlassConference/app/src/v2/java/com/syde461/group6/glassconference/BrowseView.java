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
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.syde461.group6.glassconference.util.ImageUtil;
import com.syde461.group6.glassconference.util.MathUtil;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by Jeff on 10/03/2015.
 */
public class BrowseView extends View {

    private static final float DIRECTION_TEXT_HEIGHT = 84.0f;
    private static final float USER_TEXT_HEIGHT = 24.0f;

    private static final float MIN_DISTANCE_TO_ANIMATE = 15.0f;

    private final Bitmap defaultProfile;

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
    private UserManager userManager;

    private GestureDetector gestureDetector;

    private User[] users = new User[0];

    private User selectedUser;
    private User bestUser;

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
        userNamePaint.setTextSize(USER_TEXT_HEIGHT);
        userNamePaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        defaultProfile = ImageUtil.getRoundedCornerBitmap(
                Bitmap.createScaledBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.profile_default),
                        140, 140, false));

        animatedHeading = Float.NaN;

        animator = new ValueAnimator();
        setupAnimator();

        gestureDetector = new GestureDetector(getContext()).setBaseListener(
                new GestureDetector.BaseListener() {
                    @Override
                    public boolean onGesture(Gesture gesture) {
                        if (gesture == Gesture.SWIPE_LEFT || gesture == Gesture.SWIPE_RIGHT) {
                            Log.e("confv2", "Entering interaction mode.");
                            switchSelection(gesture == Gesture.SWIPE_LEFT ? -1 : 1);
                            return true;
                        }
                        return false;
                    }
                });
    }

    public void setOrientationManager(OrientationManager orientationManager) {
        this.orientationManager = orientationManager;
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void setNearbyPeople(User[] nearbyPeople) {
        this.users = nearbyPeople;
        Arrays.sort(users, new Comparator<User>() {
            @Override
            public int compare(User user, User user2) {
                return user.getDistance() > user2.getDistance() ? -1 : 1;
            }
        });
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
        float canvasY = getHeight() - 115;
        l("Drawing canvas, X: " + canvasX);
        canvas.translate(canvasX, canvasY);

        for (int i = -1; i <= 1; i++) {
            drawNearbyPeople(canvas, pixelsPerDegree, i * pixelsPerDegree * 360);
        }

        canvas.restore();
    }

    public int getSelectedUserId() {
        return selectedUser != null ? selectedUser.getId() : -1;
    }

    public User getSelectedUser() {
        return selectedUser;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return gestureDetector.onMotionEvent(event);
    }

    public void switchSelection(int direction) {
        synchronized (users) {
            User[] users = this.users.clone();
            final float selectedBearing = (float) selectedUser.getBearing();
            Arrays.sort(users, new Comparator<User>() {
                @Override
                public int compare(User user, User user2) {
                    return MathUtil.diff((float)user.getBearing(), selectedBearing) > MathUtil.diff((float)user2.getBearing(), selectedBearing) ? 1 : -1;
                }
            });
            User nextSelection = null;
            for (int i = 1; i < users.length; i++) {
                if (direction * (users[i].getBearing() - selectedBearing) > 0) {
                    nextSelection = users[i];
                    break;
                }
            }
            if (nextSelection != null) {
                float pixelsPerDegree = getWidth() / 90.0f;
                if (MathUtil.diff((float) nextSelection.getBearing(), bearing) * pixelsPerDegree <= getWidth() / 2.0f) {
                    selectedUser = nextSelection;
                }
            }
        }
    }

    private void drawNearbyPeople(Canvas canvas, float pixelsPerDegree, float offset) {
        l("Drawing people: " + users.length);
        synchronized (users) {
            if (users.length ==  0) {
                return;
            }
            User shortestUser = null;
            float shortest = Float.MAX_VALUE;
            for (User user : users) {
                float diff = MathUtil.diff(this.bearing, (float)user.getBearing());
                if (shortestUser == null || diff < shortest) {
                    shortestUser = user;
                    shortest = diff;
                }
            }
            if (selectedUser != null
                    && MathUtil.diff(this.bearing, (int)selectedUser.getBearing()) * pixelsPerDegree > getWidth() / 2.0f) {
                selectedUser = shortestUser;
            }
            if (bestUser == null || shortest < MathUtil.diff(this.bearing, (float)bestUser.getBearing())) {
                if (bestUser != null && bestUser.equals(selectedUser)) {
                    selectedUser = shortestUser;
                }
                if (selectedUser == null) {
                    selectedUser = shortestUser;
                }
                bestUser = shortestUser;
            }
            float maxDistance = (float)users[0].getDistance();
            float minDistance = (float)users[users.length - 1].getDistance();
            for (User user : users) {
                Bitmap bmp = userManager.getBitmapFromMemCache(user.makeKey());
                if (bmp == null) {
                    bmp = defaultProfile;
                }
                float bearing = (float) user.getBearing();

                double distance = user.getDistance();
                float distRatio = ((float)distance - minDistance) / (maxDistance - minDistance);
                float distOffset = 170 * distRatio;
                Rect textBounds = new Rect();
                String text = user.getName();
                userNamePaint.getTextBounds(text, 0, text.length(), textBounds);
                //textBounds.offsetTo((int)(offset + bearing * pixelsPerDegree), 5);

                float bmpHeight = bmp.getHeight();
                float bmpWidth = bmp.getWidth();
                float bmpX = offset + bearing * pixelsPerDegree - bmpWidth / 2;
                float bmpY = -bmpHeight / 2 - distOffset;
                paint.setAlpha(255 - (int)(distRatio * 160));
                canvas.drawBitmap(bmp, bmpX, bmpY, paint);

//                if (user == bestUser) {
//                    float textX = offset + bearing * pixelsPerDegree - textWidth / 2;
//                    float textY = 5 + USER_TEXT_HEIGHT + bmp.getHeight() / 2 - distOffset;
//                    canvas.drawText(text, textX, textY, userNamePaint);
//                }
            }

            Bitmap bmp = userManager.getBitmapFromMemCache(selectedUser.makeKey());
            if (bmp == null) {
                bmp = defaultProfile;
            }
            float bearing = (float) selectedUser.getBearing();

            double distance = selectedUser.getDistance();
            float distRatio = ((float)distance - minDistance) / (maxDistance - minDistance);
            float distOffset = 170 * distRatio;
            Rect textBounds = new Rect();
            String text = selectedUser.getName();
            String text2 = selectedUser.getEmployer();
            userNamePaint.getTextBounds(text, 0, text.length(), textBounds);
            float text2Width = userNamePaint.measureText(text2);
            //textBounds.offsetTo((int)(offset + bearing * pixelsPerDegree), 5);

            float bmpHeight = bmp.getHeight();
            float bmpWidth = bmp.getWidth();
            float bmpX = offset + bearing * pixelsPerDegree - bmpWidth / 2;
            float bmpY = -bmpHeight / 2 - distOffset;
            paint.setAlpha(255);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(offset + bearing * pixelsPerDegree, -distOffset, bmpHeight / 2 + 3, paint);
            canvas.drawBitmap(bmp, bmpX, bmpY, paint);

            float textX = offset + bearing * pixelsPerDegree - textBounds.width() / 2;
            float textY = 4 + USER_TEXT_HEIGHT + bmp.getHeight() / 2 - distOffset;
            canvas.drawText(text, textX, textY, userNamePaint);

            textX = offset + bearing * pixelsPerDegree - text2Width / 2;
            textY += textBounds.height() + 2;
            canvas.drawText(text2, textX, textY, userNamePaint);
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
