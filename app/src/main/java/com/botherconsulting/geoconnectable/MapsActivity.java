package com.botherconsulting.geoconnectable;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static android.os.SystemClock.uptimeMillis;

//import com.google.maps.android.data.kml.KmlLayer;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    static final int EAT_PREFERENCES = 12345;
    final Handler asyncTaskHandler = new Handler();
    final Runnable idleMonitor = new Runnable(){
        public void run() {
            long timeSinceLastInteraction = (uptimeMillis() - lastInteractionTime);
            //Log.i("idle timer", "Time's up " + Long.toString(timeSinceLastInteraction));
            if (timeSinceLastInteraction > idleTime*idleTimeScaler) {
                doIdle();
            } else {
                //Toast.makeText(MapsActivity.this, "C'Mom no hands!", Toast.LENGTH_SHORT).show();
                asyncTaskHandler.postAtTime(this, uptimeMillis() + idleCheckTime);
            }
        }
    };

    final Runnable sensorConnectionLauncher = new Runnable(){
        public void run() {
            launchServerConnection();
        }
    };

    private ZoomLens zoomer = new ZoomLens(0,0,19,0,13.5);

    private TablePanner panner = new TablePanner(zoomer.maxZoom, zoomer.minZoom);

    private boolean logZoom = true;
    private boolean logTilt = true;
    private boolean logSensors = true;
    private GoogleMap mMap;
    private boolean useHybridMap = true; // in settings
    private WebSocketClient mWebSocketClient;
    private String targetColor = "#ff0000"; // in settings
    private double targetWidth = 0.03; // portion of visible map  // in settings
    private boolean targetVisible = false; // in settings
    private int horizontalBump = 0; // in settings
    //var targetRectangle;
    private double currentScale = 1.0;
    //var mapData = [];
    //private double maxClicks = clicksPerRev * revsPerFullZoom * 1.0;
    private String idleMessageTop = "Spin tabletop to zoom";  // in settings
    private String idleMessageBottom = "Tilt tabletop to pan";  // in settings
    private int idleTime = 600; // in settings
    private int idleCheckTime = 10000; // once a minute look aat last interaction time
    private LatLng idleHome = new LatLng(40.76667,-111.903373);  // in settings
    private long lastInteractionTime = uptimeMillis();
    private int idleTimeScaler = 1000; // 1000 lets idleTime be in seconds
    private boolean idling = false;
    private int animateToHomeMS = 10000; // needs to be in settings
    private int settings_button_offset_x =  0;
    String idleTitle = "Clark Planetarium"; // needs to be in settings
    //String sensorServerAddress = "192.168.1.73";  // in settings
    String sensorServerAddress = "10.21.25.110";  // in settings
    String sensorServerPort = "5678";  // in settings
    BackgroundWebSocket bws;
    OuterCircleTextView idleMessageTopView;
    OuterCircleTextView idleMessageBottomView;

    /* need kml section as it appears in settings */
    /* need location stats params as they appear in settings */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        eatPreferences();
       // bws.execute("ws://192.168.1.73:5678");
        Log.w("idleTime set to", Integer.toString(idleTime));
        launchServerConnection();


        asyncTaskHandler.postAtTime(idleMonitor, uptimeMillis()+idleTime*idleTimeScaler);
        //idleHandler.postDelayed(runnable, idleTime+100);
        final Intent intent = new Intent(this, SettingsActivity.class);

        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                startActivityForResult(intent, EAT_PREFERENCES);
            }
        });
        floatingActionButton.setBackgroundColor(0x0);
        floatingActionButton.setRippleColor(0x0);
        floatingActionButton.setBackground(null);
        floatingActionButton.setBackgroundTintMode(PorterDuff.Mode.CLEAR);
        hideSystemUI();

        // if showScaleBar the below is wrong needs to be adapted for ViewOverlay
        /*scaleBarOverlay = new ScaleBarOverlay(this.getBaseContext(), this, myMapView);
        List<Overlay> overlays = myMapView.getOverlays();
// Add scale bar overlay
        scaleBarOverlay.setMetric();
        overlays.add(scaleBarOverlay);*/
        float instructionTextRadius = 640f;
        idleMessageTopView =  findViewById(R.id.IdleTopText);
        idleMessageTopView.setPath(760,
                                    640,
                                    instructionTextRadius,
                                    Path.Direction.CCW,
                                   0.85f * (float)Math.PI * instructionTextRadius * 2,
        -5f);
        idleMessageTopView.setText(idleMessageTop);
        idleMessageBottomView = findViewById(R.id.IdleBottomText);
        idleMessageBottomView.setPath(1260,
                                        980,
                                        instructionTextRadius,
                                        Path.Direction.CW,
                                        0.6f * (float)Math.PI * instructionTextRadius * 2,
                                        20f);
        idleMessageBottomView.setText(idleMessageBottom);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        if (logSensors) Toast.makeText(MapsActivity.this, "Screen res in pixels" + Integer.toString(width)+"x" + Integer.toString(height), Toast.LENGTH_LONG).show();

        doIdle();
    }

    protected void animateByTable() {
        zoomer.setZoomBounds(0, mMap.getMaxZoomLevel());
        mMap.moveCamera(CameraUpdateFactory.zoomTo((float) (zoomer.currentZoom)));
        mMap.animateCamera(CameraUpdateFactory.newLatLng(panner.currentPosition),10,null);

    }

    protected void doIdle() {
        Log.i("Idle", "going into idle");
        idling = true;
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(idleHome, (float) (zoomer.idleZoom)), animateToHomeMS, null);
        }
        idleMessageTopView.setText(idleMessageTop);
        idleMessageBottomView.setText(idleMessageBottom);

    }

    protected void emergeFromIdle() {
        Log.i("Idle", "emerging from idle");
        idling = false;

        idleMessageTopView.setText("");
        idleMessageBottomView.setText("");
        asyncTaskHandler.postAtTime(idleMonitor, uptimeMillis()+idleCheckTime);

    }


    protected void eatPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.gct_preferences, true);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String oldServerAddress = sensorServerAddress;
        String oldServerPort = sensorServerPort;
        sensorServerAddress = sharedPref.getString(SettingsActivity.KEY_PREF_SENSOR_SERVER, sensorServerAddress);
        sensorServerPort = sharedPref.getString(SettingsActivity.KEY_PREF_SENSOR_SERVER_PORT, sensorServerPort);
        if (!sensorServerAddress.equals(oldServerAddress) || !sensorServerPort.equals(oldServerPort)) {
           Log.i("wtf", "new " + sensorServerAddress+":"+sensorServerPort + " old " + oldServerAddress +":"+oldServerPort );
           //ltime'saunchServerConnection();
        }
        zoomer.configure(Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_SPIN_SENSOR_CLICKS_PER_REV,Integer.toString(zoomer.clicksPerRev))),
                        Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_SPIN_SENSOR_REVS_PER_FULL_ZOOM,Integer.toString(zoomer.revsPerFullZoom))),
                 Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_ZOOM, Double.toString(zoomer.idleZoom))));

        panner.configure(Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TILT_SENSOR_SCALE_FACTOR, Double.toString(panner.TiltScaleX))),
                Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TILT_SENSOR_SCALE_FACTOR, Double.toString(panner.TiltScaleY))));
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
        horizontalBump = Integer.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_HORIZONTAL_BUMP, Integer.toString(horizontalBump)));
        double idleHomeLat = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_LAT, Double.toString(idleHome.latitude)));
        double idleHomeLon = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_LON, Double.toString(idleHome.longitude)));
        if (idleHomeLat != idleHome.latitude || idleHomeLon != idleHome.longitude) {
            idleHome = new LatLng(idleHomeLat, idleHomeLon);
            Log.i("new home? " , Double.toString(idleHomeLat) +","+ Double.toString(idleHome.latitude)  +" "+
                    Double.toString(idleHomeLon)  +","+  Double.toString(idleHome.longitude));
        }
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
        /*FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        CoordinatorLayout bigwin = ((CoordinatorLayout) floatingActionButton.getParent());
        bigwin.setLayoutParams(new CoordinatorLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        */
    }

   private void retrieveFileFromUrl() {
        new DownloadKmlFile(getString(R.string.kml_url)).execute();
    }
    private static class DownloadKmlFile extends AsyncTask<String, Void, byte[]> {
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
       /*      try {
               KmlLayer kmlLayer = new KmlLayer(mMap, new ByteArrayInputStream(byteArr),
                        getApplicationContext());
                kmlLayer.addLayerToMap();

            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } */
        }
    }

    private void launchServerConnection() {
        bws = new BackgroundWebSocket();
        Log.i("starting websocket", sensorServerAddress +":"+sensorServerPort);
        bws.execute("ws://"+ sensorServerAddress + ":" + sensorServerPort);

    }

    private void complain(String... message) {
        Toast.makeText(MapsActivity.this, message[0] + ":" + message[1], Toast.LENGTH_LONG).show();
        if (message[0].equals("Connection closed")) asyncTaskHandler.postDelayed(sensorConnectionLauncher,15000);
    }

   private void onMessage(String messageString) {

        if (mMap == null) return;


        JSONObject message;
        try {
            message = new JSONObject(messageString);
        } catch (org.json.JSONException e) {
            Log.i("odd JSON",messageString);
            return;
        }
        String messageType;
        String gestureType;
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
            return;
        }

        LatLngBounds curScreen = mMap.getProjection()
               .getVisibleRegion().latLngBounds;

        if (gestureType.equals("pan")) {
            panner.handleJSON(message,mMap, logSensors || logTilt);
            //var dampingZoom = map.getZoom()*minZoom/maxZoom;

            //paintTarget();
        } else if (gestureType.equals("zoom")) {
            zoomer.handleJSON(message,mMap, logSensors || logZoom);


        }

       lastInteractionTime = uptimeMillis();
       if (idling) emergeFromIdle();

       animateByTable();

       TextView latDisplay  = findViewById(R.id.currentLatitude);
       latDisplay.setText("Latitude: "+ String.format ("%.2f", mMap.getCameraPosition().target.latitude));
       TextView lonDisplay  = findViewById(R.id.currentLongitude);
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
                    publishProgress("message", message);

                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    Log.i("Websocket", "Closed " + s);
                    publishProgress("Connection closed", s);
                    //mWebSocketClient.connect();
                }

                @Override
                public void onError(Exception e) {

                    Log.i("Websocket", "Error " + e.getMessage());
                    publishProgress("Connection Error", e.getMessage());

                }
            };
            mWebSocketClient.connect();
            return null;
        }

        protected void onProgressUpdate(String... progress) {

            //update the progress
            if (progress.length >=2 ) {
                if (progress[0].equals("message")) {
                    onMessage(progress[1]);
                } else {
                    //onMessage(progress[0]);
                    complain(progress);
                }
            }

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
Log.i("map ready",idleHome.toString());
        // Add a marker in Sydney and move the camera
        mMap.addMarker(new MarkerOptions().position(idleHome).title(idleTitle).icon(BitmapDescriptorFactory.fromResource(R.drawable.clark_planetarium_logo_50)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(idleHome,(float) zoomer.idleZoom));
        LatLngBounds curScreen = mMap.getProjection()
                .getVisibleRegion().latLngBounds;
        ScaleBarOverlay mScaleBar = new ScaleBarOverlay(this, mMap);
        //LatLng radius = new LatLng(curScreen.northeast.latitude, curScreen.getCenter().longitude);
        //Log.i("mask radius", radius.toString());
        //mMap.addPolygon(MapMask.createPolygonWithCircle(this, sydney, radius));
        //mMap.addPolygon(MapMask.createPolygonWithCircle(this, sydney, 100));
        //Intent i = new Intent(this,TablePreferencesActivity.class);
        //startActivityForResult(i, SHOW_PREFERENCES);
    }



}
