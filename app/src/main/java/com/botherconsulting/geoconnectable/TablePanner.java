package com.botherconsulting.geoconnectable;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import static android.os.SystemClock.uptimeMillis;

/**
 * Created by dalemacdonald on 11/28/17.
 */

public class TablePanner {
    public double TiltScaleX = 0.04; // in settings
    public double TiltScaleY = 0.04; // in settings
    private double validTiltThreshold = 0.025; // needs to be in settings
    private double maxZoom = 19; // needs to be in settings
    private double minZoom = 3; // needs to be in settings
    private double panMax = 0.01;
    public LatLng currentPosition = new LatLng(0,0);
    public LatLng position = new LatLng(0,0);
    private LatLng idleHome;
    private CameraPosition cameraPosition;
    public boolean newData = false;
    private int eventCount = 0;
    long lastTiltMessageTime = uptimeMillis();
    static final int eventWindowLength = 10000;
    long[] eventWindow = new long[eventWindowLength];
    long sumElapsedTimes = 0;
    long sumSquaredElapsedTimes = 0;
    private  long bigDataArrivalGap = 150000000; // 150ms

    public TablePanner(double _maxZoom, double _minZoom) {
        maxZoom = _maxZoom;
        minZoom = _minZoom;
    }

    public void configure(double _TiltScaleX, double _TiltScaleY, LatLng _idleHome, Double _panMax) {
        TiltScaleX = _TiltScaleX;
        TiltScaleY = _TiltScaleY;
        idleHome = _idleHome;
        currentPosition = idleHome;
        panMax = _panMax;
        logstate();
    }

    public Object[] getCurrentPosition() {
        newData = false;
        return new Object[]{position, lastTiltMessageTime};
    }

    private void logstate() {
        Log.i("GCT: tilt state", "TiltScaleX:" + String.format ("%.2f", TiltScaleX) +
                " TiltScaleY:" + String.format ("%.2f", TiltScaleY) +
                "\ncurrentPosition:" + currentPosition.toString() +
                " idleHome: " + idleHome.toString()

        );
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
        Log.i("GCT: Tilt data flow stats",
                "\nTotal events: " + eventCount +
                        "\nTotal mean elapsed Time: " + (sumElapsedTimes/eventCount) +
                        "\nwindow mean elapsedTime: " + (windowSum / eventWindowLength) +
                        "\nWindow max ET: " + maxEt +
                        "\nWindow min ET: " + minEt
        );
    }

    private void updateStats(boolean doLog) {
        long elapsedTime = uptimeMillis() - lastTiltMessageTime;
        lastTiltMessageTime = uptimeMillis();
        if (doLog && elapsedTime > bigDataArrivalGap) Log.i("GCT: tilt Big data gap", Long.toString(elapsedTime / 1000000)+"ms");
        eventCount++;
        sumElapsedTimes += elapsedTime;
        sumSquaredElapsedTimes += elapsedTime * elapsedTime;
        eventWindow[eventCount % eventWindowLength] = elapsedTime;
        if (eventCount % eventWindowLength == 0) {
            reportStats();
        }
    }
    private JSONObject message;
    private boolean doLog;
    private GoogleMap mMap;
    private double screenWidthDegrees, screenHeightDegrees;

    public void setMessage(JSONObject _message) {
        this.message = _message;
    }

    public void setMap(GoogleMap _mMap) {
        this.mMap = _mMap;
    }

    public void setLogging(boolean _doLog) {
        this.doLog = _doLog;
    }

    public void setScreenBounds(double _screenWidthDegrees,
                                double _screenHeightDegrees)
    {
        this.screenWidthDegrees = _screenWidthDegrees;
        this.screenHeightDegrees = _screenHeightDegrees;
    }

    public void setMapPosition(CameraPosition newPosition) {
        cameraPosition = newPosition;
    }
    public final Runnable  handleJSON  = new Runnable() {


        public void run() {
//    public void handleJSON(JSONObject message,
            //                       GoogleMap mMap,
            //                       boolean doLog,
            //                       double screenWidthDegrees,
            //                       double screenHeightDegrees){
            //LatLngBounds curScreen = mMap.getProjection()
            //        .getVisibleRegion().latLngBounds;
            double deltaX = 0.0;
            double deltaY = 0.0;
            JSONObject vector = new JSONObject();
            try {
                vector = message.getJSONObject("vector");
            } catch (org.json.JSONException e) {
                Log.e("GCT pan: reading", "no vector " + message.toString());
            }
            try {
                //double screenWidthDegrees = Math.abs(curScreen.southwest.longitude - curScreen.northeast.longitude);
                //double screenHeightDegrees = Math.abs(curScreen.southwest.latitude - curScreen.northeast.latitude);
                /*
                 *
                 * * Danger will robinson!  x and y swapped for SFSU installation
                 *
                 */
                double rawX = vector.getDouble("y");

                double percentChangeInX = 0;
                double percentChangeInY = 0;
                double rawY = vector.getDouble("x");

                if (doLog) {
                    Log.i("GCT pan: incoming", " raw x: " + Double.toString(rawX) +
                            " raw y: " + Double.toString(rawY) + " screenWidthDegrees: " + Double.toString(screenWidthDegrees) +
                            " screenHeightDegrees: " + Double.toString(screenHeightDegrees));
                }
                if (Math.abs(rawX) < validTiltThreshold && Math.abs(rawY) < validTiltThreshold) {
                    if (doLog) {
                        Log.d("GCT pan: rejected", " raw x: " + Double.toString(rawX) +
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
                    percentChangeInY = Math.min(Math.max(TiltScaleY * rawY, -panMax), panMax);
                    deltaY = screenHeightDegrees * percentChangeInY;

                    percentChangeInX = Math.min(Math.max(TiltScaleX * rawX, -panMax), panMax);
                    deltaX = screenWidthDegrees * percentChangeInX;
                }

                //if (zoomLayers[currentZoom]["pannable"])
                //Log.i("incoming pan",x + "," + y);
                if (doLog) {
                    Log.i("GCT: pan update", "%x: " + Double.toString(percentChangeInX) +
                            " %y: " + Double.toString(percentChangeInY) +
                            " deltax: " + Double.toString(deltaX) +
                            " deltay: " + Double.toString(deltaY) +
                            " current Lat:" +
                            Double.toString(cameraPosition.target.latitude) +
                            " current Lon:" +
                            Double.toString(cameraPosition.target.longitude)

                    );
                }
                //mMap.animateCamera(CameraUpdateFactory.scrollBy((float) ( x), (float) ( y)));
                LatLng currentCameraPosition = cameraPosition.target;
                LatLng nextPosition = new LatLng(currentCameraPosition.latitude + deltaY, currentCameraPosition.longitude + deltaX);
                if (nextPosition != currentCameraPosition) {
                    updateStats(doLog);
                    position = currentPosition;
                    currentPosition = nextPosition;
                    newData = true;
                }
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));

            } catch (org.json.JSONException e) {
                Log.e("GCT pan: reading", "invalid vector " + vector.toString());
            }
        }
    };
}
