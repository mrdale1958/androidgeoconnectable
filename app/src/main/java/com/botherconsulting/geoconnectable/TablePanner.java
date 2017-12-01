package com.botherconsulting.geoconnectable;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONObject;

/**
 * Created by dalemacdonald on 11/28/17.
 */

public class TablePanner {
    public double TiltScaleX = 0.04; // in settings
    public double TiltScaleY = 0.04; // in settings
    private double validTiltThreshold = 0.01; // needs to be in settings
    private double maxZoom = 19; // needs to be in settings
    private double minZoom = 0; // needs to be in settings
    public LatLng currentPosition = new LatLng(0,0);

    public TablePanner(double _maxZoom, double _minZoom) {
        maxZoom = _maxZoom;
        minZoom = _minZoom;
    }

    public void configure(double _TiltScaleX, double _TiltScaleY ) {
        TiltScaleX = _TiltScaleX;
        TiltScaleY = _TiltScaleY;
    }

    public void handleJSON(JSONObject message, GoogleMap mMap, boolean doLog){
    LatLngBounds curScreen = mMap.getProjection()
            .getVisibleRegion().latLngBounds;
    double deltaX = 0.0;
    double deltaY = 0.0;
    JSONObject vector = new JSONObject();
    try {
        vector = message.getJSONObject("vector");
    } catch (org.json.JSONException e) {
        Log.e("reading pan message", "no vector " + message.toString());
    }
    try {
        double screenWidthDegrees = Math.abs(curScreen.southwest.longitude - curScreen.northeast.longitude);
        double screenHeightDegrees = Math.abs(curScreen.southwest.latitude - curScreen.northeast.latitude);
        double rawX = vector.getDouble("x");
        double percentChangeInX = 0;
        double percentChangeInY = 0;
        double rawY = vector.getDouble("y");

        if (doLog) {
            Log.i("pan update", " raw x: " + Double.toString(rawX) +
                    " raw y: " + Double.toString(rawY) + " screenWidthDegrees: " + Double.toString(screenWidthDegrees) +
                    " screenHeightDegrees: " + Double.toString(screenHeightDegrees) );
        }
        if (Math.abs(rawX) < validTiltThreshold && Math.abs(rawY) < validTiltThreshold) {
            if (doLog) {
                Log.d("rejected pan update", " raw x: " + Double.toString(rawX) +
                        " raw y: " + Double.toString(rawY) + " validTiltThreshold: " + Double.toString(validTiltThreshold));
            }
            return;
        } else {

            double zoomFudge = (minZoom + 7) +
                    ((minZoom + 7) - (maxZoom-3 ))/(minZoom-maxZoom) *
                            (mMap.getCameraPosition().zoom-minZoom);
            //Log.i("fudge", Double.toString(zoomFudge) + ":" +  Double.toString(mMap.getCameraPosition().zoom));
            percentChangeInY = TiltScaleY * rawY *zoomFudge/maxZoom;
            deltaY = screenHeightDegrees * percentChangeInY;

            percentChangeInX = TiltScaleX * rawX *zoomFudge/maxZoom;
            deltaX = screenWidthDegrees * percentChangeInX;
        }

        //if (zoomLayers[currentZoom]["pannable"])
        //Log.i("incoming pan",x + "," + y);
        if (doLog) {
            Log.i("pan update", "%x: " + Double.toString(percentChangeInX) +
                    " %y: " + Double.toString(percentChangeInY) +
                    " deltax: " + Double.toString(deltaX) +
                    " deltay: " + Double.toString(deltaY) +
                    " current Lat:" +
                    Double.toString(mMap.getCameraPosition().target.latitude) +
                    " current Lon:" +
                    Double.toString(mMap.getCameraPosition().target.longitude)

            );
        }
        //mMap.animateCamera(CameraUpdateFactory.scrollBy((float) ( x), (float) ( y)));
        LatLng currentCameraPosition = mMap.getCameraPosition().target;
        currentPosition = new LatLng(currentCameraPosition.latitude+deltaY, currentCameraPosition.longitude + deltaX);
        //mMap.animateCamera(CameraUpdateFactory.newLatLng(newPosition),1,null);
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));

    } catch (org.json.JSONException e) {
        Log.e("reading pan message", "invalid vector " + vector.toString());
    }
}}
