package com.liquidware.cloudview;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class RemoteImageView extends ImageView implements Event
{
    private static final String TAG = "RemoteImageView";
    CloudViewActivity mParent;
    boolean mAttached;
    int mAssetId = -1;

    public RemoteImageView(Context context) {
        this(context, null);
    }

    public RemoteImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RemoteImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
        }
    }

    public void setParent(CloudViewActivity parent) {
        mParent = parent;
        mParent.getNotifier().addListener(this);
    }

    public void setAssetId(int id) {
        mAssetId = id;
    }

    public void onTimerTick(long millisUpTime) {
        Log.d(TAG, "onTimerTick");
    }

    public void onRemoteAssetUpdated(int id, final Object object) {
        if (id != mAssetId)
            return;

        mParent.runOnUiThread(new Runnable() {
            public void run() {
                setImageBitmap((Bitmap) object);
            }
        });
    }
}