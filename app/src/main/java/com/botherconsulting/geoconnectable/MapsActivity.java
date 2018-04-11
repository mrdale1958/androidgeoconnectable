package com.botherconsulting.geoconnectable;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
//import android.support.v7.preference.PreferenceManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pengrad.mapscaleview.MapScaleView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static android.os.SystemClock.uptimeMillis;

//import com.google.maps.android.data.kml.KmlLayer;

public class MapsActivity
        extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnCameraChangeListener {

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

    private ZoomLens zoomer;

    private TablePanner panner = new TablePanner(zoomer.maxZoom, zoomer.minZoom);

    private int[][] maxZoomCache = new int[181][360];
    private boolean logZoom = false;
    private boolean logTilt = false;
    private boolean logSensors = false;
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
    private int idleCheckTime = 60000; // once a minute look aat last interaction time
    private LatLng idleHome = new LatLng(40.76667,-111.903373);  // in settings
    private long lastInteractionTime = uptimeMillis();
    private int idleTimeScaler = 1000; // 1000 lets idleTime be in seconds
    private boolean idling = false;
    private int animateToHomeMS = 10000; // needs to be in settings
    private int settings_button_offset_x =  0;
    String idleTitle = "Clark Planetarium"; // needs to be in settings
    //String sensorServerAddress = "192.168.1.73";  // in settings
    String sensorServerAddress = "10.21.3.42";  // in settings
    String sensorServerPort = "5678";  // in settings
    BackgroundWebSocket bws;
    OuterCircleTextView idleMessageTopView;
    OuterCircleTextView idleMessageBottomView;

    /* need kml section as it appears in settings */
    /* need location stats params as they appear in settings */

    @Override
    protected void onStop() {
        // call the superclass method first
        super.onStop();
        // need to save maxZoomCache
        try {
            JSONArray maxzoomarray = new JSONArray();
            for (int lat = 0; lat < 181; lat++) {
                JSONArray latarray = new JSONArray();
                for (int lon = 0; lon < 360; lon++) {
                    latarray.put(maxZoomCache[lat][lon]);
                }
                maxzoomarray.put(latarray);
            }
            String maxCacheAsString = maxzoomarray.toString();
            FileOutputStream fos = openFileOutput("maxZoomData", Context.MODE_PRIVATE);
            fos.write(maxCacheAsString.getBytes());
            fos.close();
        //} catch (JSONException e) {
        //    Log.e("maxZoomCache", "oops broke it" + e.getMessage());
        } catch (java.io.FileNotFoundException e) {
        Log.e("writing maxZoomCache", "oops broke it" + e.getMessage());
        } catch (java.io.IOException e) {
            Log.e("writing maxZoomCache", "oops broke it" + e.getMessage());
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            FileInputStream fis = openFileInput("maxZoomData");
            if ( fis != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(fis);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                fis.close();
                String maxCacheAsString = stringBuilder.toString();
                JSONArray maxzoomarray = new JSONArray(maxCacheAsString);
                //Log.i("reading maxzoomarray:\n", maxzoomarray.toString(4));
                for (int lat = 0; lat < 181; lat++) {
                    JSONArray latarray = maxzoomarray.getJSONArray(lat);
                    for (int lon = 0; lon < 360; lon++) {
                        maxZoomCache[lat][lon] = latarray.getInt(lon);
                    }
                }

            }
        } catch (JSONException e) {
            Log.e("reading maxZoomCache", "oops broke it" + e.getMessage());
        } catch (java.io.FileNotFoundException e) {
            Log.e("reading maxZoomCache", "no file no foul" + e.getMessage());
        } catch (java.io.IOException e) {
            Log.e("reading maxZoomCache", "oops broke it" + e.getMessage());
        }
         setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //mapFragment.mumble(GoogleMap.OnMapLoadedCallback)
        eatPreferences();
       // bws.execute("ws://192.168.1.73:5678");
        Log.w("idleTime set to", Integer.toString(idleTime));
        launchServerConnection();

        WebView webView = (WebView) findViewById(R.id.maxZoomPortal);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");


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

        final Intent fabintent = new Intent(this, ContentActivity.class);

        FloatingActionButton contentEditorActionButton = findViewById(R.id.content);
        contentEditorActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(fabintent, EAT_PREFERENCES);
            }
        });
        contentEditorActionButton.setBackgroundColor(0x0);
        contentEditorActionButton.setRippleColor(0x0);
        floatingActionButton.setBackground(null);
        contentEditorActionButton.setBackgroundTintMode(PorterDuff.Mode.CLEAR);
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
        if (idling) emergeFromIdle();

    }
    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface() {
            //mContext = c;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
        public void adjustMaxZoom(String response) {
            Log.w("adjustMaxZoom", response);
            JSONObject message;
            try {
                message = new JSONObject(response);
            } catch (org.json.JSONException e) {
                Log.i("odd JSON",response);
                return;
            }

            try {
                double lat = message.getDouble("lat");
                double lon = message.getDouble("lon");
                int zoom = message.getInt("zoom");
                lat = Math.min(Math.max(lat, -90f), 90f);
                int latIndex = (int) Math.round(lat) + 90;
                lon = lon % 360;
                int lonIndex = (int) Math.round(lon) + 180; // not quite right need to cope with
                maxZoomCache[latIndex][lonIndex] = Math.min(19,zoom-1);

            }catch (org.json.JSONException e) {
                Log.i("odd JSON",response);
                return;
            }
        }

        public void bindMZS(String mzs){
            Log.w("bindMZS", mzs);
        }
    }


    final Runnable  animateByTable  = new Runnable() {
        long lastRuntime;
        static final int PAN = 0;
        static final int GESTURE_TIME = 1;

        public void run() {

            zoomer.setZoomBounds(3, Math.min(19.0,mMap.getMaxZoomLevel()));
            long lastZoomTime = 0;
            boolean doAnimate = false;
            if (zoomer.newData) {

                //if (Math.floor(newZoom) != Math.floor(mMap.getCameraPosition().zoom)) Log.i("new zoom layer", Float.toString(newZoom));
                doAnimate = zoomer.needToAnimate();
            }
            LatLng newPos = mMap.getCameraPosition().target;
            long lastPanTime = 0;
            if (panner.newData) {
                Object[] pannerData = panner.getCurrentPosition();
                newPos = (LatLng) pannerData[PAN];
                lastPanTime = (long) pannerData[GESTURE_TIME];
                //Log.i("new position", newPos.toString());
                doAnimate = true;
            }
            //mMap.moveCamera(CameraUpdateFactory.zoomTo((float) (zoomer.currentZoom)));
            if (doAnimate) {
                int animateTime = (int) Math.max(1,(uptimeMillis() - Math.max(lastPanTime,lastZoomTime)));
                //Log.i("animating camera", newPos.toString() + ',' + Float.toString(newZoom)  + " in " + Integer.toString(animateTime) + "ms");
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPos, zoomer.newZoom), animateTime, new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        TextView latDisplay = findViewById(R.id.currentLatitude);
                        latDisplay.setText("Latitude: " + String.format("%.2f", mMap.getCameraPosition().target.latitude));
                        TextView lonDisplay = findViewById(R.id.currentLongitude);
                        lonDisplay.setText("Longitude: " + String.format("%.2f", mMap.getCameraPosition().target.longitude));

                        if (!idling){
                            //Log.i("animateByTable", "now");
                            asyncTaskHandler.post(animateByTable);
                        }
                    }

                    @Override
                    public void onCancel() {
                        Log.w("animateByTable", "hmm animation got canceled");
                    }
                });
                if (idling) emergeFromIdle();
                lastInteractionTime = uptimeMillis();
            } else {
                //Log.i("animateByTable", "in the future");
                asyncTaskHandler.postAtTime(animateByTable, uptimeMillis() + 100);

            }

        }
    };

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
        asyncTaskHandler.post(animateByTable);
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

        try {
            double idleHomeLat = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_LAT, Double.toString(idleHome.latitude)));
            double idleHomeLon = Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_IDLE_LON, Double.toString(idleHome.longitude)));
            if (idleHomeLat != idleHome.latitude || idleHomeLon != idleHome.longitude) {
                idleHome = new LatLng(idleHomeLat, idleHomeLon);
                Log.i("new home? ", Double.toString(idleHomeLat) + "," + Double.toString(idleHome.latitude) + " " +
                        Double.toString(idleHomeLon) + "," + Double.toString(idleHome.longitude));
            }
        } catch (java.lang.NumberFormatException e) {
            Toast.makeText(MapsActivity.this, "Invalid input" + ":" + e.getCause(), Toast.LENGTH_LONG).show();

        }

        panner.configure(Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TILT_SENSOR_SCALE_FACTOR, Double.toString(panner.TiltScaleX))),
                    Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TILT_SENSOR_SCALE_FACTOR, Double.toString(panner.TiltScaleY))),
                    idleHome);

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

            //paintTarget();
        } else if (gestureType.equals("zoom")) {
            zoomer.handleJSON(message,mMap, logSensors || logZoom);


        }


       /*animateByTable();

       TextView latDisplay  = findViewById(R.id.currentLatitude);
       latDisplay.setText("Latitude: "+ String.format ("%.2f", mMap.getCameraPosition().target.latitude));
       TextView lonDisplay  = findViewById(R.id.currentLongitude);
       lonDisplay.setText("Longitude: "+ String.format ("%.2f", mMap.getCameraPosition().target.longitude));
       */
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


    @Override
    public void onCameraMove() {
        MapScaleView scaleView = (MapScaleView) findViewById(R.id.scaleView);
        CameraPosition cameraPosition = mMap.getCameraPosition();
        scaleView.update(cameraPosition.zoom, cameraPosition.target.latitude);
    }

    @Override
    public void onCameraIdle() {
        MapScaleView scaleView = (MapScaleView) findViewById(R.id.scaleView);
        CameraPosition cameraPosition = mMap.getCameraPosition();
        scaleView.update(cameraPosition.zoom, cameraPosition.target.latitude);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        MapScaleView scaleView = (MapScaleView) findViewById(R.id.scaleView);
        scaleView.update(cameraPosition.zoom, cameraPosition.target.latitude);
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
        zoomer = new MapZoomer(mMap, maxZoomCache, (WebView) findViewById(R.id.maxZoomPortal), 0,0,19,3,13.5);
        mMap.setOnCameraMoveListener(this);
        mMap.setOnCameraIdleListener(this);
        mMap.setOnCameraChangeListener(this);
       /* mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                Log.i("mapLoaded", "hmmm");
            }
        });*/
        /*
        Drawable drawable = getResources().getDrawable(R.drawable.nodatatile,getTheme());

        DeadTileProvider dtp = new DeadTileProvider(drawable);
        TileOverlay tileOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                .tileProvider(dtp)
                .zIndex((-1f)));*/

        Log.i("map ready",idleHome.toString());
        // Add a marker in Sydney and move the camera
        mMap.addMarker(new MarkerOptions().position(idleHome).title(idleTitle).icon(BitmapDescriptorFactory.fromResource(R.drawable.clark_planetarium_logo_50)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(idleHome,(float) zoomer.idleZoom));
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
