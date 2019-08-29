package com.botherconsulting.geoconnectable;

import android.util.Log;
import android.webkit.WebView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import static android.os.SystemClock.uptimeMillis;

/**
 * Created by dwm160130 on 3/22/18.
 */

public class Hotspot {
    // tilt stuff
    public double TiltScaleX = 0.04; // in settings
    public double TiltScaleY = 0.04; // in settings
    private double panMax = 0.01;
    private int minSpin;
    private int maxSpin = 100000000;
    private double validTiltThreshold = 0.025; // needs to be in settings
    long lastTiltMessageTime = System.nanoTime();
    private int eventTiltCount = 0;
    static final int eventTiltWindowLength = 10000;
    long[] eventTiltWindow = new long[eventTiltWindowLength];
    long sumTiltElapsedTimes = 0;
    long sumTiltSquaredElapsedTimes = 0;
    int lastTiltMessageID = 0;

    //zoom stuff
    public double maxZoom = 19; // needs to be in settings
    public double minZoom = 3; // needs to be in settings
    public double currentZoom = 0;
    private int currentSpinPosition = 0;
    public int clicksPerRev = 2400; // in settings
    public int revsPerFullZoom = 19;  // in settings
    private int clicksPerZoomLevel;
    private int idleSpin = 0;
    public double idleZoom = 13.5; // in settings
    public double zoom;
    private  int deltaZ = 0;
    long lastZoomMessageTime = System.nanoTime();
    private int eventZoomCount = 0;
    static final int eventZoomWindowLength = 10000;
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
    public Double[] hotSpotZoomTriggerRange = {12.0, 15.0};
    public Double[] currentTilt = {0.0, 0.0};
    private WebView displaySurface;
    private GoogleMap mMap;
    public enum States {
        CLOSED,
        OPENING,
        CLOSING,
        OPEN,
        THURSDAY,
        FRIDAY,
        SATURDAY;

    }
    private States state;


