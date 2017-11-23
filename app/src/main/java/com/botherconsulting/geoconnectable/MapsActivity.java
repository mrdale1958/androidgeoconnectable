package com.botherconsulting.geoconnectable;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
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
import android.widget.TextView;

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

import static android.os.SystemClock.uptimeMillis;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    static final int EAT_PREFERENCES = 12345;
    final Handler idleHandler = new Handler();
    final Runnable runnable = new Runnable(){
        public void run() {
            long timeSinceLastInteraction = (uptimeMillis() - lastInteractionTime);
            Log.i("idle timer", "Time's up " + Long.toString(timeSinceLastInteraction));
            if (timeSinceLastInteraction > idleTime*idleTimeScaler) {
                doIdle();
            } else {
                //Toast.makeText(MapsActivity.this, "C'Mom no hands!", Toast.LENGTH_SHORT).show();
                idleHandler.postAtTime(this, uptimeMillis() + idleTime * idleTimeScaler);
            }
        }
    };
    private boolean logSensors = false;
    private GoogleMap mMap;
    private boolean useHybridMap = true;
    private WebSocketClient mWebSocketClient;
    private int currentSpinPosition = 0;
    private String targetColor = "#ff0000";
    private double targetWidth = 0.03; // portion of visible map
    private boolean targetVisible = false;
    private int horizontalBump = 0;
    private double TiltScaleX = 500;
    private double TiltScaleY = 500;
    private double validTiltThreshold = 0.1;
    private double maxZoom = 19;
    private double minZoom = 0;
    private double currentZoom = 0;
    //var targetRectangle;
    private double currentScale = 1.0;
    //var mapData = [];
    private int clicksPerRev = 256; // weirdly not 3.14159 * 4 *
    private int revsPerFullZoom = 8;
    private int clicksPerZoomLevel = clicksPerRev * revsPerFullZoom / (int)(maxZoom - minZoom) ;
    //private double maxClicks = clicksPerRev * revsPerFullZoom * 1.0;
    private String idleMessageTop = "Spin table top to zoom";
    private String idleMessageBottom = "Tilt table top to pan";
    private int idleTime = 600;
    private LatLng idleHome = new LatLng(40.76667,-111.903373);
    private double idleZoom = 13.5;
    private long lastInteractionTime = uptimeMillis();
    private int idleTimeScaler = 100; // 1000 lets idleTime be in seconds
    private boolean idling = false;
    private int animateToHomeMS = 10000;
    private int idleSpin = 0;
    private int minSpin = -(int)((idleZoom - minZoom) * (double)clicksPerZoomLevel);
    private int maxSpin = (int)((maxZoom - idleZoom) * (double)clicksPerZoomLevel);

    String idleTitle = "Clark Planetarium";
    String sensorServerAddress = "192.168.1.73";
    String sensorServerPort = "5678";

    OuterCircleTextView idleMessageTopView;
    OuterCircleTextView idleMessageBottomView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        BackgroundWebSocket bws = new BackgroundWebSocket();
        eatPreferences();
       // bws.execute("ws://192.168.1.73:5678");
        Log.w("idleTime set to", Integer.toString(idleTime));
        bws.execute("ws://"+ sensorServerAddress + ":" + sensorServerPort);



        idleHandler.postAtTime(runnable, uptimeMillis()+idleTime*idleTimeScaler);
        //idleHandler.postDelayed(runnable, idleTime+100);
        final Intent intent = new Intent(this, SettingsActivity.class);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(intent, EAT_PREFERENCES);
            }
        });
        hideSystemUI();

        // if showScaleBar the below is wrong needs to be adapted for ViewOverlay
        /*scaleBarOverlay = new ScaleBarOverlay(this.getBaseContext(), this, myMapView);
        List<Overlay> overlays = myMapView.getOverlays();
// Add scale bar overlay
        scaleBarOverlay.setMetric();
        overlays.add(scaleBarOverlay);*/

        idleMessageTopView = (OuterCircleTextView) findViewById(R.id.IdleTopText);
        idleMessageTopView.setPath(600,
        600,
        600.0f,
        Path.Direction.CCW,
                0.7f * (float)Math.PI * 1200f,
        -25f);
        idleMessageTopView.setText(idleMessageTop);
        idleMessageBottomView = (OuterCircleTextView) findViewById(R.id.IdleBottomText);
        idleMessageBottomView.setPath(600,
                600,
                600.0f,
                Path.Direction.CW,
                0.80f * (float)Math.PI * 1200f,
                20f);
        idleMessageBottomView.setText(idleMessageBottom);
    }

    protected void doIdle() {
        Log.i("Idle", "going into idle");
        idling = true;
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(idleHome), animateToHomeMS, null);
            mMap.animateCamera(CameraUpdateFactory.zoomTo((float) (idleZoom)), animateToHomeMS, null);
        }
        idleMessageTopView.setText(idleMessageTop);
        idleMessageBottomView.setText(idleMessageBottom);

    }

    protected void emergeFromIdle() {
        Log.i("Idle", "emerging from idle");
        idling = false;

        idleMessageTopView.setText("");
        idleMessageBottomView.setText("");
        idleHandler.postAtTime(runnable, uptimeMillis()+idleTime*idleTimeScaler);

    }

    protected void eatPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.gct_preferences, true);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        sensorServerAddress = sharedPref.getString(SettingsActivity.KEY_PREF_SENSOR_SERVER, sensorServerAddress);
        sensorServerPort = sharedPref.getString(SettingsActivity.KEY_PREF_SENSOR_SERVER_PORT, sensorServerPort);
        clicksPerRev = Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_SPIN_SENSOR_CLICKS_PER_REV,Integer.toString(clicksPerRev)));
        revsPerFullZoom = Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_SPIN_SENSOR_REVS_PER_FULL_ZOOM,Integer.toString(revsPerFullZoom)));
        TiltScaleX = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TILT_SENSOR_SCALE_FACTOR, Double.toString(TiltScaleX)));
        TiltScaleY = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TILT_SENSOR_SCALE_FACTOR, Double.toString(TiltScaleY)));
        useHybridMap = sharedPref.getBoolean(SettingsActivity.KEY_PREF_USE_HYBRID_MAP, useHybridMap);
        if (mMap != null) {
            if (useHybridMap) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            } else {
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            }
        }
        targetVisible = sharedPref.getBoolean(SettingsActivity.KEY_PREF_TARGET_VISIBLE, targetVisible);
        targetColor = sharedPref.getString(SettingsActivity.KEY_PREF_TARGET_COLOR, targetColor);
        idleMessageBottom = sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_TEXT_BOTTOM,idleMessageBottom);
        idleMessageTop = sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_TEXT_TOP,idleMessageTop);
        idleTime = Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_TIME, Integer.toString(idleTime)));
        idleZoom = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_ZOOM, Double.toString(idleZoom)));
        horizontalBump = Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_HORIZONTAL_BUMP, Integer.toString(horizontalBump)));
        double idleHomeLat = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_LAT, Double.toString(idleHome.latitude)));
        double idleHomeLon = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_LON, Double.toString(idleHome.longitude)));
        idleHome = new LatLng(idleHomeLat, idleHomeLon);
        //targetWidth = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TARGET_SIZE, Double.toString(targetWidth)));

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case EAT_PREFERENCES:
                eatPreferences();
                break;

        }

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


        JSONObject message = new JSONObject();
        try {
            message = new JSONObject(messageString);
        } catch (org.json.JSONException e) {
            Log.i("odd JSON",messageString);
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
            Log.i("no gesture message",message.toString());
        }

        lastInteractionTime = uptimeMillis();
        if (idling) emergeFromIdle();


        if (gestureType.equals("pan")) {
            //var dampingZoom = map.getZoom()*minZoom/maxZoom;
            double x = 0.0;
            double y = 0.0;
            JSONObject vector = new JSONObject();
            try {
                vector = message.getJSONObject("vector");
            } catch (org.json.JSONException e) {
                Log.e("reading pan message", "no vector " + message.toString());
            }
            try {

                x = TiltScaleX * vector.getDouble("x");
                y = TiltScaleY * vector.getDouble("y");
                if (Math.abs(x) < validTiltThreshold && Math.abs(y) < validTiltThreshold) return;

                //if (zoomLayers[currentZoom]["pannable"])
                //Log.i("incoming pan",x + "," + y);
                if (logSensors) {
                    Log.i("pan update", "x: " + Double.toString(x) +
                            " y: " + Double.toString(y) +
                            " current Lat:" +
                            Double.toString(mMap.getCameraPosition().target.latitude) +
                            " current Lon:" +
                            Double.toString(mMap.getCameraPosition().target.longitude)

                    );
                }
                mMap.animateCamera(CameraUpdateFactory.scrollBy((float) ( x), (float) ( y)));

            } catch (org.json.JSONException e) {
                Log.e("reading pan message", "invalid vector " + vector.toString());
            }
            //paintTarget();
        } else if (gestureType.equals("zoom")) {
            int delta = 0;
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
            currentSpinPosition += delta;
            currentSpinPosition = Math.max(minSpin,Math.min(currentSpinPosition,maxSpin));

            double proposedZoom = idleZoom + (double)currentSpinPosition / (double)clicksPerZoomLevel;
                if (logSensors) {
                Log.i("zoom update", "maxSpin: " + Integer.toString(maxSpin) +
                        "minSpin: " + Integer.toString(minSpin) +
                        " new zoom: " +
                        Double.toString(proposedZoom) +
                        " currentSpinPosition: " +
                        Integer.toString(currentSpinPosition));
                 }

            //restartIdleTimer();

            if (proposedZoom != currentZoom) {
                //doZoom(Math.min(Object.keys(zoomLayers).length - 1, Math.max(0,proposedZoom)));
                mMap.animateCamera(CameraUpdateFactory.zoomTo((float) (proposedZoom)));
                currentZoom = proposedZoom;

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
                Log.e("reading combo message", "no vector " + message.toString());
            }
            try {

                x = vector.getDouble("x");
                y = vector.getDouble("y");
                delta = vector.getDouble("delta");
            } catch (org.json.JSONException e) {
                Log.e("reading combo message", "invalid vector " + vector.toString());
            }
            if (x != 0.0 && y != 0.0) {
                mMap.animateCamera(CameraUpdateFactory.scrollBy((float) (100 * x), (float) (100 * y)));
            }
            currentSpinPosition += delta;
            double proposedZoom = currentSpinPosition / clicksPerZoomLevel;
            //restartIdleTimer();

            if (proposedZoom != currentZoom) {
                //doZoom(Math.min(Object.keys(zoomLayers).length - 1, Math.max(0,proposedZoom)));
                mMap.animateCamera(CameraUpdateFactory.zoomTo((float) (proposedZoom)));
                currentZoom = proposedZoom;

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

       TextView latDisplay  = (TextView)findViewById(R.id.currentLatitude);
       latDisplay.setText("Latitude: "+ String.format ("%.2f", mMap.getCameraPosition().target.latitude));
       TextView lonDisplay  = (TextView)findViewById(R.id.currentLongitude);
       lonDisplay.setText("Longitude: "+ String.format ("%.2f", mMap.getCameraPosition().target.longitude));
       //TextView altDisplay  = (TextView)findViewById(R.id.currentAltitude);
       //latDisplay.setText("Altitude: "+ Double.toString(mMap.getCameraPosition().target.));

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
        if (useHybridMap) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
Log.i("map ready","ok");
        // Add a marker in Sydney and move the camera
        mMap.addMarker(new MarkerOptions().position(idleHome).title(idleTitle));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(idleHome));
        mMap.animateCamera(CameraUpdateFactory.zoomTo((float) (idleZoom)));
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
