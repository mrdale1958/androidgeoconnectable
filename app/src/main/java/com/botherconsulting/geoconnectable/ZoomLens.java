package com.botherconsulting.geoconnectable;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONObject;

/**
 * Created by dalemacdonald on 11/28/17.
 */

public class ZoomLens {
    public double maxZoom = 19; // needs to be in settings
    public double minZoom = 0; // needs to be in settings
    public double currentZoom = 0;
    private int currentSpinPosition = 0;
    public int clicksPerRev = 2400; // in settings
    public int revsPerFullZoom = 19;  // in settings
    private int clicksPerZoomLevel;
    private int idleSpin = 0;
    public double idleZoom = 13.5; // in settings
    private int minSpin;
    private int maxSpin;

    public ZoomLens(int _clicksPerRev, int _revsPerFullZoom, double _maxZoom, double _minZoom, double _idleZoom) {
        minZoom = _minZoom;
        maxZoom = _maxZoom;
        configure(_clicksPerRev,_revsPerFullZoom, _idleZoom);
    }

    private void setSpinBounds(){
        minSpin = -(int)((idleZoom - minZoom) * (double)clicksPerZoomLevel);
        maxSpin = (int)((maxZoom - idleZoom) * (double)clicksPerZoomLevel);
    }

    public void handleJSON(JSONObject message, GoogleMap mMap, boolean doLog) {
        int delta = 0;
        JSONObject vector = new JSONObject();
        try {
            vector = message.getJSONObject("vector");
        } catch (org.json.JSONException e) {
            Log.e("reading zoom message", "no vector " + message.toString());
        }
        try {
            delta = vector.getInt("delta");
        } catch (org.json.JSONException e) {
            Log.e("reading zoom message", "invalid vector " + vector.toString());
        }
        currentSpinPosition += delta;
        currentSpinPosition = Math.max(minSpin,Math.min(currentSpinPosition,maxSpin));

        double proposedZoom = idleZoom + (double)currentSpinPosition / (double)clicksPerZoomLevel;
        if (doLog) {
            Log.i("zoom update", "delta:" + Integer.toString(delta) +
                    " new zoom: " +
                    Double.toString(proposedZoom) +
                    " currentSpinPosition: " +
                    Integer.toString(currentSpinPosition));
        }

        //restartIdleTimer();

        //if (proposedZoom != currentZoom) {
        //doZoom(Math.min(Object.keys(zoomLayers).length - 1, Math.max(0,proposedZoom)));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo((float) (proposedZoom)),1,null);
        //mMap.moveCamera(CameraUpdateFactory.zoomTo((float) (proposedZoom)));
        currentZoom = proposedZoom;

        //}


    }

    public void setZoomBounds(double _minZoom, double _maxZoom) {
        minZoom = _minZoom;
        maxZoom = _maxZoom;
        minSpin = -(int) ((idleZoom - minZoom) * (double) clicksPerZoomLevel);
        maxSpin = (int) ((maxZoom - idleZoom) * (double) clicksPerZoomLevel);
    }

    public void configure(int _clicksPerRev,
                          int _revsPerFullZoom,
                          double _idleZoom) {
        clicksPerRev = _clicksPerRev;
        revsPerFullZoom = _revsPerFullZoom;
        idleZoom = _idleZoom;
        clicksPerZoomLevel = clicksPerRev * revsPerFullZoom / (int) (maxZoom - minZoom);
        setZoomBounds(minZoom, maxZoom);
        minSpin = -(int) ((idleZoom - minZoom) * (double) clicksPerZoomLevel);
        maxSpin = (int) ((maxZoom - idleZoom) * (double) clicksPerZoomLevel);
    }
}
