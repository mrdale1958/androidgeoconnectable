package com.botherconsulting.geoconnectable;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private WebSocketClient mWebSocketClient;
    private double currentSpinPosition = 0.0;
    private  int targetColor = 0xff0000;
    private  double targetWidth = 0.03; // portion of visible map

    private  int maxZoom = 19;
    private  int minZoom = 3;
    private  double currentZoom = 0;
    //var targetRectangle;
    private  double currentScale = 1.0;
    //var mapData = [];
    private  int clicksPerRev =  256; // weirdly not 3.14159 * 4 *
    private  double revsPerFullZoom = (maxZoom - minZoom)/8;
    private  double clicksPerZoomLevel =  clicksPerRev / revsPerFullZoom;
    private  double maxClicks = clicksPerRev * revsPerFullZoom * 1.0;
    private void restartIdleTimer() {}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void connectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://192.168.1.73:5678");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri, new org.java_websocket.drafts.Draft_17()) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                JSONObject jsonData  = new JSONObject();
                try {
                    jsonData = new JSONObject(message);
                }  catch(org.json.JSONException e) {
                    e.printStackTrace();
                }


                if (mMap == null) return;
                //var currentZoom = map.getZoom();
                //currentFeatureSet = zoomLayers[lastZoom];
                String messageType = "";
                String gestureType = "";
                try {
                    messageType = jsonData.getString("type");
                    gestureType = jsonData.getString("gesture");
                } catch  (org.json.JSONException e)
                {
                    e.printStackTrace();
                }
                            

                if (  messageType == "spin") {
                    try {
                        String encoderID = jsonData.getJSONObject("packet").getString("sensorID");
                        Log.i("EncoderID", encoderID);
                        String encoderIndex = jsonData.getJSONObject("packet").getString("encoderIndex");
                        Log.i("EncoderIndex", encoderIndex);
                        String encoderDelta = jsonData.getJSONObject("packet").getString("encoderDelta");
                        Log.i("EncoderDelta", encoderDelta);
                        String encoderET = jsonData.getJSONObject("packet").getString("encoderElapsedTime");
                        Log.i("EncoderElapsedTime", encoderET);
                        String encoderPosition = jsonData.getJSONObject("packet").getString("encoderPosition");
                        Log.i("EncoderPosition", encoderPosition);
                    } catch (org.json.JSONException e)
                    {
                        e.printStackTrace();
                    }
                    
                } else if (messageType == "tilt") {
                    try {
                        String accelerometerID = jsonData.getJSONObject("packet").getString("sensorID");
                        Log.i("TiltsensorID", accelerometerID);
                        String tiltX = jsonData.getJSONObject("packet").getString("tiltX");
                        Log.i("TiltX", tiltX);
                        String tiltY = jsonData.getJSONObject("packet").getString("tiltY");
                        Log.i("TiltY", tiltY);
                        String tiltMagnitude = jsonData.getJSONObject("packet").getString("tiltMagnitude");
                        Log.i("TiltMagnitude", tiltMagnitude);

                    } catch (org.json.JSONException e)
                    {
                        e.printStackTrace();
                    }} else if (gestureType == "pan") {
                    //var dampingZoom = map.getZoom()*minZoom/maxZoom;
                    double x = 0.0;
                    double y = 0.0;
                    JSONObject vector = new JSONObject();
                    try {
                        vector = jsonData.getJSONObject("vector");
                    } catch (org.json.JSONException e)
                    {
                        e.printStackTrace();
                    }
                    try {

                        x = vector.getDouble("x");
                        y = vector.getDouble("y");
                        if ( x == 0.0 && y == 0.0) return;
                        //console.log("sensor message: " + jsonData.type + "-" + jsonData.vector.x + "," +jsonData.vector.y);

                        //if (zoomLayers[currentZoom]["pannable"])
                        mMap.animateCamera(CameraUpdateFactory.scrollBy((float)(100*x), (float)(100*y)));
                        restartIdleTimer();

                    } catch (org.json.JSONException e)
                    {
                        e.printStackTrace();
                    }
                    //paintTarget();
                }
                else if (gestureType == "zoom")
                {
                    double delta = 0.0;
                    JSONObject vector = new JSONObject();
                    try {
                        vector = jsonData.getJSONObject("vector");
                    } catch (org.json.JSONException e)
                    {
                        e.printStackTrace();
                    }
                    try {
                        delta = vector.getDouble("delta");
                    } catch (org.json.JSONException e)
                    {
                        e.printStackTrace();
                    }
                    currentSpinPosition += delta;
                    //console.log(currentSpinPosition);
                    //console.log("sensor message: " + jsonData.gesture + " " + jsonData.vector.delta + "; currentSpinPosition=" +currentSpinPosition);
                    if (currentSpinPosition < 0) currentSpinPosition = 0;
                    double proposedZoom =  currentSpinPosition/clicksPerZoomLevel;
                    //restartIdleTimer();

                    if (proposedZoom != currentZoom)
                    {
                        //doZoom(Math.min(Object.keys(zoomLayers).length - 1, Math.max(0,proposedZoom)));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo((float)(proposedZoom)));
                        currentZoom = proposedZoom;
                        restartIdleTimer();

                    }
                    else
                    {
                        /*
                        currView = mMap.getBounds();
                        if (currView != undefined)
                        {
                            currLeft = currView.getNorthEast().lng();
                            currRight = currView.getSouthWest().lng();
                            currTop = currView.getNorthEast().lat();
                            currBottom = currView.getSouthWest().lat();
                            currWidth = currLeft - currRight;
                            currHeight = currTop - currBottom;
                            currCenter = map.getCenter();
                            hotBounds = new google.maps.LatLngBounds(
                                    {lat: currCenter.lat()-targetWidth*currHeight, lng: currCenter.lng()-targetWidth*currWidth},
                            {lat: currCenter.lat()+targetWidth*currHeight, lng: currCenter.lng()+targetWidth*currWidth});
                            var hotspotFound = false;
                            for (featureKey  in currentFeatureSet)
                            {
                                if ( typeof(currentFeatureSet[featureKey]) == "string" )
                                {
                                    if (hotspot[currentFeatureSet[featureKey]])
                                    {
                                        if (hotBounds.contains(hotspot[currentFeatureSet[featureKey]].position))
                                        {
                                            if ( ! hotspotFound )
                                            {
                                                console.log("zoomed in on " + currentFeatureSet[featureKey] + " in " + hotBounds );
                                                openCedula(currentFeatureSet[featureKey]);
                                                hotspotFound = true;
                                            }
                                            else
                                                console.log("would like to have zoomed in on " + currentFeatureSet[featureKey] + " in " + hotBounds );

                                        }
                                        else
                                        {
                                            //console.log("zoomed in on something else. Closing " + currentFeatureSet[featureKey] + " in " + hotBounds );
                                            closeCedula(currentFeatureSet[featureKey]);
                                        }
                                    }
                                }
                            }
                        }
                    */}


                }
                else if (gestureType == "combo")
                {
                    double dampingZoom = mMap.getCameraPosition().zoom*minZoom/maxZoom;
                    double x = 0.0;
                    double y = 0.0;
                    double delta = 0.0;
                    JSONObject vector = new JSONObject();
                    try {
                        vector = jsonData.getJSONObject("vector");
                    } catch (org.json.JSONException e)
                    {
                        e.printStackTrace();
                    }
                    try {

                        x = vector.getDouble("x");
                        y = vector.getDouble("y");
                        delta = vector.getDouble("delta");
                    } catch (org.json.JSONException e)
                    {
                        e.printStackTrace();
                    }
                        if (x != 0.0 && y != 0.0)
                    {
                        mMap.animateCamera(CameraUpdateFactory.scrollBy((float)(100*x), (float)(100*y)));
                        restartIdleTimer();
                    }
                    currentSpinPosition += delta;
                    if (currentSpinPosition < 0) currentSpinPosition = 0;
                    double proposedZoom =  currentSpinPosition/clicksPerZoomLevel;
                    //restartIdleTimer();

                    if (proposedZoom != currentZoom)
                    {
                        //doZoom(Math.min(Object.keys(zoomLayers).length - 1, Math.max(0,proposedZoom)));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo((float)(proposedZoom)));
                        currentZoom = proposedZoom;

                        restartIdleTimer();
                    }


                } else {
                   /* messages = document.getElementsByTagName("ul")[0];
                    var message = document.createElement("li");
                    var content = document.createTextNode(event.data);
                    message.appendChild(content);
                    messages.appendChild(message);*/
                }
                LatLngBounds curScreen = mMap.getProjection()
                        .getVisibleRegion().latLngBounds;
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        LatLngBounds curScreen = mMap.getProjection()
                .getVisibleRegion().latLngBounds;
        LatLng radius = new LatLng(curScreen.northeast.latitude, curScreen.getCenter().longitude);
        Log.i("mask radius", radius.toString());
        mMap.addPolygon(MapMask.createPolygonWithCircle(this, sydney, radius));
        //mMap.addPolygon(MapMask.createPolygonWithCircle(this, sydney, 100));

        connectWebSocket();
    }
}
