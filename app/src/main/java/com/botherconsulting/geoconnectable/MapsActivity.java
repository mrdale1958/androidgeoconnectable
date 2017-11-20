package com.botherconsulting.geoconnectable;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.data.kml.KmlLayer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    static final int SHOW_PREFERENCES = 1;

    private GoogleMap mMap;
    private boolean useHybridMap = true;
    private WebSocketClient mWebSocketClient;
    private double currentSpinPosition = 0.0;
    private String targetColor = "#ff0000";
    private double targetWidth = 0.03; // portion of visible map
    private boolean targetVisible = false;
    private int horizontalBump = 0;
    private double TiltScaleX = 500;
    private double TiltScaleY = 500;
    private int maxZoom = 19;
    private int minZoom = 3;
    private double currentZoom = 0;
    //var targetRectangle;
    private double currentScale = 1.0;
    //var mapData = [];
    private int clicksPerRev = 256; // weirdly not 3.14159 * 4 *
    private double revsPerFullZoom = (maxZoom - minZoom) / 8;
    private double clicksPerZoomLevel = clicksPerRev / revsPerFullZoom;
    private double maxClicks = clicksPerRev * revsPerFullZoom * 1.0;
    private String idleMessageTop = "Spin table top to zoom";
    private String idleMessageBottom = "Tilt table top to pan";
    int idleTime = 600;
    String sensorServerAddress = "192.168.1.73";
    String sensorServerPort = "5678";



    private void restartIdleTimer() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        BackgroundWebSocket bws = new BackgroundWebSocket();
       // bws.execute("ws://192.168.1.73:5678");
        PreferenceManager.setDefaultValues(this, R.xml.gct_preferences, true);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        sensorServerAddress = sharedPref.getString(SettingsActivity.KEY_PREF_SENSOR_SERVER, sensorServerAddress);
        sensorServerPort = sharedPref.getString(SettingsActivity.KEY_PREF_SENSOR_SERVER_PORT, sensorServerPort);
        clicksPerRev = Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_SPIN_SENSOR_CLICKS_PER_REV,Integer.toString(clicksPerRev)));
        TiltScaleX = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TILT_SENSOR_SCALE_FACTOR, Double.toString(TiltScaleX)));
        TiltScaleY = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TILT_SENSOR_SCALE_FACTOR, Double.toString(TiltScaleY)));
        useHybridMap = sharedPref.getBoolean(SettingsActivity.KEY_PREF_USE_HYBRID_MAP, useHybridMap);
        targetVisible = sharedPref.getBoolean(SettingsActivity.KEY_PREF_TARGET_VISIBLE, targetVisible);
        targetColor = sharedPref.getString(SettingsActivity.KEY_PREF_TARGET_COLOR, targetColor);
        idleMessageBottom = sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_TEXT_BOTTOM,idleMessageBottom);
        idleMessageTop = sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_TEXT_TOP,idleMessageTop);
        idleTime = Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_TIME, Integer.toString(idleTime)));
        horizontalBump = Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_HORIZONTAL_BUMP, Integer.toString(horizontalBump)));
        //targetWidth = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TARGET_SIZE, Double.toString(targetWidth)));

        bws.execute("ws://"+ sensorServerAddress + ":" + sensorServerPort);


        Handler idleHandler = new Handler();
        Runnable runnable = new Runnable(){
            public void run() {
                Toast.makeText(MapsActivity.this, "C'Mom no hands!", Toast.LENGTH_SHORT).show();
            }
        };

        idleHandler.postAtTime(runnable, System.currentTimeMillis()+idleTime*1000);
        idleHandler.postDelayed(runnable, idleTime+1000);
        final Intent intent = new Intent(this, SettingsActivity.class);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(intent, SHOW_PREFERENCES);
            }
        });
        hideSystemUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // launch settings activity
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void hideSystemUI() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        // remove the following flag for version < API 19
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );
    }

   private void retrieveFileFromUrl() {
        new DownloadKmlFile(getString(R.string.kml_url)).execute();
    }
    private class DownloadKmlFile extends AsyncTask<String, Void, byte[]> {
        private final String mUrl;

        public DownloadKmlFile(String url) {
            mUrl = url;
        }

        protected byte[] doInBackground(String... params) {
            try {
                InputStream is =  new URL(mUrl).openStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                return buffer.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(byte[] byteArr) {
            try {
                KmlLayer kmlLayer = new KmlLayer(mMap, new ByteArrayInputStream(byteArr),
                        getApplicationContext());
                kmlLayer.addLayerToMap();

            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

   private void onMessage(String messageString) {

        if (mMap == null) return;
        //var currentZoom = map.getZoom();
        //currentFeatureSet = zoomLayers[lastZoom];
        JSONObject message = new JSONObject();
        try {
            message = new JSONObject(messageString);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        String messageType = "";
        String gestureType = "";
/*        try {
 messageType = message.getString("type");
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }*/
        try {
            gestureType = message.getString("gesture");
            //Log.i("incoming message",message.toString());
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }


        if (messageType.equals("spin")) {
            try {
                String encoderID = message.getJSONObject("packet").getString("sensorID");
                Log.i("EncoderID", encoderID);
                String encoderIndex = message.getJSONObject("packet").getString("encoderIndex");
                Log.i("EncoderIndex", encoderIndex);
                String encoderDelta = message.getJSONObject("packet").getString("encoderDelta");
                Log.i("EncoderDelta", encoderDelta);
                String encoderET = message.getJSONObject("packet").getString("encoderElapsedTime");
                Log.i("EncoderElapsedTime", encoderET);
                String encoderPosition = message.getJSONObject("packet").getString("encoderPosition");
                Log.i("EncoderPosition", encoderPosition);
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }

        } else if (messageType.equals("tilt")) {
            try {
                String accelerometerID = message.getJSONObject("packet").getString("sensorID");
                Log.i("TiltsensorID", accelerometerID);
                String tiltX = message.getJSONObject("packet").getString("tiltX");
                Log.i("TiltX", tiltX);
                String tiltY = message.getJSONObject("packet").getString("tiltY");
                Log.i("TiltY", tiltY);
                String tiltMagnitude = message.getJSONObject("packet").getString("tiltMagnitude");
                Log.i("TiltMagnitude", tiltMagnitude);

            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
        } else if (gestureType.equals("pan")) {
            //var dampingZoom = map.getZoom()*minZoom/maxZoom;
            double x = 0.0;
            double y = 0.0;
            JSONObject vector = new JSONObject();
            try {
                vector = message.getJSONObject("vector");
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
            try {

                x = vector.getDouble("x");
                y = vector.getDouble("y");
                if (Math.abs(x) < 0.1 && Math.abs(y) < 0.1) return;
                //console.log("sensor message: " + jsonData.type + "-" + jsonData.vector.x + "," +jsonData.vector.y);

                //if (zoomLayers[currentZoom]["pannable"])
                //Log.i("incoming pan",x + "," + y);
                mMap.animateCamera(CameraUpdateFactory.scrollBy((float) (TiltScaleX * x), (float) (TiltScaleY * y)));
                restartIdleTimer();

            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
            //paintTarget();
        } else if (gestureType.equals("zoom")) {
            double delta = 0.0;
            JSONObject vector = new JSONObject();
            try {
                vector = message.getJSONObject("vector");
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
            try {
                delta = vector.getDouble("delta");
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
            currentSpinPosition += delta;
            //console.log(currentSpinPosition);
            //console.log("sensor message: " + jsonData.gesture + " " + jsonData.vector.delta + "; currentSpinPosition=" +currentSpinPosition);
            if (currentSpinPosition < 0) currentSpinPosition = 0;
            double proposedZoom = currentSpinPosition / clicksPerZoomLevel;
            //restartIdleTimer();

            if (proposedZoom != currentZoom) {
                //doZoom(Math.min(Object.keys(zoomLayers).length - 1, Math.max(0,proposedZoom)));
                mMap.animateCamera(CameraUpdateFactory.zoomTo((float) (proposedZoom)));
                currentZoom = proposedZoom;
                restartIdleTimer();

            } else {
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
                    */
            }


        } else if (gestureType == "combo") {
            double dampingZoom = mMap.getCameraPosition().zoom * minZoom / maxZoom;
            double x = 0.0;
            double y = 0.0;
            double delta = 0.0;
            JSONObject vector = new JSONObject();
            try {
                vector = message.getJSONObject("vector");
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
            try {

                x = vector.getDouble("x");
                y = vector.getDouble("y");
                delta = vector.getDouble("delta");
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
            if (x != 0.0 && y != 0.0) {
                mMap.animateCamera(CameraUpdateFactory.scrollBy((float) (100 * x), (float) (100 * y)));
                restartIdleTimer();
            }
            currentSpinPosition += delta;
            if (currentSpinPosition < 0) currentSpinPosition = 0;
            double proposedZoom = currentSpinPosition / clicksPerZoomLevel;
            //restartIdleTimer();

            if (proposedZoom != currentZoom) {
                //doZoom(Math.min(Object.keys(zoomLayers).length - 1, Math.max(0,proposedZoom)));
                mMap.animateCamera(CameraUpdateFactory.zoomTo((float) (proposedZoom)));
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

    class BackgroundWebSocket extends AsyncTask<String, String, String> {

        //inputarg can contain array of values
        @Override
        protected String doInBackground(String... URIString) {
            URI uri;
            try {
                uri = new URI(URIString[0]);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return "Invalid URI";
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
                    //Log.i("incoming message", message);
                    /*JSONObject jsonData = new JSONObject();
                    try {
                        jsonData = new JSONObject(message);
                    } catch (org.json.JSONException e) {
                        e.printStackTrace();
                    }*/
                    publishProgress(message);

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
            return null;
        }

        protected void onProgressUpdate(String... progress) {

            //update the progress
            onMessage(progress[0]);

        }

        //this will call after finishing the doInBackground function
        protected void onPostExecute(String result) {

            // Update the ui elements
            //show some notification
            //showDialog("Task done " + result);

        }
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
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
Log.i("map ready","ok");
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(40.76667,-111.903373);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker at Clark Planetarium"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.animateCamera(CameraUpdateFactory.zoomTo((float) (13.5)));
        LatLngBounds curScreen = mMap.getProjection()
                .getVisibleRegion().latLngBounds;
        //LatLng radius = new LatLng(curScreen.northeast.latitude, curScreen.getCenter().longitude);
        //Log.i("mask radius", radius.toString());
        //mMap.addPolygon(MapMask.createPolygonWithCircle(this, sydney, radius));
        //mMap.addPolygon(MapMask.createPolygonWithCircle(this, sydney, 100));
        //Intent i = new Intent(this,TablePreferencesActivity.class);
        //startActivityForResult(i, SHOW_PREFERENCES);
    }



}
