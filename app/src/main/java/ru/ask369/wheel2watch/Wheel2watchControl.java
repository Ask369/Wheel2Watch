/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB
Copyright (c) 2011-2013, Sony Mobile Communications AB

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB / Sony Mobile
 Communications AB nor the names of its contributors may be used to endorse or promote
 products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ru.ask369.wheel2watch;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlObjectClickEvent;
import com.sonyericsson.extras.liveware.extension.util.control.ControlTouchEvent;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * This demonstrates two different approaches, bitmap and layout, for displaying
 * a UI. The bitmap approach is useful for accessories without layout support,
 * e.g. SmartWatch.
 * This sample shows all UI components that can be used, except Gallery and
 * ListView.
 */
class Wheel2watchControl extends ControlExtension {
    /**
     * Intent broadcast from pebble.apk containing one-or-more key-value pairs sent from the watch to the phone.
     */
    public static final String INTENT_APP_RECEIVE = "com.getpebble.action.app.RECEIVE";
    public static final UUID PEBBLE_APP_UUID = UUID.fromString("185c8ae9-7e72-451a-a1c7-8f1e81df9a3d");
    public static final String APP_UUID = "uuid";


    WheelDataReceiver wdr = new WheelDataReceiver(this);

    Timer mTimer;

    String currTime = "00:00";
    String speedLabel = "0.0";
    String rideTimeLabel = "000/000m";
    String topSpeedLabel = "0.0";
    String distanceLabel = "0.0km";
    String battLabel = "0%";
    String tempLabel = "0°";

    long lastVibe = 0;
    long readyTime = Calendar.getInstance().getTimeInMillis();;

    public void setReadyTime() {
        this.readyTime = Calendar.getInstance().getTimeInMillis();
    }


    public void setVibe (String val, boolean send){
        long now = Calendar.getInstance().getTimeInMillis();
        if (send && now - lastVibe > 2000){
            if ("0".equals(val))
                startVibrator(400, 300, 2);
            else
                startVibrator(1000, 0, 1);
            lastVibe = now;
        }
    }
    public void setTempLabel(String val, boolean send) {
        tempLabel = val + "°";
        if (send)
            sendText(R.id.tv_temp, tempLabel);
    }

    public void setBattLabel(String val, boolean send) {
        battLabel = val + "%";
        if (send)
            sendText(R.id.tv_battery, battLabel);
    }

    public void setDistanceLabel(String val, boolean send) {
        distanceLabel = val.substring(0, val.length() - 1) + "." + val.charAt(val.length()-1) + "km";
        if (send)
            sendText(R.id.tv_distance, distanceLabel);
    }

    public void setTopSpeedLabel(String val, boolean send) {
        topSpeedLabel = val.substring(0, val.length() - 1) + "." + val.charAt(val.length()-1);
        if (send)
            sendText(R.id.tv_topspeed, topSpeedLabel);
    }

    public void setRideTimeLabel(String val, boolean send) {
        rideTimeLabel = Integer.parseInt(val)/60 + " / " +
                (Calendar.getInstance().getTimeInMillis()-readyTime)/60000L +"m";
        if (send)
            sendText(R.id.tv_ridetime, rideTimeLabel);
    }

    public void setSpeedLabel(String val, boolean send) {
        speedLabel = val.substring(0, val.length() - 1) + "." + val.charAt(val.length()-1);
        if (send)
            sendText(R.id.tv_speed, speedLabel);
    }

    public void setCurrTime(boolean send) {
        currTime = String.format("%1$tI:%1$tM", Calendar.getInstance());
        if (send)
            sendText(R.id.tv_time, currTime);
    }

    enum RenderType {
        LAYOUT, BITMAP
    }

    /** Contains the chosen UI to render, e.g. layout or bitmap. */
    private RenderType mRenderType = RenderType.LAYOUT;


    /**
     * Create control extension.
     *
     * @param hostAppPackageName Package name of host application.
     * @param context The context.
     * @param handler The handler to use.
     */
    Wheel2watchControl(final String hostAppPackageName, final Context context, Handler handler) {
        super(context, hostAppPackageName);
        Log.d(Wheel2watchExtensionService.LOG_TAG, "onCreate: Wheel2watchControl hostAppPackageName:" + hostAppPackageName);
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        SharedPreferences pref = context.getSharedPreferences(Wheel2watchRegistrationInformation.EXTENSION_KEY_PREF,
                Context.MODE_PRIVATE);
        pref.edit().putString(Wheel2watchExtensionReceiver.HOST_APP_PACKAGE_NAME, hostAppPackageName).commit();
    }

