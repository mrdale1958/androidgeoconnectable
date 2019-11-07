package com.botherconsulting.geoconnectable;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

    static double validTiltThreshold = 0.025; // needs to be in settings
    static final int eventTiltWindowLength = 10000;
    private long lastTiltMessageTime = System.nanoTime();
    private int eventTiltCount = 0;
    private long[] eventTiltWindow = new long[eventTiltWindowLength];
    private long sumTiltElapsedTimes = 0;
    private long sumTiltSquaredElapsedTimes = 0;
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
    private int currentSpinPosition = 0;
    private int deltaZ = 0;
    private long lastZoomMessageTime = System.nanoTime();
    private int eventZoomCount = 0;
    private long[] eventZoomWindow = new long[eventZoomWindowLength];
    private long sumZoomElapsedTimes = 0;
    private long sumZoomSquaredElapsedTimes = 0;
    private int lastZoomMessageID = 0;
    private String title;
    private BitmapDescriptor icon;
    private BitmapDescriptor selectedIcon;

    // public stuff
    public boolean newData = false;
     boolean enabled;
    public String set;
    public java.net.URL URL;
     Marker marker;
     Double[] hotSpotZoomTriggerRange = {13.0, 19.0};
    public Double[] currentTilt = {0.0, 0.0};
     int minSpin = -100;
     int maxSpin = 100000000;
    private GoogleMap mMap;
    public LatLng position;

     enum States {
        CLOSED,
        OPENING,
        CLOSING,
        OPEN
    }

    States state;


     Hotspot(GoogleMap map) {
        state = States.CLOSED;
        this.enabled = false;
        this.set = "default";
        this.marker = null;
        this.title="location0";
        this.selectedIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
        this.mMap = map;
/*        this.marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(0.0,0.0))
                .title("some pithy name")
                .snippet("Even pithier label")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
*/
    }

    public void setIcon(BitmapDescriptor icon) {
        this.icon = icon;
        if (this.marker == null) {
            this.marker = this.mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(0.0, 0.0))
                    .title("some pithy name")
                    .snippet("")
            .anchor(0.5f,0.5f));
        }
        this.marker.setIcon(icon);
        this.position = this.marker.getPosition();
    }

    public void select() {
        this.marker.setIcon(this.selectedIcon);
    }

    public void deselect() {
        this.marker.setIcon(this.icon);
    }

    public void setURL(String url) {
        try {
            this.URL = new java.net.URL(url);
        } catch (java.net.MalformedURLException e) {
            Log.e("GCT HotSpot error: bad url", e.getMessage());
        }
    }

    public void setTitle(String title) {
       this.title = title;
       this.marker.setTitle(title);
    }

    public String getTitle() {
        return (this.title);
    }

    public void setSnippet(String snippet) {
        this.marker.setSnippet(snippet);
    }

    public void setPosition(LatLng position) {
        this.marker.setPosition(position);
        this.position = position;
    }

    public void setZoomRange(Double minZoom, Double maxZoom) {
         this.hotSpotZoomTriggerRange[0] =  minZoom;
            this.hotSpotZoomTriggerRange[1] =  maxZoom;
    }

    public LatLng getPosition() {
        return (this.position);
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

    private void reportZoomStats() {
        long windowSum = 0;
        long windowSumSquared = 0;
        long maxEt = 0;
        long minEt = Long.MAX_VALUE;
        for (long i : eventZoomWindow) {
            windowSum += i;
            windowSumSquared += i * i;
            maxEt = Math.max(maxEt, i);
            minEt = Math.min(minEt, i);
        }
        double diff = (windowSumSquared - eventZoomWindow.length * (windowSum / eventZoomWindowLength) * (windowSum / eventZoomWindowLength));

        Log.i("GCT HS: Zoom data stats",
                "\nTotal events: " + eventZoomCount +
                        "\nTotal mean elapsed Time: " + (sumZoomElapsedTimes / eventZoomCount) +
                        "\nwindow mean elapsedTime: " + (windowSum / eventZoomWindowLength) +
                        "\nWindow total ET: " + windowSum +
                        "\nWindow sigma ET: " + Math.sqrt(diff / eventZoomWindowLength) +
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
        for (long i : eventTiltWindow) {
            windowSum += i;
            windowSumSquared += i * i;
            maxEt = Math.max(maxEt, i);
            minEt = Math.min(minEt, i);
        }
        Log.i("GCT HS: Tilt data stats",
                "\nTotal events: " + eventTiltCount +
                        "\nTotal mean elapsed Time: " + (sumTiltElapsedTimes / eventTiltCount) +
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


    public final Runnable handleJSON = new Runnable() {
        JSONObject message;
        GoogleMap mMap;
        boolean doLog;

        public void setMessage(JSONObject _message) {
            this.message = _message;
        }

        public void setMap(GoogleMap _mMap) {
            this.mMap = _mMap;
        }

        public void setLogging(boolean _doLog) {
            this.doLog = _doLog;
        }

        public void run() {

        }
    };
}
