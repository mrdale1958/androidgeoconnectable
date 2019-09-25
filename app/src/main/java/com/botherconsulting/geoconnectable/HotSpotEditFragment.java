package com.botherconsulting.geoconnectable;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

/**
 * Created by dalemacdonald on 11/19/17.
 */

public class HotSpotEditFragment
        extends PreferenceFragmentCompat
{
    SharedPreferences sharedPreferences;
    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        // Load the Preferences from the XML file
        setPreferencesFromResource(R.xml.gct_preferences, rootKey); //
    }
 }