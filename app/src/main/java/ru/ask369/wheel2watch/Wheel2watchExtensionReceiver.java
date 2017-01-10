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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.registration.Registration;

/**
 * The extension receiver receives the extension intents and starts the
 * extension service when they arrive.
 */
public class Wheel2watchExtensionReceiver extends BroadcastReceiver {
    /**
     * Intent broadcast to pebble.apk responsible for launching a watch-app on the connected watch. This intent is
     * idempotent.
     */
    public static final String INTENT_APP_START = "com.getpebble.action.app.START";

    /**
     * Intent broadcast to pebble.apk responsible for closing a running watch-app on the connected watch. This intent is
     * idempotent.
     */
    public static final String INTENT_APP_STOP = "com.getpebble.action.app.STOP";
    public static final String HOST_PACKAGE_DEF = "com.sonymobile.smartconnect.smartwatch2";
    public static final String HOST_APP_PACKAGE_NAME = "HOST_APP_PACKAGE_NAME";

    public static String HOST_PACKAGE;

    private String getHostPackage(final Context context){
        if (HOST_PACKAGE == null){
            SharedPreferences pref = context.getSharedPreferences(Wheel2watchRegistrationInformation.EXTENSION_KEY_PREF,
                    Context.MODE_PRIVATE);
            if (pref.contains(HOST_APP_PACKAGE_NAME)) {
                HOST_PACKAGE = pref.getString(HOST_APP_PACKAGE_NAME, HOST_PACKAGE_DEF);
            } else
                return HOST_PACKAGE_DEF;
        }
        return HOST_PACKAGE;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

        Log.d(Wheel2watchExtensionService.LOG_TAG, "onReceive: " + intent.getAction());
        if (intent.getAction().equals(INTENT_APP_START)) {
            Log.d(Wheel2watchExtensionService.LOG_TAG, "onReceive: INTENT_APP_START: " + context.getPackageName());
            Intent i = new Intent(Control.Intents.CONTROL_START_REQUEST_INTENT);
            i.putExtra(Control.Intents.EXTRA_AEA_PACKAGE_NAME, context.getPackageName());
            i.setPackage(getHostPackage(context));
            context.sendBroadcast(i, Registration.HOSTAPP_PERMISSION);
        }else if (intent.getAction().equals(INTENT_APP_STOP)){
            Intent i = new Intent(Control.Intents.CONTROL_STOP_REQUEST_INTENT);
            i.putExtra(Control.Intents.EXTRA_AEA_PACKAGE_NAME, context.getPackageName());
            i.setPackage(getHostPackage(context));
            context.sendBroadcast(i, Registration.HOSTAPP_PERMISSION);
        }else{
            intent.setClass(context, Wheel2watchExtensionService.class);
            context.startService(intent);
        }
    }
}
