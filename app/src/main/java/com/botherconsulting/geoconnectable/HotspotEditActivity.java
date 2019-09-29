package com.botherconsulting.geoconnectable;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.EditTextPreference;
import android.util.Log;
import android.view.WindowManager;

import java.util.Map;

public class HotspotEditActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    // figure out list manipulation
    // figure out editing list items
    // list items consist of Title, Lat, Long
    public static final String
            KEY_PREF_SENSOR_SERVER = "sensor_server";
    public static final String
            KEY_PREF_SENSOR_SERVER_PORT = "sensor_server_port";
    public static final String
            KEY_PREF_SPIN_SENSOR_CLICKS_PER_REV = "spin_sensor_clicks_per_rev";
    public static final String
            KEY_PREF_SPIN_SENSOR_REVS_PER_FULL_ZOOM = "spin_sensor_revs_per_full_zoom";
    public static final String
            KEY_PREF_TILT_SENSOR_SCALE_FACTOR = "tilt_sensor_scale_factor";
    public static final String
            KEY_PREF_USE_HYBRID_MAP = "use_hybrid_map";
    public static final String
            KEY_PREF_TARGET_VISIBLE = "target_visible";
    public static final String
            KEY_PREF_TARGET_COLOR = "target_color";
    public static final String
            KEY_PREF_TARGET_SIZE = "target_size";
    public static final String
            KEY_PREF_IDLE_TEXT_BOTTOM = "idle_text_bottom";
    public static final String
            KEY_PREF_IDLE_TEXT_TOP = "idle_text_top";
    public static final String
            KEY_PREF_HORIZONTAL_BUMP = "horizontal_bump";
    public static final String
            KEY_PREF_IDLE_TIME = "idle_time";
    public static final String
            KEY_PREF_IDLE_LAT = "idle_latitude";
    public static final String
            KEY_PREF_IDLE_LON = "idle_longitude";
    public static final String
            KEY_PREF_IDLE_ZOOM = "idle_zoom";
    public static final String
            KEY_PREF_VIEW_STATS_LOCH = "view_stats_location_horizontal";
    public static final String
            KEY_PREF_VIEW_STATS_LOCV = "view_stats_location_vertical";
    public static final String
            KEY_PREF_VIEW_STATS_UNITS = "view_stats_units";
     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new HotSpotEditFragment())
            .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("HotspotEditActivity", "resuming preference edit");
        //getPreferenceScreen().getSharedPreferences()
        //        .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("HotspotEditActivity", "pausing preference edit");
        //getPreferenceScreen().getSharedPreferences()
        //        .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Map<String, ?> preferencesMap = sharedPreferences.getAll();

        Log.i("HotspotEditActivity", "onSharedPreferenceChanged");
        // get the preference that has been changed
        Object changedPreference = preferencesMap.get(key);
        // and if it's an instance of EditTextPreference class, update its summary
        if (preferencesMap.get(key) instanceof EditTextPreference) {
            updateSummary((EditTextPreference) changedPreference);
        }
    }

    private void updateSummary(EditTextPreference preference) {
        // set the EditTextPreference's summary value to its current text
        Log.i("HotspotEditActivity", "updateSummary: " + preference.getText());
        preference.setSummary(preference.getText());
    }


}