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

package net.filiph.mothership;

import java.util.Date;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import android.widget.*;
import net.filiph.mothership.gcm.CommonUtilities;
import net.filiph.mothership.gcm.ServerUtilities;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View.MeasureSpec;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.android.gcm.GCMRegistrar;

/**
 * This is the main activity, meaning the UI screen.
 */
@TargetApi(8)
public class MainActivity extends FragmentActivity {

    private static final String TAG = "motherShip MainActivity";

    private UpdateReceiver updateReceiver;
    final private Handler typeHandler = new Handler();
    AsyncTask<Void, Void, Void> mRegisterTask;

    public MainActivity() {
    }

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.main_activity);

        // TODO: only do if there is no alarm set
        AlarmReceiver.setAlarmForNextMessage(getBaseContext());

        setupGCM();
    }

    VideoView vv = null;

    /**
     * Called when the activity is about to start interacting with the user.
     */
    private Thread thread;
    @Override
    protected void onResume() {
        super.onResume();

        if (!getSharedPreferences(PREFS_NAME, 0).contains(ShowNotificationDialog.notificationDbValue)) {
            new ShowNotificationDialog().show(getSupportFragmentManager(), "ShowNotificationDialogFragment");
        }

        showCurrentMessage(TYPING_DEFAULT);

        updateReceiver = new UpdateReceiver(this);
        registerReceiver(updateReceiver, new IntentFilter("net.filiph.mothership.NEW_MOTHERSHIP_MESSAGE"));

        if (vv == null) {
            // getWindow().setFormat(PixelFormat.TRANSLUCENT);
            vv = (VideoView) findViewById(R.id.videoView1);
            // video from
            // http://www.istockphoto.com/stock-video-17986614-data-servers-loopable-animation.php
            Uri video = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.servers);

            // this horrible spaghetti is responsible for showing the background
            // video
            // with as few flicker as possible
            try {
                vv.setVideoURI(video);
                vv.setKeepScreenOn(false);

                // we can only access the MediaPlaye instance after video is
                // Prepared
                vv.setOnPreparedListener(new OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.setOnSeekCompleteListener(new OnSeekCompleteListener() {
                            @Override
                            public void onSeekComplete(MediaPlayer mp) {
                                mp.start();

                                // give the player 100 milliseconds to start
                                // playing
                                // then hide the placeholder static image
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
                                        sv.setBackgroundResource(0);
                                    }
                                }, 100);
                            }
                        });
                        mp.setLooping(true);
                        mp.seekTo(0);
                    }
                });

                vv.setOnErrorListener(new OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        Log.e(TAG, "Error with video playback. Reverting to static image.");

                        // something went wrong, fall back to static image
                        vv.suspend();
                        vv = null;
                        ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
                        sv.setBackgroundResource(R.drawable.servers);

                        return true; // don't report
                    }
                });

                // stretch the video full view
                Display display = getWindowManager().getDefaultDisplay();
                int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(display.getWidth(), MeasureSpec.UNSPECIFIED);
                int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(display.getHeight(), MeasureSpec.UNSPECIFIED);
                vv.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            } catch (Exception e) {
                Log.e(TAG, "Error with background video, falling back to static background.");
                e.printStackTrace();

                // something went wrong, fall back to static image
                vv.suspend();
                vv = null;
                ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
                sv.setBackgroundResource(R.drawable.servers);
            }
        }

        if (vv != null && vv.canSeekBackward() && vv.canSeekForward()) {
            vv.seekTo(0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (vv != null && vv.canPause()) {
            vv.pause();
            // vv.suspend();
        }

        unregisterReceiver(updateReceiver); // don't update the activity when
        // paused
    }

    @Override
    protected void onDestroy() {
        if (mRegisterTask != null) {
            mRegisterTask.cancel(true);
        }
        unregisterReceiver(mHandleMessageReceiver);
        GCMRegistrar.onDestroy(getApplicationContext());
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // the new messages may create steps in activity stack
        // this makes sure that pressing the back button exits the activity
        finish();
    }

    private int funnyRemarkIndex = 0;

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Clicking the menu button prints funny messages.
     */

    public boolean onPrepareOptionsMenu(final Menu menu) {
        final MenuItem menuButton = menu.findItem(R.id.menu_changeNotification);
        if (!getSharedPreferences(PREFS_NAME, 0).getBoolean(ShowNotificationDialog.notificationDbValue, false)) {
            menuButton.setTitle(R.string.menu_notification_on);
        } else {
            menuButton.setTitle(R.string.menu_notification_off);
        }
        if (funnyRemarkIndex != 1) {
            menuButton.setVisible(false);
        } else {
            menuButton.setVisible(true);
        }

        final MainActivity mainActivity = this;
        final TextView t = (TextView) findViewById(R.id.textView);
        final TextView signature = (TextView) findViewById(R.id.signature);
        final ScrollView scroll = (ScrollView) findViewById(R.id.scrollView);

        // Remove dialog from screen
        final LinearLayout ll = (LinearLayout) findViewById(R.id.linearLayout);
        if (ll.getChildCount() > 3)
            ll.removeViews(2, ll.getChildCount() - 3);

        t.setText("");
        signature.setText("");

        String[] funnyRemarks = getResources().getStringArray(R.array.menu_mothership_funny_remarks);
        final String str = funnyRemarks[funnyRemarkIndex];
        if (funnyRemarkIndex < funnyRemarks.length - 1) {
            funnyRemarkIndex++;
        }

        typeHandler.removeCallbacksAndMessages(null);

        typeHandler.postDelayed(new Runnable() {
            int index = 0;
            int lineCount = 0;

            @Override
            public void run() {
                // skip insides of HTML tags
                if (index < str.length() && str.charAt(index) == '<') {
                    int closingIndex = str.indexOf('>', index);
                    if (closingIndex > index)
                        index = closingIndex;
                }
                t.setText(Html.fromHtml((String) str.subSequence(0, index++)));
                if (lineCount != t.getLineCount()) {
                    lineCount = t.getLineCount();
                    scroll.smoothScrollBy(0, scroll.getMaxScrollAmount());
                }
                if (index <= str.length()) {
                    typeHandler.postDelayed(this, 10);
                } else {
                    typeHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            closeOptionsMenu();
                            mainActivity.showCurrentMessage(TYPING_FORCE_SHOW);
                        }
                    }, 5000);
                }
            }
        }, 10);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_changeNotification:
                boolean currentValue = getSharedPreferences(PREFS_NAME, 0).getBoolean(ShowNotificationDialog.notificationDbValue, false);

                getSharedPreferences(PREFS_NAME, 0).edit()
                        .putBoolean(ShowNotificationDialog.notificationDbValue, !currentValue)
                        .commit();
                if(currentValue){
                    Toast.makeText(getApplicationContext(), R.string.toast_notification_off, Toast.LENGTH_SHORT)
                        .show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.toast_notification_on, Toast.LENGTH_SHORT)
                            .show();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static final String PREFS_NAME = "MothershipPrefs";

    public static final int TYPING_DEFAULT = 0;
    public static final int TYPING_FORCE_SHOW = 1;

    /**
     * Checks which message should be shown. If that particular message has been
     * already shown, just make sure it's showing again (with setText). If it's
     * a new message, type it out ("typewriter effect").
     *
     * @param typingOption When set to TYPING_FORCE_SHOW, the message will be typed out
     *                     no matter if it's been already shown or not. Default is
     *                     TYPING_DEFAULT.
     */
    public void showCurrentMessage(int typingOption) {
        int currentUid = getSharedPreferences(PREFS_NAME, 0).getInt("currentUid", 0);
        Message currentMessage = Schedule.getCurrentMessage(currentUid);

        if (currentMessage == null) {
            currentMessage = new Message(Schedule.date(2012, 10, 7, 16, 3), "...");
        }

        // check if we ought to be showing a message sent via GCM
        long lastGcmMessageTime = getSharedPreferences(PREFS_NAME, 0).getLong("gcmMessageTime", 0);
        if (lastGcmMessageTime > currentMessage.time.getTime()) {
            String[] gcmMessages = Utils.loadArray("gcmMessages", getApplicationContext());
            if(gcmMessages.length == 0) {
                Log.v("TEST:" + TAG, "Showing single message from app schedule because of empty array!");
                showMessage(typingOption, currentMessage);
                return;
            }

            if (gcmMessages.length == 1) {
                Log.v("TEST:" + TAG, "Showing single message");

                currentMessage = new Message(new Date(lastGcmMessageTime), gcmMessages[0]);
                showMessage(typingOption, currentMessage);
            } else {
                Log.v("TEST:" + TAG, "Showing dialog with " + gcmMessages.length + " messages");

                showDialogMessages(typingOption, gcmMessages, new Date(lastGcmMessageTime));
            }
        } else {
            Log.v("TEST:" + TAG, "Showing single message from app schedule");
            showMessage(typingOption, currentMessage);
        }
    }

    private void showDialogMessages(int typingOption, String[] messages, Date messageTime) {
        if (messages != null) {
            Message message = new Message(messageTime, messages[messages.length - 1]);
            int currentUid = getSharedPreferences(PREFS_NAME, 0).getInt("currentUid", 0);
            boolean isNewMessage = message.uid != currentUid;
            Log.v(TAG, "Saving new currentUid = " + message.uid);
            getSharedPreferences(PREFS_NAME, 0).edit().putInt("currentUid", message.uid).commit();

            final LinearLayout ll = (LinearLayout) findViewById(R.id.linearLayout);
            if (ll.getChildCount() > 3)
                ll.removeViews(2, ll.getChildCount() - 3);

            message = new Message(messageTime, messages[0]);
            final TextView t = (TextView) findViewById(R.id.textView);
            final TextView signature = (TextView) findViewById(R.id.signature);
            final TextView mothershipSays = (TextView) findViewById(R.id.mothershipSays);


            if (message.ai_id == 1) {
                mothershipSays.setText(R.string.eddieSays);
                mothershipSays.setBackgroundColor(Color.BLUE);
                mothershipSays.setTextColor(Color.WHITE);
            } else {
                mothershipSays.setText(R.string.mothershipSays);
                mothershipSays.setBackgroundColor(Color.RED);
                mothershipSays.setTextColor(Color.BLACK);
            }

            t.setText(Html.fromHtml(message.text));

            for (int i = 1; i < messages.length; i++) {
                message = new Message(messageTime, messages[i]);
                int template = R.layout.agnes_says_template;
                if (message.ai_id == 1) {
                    template = R.layout.eddie_says_template;
                }
                TextView says = (TextView) getLayoutInflater().inflate(template, ll, false);
                final TextView text = (TextView) getLayoutInflater().inflate(R.layout.text_template, ll, false);
                final ScrollView scroll = ((ScrollView) findViewById(R.id.scrollView));

                ll.addView(says, (2 * i));
                if (message.ai_id == 0) {
                    // This is necessary, but I don't know why.
                    says.setBackgroundColor(Color.RED);
                }
                ll.addView(text, (2 * i + 1));

                if (i == (messages.length - 1) && (isNewMessage || typingOption == TYPING_FORCE_SHOW)) {
                    text.setText("");
                    signature.setText("");
                    final Animation fadeinAniDelayed = AnimationUtils.loadAnimation(this, R.anim.fade_in_delayed);

                    // typewriter effect
                    typeHandler.removeCallbacksAndMessages(null);
                    final Message finalMessage = message;
                    typeHandler.postDelayed(new Runnable() {
                        int index = 0;
                        int lineCount = 0;
                        String str = finalMessage.text;

                        @Override
                        public void run() {
                            // skip insides of HTML tags
                            if (index < str.length()) {
                                while (index < str.length() && str.charAt(index) == '<') {
                                    int closingIndex = str.indexOf('>', index);
                                    if (closingIndex > index)
                                        index = closingIndex + 1;
                                }
                                if (index < str.length()) {
                                    text.append(str.substring(index, index + 1));
                                    if (lineCount != text.getLineCount()) {
                                        lineCount = text.getLineCount();
                                        scroll.smoothScrollBy(0, scroll.getMaxScrollAmount());
                                    }
                                    index++;
                                }
                                if (index <= str.length()) {
                                    typeHandler.postDelayed(this, 30);
                                }
                            } else {
                                // now add with all markup, too
                                text.setText(Html.fromHtml((String) str));
                                // the small print should just fade in
                                signature.startAnimation(fadeinAniDelayed);
                                signature.setText(finalMessage.getTimeString());
                                scroll.smoothScrollBy(0, scroll.getMaxScrollAmount());
                            }
                        }
                    }, 10);
                } else {
                    // no typewriter effect
                    if (!typeHandler.hasMessages(0)) {
                        text.setText(Html.fromHtml(message.text));
                        signature.setText(message.getTimeString());
                        scroll.post(new Runnable() {
                            @Override
                            public void run() {
                                scroll.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
                    }
                }
            }
        }
    }


    /**
     * Shows new message either from saved messages or GCM.
     *
     * @param typingOption When set to TYPING_FORCE_SHOW, the message will be typed out
     *                     no matter if it's been already shown or not. Default is
     *                     TYPING_DEFAULT.
     * @param message      Message to be shown.
     */
    private void showMessage(int typingOption, final Message message) {
        if (message != null) {
            int currentUid = getSharedPreferences(PREFS_NAME, 0).getInt("currentUid", 0);
            boolean isNewMessage = message.uid != currentUid;
            Log.v(TAG, "Saving new currentUid = " + message.uid);
            getSharedPreferences(PREFS_NAME, 0).edit().putInt("currentUid", message.uid).commit();

            // Remove dialog from screen
            final LinearLayout ll = (LinearLayout) findViewById(R.id.linearLayout);
            if (ll.getChildCount() > 3)
                ll.removeViews(2, ll.getChildCount() - 3);

            final TextView t = (TextView) findViewById(R.id.textView);
            final TextView signature = (TextView) findViewById(R.id.signature);
            final TextView mothershipSays = (TextView) findViewById(R.id.mothershipSays);
            final ScrollView scroll = (ScrollView) findViewById(R.id.scrollView);

            if (message.ai_id == 1) {
                mothershipSays.setText(R.string.eddieSays);
                mothershipSays.setBackgroundColor(Color.BLUE);
                mothershipSays.setTextColor(Color.WHITE);
            } else {
                mothershipSays.setText(R.string.mothershipSays);
                mothershipSays.setBackgroundColor(Color.RED);
                mothershipSays.setTextColor(Color.BLACK);
            }
            // make sure system takes care of links in messages
            t.setMovementMethod(LinkMovementMethod.getInstance());

            if (isNewMessage || typingOption == TYPING_FORCE_SHOW) {
                t.setText("");
                signature.setText("");
                final Animation fadeinAniDelayed = AnimationUtils.loadAnimation(this, R.anim.fade_in_delayed);

                // typewriter effect
                typeHandler.removeCallbacksAndMessages(null);
                typeHandler.postDelayed(new Runnable() {
                    int index = 0;
                    int lineCount = 0;
                    String str = message.text;

                    @Override
                    public void run() {
                        // skip insides of HTML tags
                        if (index < str.length()) {
                            while (index < str.length() && str.charAt(index) == '<') {
                                int closingIndex = str.indexOf('>', index);
                                if (closingIndex > index)
                                    index = closingIndex + 1;
                            }
                            if (index < str.length()) {
                                t.append(str.substring(index, index + 1));
                                if (lineCount != t.getLineCount()) {
                                    lineCount = t.getLineCount();
                                    scroll.smoothScrollBy(0, scroll.getMaxScrollAmount());
                                }
                                index++;
                            }
                            if (index <= str.length()) {
                                typeHandler.postDelayed(this, 30);
                            }
                        } else {
                            // now add with all markup, too
                            t.setText(Html.fromHtml((String) str));
                            // the small print should just fade in
                            signature.startAnimation(fadeinAniDelayed);
                            signature.setText(message.getTimeString());
                            scroll.smoothScrollBy(0, scroll.getMaxScrollAmount());
                        }
                    }
                }, 10);
            } else {
                // no typewriter effect
                if (!typeHandler.hasMessages(0)) {
                    t.setText(Html.fromHtml(message.text));
                    signature.setText(message.getTimeString());
                }
            }
        }
    }

    /**
     * Sets up Google Cloud Messaging and registers with server if necessary.
     */
    private void setupGCM() {
        // Make sure the device has the proper dependencies.
        GCMRegistrar.checkDevice(this);
        // Make sure the manifest was properly set - comment out this line
        // while developing the app, then uncomment it when it's ready.
        GCMRegistrar.checkManifest(this);
        registerReceiver(mHandleMessageReceiver, new IntentFilter(CommonUtilities.DISPLAY_MESSAGE_ACTION));
        final String regId = GCMRegistrar.getRegistrationId(this);
        if (regId.equals("")) {
            // Automatically registers application on startup.
            GCMRegistrar.register(getApplicationContext(), CommonUtilities.SENDER_ID);
        } else {
            // Device is already registered on GCM, check server.
            if (GCMRegistrar.isRegisteredOnServer(this)) {
                // Skips registration.
                Log.i(TAG, "Skipping registration");
            } else {
                // Try to register again, but not in the UI thread.
                // It's also necessary to cancel the thread onDestroy(),
                // hence the use of AsyncTask instead of a raw thread.
                final Context context = getApplicationContext();
                mRegisterTask = new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        boolean registered = ServerUtilities.register(context, regId);
                        // At this point all attempts to register with the app
                        // server failed, so we need to unregister the device
                        // from GCM - the app will try to register again when
                        // it is restarted. Note that GCM will send an
                        // unregistered callback upon completion, but
                        // GCMIntentService.onUnregistered() will ignore it.
                        if (!registered) {
                            GCMRegistrar.unregister(context);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        mRegisterTask = null;
                    }

                };
                mRegisterTask.execute(null, null, null);
            }
        }
    }

    /**
     * Handles messages from GCM.
     */
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: Why is it there?
//            String newMessage = intent.getExtras().getString(CommonUtilities.EXTRA_MESSAGE);
//            Message message = new Message(new Date(), newMessage);
//            showMessage(TYPING_DEFAULT, message);
//            Log.v("TEST:" + TAG, "WTF?? Showing message: " + newMessage);
//
//            // put the newest message into a pref so we show it even after resuming the app
//            getSharedPreferences(PREFS_NAME, 0).edit()
//                    //	.putString("gcmMessageString", newMessage)
//                    .putLong("gcmMessageTime", new Date().getTime())
//                    .commit();
//            Utils.addToArray(newMessage, "gcmMessages", context);
        }
    };
}
