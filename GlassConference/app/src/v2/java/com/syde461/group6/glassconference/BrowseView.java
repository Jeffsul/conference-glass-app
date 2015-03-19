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
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.syde461.group6.glassconference.util.ImageUtil;
import com.syde461.group6.glassconference.util.MathUtil;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Dynamic view for browsing nearby people in "compass" mode.
 */
public class BrowseView extends View {

    private static final float DIRECTION_TEXT_HEIGHT = 84.0f;
    private static final float USER_TEXT_HEIGHT = 24.0f;

    /** Transparency for the black rectangle used as background for user name & company. */
    private static final int TEXT_BG_ALPHA = 100;
    /** Minimum transparency of a profile picture, used for the furthest away by distance. */
    private static final int MIN_PROFILE_ALPHA = 180;

    /** Maximum size for a profile picture, used for closest by distance. */
    private static final int MAX_PROFILE_SIZE = 160;
    /** Minimum size for a profile picture, used for furthest away by distance. */
    private static final int MIN_PROFILE_SIZE = 120;

    /** Smallest offset at which a profile picture can be drawn from the bottom of the View. */
    private static final float BOTTOM_BUFFER = MAX_PROFILE_SIZE / 2 + 2 * USER_TEXT_HEIGHT + 5;
    /** Smallest offset at which a profile picture can be drawn from the top of the View. */
    private static final float TOP_BUFFER = MIN_PROFILE_SIZE / 2 + 5;

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
        if (selectedUser != null) {
            for (User user : users) {
                if (user.equals(selectedUser)) {
                    selectedUser = user;
                    break;
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // The view displays 90 degrees across its width so that one 90 degree head rotation is
        // equal to one full view cycle.
        float pixelsPerDegree = getWidth() / 90.0f;
        float centerX = getWidth() / 2.0f;

        canvas.save();
        float canvasX = -animatedHeading * pixelsPerDegree + centerX;
        float canvasY = getHeight() - BOTTOM_BUFFER;
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

    public void switchSelection(int direction) {
        if (selectedUser == null) {
            return;
        }
        synchronized (users) {
            User[] usersClone = users.clone();
            final float selectedBearing = (float) selectedUser.getBearing();
            Arrays.sort(usersClone, new Comparator<User>() {
                @Override
                public int compare(User user, User user2) {
                    return MathUtil.diff((float)user.getBearing(), selectedBearing) > MathUtil.diff((float)user2.getBearing(), selectedBearing) ? 1 : -1;
                }
            });
            User nextSelection = null;
            for (int i = 1; i < usersClone.length; i++) {
                float diffCW;
                if (usersClone[i].getBearing() > selectedBearing) {
                    diffCW = (float)usersClone[i].getBearing() - selectedBearing;
                } else {
                    diffCW = 360 + (float)usersClone[i].getBearing() - selectedBearing;
                }
                float diffCCW = 360 - diffCW;
                if (direction * (diffCCW - diffCW) > 0) {
                    nextSelection = usersClone[i];
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
            float maxDistance = (float) users[0].getDistance();
            float minDistance = (float) users[users.length - 1].getDistance();
            for (User user : users) {
                if (user.equals(selectedUser)) {
                    continue;
                }
                Bitmap bmp = userManager.getBitmapFromMemCache(user.makeKey());
                if (bmp == null) {
                    bmp = defaultProfile;
                }
                float bearing = (float) user.getBearing();
                float distRatio = ((float) user.getDistance() - minDistance) / (maxDistance - minDistance);
                // Assume circular, and thus equal width and height.
                float bmpSize = MAX_PROFILE_SIZE - distRatio * (MAX_PROFILE_SIZE - MIN_PROFILE_SIZE);
                float distOffset = (getHeight() - BOTTOM_BUFFER - TOP_BUFFER) * distRatio;
                float bmpX = offset + bearing * pixelsPerDegree - bmpSize / 2;
                float bmpY = -bmpSize / 2 - distOffset;
                RectF bmpRect = new RectF(bmpX, bmpY, bmpX + bmpSize, bmpY + bmpSize);
                paint.setAlpha(255 - (int)(distRatio * MIN_PROFILE_ALPHA));
                canvas.drawBitmap(bmp, null, bmpRect, paint);
            }

            // Paint the selected user on top of everything else.
            Bitmap bmp = userManager.getBitmapFromMemCache(selectedUser.makeKey());
            if (bmp == null) {
                bmp = defaultProfile;
            }
            float bearing = (float) selectedUser.getBearing();
            float distance = (float) selectedUser.getDistance();
            float distRatio = users.length == 1 ? 0 : (distance - minDistance) / (maxDistance - minDistance);
            float distOffset = (getHeight() - BOTTOM_BUFFER - TOP_BUFFER) * distRatio;
            Rect text1Bounds = new Rect();
            Rect text2Bounds = new Rect();
            String text = selectedUser.getName();
            String text2 = selectedUser.getEmployer();
            userNamePaint.getTextBounds(text, 0, text.length(), text1Bounds);
            userNamePaint.getTextBounds(text2, 0, text2.length(), text2Bounds);

            // Draw user profile image and circle border.
            float bmpHeight = bmp.getHeight();
            float bmpWidth = bmp.getWidth();
            float bmpX = offset + bearing * pixelsPerDegree - bmpWidth / 2;
            float bmpY = -bmpHeight / 2 - distOffset;
            paint.setAlpha(255);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(offset + bearing * pixelsPerDegree, -distOffset, bmpHeight / 2 + 3, paint);
            canvas.drawBitmap(bmp, bmpX, bmpY, paint);

            // Calculate text positions.
            float text1X = offset + bearing * pixelsPerDegree - text1Bounds.width() / 2;
            float text1Y = 3 + USER_TEXT_HEIGHT + bmp.getHeight() / 2 - distOffset;
            text1Bounds.offsetTo((int) text1X, (int) (text1Y - text1Bounds.height()));
            float text2X = offset + bearing * pixelsPerDegree - text2Bounds.width() / 2;
            float text2Y = text1Y + text1Bounds.height() + 2;
            text2Bounds.offsetTo((int) text2X, (int) (text2Y - text2Bounds.height()));

            // Draw transparent rectangle underneath text.
            Rect textBounds = new Rect(text1Bounds);
            textBounds.union(text2Bounds);
            paint.setColor(Color.BLACK);
            paint.setAlpha(TEXT_BG_ALPHA);
            int padding = 3;
            canvas.drawRect(textBounds.left - padding, textBounds.top - padding,
                    textBounds.right + padding, textBounds.bottom + padding, paint);

            // Draw two lines of text.
            canvas.drawText(text, text1X, text1Y, userNamePaint);
            canvas.drawText(text2, text2X, text2Y, userNamePaint);
        }
    }

    public void setBearing(float bearing) {
        this.bearing = MathUtil.mod(bearing, 360.0f);
        animateTo(bearing);
    }

    private void setupAnimator() {
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(250);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                animatedHeading = MathUtil.mod((Float) animator.getAnimatedValue(), 360.0f);
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

    private static void l(String msg) {
        //Log.e("glassconfv2", msg);
    }
}
