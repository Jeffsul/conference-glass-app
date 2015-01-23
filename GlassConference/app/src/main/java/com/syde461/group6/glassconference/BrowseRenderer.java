package com.syde461.group6.glassconference;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterViewFlipper;
import android.widget.BaseAdapter;

import com.google.android.glass.timeline.DirectRenderingCallback;

/**
 * Created by Jeff on 16/01/2015.
 */
public class BrowseRenderer implements DirectRenderingCallback {

    private final BrowseView browseView;

    private SurfaceHolder holder;
    private boolean renderingPaused;

    public BrowseRenderer(Context context) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        browseView = (BrowseView) inflater.inflate(R.layout.browse, null);
        browseView.setWillNotDraw(false);
        browseView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public Object getItem(int i) {
                return new Object();
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                return inflater.inflate(R.layout.user, browseView, false);
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do layout.
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);

        browseView.measure(measuredWidth, measuredHeight);
        browseView.layout(0, 0, browseView.getMeasuredWidth(), browseView.getMeasuredHeight());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        renderingPaused = false;
        this.holder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.holder = null;
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused) {
        renderingPaused = true;
    }
}
