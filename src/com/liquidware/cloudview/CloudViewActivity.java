/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liquidware.cloudview;

import java.io.InputStreamReader;
import java.net.URL;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.VideoView;
import android.widget.ViewSwitcher;

/**
 * This class provides a basic demonstration of how to write an Android
 * activity. Inside of its window, it places a single view: an EditText that
 * displays and edits some internal text.
 */
public class CloudViewActivity extends Activity implements Event {
    private static final String TAG = "CloudViewActivity";
    Handler mHandler = new Handler();
    EventNotifier mNotifier;
    Gson gson = new Gson();

    Bitmap[] mImages = new Bitmap[3];
    ImageButton[] mButtons = new ImageButton[3];

    View mImageView;

    ViewSwitcher mSwitcher;
    VideoView mActiveVideo;
    boolean mIsUpdating = false;

    CloudViewActivity mCloudViewActivity;

    public CloudViewActivity() {
    }

    public class OpenVideo extends Thread {
        @Override
        public void run() {
            try {
                mActiveVideo = (VideoView) findViewById(R.id.myvideoview);
                mActiveVideo.setMediaController(new MediaController(mCloudViewActivity));
                Uri video = Uri.parse("android.resource://" + getPackageName() + "/"
                        + R.raw.cloud_video);
                mActiveVideo.setVideoURI(video);

                mActiveVideo.requestFocus();
                mActiveVideo.start();
                mActiveVideo.setOnPreparedListener (new OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        mp.setLooping(true);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCloudViewActivity = this;
        getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.cloudview_activity);

        mSwitcher = (ViewSwitcher) findViewById(R.id.profileSwitcher);

        new OpenVideo().run();

        mButtons[0] = (ImageButton) findViewById(R.id.ImageButton1);
        mButtons[0].setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                RemoteImageView image = (RemoteImageView) findViewById(R.id.ImageView01);
                image.setImageBitmap(mImages[0]);
                image.setParent(mCloudViewActivity);
                image.setAssetId(0);
                Button back = (Button) findViewById(R.id.button1);
                back.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        mSwitcher.showPrevious();
                    }
                });
                image.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        mSwitcher.showPrevious();
                    }
                });
                mSwitcher.showNext();
            }
        });

        mButtons[1] = (ImageButton) findViewById(R.id.ImageButton2);
        mButtons[1].setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                RemoteImageView image = (RemoteImageView) findViewById(R.id.ImageView01);
                image.setImageBitmap(mImages[1]);
                image.setParent(mCloudViewActivity);
                image.setAssetId(1);
                Button back = (Button) findViewById(R.id.button1);
                back.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        mSwitcher.showPrevious();
                    }
                });
                image.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        mSwitcher.showPrevious();
                    }
                });
                mSwitcher.showNext();
            }
        });

        mNotifier = new EventNotifier(this);
        mUpdateTimeTask.run();
    }

    /**
     * A global application clock ticker
     */
    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long millisUpTime = SystemClock.uptimeMillis();
            mNotifier.onTimerTick(millisUpTime);
            mHandler.postAtTime(this, millisUpTime + 1000);
        }
    };

    public class ResponseData {
        private Asset asset;

        public class Asset {
            public String source_url;
        }
    }

    /**
     * Called when the activity is about to start interacting with the user.
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    public class ResourceUpdater extends Thread {
        @Override
        public void run() {
            mIsUpdating = true;
            URL url;
            try {
                url = new URL("http", "ec2-50-19-144-125.compute-1.amazonaws.com", 80,
                        "/adverts/1.json");
                Log.d(TAG, "Reading " + url);

                JsonReader reader = new JsonReader(new InputStreamReader(url.openStream(),
                        "UTF-8"));

                /* Loop through the stream */
                int i = 0;
                reader.beginArray();
                while (reader.hasNext()) {
                    ResponseData r = gson.fromJson(reader, ResponseData.class);
                    Log.d(TAG, "source_url=" + r.asset.source_url);
                    Bitmap image = BitmapFactory.decodeStream(new URL("http",
                            "ec2-50-19-144-125.compute-1.amazonaws.com", 80, r.asset.source_url)
                            .openConnection().getInputStream());
                    mImages[i] = image;
                    mNotifier.onRemoteAssetUpdated(i, image);
                    i++;
                }
                reader.endArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mIsUpdating = false;
        }
    }

    public EventNotifier getNotifier() {
        return mNotifier;
    }

    public void onTimerTick(long millisUpTime) {
        Log.d(TAG, "onTimerTick");

        if (!mIsUpdating)
            new ResourceUpdater().start();
    }

    public void onRemoteAssetUpdated(int id, Object object) {
    }
}
