package com.botherconsulting.geoconnectable;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;

/**
 * Created by dalemacdonald on 4/6/18.
 */

public class MapZoomer extends ZoomLens {
    protected GoogleMap zoomObject;
    static final int ZOOM = 0;

    public MapZoomer( GoogleMap _zoomObject,
    int _clicksPerRev,
    int _revsPerFullZoom,
    double _maxZoom,
    double _minZoom,
    double _idleZoom) {

    }

    public void doZoom(int delta, boolean doLog) {
        currentSpinPosition += delta;
        currentSpinPosition = Math.max(minSpin, Math.min(currentSpinPosition, maxSpin));

        double proposedZoom = idleZoom + (double) currentSpinPosition / (double) clicksPerZoomLevel;
        if (doLog) {
            Log.i("zoom update", "delta:" + Integer.toString(delta) +
                    " new zoom: " +
                    //String.format ("%.2d", proposedZoom) +
                    proposedZoom +
                    " cSP: " +
                    Integer.toString(currentSpinPosition) +
                    " min:" + Integer.toString(minSpin) +
                    " max:" + Integer.toString(maxSpin)
            );
        }

        //restartIdleTimer();
        updateStats(delta);

        if (proposedZoom != currentZoom) {
            zoom = currentZoom;
            currentZoom = proposedZoom;
            newData = true;

        }

    }

    public boolean needToAnimate() {
        Object[] zoomData = this.getCurrentZoom();
        float newZoom = (float) zoomData[ZOOM];
        long lastZoomTime = (long) zoomData[GESTURE_TIME];
        int latIndex = (int) Math.round(zoomObject.getCameraPosition().target.latitude) + 90;
        int lonIndex = (int) Math.round(zoomObject.getCameraPosition().target.longitude) + 180;
        if (maxZoomCache[latIndex][lonIndex] > 0) {
            this.setZoomBounds(this.minZoom, maxZoomCache[latIndex][lonIndex]);
        } else if(Math.floor(newZoom) > zoomObject.getCameraPosition().zoom)  {
            checkMaxZoom( newZoom);
        }
    }

}
