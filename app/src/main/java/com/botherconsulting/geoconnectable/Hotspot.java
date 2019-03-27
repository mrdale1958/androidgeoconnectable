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

/**
 * Created by dwm160130 on 3/22/18.
 */

public class Hotspot {
    public boolean enabled;
    public String set;
    public java.net.URL URL;
    public Marker marker;
    private double validTiltThreshold = 0.025; // needs to be in settings
    public Double[] hotSpotZoomTriggerRange = {12.0, 15.0};
    private WebView displaySurface;
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


    public Hotspot(GoogleMap map) {
        state = States.CLOSED;
        this.enabled = false;
        this.set = "default";
        this.marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(0.0,0.0))
                .title("some pithy name")
                .snippet("Even pithier label")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

    }

    public void setIcon(BitmapDescriptor icon) {
        this.marker.setIcon(icon);
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

    public void handleJSON(JSONObject message, GoogleMap mMap, boolean doLog)
        {
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
                            " raw y: " + Double.toString(rawY) );
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
                    percentChangeInY = Math.min(Math.max(TiltScaleY * rawY, -panMax), panMax) ;
                    deltaY = screenHeightDegrees * percentChangeInY;

                    percentChangeInX = Math.min(Math.max(TiltScaleX * rawX, -panMax), panMax);
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
                LatLng  nextPosition = new LatLng(currentCameraPosition.latitude+deltaY, currentCameraPosition.longitude + deltaX);
                if (nextPosition != currentCameraPosition) {
                    updateStats();
                    position = currentPosition;
                    currentPosition = nextPosition;
                    newData = true;
                }
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));

            } catch (org.json.JSONException e) {
                Log.e("reading pan message", "invalid vector " + vector.toString());
            }

        }
}
