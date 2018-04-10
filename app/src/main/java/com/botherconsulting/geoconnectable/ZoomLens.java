package com.botherconsulting.geoconnectable;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONObject;

/**
 * Created by dalemacdonald on 11/28/17.
 */

public class ZoomLens {
    public double maxZoom = 19; // needs to be in settings
    public double minZoom = 3; // needs to be in settings
    public double currentZoom = 0;
    protected int currentSpinPosition = 0;
    public int clicksPerRev = 2400; // in settings
    public int revsPerFullZoom = 19;  // in settings
    protected int clicksPerZoomLevel;
    protected int idleSpin = 0;
    public double idleZoom = 13.5; // in settings
    public double zoom;
    protected int minSpin;
    protected int maxSpin;
    public boolean newData = false;
    protected int eventCount = 0;
    long lastZoomMessageTime = System.nanoTime();
    static final int eventWindowLength = 10000;
    long[] eventWindow = new long[eventWindowLength];
    long sumElapsedTimes = 0;
    long sumSquaredElapsedTimes = 0;
    protected  int delta = 0;
    public Object zoomObject = mapZoom;

    public ZoomLens() {

    }

    public ZoomLens(Object _zoomObject,
                    int _clicksPerRev,
                    int _revsPerFullZoom,
                    double _maxZoom,
                    double _minZoom,
                    double _idleZoom) {
        zoomObject = zoomObject;
        minZoom = _minZoom;
        maxZoom = _maxZoom;
        configure(_clicksPerRev,_revsPerFullZoom, _idleZoom);
    }

    public Object[] getCurrentZoom() {
        newData = false;
        return new Object[]{(float) zoom, lastZoomMessageTime} ;
    }

    protected void reportStats() {
        long windowSum = 0;
        long windowSumSquared = 0;
        long maxEt = 0;
        long minEt = Long.MAX_VALUE;
        for (long i:eventWindow) {
            windowSum+=i;
            windowSumSquared+= i*i;
            maxEt = Math.max(maxEt, i);
            minEt = Math.min(minEt, i);
        }
        Log.i("Zoom data flow stats",
                "\nTotal events: " + eventCount +
                        "\nTotal mean elapsed Time: " + (sumElapsedTimes/eventCount) +
                        "\nwindow mean elapsedTime: " + (windowSum / eventWindowLength) +
                        "\nWindow total ET: " + windowSum +
                        "\nWindow max ET: " + maxEt +
                        "\nWindow min ET: " + minEt +
                "\nlast delta: " + delta
        );
    }

    protected void updateStats() {
        long elapsedTime = System.nanoTime() - lastZoomMessageTime;

        lastZoomMessageTime = System.nanoTime();
        if (elapsedTime > 100000000) Log.i("Big data gap", Long.toString(elapsedTime));
        eventCount++;
        sumElapsedTimes += elapsedTime;
        sumSquaredElapsedTimes += elapsedTime * elapsedTime;
        eventWindow[eventCount % eventWindowLength] = elapsedTime;
        if (eventCount % eventWindowLength == 0) {
            reportStats();
        }
    }


    public void handleJSON(JSONObject message, GoogleMap mMap, boolean doLog) {

        delta = 0;
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
        if (zoomObject == mapZoom) {
        } else if (zoomObject == hotspotZoom) {

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

    protected void logstate() {
        Log.i("zoom state", "maxZoom:" + String.format("%.2f", maxZoom) +
                " minZoom:" + String.format("%.2f", minZoom) +
                " idleZoom:" + String.format("%.2f", idleZoom) +
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
