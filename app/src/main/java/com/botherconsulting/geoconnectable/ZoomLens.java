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
    public int clicksPerRev = 4800; // in settings
    public int revsPerFullZoom = 19;  // in settings
    private int clicksPerZoomLevel;
    private int idleSpin = 0;
    public double idleZoom = 13.5; // in settings
    public double zoom;
    private int minSpin;
    private int maxSpin;
    public boolean newData = false;
    private int eventCount = 0;
    long lastZoomMessageTime = System.nanoTime();
    static final int eventWindowLength = 10000;
    long[] eventWindow = new long[eventWindowLength];
    long sumElapsedTimes = 0;
    long sumSquaredElapsedTimes = 0;
    int lastMessageID = 0;
    private  int delta = 0;
    private  long bigDataArrivalGap = 150000000; // 150ms


    public ZoomLens(int _clicksPerRev, int _revsPerFullZoom, double _maxZoom, double _minZoom, double _idleZoom) {
        minZoom = _minZoom;
        maxZoom = _maxZoom;
        configure(_clicksPerRev,_revsPerFullZoom, _idleZoom);
    }

    public Object[] getCurrentZoom() {
        newData = false;
        return new Object[]{(float) zoom, lastZoomMessageTime} ;
    }

    private void reportStats() {
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
        double diff = (windowSumSquared - eventWindow.length * (windowSum / eventWindowLength)*(windowSum / eventWindowLength));

        Log.i("GCT: Zoom data flow stats",
                "\nTotal events: " + eventCount +
                        "\nTotal mean elapsed Time: " + (sumElapsedTimes/eventCount) +
                        "\nwindow mean elapsedTime: " + (windowSum / eventWindowLength) +
                        "\nWindow total ET: " + windowSum +
                        "\nWindow sigma ET: " + Math.sqrt(diff/eventWindowLength) +
                        "\nWindow max ET: " + maxEt +
                        "\nWindow min ET: " + minEt +
                        "\nlast message ID: " + lastMessageID +
                "\nlast delta: " + delta
        );
    }

    private void updateStats() {
        long elapsedTime = System.nanoTime() - lastZoomMessageTime;

        lastZoomMessageTime = System.nanoTime();
        if (elapsedTime > bigDataArrivalGap) Log.i("GCT: zoom Big data gap", Long.toString(elapsedTime / 1000000)+"ms");
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
            Log.e("GCT error: reading zoom message", "no vector " + message.toString());
        }
        try {
            delta = vector.getInt("delta");
            //int messageID = message.getInt("id");
            //if (messageID > lastMessageID + 1)
            //    Log.w("reading zoom data","got" + Integer.toString(messageID) + " after" + Integer.toString(lastMessageID));
            //lastMessageID = messageID;
        } catch (org.json.JSONException e) {
            Log.e("GCT error: reading zoom message", "invalid vector " + vector.toString() + " or ID " + message);
        }
        currentSpinPosition += delta;
        currentSpinPosition = Math.max(minSpin,Math.min(currentSpinPosition,maxSpin));

        double proposedZoom = idleZoom + (double)currentSpinPosition / (double)clicksPerZoomLevel;
        if (doLog) {
            Log.i("GCT: zoom update", "delta:" + Integer.toString(delta) +
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
        updateStats();

        if (proposedZoom != currentZoom) {
            zoom = currentZoom;
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
