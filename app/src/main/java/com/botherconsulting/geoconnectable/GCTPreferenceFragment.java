package com.botherconsulting.geoconnectable;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

/**
 * Created by dalemacdonald on 11/19/17.
 */

public class GCTPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            // Load the Preferences from the XML file
            setPreferencesFromResource(R.xml.gct_preferences, rootKey); //
        }
    }