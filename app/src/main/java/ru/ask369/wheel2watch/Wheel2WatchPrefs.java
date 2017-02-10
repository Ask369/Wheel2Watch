package ru.ask369.wheel2watch;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by user on 03.02.2017.
 */

public class Wheel2WatchPrefs extends PreferenceActivity {
    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Wheel2watchControl wc = Wheel2watchControl.getInstance();
        if (wc != null){
            wc.loadPrefs();
        }
    }
}
