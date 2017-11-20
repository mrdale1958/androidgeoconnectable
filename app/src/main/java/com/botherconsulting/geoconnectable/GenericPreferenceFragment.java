package com.botherconsulting.geoconnectable;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by dalemacdonald on 11/19/17.
 */

public class GenericPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int preferenceFile_toLoad=-1;
        String settings = getArguments().getString("settings");
        if (Constants.SETTING_SENSORS.equals(settings)) {
            // Load the preferences from an XML resource
            preferenceFile_toLoad= R.xml.sensor_settings;
        }else if (Constants.SETTING_UI.equals(settings)) {
            // Load the preferences from an XML resource
            preferenceFile_toLoad=R.xml.ui_settings;
        }/*else if (Constants.SETTING_NOTIFY.equals(settings)) {
            // Load the preferences from an XML resource
            preferenceFile_toLoad=R.xml.preference_notify;
        }*/

        addPreferencesFromResource(preferenceFile_toLoad);
    }
}