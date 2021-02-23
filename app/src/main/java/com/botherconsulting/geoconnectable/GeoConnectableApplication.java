package com.botherconsulting.geoconnectable;

import android.app.Application;


/**
 * Created by dalemacdonald on 11/28/17.
 */

public class GeoConnectableApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        //AppWatcher.config = AppWatcher.config.copy(watchFragmentViews = false)
         // Normal app init code...
    }
}
