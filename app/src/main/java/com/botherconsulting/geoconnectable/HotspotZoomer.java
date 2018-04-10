package com.botherconsulting.geoconnectable;

import android.util.Log;

/**
 * Created by dalemacdonald on 4/6/18.
 */

public class HotspotZoomer extends ZoomLens {
        public void doZoom(int delta, boolean doLog) {
            currentSpinPosition += delta;
            currentSpinPosition = Math.max(minSpin, Math.min(currentSpinPosition, maxSpin));
        }
}