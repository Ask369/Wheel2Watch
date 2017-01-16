package ru.ask369.wheel2watch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class WheelDataReceiver extends BroadcastReceiver {
    public static final String INTENT_APP_SEND = "com.getpebble.action.app.SEND";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String APP_UUID = "uuid";
    public static final String MSG_DATA = "msg_data";
    public static final String INTENT_APP_RECEIVE_ACK = "com.getpebble.action.app.RECEIVE_ACK";

    public static final String ACTION_BLUETOOTH_CONNECTION_STATE = "com.cooper.wheellog.bluetoothConnectionState";
    public static final String INTENT_EXTRA_CONNECTION_STATE = "connection_state";

    public static final String ACTION_PEBBLE_APP_SCREEN = "com.cooper.wheellog.pebbleAppScreen";
    public static final String INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN = "pebble_displayed_Screen";

    public static final long DETAIL_MODE_DELAY = 5000L;
    public static final long MIN_GET_INFO_DELAY = 200L;

    static final String KEY_SPEED = "0";
    static final String KEY_BATTERY = "1";
    static final String KEY_TEMPERATURE = "2";
    static final String KEY_FAN_STATE = "3";
    static final String KEY_BT_STATE = "4";
    static final String KEY_VIBE_ALERT = "5";
    static final String KEY_USE_MPH = "6";
    static final String KEY_MAX_SPEED = "7";
    static final String KEY_RIDE_TIME = "8";
    static final String KEY_DISTANCE = "9";
    static final String KEY_TOP_SPEED = "10";
    static final String KEY_READY = "11";

    Wheel2watchControl wheelControl;
    final Intent intent_received = new Intent(INTENT_APP_RECEIVE_ACK);
    long lastTimeReceive;
    long lastTimeDetails;
    boolean isDetailsMode = false;

    public WheelDataReceiver(Wheel2watchControl main) {
        super();
        Log.d(Wheel2watchExtensionService.LOG_TAG, "onCreate: WheelDataReceiver");
        wheelControl = main;
        intent_received.putExtra(TRANSACTION_ID, 1);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Wheel2watchExtensionService.LOG_TAG, "onReceive: WheelDataReceiver");
        String action = intent.getAction();
        if (INTENT_APP_SEND.equals(action)){
            if (isDetailsMode)
                switchMode(context);
            long newTime = Calendar.getInstance().getTimeInMillis();
            if ( newTime - lastTimeReceive > MIN_GET_INFO_DELAY) {
                Log.d(Wheel2watchExtensionService.LOG_TAG, "newTime:" + newTime + ", lastTimeReceive:" + lastTimeReceive);
                lastTimeReceive =  newTime;
                int tran = intent.getIntExtra(TRANSACTION_ID, 0);
                parseData(intent.getStringExtra(MSG_DATA));
                context.sendBroadcast(intent_received);
                if (!isDetailsMode && newTime - lastTimeDetails > DETAIL_MODE_DELAY){
                    lastTimeDetails = newTime;
                    wheelControl.setWheelConnected(true);
                    switchMode(context);
                }
            }
        } else if (ACTION_BLUETOOTH_CONNECTION_STATE.equals(action)){
            wheelControl.setWheelConnected(intent.getBooleanExtra(INTENT_EXTRA_CONNECTION_STATE, false));
        }

    }

    private void switchMode(Context context) {
        Log.d(Wheel2watchExtensionService.LOG_TAG, "switchMode: WheelDataReceiver isDetailsMode:" + isDetailsMode);
        final Intent i = new Intent(ACTION_PEBBLE_APP_SCREEN);
        if (!isDetailsMode)
            i.putExtra(INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN, 1);
        else
            i.putExtra(INTENT_EXTRA_PEBBLE_DISPLAYED_SCREEN, 0);
        context.sendBroadcast(i);
        isDetailsMode = !isDetailsMode;
    }

    private void parseData(String data){
        Log.d(Wheel2watchExtensionService.LOG_TAG, "parseData: WheelDataReceiver");
        if (data != null) {
            Log.d(Wheel2watchExtensionService.LOG_TAG, "parseData: data:" + data);
            boolean refreshAll = data.length() > 200;

            int i = 0; // start key
            int e;     // end key and val
            int v;     // start value

            // extract key and value from string ex: {"key":4,"type":"int","length":4,"value":1},{"key":1,"type":"int","length":4,"value":100}
            while (i < data.length() && i >= 0) {
                String key;
                String val;
                i = data.indexOf('{', i);
                if (i != -1) {
                    i+=7;
                    e = data.indexOf(',', i);
                    if (e != -1) {
                        key = data.substring(i, e);
                        e = data.indexOf('}', i);
                        if (e != -1) {
                            v = data.lastIndexOf(':', e);
                            if (v != -1) {
                                val = data.substring(++v, e);
                                Log.d(Wheel2watchExtensionService.LOG_TAG, "parseData: key:" + key + ", val:" + val);
                                setWheelVar(key, val, !refreshAll);
                            }
                        }
                    }
                }
            }
            if (refreshAll) {
                wheelControl.updateLayout();
            }
        }

    }

    private void setWheelVar(String key, String val, boolean immediate) {
        Log.d(Wheel2watchExtensionService.LOG_TAG, "setWheelVar: WheelDataReceiver: key:" + key + ", val:" + val);

        if (KEY_SPEED.equals(key)){
            wheelControl.setSpeedLabel(val, immediate);
        } else if (KEY_BATTERY.equals(key)){
            wheelControl.setBattLabel(val, immediate);
        } else if (KEY_TEMPERATURE.equals(key)){
            wheelControl.setTempLabel(val, immediate);
        } else if (KEY_VIBE_ALERT.equals(key)){
            wheelControl.setVibe(val, immediate);
        } else if (KEY_RIDE_TIME.equals(key)){
            wheelControl.setRideTimeLabel(val, immediate);
        } else if (KEY_DISTANCE.equals(key)){
            wheelControl.setDistanceLabel(val, immediate);
        } else if (KEY_TOP_SPEED.equals(key)){
            wheelControl.setTopSpeedLabel(val, immediate);
        } else if (KEY_READY.equals(key)){
            wheelControl.setReadyTime();
        }
    }
}