    public Hotspot(GoogleMap map, WebView webView) {
        state = States.CLOSED;
        this.displaySurface = webView;
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
            this.URL = new java.net.URL("file://android_asset/www/" + url);
        }
        catch (java.net.MalformedURLException e) {
            Log.e("bad url", e.getMessage());
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

    private void reportZoomStats() {
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

        Log.i("Zoom data flow stats",
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

    private void updateZoomStats() {
        long elapsedTime = System.nanoTime() - lastZoomMessageTime;

        lastZoomMessageTime = System.nanoTime();
        if (elapsedTime > 150000000) Log.i("Big data gap", Long.toString(elapsedTime));
        eventZoomCount++;
        sumZoomElapsedTimes += elapsedTime;
        sumZoomSquaredElapsedTimes += elapsedTime * elapsedTime;
        eventZoomWindow[eventZoomCount % eventZoomWindowLength] = elapsedTime;
        if (eventZoomCount % eventZoomWindowLength == 0) {
            reportZoomStats();
        }
    }

    private void reportTiltStats() {
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
        Log.i("Tilt data flow stats",
                "\nTotal events: " + eventTiltCount +
                        "\nTotal mean elapsed Time: " + (sumTiltElapsedTimes/eventTiltCount) +
                        "\nwindow mean elapsedTime: " + (windowSum / eventTiltWindowLength) +
                        "\nWindow max ET: " + maxEt +
                        "\nWindow min ET: " + minEt
        );
    }

    private void updateTiltStats() {
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


    public void handleJSON(JSONObject message, GoogleMap mMap, boolean doLog)
        {
            String gestureType;
            try {
                gestureType = message.getString("gesture");
                //Log.i("incoming message",message.toString());
            } catch (org.json.JSONException e) {
                Log.i("no gesture message",message.toString());
                return;
            }
            if (gestureType.equals("pan")) {
                double deltaX = 0.0;

                double deltaY = 0.0;
                JSONObject vector = new JSONObject();
                try {
                    vector = message.getJSONObject("vector");
                } catch (org.json.JSONException e) {
                    Log.e("reading pan message", "no vector " + message.toString());
                }
                try {
                    //double screenWidthDegrees = Math.abs(curScreen.southwest.longitude - curScreen.northeast.longitude);
                    //double screenHeightDegrees = Math.abs(curScreen.southwest.latitude - curScreen.northeast.latitude);
                    double rawX = vector.getDouble("x");
                    double percentChangeInX = 0;
                    double percentChangeInY = 0;
                    double rawY = vector.getDouble("y");

                    if (doLog) {
                        Log.i("pan update", " raw x: " + Double.toString(rawX) +
                                " raw y: " + Double.toString(rawY));
                    }
                    if (Math.abs(rawX) < validTiltThreshold && Math.abs(rawY) < validTiltThreshold) {
                        if (doLog) {
                            Log.d("rejected pan update", " raw x: " + Double.toString(rawX) +
                                    " raw y: " + Double.toString(rawY) + " validTiltThreshold: " + Double.toString(validTiltThreshold));
                        }
                        return;
                    } else {

                        //double zoomFudge = (minZoom + 7) +
                        //        ((minZoom + 7) - (maxZoom-3 ))/(minZoom-maxZoom) *
                        //                (mMap.getCameraPosition().zoom-minZoom);
                        //Log.i("fudge", Double.toString(zoomFudge) + ":" +  Double.toString(mMap.getCameraPosition().zoom));
                /*percentChangeInY = TiltScaleY * rawY *zoomFudge/maxZoom;
                deltaY = screenHeightDegrees * percentChangeInY;

                percentChangeInX = TiltScaleX * rawX *zoomFudge/maxZoom;
                deltaX = screenWidthDegrees * percentChangeInX;*/
                        deltaY = Math.min(Math.max(TiltScaleY * rawY, -panMax), panMax);

                        deltaX = Math.min(Math.max(TiltScaleX * rawX, -panMax), panMax);
                    }

                    //if (zoomLayers[currentZoom]["pannable"])
                    //Log.i("incoming pan",x + "," + y);
                    if (doLog) {
                        Log.i("pan hotspot update", " deltax: " + Double.toString(deltaX) +
                                " deltay: " + Double.toString(deltaY)
                        );
                    }
                    //mMap.animateCamera(CameraUpdateFactory.scrollBy((float) ( x), (float) ( y)));
                    Double[] nextTilt = { deltaY, deltaX };
                    if (nextTilt != currentTilt) {
                        updateTiltStats();
                        Double[]  position = currentTilt;
                        currentTilt = nextTilt;
                        newData = true;
                    }
                    //mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));

                } catch (org.json.JSONException e) {
                    Log.e("reading pan message", "invalid vector " + vector.toString());
                }
            }
            else
            {
                deltaZ = 0;
                JSONObject vector = new JSONObject();
                try {
                    vector = message.getJSONObject("vector");
                } catch (org.json.JSONException e) {
                    Log.e("reading zoom message", "no vector " + message.toString());
                }
                try {
                    deltaZ = vector.getInt("delta");
                    int messageID = message.getInt("id");
                    //if (messageID > lastMessageID + 1)
                    //    Log.w("reading zoom data","got" + Integer.toString(messageID) + " after" + Integer.toString(lastMessageID));
                    lastZoomMessageID = messageID;
                } catch (org.json.JSONException e) {
                    Log.e("reading zoom message", "invalid vector " + vector.toString());
                }
                currentSpinPosition += deltaZ;
                currentSpinPosition = Math.max(minSpin,Math.min(currentSpinPosition,maxSpin));

                double proposedZoom = idleZoom + (double)currentSpinPosition / (double)clicksPerZoomLevel;
                if (doLog) {
                    Log.i("zoom update", "delta:" + Integer.toString(deltaZ) +
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
                updateZoomStats();

                if (proposedZoom != currentZoom) {
                    zoom = currentZoom;
                    currentZoom = proposedZoom;
                    newData = true;

                }


            }
            if (newData)
            {
                displaySurface.loadUrl("javascript://table.update(" + currentTilt + " , " + currentZoom + ")" );
            }

        }
}
