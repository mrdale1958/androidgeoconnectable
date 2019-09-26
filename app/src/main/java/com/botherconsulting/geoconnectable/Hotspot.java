package com.botherconsulting.geoconnectable;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

/**
 * Created by dwm160130 on 3/22/18.
 */

public class Hotspot {
    // tilt stuff
    static double TiltScaleX = 0.04; // in settings
    static double TiltScaleY = 0.04; // in settings
    static double panMax = 0.01;
    static int minSpin = -100;
    static int maxSpin = 100000000;
    static double validTiltThreshold = 0.025; // needs to be in settings
    static final int eventTiltWindowLength = 10000;
    long lastTiltMessageTime = System.nanoTime();
    int eventTiltCount = 0;
    long[] eventTiltWindow = new long[eventTiltWindowLength];
    long sumTiltElapsedTimes = 0;
    long sumTiltSquaredElapsedTimes = 0;
    int lastTiltMessageID = 0;

    //zoom stuff
    static double maxZoom = 19; // needs to be in settings
    static double minZoom = 3; // needs to be in settings
    static double currentZoom = 0;
    static int clicksPerRev = 2400; // in settings
    static int revsPerFullZoom = 19;  // in settings
    static int clicksPerZoomLevel = 1000;
    static int idleSpin = 0;
    static double idleZoom = 13.5; // in settings
    static double zoom = 0d;
    static final int eventZoomWindowLength = 10000;
    int currentSpinPosition = 0;
      int deltaZ = 0;
    long lastZoomMessageTime = System.nanoTime();
     int eventZoomCount = 0;
    long[] eventZoomWindow = new long[eventZoomWindowLength];
    long sumZoomElapsedTimes = 0;
    long sumZoomSquaredElapsedTimes = 0;
    int lastZoomMessageID = 0;

// public stuff
    public boolean newData = false;
    public boolean enabled;
    public String set;
    public java.net.URL URL;
    public Marker marker;
    public Double[] hotSpotZoomTriggerRange = {15.0, 19.0};
    public Double[] currentTilt = {0.0, 0.0};
    GoogleMap mMap;
    static enum States {
        CLOSED,
        OPENING,
        CLOSING,
        OPEN,
        THURSDAY,
        FRIDAY,
        SATURDAY;

    }
    States state;


    public Hotspot(GoogleMap map) {
        state = States.CLOSED;
        this.enabled = false;
        this.set = "default";
        this.marker=null;
        this.mMap = map;
/*        this.marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(0.0,0.0))
                .title("some pithy name")
                .snippet("Even pithier label")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
*/
    }

    public void setIcon(BitmapDescriptor icon) {
        if (this.marker == null) {
            this.marker = this.mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(0.0,0.0))
                    .title("some pithy name")
                    .snippet("Even pithier label"));
        }
        this.marker.setIcon(icon);
    }
    public void setURL(String url) {
        try {
            this.URL = new java.net.URL(url);
        }
        catch (java.net.MalformedURLException e) {
            Log.e("GCT HotSpot error: bad url", e.getMessage());
        }
    }
    public void setTitle(String title) {
        this.marker.setTitle(title);
    }
    public void setSnippet(String snippet) {
        this.marker.setSnippet(snippet);
    }
    public void setPosition(LatLng position) {
        this.marker.setPosition(position);
    }
    public void setZoomTriggerRange(Double minZoom, Double maxZoom) {
        this.hotSpotZoomTriggerRange[0] = minZoom;
        this.hotSpotZoomTriggerRange[1] = maxZoom;
    }

    public void manageState() {
        switch (state) {
            case CLOSED:
                break;
            case OPEN:
                break;
            case OPENING:
                break;
            case CLOSING:
                break;

        }
    }

    protected void reportZoomStats() {
        long windowSum = 0;
        long windowSumSquared = 0;
        long maxEt = 0;
        long minEt = Long.MAX_VALUE;
        for (long i:eventZoomWindow) {
            windowSum+=i;
            windowSumSquared+= i*i;
            maxEt = Math.max(maxEt, i);
            minEt = Math.min(minEt, i);
        }
        double diff = (windowSumSquared - eventZoomWindow.length * (windowSum / eventZoomWindowLength)*(windowSum / eventZoomWindowLength));

        Log.i("GCT HS: Zoom data stats",
                "\nTotal events: " + eventZoomCount +
                        "\nTotal mean elapsed Time: " + (sumZoomElapsedTimes/eventZoomCount) +
                        "\nwindow mean elapsedTime: " + (windowSum / eventZoomWindowLength) +
                        "\nWindow total ET: " + windowSum +
                        "\nWindow sigma ET: " + Math.sqrt(diff/eventZoomWindowLength) +
                        "\nWindow max ET: " + maxEt +
                        "\nWindow min ET: " + minEt +
                        "\nlast message ID: " + lastZoomMessageID +
                        "\nlast delta: " + deltaZ
        );
    }

    protected void updateZoomStats() {
        long elapsedTime = System.nanoTime() - lastZoomMessageTime;

        lastZoomMessageTime = System.nanoTime();
        if (elapsedTime > 150000000) Log.w("Big data gap", Long.toString(elapsedTime));
        eventZoomCount++;
        sumZoomElapsedTimes += elapsedTime;
        sumZoomSquaredElapsedTimes += elapsedTime * elapsedTime;
        eventZoomWindow[eventZoomCount % eventZoomWindowLength] = elapsedTime;
        if (eventZoomCount % eventZoomWindowLength == 0) {
            reportZoomStats();
        }
    }

    protected void reportTiltStats() {
        long windowSum = 0;
        long windowSumSquared = 0;
        long maxEt = 0;
        long minEt = Long.MAX_VALUE;
        for (long i:eventTiltWindow) {
            windowSum+=i;
            windowSumSquared+= i*i;
            maxEt = Math.max(maxEt, i);
            minEt = Math.min(minEt, i);
        }
        Log.i("GCT HS: Tilt data stats",
                "\nTotal events: " + eventTiltCount +
                        "\nTotal mean elapsed Time: " + (sumTiltElapsedTimes/eventTiltCount) +
                        "\nwindow mean elapsedTime: " + (windowSum / eventTiltWindowLength) +
                        "\nWindow max ET: " + maxEt +
                        "\nWindow min ET: " + minEt
        );
    }

    protected void updateTiltStats() {
        long elapsedTime = System.nanoTime() - lastTiltMessageTime;
        lastTiltMessageTime = System.nanoTime();
        eventTiltCount++;
        sumTiltElapsedTimes += elapsedTime;
        sumTiltSquaredElapsedTimes += elapsedTime * elapsedTime;
        eventTiltWindow[eventTiltCount % eventTiltWindowLength] = elapsedTime;
        if (eventTiltCount % eventTiltWindowLength == 0) {
            //reportTiltStats();
        }
    }


    public Boolean handleJSON(JSONObject message, GoogleMap mMap, boolean doLog)
        {
             return false;
        }
}