    @Override
    public void onDestroy() {
        Log.d(Wheel2watchExtensionService.LOG_TAG, "onDestroy: Wheel2watchControl");
    };

    @Override
    public void onObjectClick(final ControlObjectClickEvent event) {
        super.onObjectClick(event);
        Log.d(Wheel2watchExtensionService.LOG_TAG,
                "onObjectClick: Wheel2watchControl click type: " + event.getClickType());

    }

    @Override
    public void onTouch(ControlTouchEvent event) {
        super.onTouch(event);
        // play horn
        if (event.getY() < 100) {
            if (event.getX() > 110) {
                Intent intent = new Intent(INTENT_APP_RECEIVE);
                intent.putExtra(APP_UUID, PEBBLE_APP_UUID);
                intent.putExtra(WheelDataReceiver.TRANSACTION_ID, 1);
                intent.putExtra(WheelDataReceiver.MSG_DATA, "[{\"key\":10013,\"type\":\"int\",\"length\":4,\"value\":1}]");
                mContext.sendBroadcast(intent);
            } else {

                MediaPlayer mp = MediaPlayer.create(mContext, R.raw.bicycle_bell);
                mp.start();
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                    }
                });
            }
        }
    }

    /**
     * This is an example of how to update the entire layout and some of the
     * views. For each view, a bundle is used. This bundle must have the layout
     * reference, i.e. the view ID and the content to be used. This method
     * updates an ImageView and a TextView.
     *
     * @see Control.Intents#EXTRA_DATA_XML_LAYOUT
     * @see Registration.LayoutSupport
     */
    public void updateLayout() {
        Log.d(Wheel2watchExtensionService.LOG_TAG, "updateLayout: Wheel2watchControl");
        // Prepare a bundle to update the button text.
        Bundle bundle1 = new Bundle();
        bundle1.putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.tv_time);
        bundle1.putString(Control.Intents.EXTRA_TEXT, currTime);

        Bundle bundle2 = new Bundle();
        bundle2.putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.tv_speed);
        bundle2.putString(Control.Intents.EXTRA_TEXT, speedLabel);

        Bundle bundle3 = new Bundle();
        bundle3.putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.tv_ridetime);
        bundle3.putString(Control.Intents.EXTRA_TEXT, rideTimeLabel);

        Bundle bundle4 = new Bundle();
        bundle4.putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.tv_topspeed);
        bundle4.putString(Control.Intents.EXTRA_TEXT, topSpeedLabel);

        Bundle bundle5 = new Bundle();
        bundle5.putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.tv_distance);
        bundle5.putString(Control.Intents.EXTRA_TEXT, distanceLabel);

        Bundle bundle6 = new Bundle();
        bundle6.putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.tv_battery);
        bundle6.putString(Control.Intents.EXTRA_TEXT, battLabel);

        Bundle bundle7 = new Bundle();
        bundle7.putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.tv_temp);
        bundle7.putString(Control.Intents.EXTRA_TEXT, tempLabel);

        Bundle[] bundleData = new Bundle[7];
        bundleData[0] = bundle1;
        bundleData[1] = bundle2;
        bundleData[2] = bundle3;
        bundleData[3] = bundle4;
        bundleData[4] = bundle5;
        bundleData[5] = bundle6;
        bundleData[6] = bundle7;

        showLayout(R.layout.layout, bundleData);
    }

    /**
     * This method updates a non-bitmap TextView in the layout.
     */

    @Override
    public void onResume() {
        Log.d(Wheel2watchExtensionService.LOG_TAG, "onResume: Wheel2watchControl");
        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask(){
                            @Override
                            public void run() {

                                setCurrTime(true);
                            }

        }
                , 0L, 30000L);

        IntentFilter filter = new IntentFilter(WheelDataReceiver.INTENT_APP_SEND);
        mContext.getApplicationContext().registerReceiver(wdr, filter);


        updateLayout();

        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(Wheel2watchExtensionService.LOG_TAG, "onPause: Wheel2watchControl");
        if (wdr != null)
            mContext.getApplicationContext().unregisterReceiver(wdr);
        super.onPause();
    }

    @Override public void onActiveLowPowerModeChange(boolean lowPowerModeOn) {
        if (lowPowerModeOn) {
            // Adapt your app layout when the Low Power Mode is
            // enabled
        } else {
            // Adapt your app layout to the Low Power Mode is
            // disabled
        }
    }

}
