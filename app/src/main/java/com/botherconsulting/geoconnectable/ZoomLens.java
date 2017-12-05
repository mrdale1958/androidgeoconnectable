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
    public boolean newData = false;

    public ZoomLens(int _clicksPerRev, int _revsPerFullZoom, double _maxZoom, double _minZoom, double _idleZoom) {
        minZoom = _minZoom;
        maxZoom = _maxZoom;
        configure(_clicksPerRev,_revsPerFullZoom, _idleZoom);
    }

    public float getCurrentZoom() {
        newData = false;
        return (float) currentZoom;
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
                    String.format ("%.2f", proposedZoom) +
                    " cSP: " +
                    Integer.toString(currentSpinPosition) +
                    " min:" + Integer.toString(minSpin) +
                    " max:" + Integer.toString(maxSpin)
            );
        }

        //restartIdleTimer();

        if (proposedZoom != currentZoom) {
            currentZoom = proposedZoom;
            newData = true;

        }


    }

    public void setZoomBounds(double _minZoom, double _maxZoom) {
        minZoom = _minZoom;
        maxZoom = _maxZoom;
        minSpin = -(int) ((idleZoom - minZoom) * (double) clicksPerZoomLevel);
        maxSpin = (int) ((maxZoom - idleZoom) * (double) clicksPerZoomLevel);
        //logstate();
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
        logstate();
    }

    private void logstate() {
        Log.i("zoom state", "maxZoom:" + String.format ("%.2f", maxZoom) +
                " minZoom:" + String.format ("%.2f", minZoom) +
                " idleZoom:" + String.format ("%.2f", idleZoom) +
                "\ncSP: " +
                Integer.toString(currentSpinPosition) +
                " min:" + Integer.toString(minSpin) +
                " max:" + Integer.toString(maxSpin) +
                "\nclicksPerZoomLevel:" + Integer.toString(clicksPerZoomLevel) +
                " clicksPerRev:" + Integer.toString(clicksPerRev) +
                        " revsPerFullZoom:" + Integer.toString(revsPerFullZoom)
        );
    }
}
