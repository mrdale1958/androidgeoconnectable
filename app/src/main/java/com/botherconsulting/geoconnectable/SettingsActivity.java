package com.botherconsulting.geoconnectable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    public static final String
            KEY_PREF_SENSOR_SERVER = "sensor_server";
    public static final String
            KEY_PREF_SENSOR_SERVER_PORT = "sensor_server_port";
    public static final String
            KEY_PREF_SPIN_SENSOR_CLICKS_PER_REV = "spin_sensor_clicks_per_rev";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new GCTPreferenceFragment())
            .commit();
    }

}