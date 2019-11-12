package com.botherconsulting.geoconnectable;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.github.pengrad.mapscaleview.MapScaleView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
//import com.google.maps.android.SphericalUtil;

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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static android.os.SystemClock.uptimeMillis;

//import com.google.maps.android.data.kml.KmlLayer;

public class MapsActivity
        extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraIdleListener {

    static final int EAT_PREFERENCES = 12345;
    static final int EAT_HOTSPOTS = 12346;
    HandlerThread animationHandlerThread;

    Handler animationHandler;
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

    private ZoomLens zoomer = new ZoomLens(0,0,19,4,0);

    private TablePanner panner = new TablePanner(zoomer.maxZoom, zoomer.minZoom);

    private int[][] maxZoomCache = new int[181][361];
    private boolean logZoom = false;
    private boolean logTilt = false;
    private boolean logSensors = false;
    private GoogleMap mMap;
    private boolean useHybridMap = true; // in settings
    private WebSocketClient mWebSocketClient;
    private String targetColor = "#ff0000"; // in settings
    private double targetWidth = 0.1; // portion of visible map  // in settings
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
    private LatLng idleHome = new LatLng(33.1135, -96.7129); //41.76667,-111.903373);  // in settings
    private long lastInteractionTime = uptimeMillis();
    private int idleTimeScaler = 1000; // 1000 lets idleTime be in seconds
    private boolean idling = false;
    private int nullAnimationClockTick = 100;
    private int minZoomLevel = 4; // needs to be in settings
    private int animateToHomeMS = 10000; // needs to be in settings
    private double maxPanPercent = 0.01; // needs to be in settings
    private int settings_button_offset_x =  0;
    private ArrayList<ImageHotspot>   hotspots= new ArrayList<ImageHotspot>(5);
    private Boolean hotSpotActive = false;
    private ImageHotspot liveHotSpot;
    private double currScreenWidth;
    private double currScreenHeight;
    private boolean readyToAnimate = true;
    private Polygon targetRectangle;

    String idleTitle = "SFSU"; // needs to be in settings

    String sensorServerAddress = "10.240.100.239";  // in settings
    //String sensorServerAddress = "192.168.1.73";  // in settings
    //String sensorServerAddress = "10.21.3.42";  // in settings
    String sensorServerPort = "5678";  // in settings
    BackgroundWebSocket bws;
    OuterCircleTextView idleMessageTopView;
    OuterCircleTextView idleMessageBottomView;

    String hotspotsJSONFile = "GlobalMagic.json";
    ImageHotspot.Languages  hotspotLanguage = ImageHotspot.Languages.ENGLISH;
    int lastHotSpotShown = 0;

    /* need kml section as it appears in settings */
    /* need location stats params as they appear in settings */

    private void saveMaxZoomData() {
        try {
            JSONArray maxzoomarray = new JSONArray();
            for (int lat = 0; lat < 181; lat++) {
                JSONArray latarray = new JSONArray();
                for (int lon = 0; lon < 361; lon++) {
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
            Log.e("writing maxZoomCache", "oops broke it no file" + e.getMessage());
        } catch (java.io.IOException e) {
            Log.e("writing maxZoomCache", "oops broke it no io" + e.getMessage());
        }
    }
    @Override
    protected void onStop() {
        // call the superclass method first
        super.onStop();
        // need to save maxZoomCache
        saveMaxZoomData();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        animationHandlerThread = new HandlerThread("AnimationHandlerThread", 1);
        animationHandlerThread.start();
        animationHandler = new Handler(animationHandlerThread.getLooper());

        FileInputStream fis = null;
        try {
            fis = openFileInput("maxZoomData");
        } catch (java.io.FileNotFoundException e) {
            Log.i("reading maxZoomCache", "no file no foul just use empty array" + e.getMessage());
        }
        try {
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
                    for (int lon = 0; lon < 361; lon++) {
                        maxZoomCache[lat][lon] = latarray.getInt(lon);
                    }
                }

            }
        } catch (JSONException e) {
            Log.e("reading maxZoomCache", "oops broke json" + e.getMessage());
        } catch (java.io.IOException e) {
            Log.e("reading maxZoomCache", "oops broke io" + e.getMessage());
        }
         setContentView(R.layout.activity_maps);
        /*// Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
*/        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment;
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        try {
            mapFragment.getMapAsync(this);
        }
        catch (NullPointerException e)  {
            Log.e("mapFragment.getMapAsync", "null something" + e.getMessage());
        }
        //mapFragment.mumble(GoogleMap.OnMapLoadedCallback)
        eatPreferences();
       // bws.execute("ws://192.168.1.73:5678");
        Log.i("idleTime set to", Integer.toString(idleTime));
        launchServerConnection();


        WebView hotSpotWebView = (WebView) findViewById(R.id.hotSpotWebView);
        hotSpotWebView.getSettings().setJavaScriptEnabled(true);
        hotSpotWebView.getSettings().setDomStorageEnabled(true);
        hotSpotWebView.addJavascriptInterface(new WebAppInterface(), "Android");
        hotSpotWebView.setWebViewClient(new WebViewClient(){
            // Override page so it's load on my view only
            @Override
            public boolean shouldOverrideUrlLoading(WebView maxZoomWebView, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageStarted(WebView maxZoomWebView, String url, Bitmap facIcon) {

                //mLayoutProgress.setVisibility(View.VISIBLE);

            }

            @Override
            public void onReceivedSslError(WebView maxZoomWebView, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onPageFinished(WebView maxZoomWebView, String url) {

                //mLayoutProgress.setVisibility(View.GONE);
            }
        });

        WebView maxZoomWebView = (WebView) findViewById(R.id.maxZoomPortal);
        maxZoomWebView.getSettings().setJavaScriptEnabled(true);
        maxZoomWebView.getSettings().setDomStorageEnabled(true);
        maxZoomWebView.addJavascriptInterface(new WebAppInterface(), "Android");
        maxZoomWebView.setWebViewClient(new WebViewClient(){
            // Override page so it's load on my view only
            @Override
            public boolean shouldOverrideUrlLoading(WebView maxZoomWebView, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageStarted(WebView maxZoomWebView, String url, Bitmap facIcon) {

                //mLayoutProgress.setVisibility(View.VISIBLE);

            }

            @Override
            public void onReceivedSslError(WebView maxZoomWebView, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onPageFinished(WebView maxZoomWebView, String url) {

                //mLayoutProgress.setVisibility(View.GONE);
            }
        });


        asyncTaskHandler.postAtTime(idleMonitor, uptimeMillis()+idleTime*idleTimeScaler);
        //idleHandler.postDelayed(runnable, idleTime+100);
        final Intent settingsIntent = new Intent(this, SettingsActivity.class);

        FloatingActionButton settings_floatingActionButton = findViewById(R.id.settings_Editor_fab);
        settings_floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(settingsIntent, EAT_PREFERENCES);
            }
        });
        settings_floatingActionButton.setBackgroundColor(0x0);
        settings_floatingActionButton.setRippleColor(0x0);
        settings_floatingActionButton.setBackground(null);
        settings_floatingActionButton.setBackgroundTintMode(PorterDuff.Mode.CLEAR);

        final Intent hotspotsIntent = new Intent(this, SettingsActivity.class);

        FloatingActionButton hotspots_floatingActionButton = findViewById(R.id.hotSpotEditor_fab);
        settings_floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(hotspotsIntent, EAT_HOTSPOTS);
            }
        });
        hotspots_floatingActionButton.setBackgroundColor(0x0);
        hotspots_floatingActionButton.setRippleColor(0x0);
        hotspots_floatingActionButton.setBackground(null);
        hotspots_floatingActionButton.setBackgroundTintMode(PorterDuff.Mode.CLEAR);

        final Intent htmlOverlayIntent = new Intent(this, ContentActivity.class);

       /* GridLayout hotSpotDataContainer = findViewById(R.id.hotSpotView);
        hotSpotDataContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(intent, EAT_PREFERENCES);
            }
        });
        hotSpotDataContainer.setBackgroundColor(0x0);
        hotSpotDataContainer.setBackground(null);
        hotSpotDataContainer.setBackgroundTintMode(PorterDuff.Mode.CLEAR);
*/
        hideSystemUI();

        // if showScaleBar the below is wrong needs to be adapted for ViewOverlay
        /*scaleBarOverlay = new ScaleBarOverlay(this.getBaseContext(), this, myMapView);
        List<Overlay> overlays = myMapView.getOverlays();
// Add scale bar overlay
        scaleBarOverlay.setMetric();
        overlays.add(scaleBarOverlay);*/
        float instructionTextRadius = 580f;
        idleMessageTopView =  findViewById(R.id.IdleTopText);
        idleMessageTopView.setPath(600,
                                    600,
                                    instructionTextRadius,
                                    Path.Direction.CCW,
                                   0.85f * (float)Math.PI * instructionTextRadius * 2,
        -5f);
        idleMessageTopView.setText(idleMessageTop);
        idleMessageBottomView = findViewById(R.id.IdleBottomText);
        idleMessageBottomView.setPath(600,
                                        600,
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
            Log.i("adjustMaxZoom", response);
            JSONObject message;
            try {
                message = new JSONObject(response);
            } catch (org.json.JSONException e) {
                Log.w("odd JSON",response);
                return;
            }

            try {
                double lat = message.getDouble("lat");
                double lon = message.getDouble("lon");
                int zoom = message.getInt("zoom");
                lat = Math.min(Math.max(lat, -90f), 90f);
                int latIndex = (int) Math.round(lat) + 90;
                lon = lon % 360;
                int lonIndex = Math.min(360, Math.max(0,(int) Math.round(lon) + 180)); // not quite right need to cope with
                maxZoomCache[latIndex][lonIndex] = Math.min(19,zoom-1);

            }catch (org.json.JSONException e) {
                Log.w("odd JSON",response);
            }
        }

        public void bindMZS(String mzs){
            Log.w("bindMZS", mzs);
        }

        public void returnTableControlToMap()
        {
            hotSpotActive = false;
            WebView webhotspot = (WebView) findViewById(R.id.hotSpotWebView);
            ImageView imagehotspot = (ImageView) findViewById(R.id.hotSpotImageView);
            View mapView =  (View) findViewById(R.id.map);
            webhotspot.setVisibility(View.INVISIBLE);
            imagehotspot.setVisibility(View.INVISIBLE);  // animate shrinking?
            mapView.bringToFront();

        }
    }

    public void checkMaxZoom(float newZoom, CameraPosition newPos) {
        //Log.w("zoom checking", Double.toString(Math.floor(newZoom)) +" > " + Float.toString(mMap.getCameraPosition().zoom) +
        //        "\n reported minZoom: " + mMap.getMinZoomLevel() + " max: " + mMap.getMaxZoomLevel());
        //String mzsURL = "http://192.168.1.64/mzs.html?"+
        String mzsURL = "file:///android_asset/www/mzs.html?"+
                newPos.target.latitude +
                "," +
                newPos.target.longitude;
        //Log.w("mzs", mzsURL);
        runOnUiThread(new Runnable() {
            public void run() {
                WebView maxZoomWebView = (WebView) findViewById(R.id.maxZoomPortal);
                maxZoomWebView.loadUrl(mzsURL);
            }
        });
    }
    enum Sections  {
        START,
        ZOOMNEWDATA,
        PANNEWDATA,
        ANIMATEHOTSPOT,
        SETUPMAPMOVE,
        TESTHOTSPOTS,
        ANIMATEMAP,
        POSTANIMATEMAP
    };
    enum Profilestates  {
        START,
        FINISH
    }
    float maxZoom = -1f;
    CameraPosition cameraPosition;
    Projection mapProjection;


    private void getUIObjects() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mMap != null) {
                    maxZoom = mMap.getMaxZoomLevel();
                    cameraPosition = mMap.getCameraPosition();
                    mapProjection = mMap.getProjection();
                }
            }
        });
    }


    final Runnable  animateByTable  = new Runnable() {
        long lastRuntime;
        static final int ZOOM = 0;
        static final int PAN = 0;
        static final int GESTURE_TIME = 1;
        Map<Sections, Integer> profileEntries = new EnumMap<>(Sections.class);
        Map<Sections, Long> lastStartTimes = new EnumMap<>(Sections.class);
        Map<Sections, Long> sumElapsedTimes = new EnumMap<>(Sections.class);
        Map<Sections, Long> maxElapsedTimes = new EnumMap<>(Sections.class);
        Map<Sections, Long> minElapsedTimes = new EnumMap<>(Sections.class);
        boolean initializedProfile = false;
        float newZoom;
        LatLng newPos;



        void profile(Sections section, Profilestates state) {
            if (!initializedProfile) {
                for (Sections sec : Sections.values()) {

                    profileEntries.put(sec, 1);
                    lastStartTimes.put(sec, System.nanoTime());
                    sumElapsedTimes.put(sec, 0L);
                    maxElapsedTimes.put(sec, Long.MIN_VALUE);
                    minElapsedTimes.put(sec, Long.MAX_VALUE);
                }
                initializedProfile = true;
            }
            if (state == Profilestates.START) {
                lastStartTimes.put(section, System.nanoTime());
            } else {
                Long elapsedTime = System.nanoTime() - lastStartTimes.get(section);
                sumElapsedTimes.put(section, sumElapsedTimes.get(section) + elapsedTime);
                maxElapsedTimes.put(section, Math.max(maxElapsedTimes.get(section), elapsedTime));
                minElapsedTimes.put(section, Math.min(minElapsedTimes.get(section), elapsedTime));
                profileEntries.put(section, profileEntries.get(section) + 1);

            }
            if (profileEntries.get(section) % 100000 == 0) {
//                Log.i("profile report", section +
//                        "\nmean: " + sumElapsedTimes.get(section)/(1e6*profileEntries.get(section)) +
//                        "\nmax : " + maxElapsedTimes.get(section)/1e6 +
//                        "\nmin : " + minElapsedTimes.get(section)/1e6
//                );
            }

        }

        public void run() {
            // TODO: label this 19. Does it set a maximum zoom level for the app
            long blockStartTime, blockEndTime, startTime = System.nanoTime();
            long elapsedTime = startTime - lastRuntime;
            lastRuntime = startTime;
            getUIObjects();
            profile(Sections.START, Profilestates.START);
            boolean doAnimate = false;
            long lastZoomTime = 0;
            long lastPanTime = 0;
            boolean retriggered = false;
            //Log.d("animateBytable", "starting");
            if ((mapProjection != null) && (cameraPosition != null) && (maxZoom > -1)) {
                zoomer.setZoomBounds(minZoomLevel, Math.min(19.0, maxZoom));

                 newZoom = cameraPosition.zoom;
                 newPos = cameraPosition.target;
                profile(Sections.START, Profilestates.FINISH);
                if (zoomer.newData) {
                    profile(Sections.ZOOMNEWDATA, Profilestates.START);
                    Object[] zoomData = zoomer.getCurrentZoom();
                    newZoom = (float) zoomData[ZOOM];
                    lastZoomTime = (long) zoomData[GESTURE_TIME];
                    int latIndex = (int) Math.round(cameraPosition.target.latitude) + 90;
                    int lonIndex = Math.min(360, Math.max(0, (int) Math.round(cameraPosition.target.longitude) + 180)); // not quite right need to cope with
                    if (maxZoomCache[latIndex][lonIndex] > 0) {
                        zoomer.setZoomBounds(zoomer.minZoom, maxZoomCache[latIndex][lonIndex]);
                    } else if (Math.floor(newZoom) > cameraPosition.zoom) {
                        checkMaxZoom(newZoom, cameraPosition);
                    }
                    //if (Math.floor(newZoom) != Math.floor(mMap.getCameraPosition().zoom)) Log.i("new zoom layer", Float.toString(newZoom));
                    doAnimate = true;
                    profile(Sections.ZOOMNEWDATA, Profilestates.FINISH);

                }
                if (panner.newData) {
                    profile(Sections.PANNEWDATA, Profilestates.START);
                    Object[] pannerData = panner.getCurrentPosition();
                    newPos = (LatLng) pannerData[PAN];
                    lastPanTime = (long) pannerData[GESTURE_TIME];
                    //Log.i("new position", newPos.toString());
                    doAnimate = true;
                    profile(Sections.PANNEWDATA, Profilestates.FINISH);

                }
                if (hotSpotActive) {
                    // deal with animating hotSpot
                    // need a mechanism to get clear of target voxel  before redisplaying
                    profile(Sections.ANIMATEHOTSPOT, Profilestates.START);
                    if (liveHotSpot.isClosed()) {
                        hotSpotActive = false;
                        liveHotSpot = null;
                    }
                    retriggered=true;
                    animationHandler.post(animateByTable);
                    profile(Sections.ANIMATEHOTSPOT, Profilestates.FINISH);

                } else {
                    profile(Sections.SETUPMAPMOVE, Profilestates.START);
                    VisibleRegion visibleRegion = mapProjection.getVisibleRegion();
                    double currLeft = visibleRegion.farLeft.longitude;
                    double currRight = visibleRegion.farRight.longitude;
                    double currTop = visibleRegion.farLeft.latitude;
                    double currBottom = visibleRegion.nearRight.latitude;
                    currScreenWidth = Math.abs(currLeft - currRight);
                    //if (currScreenWidth > 180) currScreenWidth -= 180;
                    currScreenHeight = Math.abs(currTop - currBottom);
                    if (currScreenHeight > 180) currScreenHeight -= 180;
                    LatLngBounds hotBounds = new LatLngBounds(
                            // southwest
                            new LatLng(newPos.latitude - targetWidth * currScreenHeight,
                                    newPos.longitude - targetWidth * currScreenWidth),
                            // northeast
                            new LatLng(newPos.latitude + targetWidth * currScreenHeight,
                                    newPos.longitude + targetWidth * currScreenWidth));
                    Boolean hotspotFound = false;
                    profile(Sections.SETUPMAPMOVE, Profilestates.FINISH);
                    profile(Sections.TESTHOTSPOTS, Profilestates.START);
                    for (int hs = 0; hs < hotspots.size(); hs++) {
                        final int hotspotnum = hs;
                        if (hotBounds.contains(hotspots.get(hs).getPosition())) {
                             if (newZoom > hotspots.get(hs).hotSpotZoomTriggerRange[0] -2) {
                                //Log.d("targeting", "distance " + SphericalUtil.computeDistanceBetween(newPos, hotspots.get(hs).getPosition()));
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        hotspots.get(hotspotnum).marker.showInfoWindow();
                                        hotspots.get(hotspotnum).select();
                                    }
                                });
                            }
                            if (newZoom > hotspots.get(hs).hotSpotZoomTriggerRange[0] &&
                                    newZoom < hotspots.get(hs).hotSpotZoomTriggerRange[1]) {
                                hotSpotActive = true;
                                liveHotSpot = hotspots.get(hs);
                                if (ImageHotspot.activate(liveHotSpot)) {
                                    View mapView = (View) findViewById(R.id.map);
                                    //mapView.setVisibility(View.INVISIBLE);
                                    Log.i("hotspot loading ", "number " + hs);
                                    liveHotSpot.setImageByLanguage(hotspotLanguage);
                                    liveHotSpot.open();
                                    retriggered = true;
                                }
                                animationHandler.post(animateByTable);
                                break;
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    hotspots.get(hotspotnum).marker.hideInfoWindow();
                                    hotspots.get(hotspotnum).deselect();
                                }
                            });
                        }
                    }
                }
                profile(Sections.TESTHOTSPOTS, Profilestates.FINISH);
                //mMap.moveCamera(CameraUpdateFactory.zoomTo((float) (zoomer.currentZoom)));
                if (doAnimate) {
                    //Log.d("animatebytable ", "in doanimate");
   /*VisibleRegion visibleRegion = mMap.getProjection().getVisibleRegion();
                double currLeft = visibleRegion.farLeft.longitude;
                double currRight = visibleRegion.farRight.longitude;
                double currTop = visibleRegion.farLeft.latitude;
                double currBottom = visibleRegion.nearRight.latitude;
                currScreenWidth = Math.abs(currLeft - currRight);
                if (currScreenWidth > 180) currScreenWidth -= 180;
                currScreenHeight = Math.abs(currTop - currBottom);
                if (currScreenHeight > 180) currScreenHeight -= 180;
                LatLngBounds hotBounds = new LatLngBounds(
                        new LatLng(newPos.latitude-targetWidth*currScreenHeight,
                                newPos.longitude+targetWidth*currScreenWidth),
                        new LatLng(newPos.latitude+targetWidth*currScreenHeight,
                                newPos.longitude-targetWidth*currScreenWidth));
                Boolean hotspotFound = false;
                */

                    profile(Sections.ANIMATEMAP, Profilestates.START);
                    int animateTime = (int) Math.max(1, (uptimeMillis() - Math.max(lastPanTime, lastZoomTime)));
                    //Log.i("animating camera", newPos.toString() + ',' + Float.toString(newZoom)  + " in " + Integer.toString(animateTime) + "ms");
                    if (readyToAnimate) {
                        readyToAnimate = false;
                        //Log.d("animatebytable on ui thread", "launching on ui thread camera move");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                //Log.d("animatebytable on ui thread", "firing camera move " + mMap.getMinZoomLevel());
                                // TODO: track down why minzoomlevel gets set to 4 and the how to switch to lite mode


                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPos, newZoom), animateTime, new GoogleMap.CancelableCallback() {
                                    @Override
                                    public void onFinish() {
                                        profile(Sections.POSTANIMATEMAP, Profilestates.START);
                                        TextView latDisplay = findViewById(R.id.currentLatitude);
                                        latDisplay.setText(getString(R.string.latitude_indicator, cameraPosition.target.latitude));
                                        TextView lonDisplay = findViewById(R.id.currentLongitude);
                                        lonDisplay.setText(getString(R.string.longitude_indicator, cameraPosition.target.longitude));
                                        TextView layerDisplay = findViewById(R.id.currentLayer);
                                        layerDisplay.setText(getString(R.string.layer_indicator, cameraPosition.zoom));
                                        if ( targetRectangle == null) {
                                            PolygonOptions currentTarget = new PolygonOptions()
                                                    .add(new LatLng(cameraPosition.target.latitude - targetWidth * currScreenHeight,
                                                            cameraPosition.target.longitude - targetWidth * currScreenWidth))
                                                    .strokeWidth(2f)
                                                    .strokeColor(Color.LTGRAY)
                                                    .strokeJointType(JointType.ROUND);

                                            targetRectangle = mMap.addPolygon(currentTarget);

                                        }
                                        List<LatLng> targetPoints = new ArrayList<LatLng>();
                                        double targetScreenWidth = currScreenWidth;
                                        if (targetScreenWidth > 180) targetScreenWidth -= 180;
                                        targetPoints.add(new LatLng(cameraPosition.target.latitude - targetWidth * currScreenHeight,
                                                        cameraPosition.target.longitude - targetWidth * targetScreenWidth));
                                        targetPoints.add(new LatLng(cameraPosition.target.latitude + targetWidth * currScreenHeight,
                                                        cameraPosition.target.longitude - targetWidth * targetScreenWidth));
                                        targetPoints.add(new LatLng(cameraPosition.target.latitude + targetWidth * currScreenHeight,
                                                        cameraPosition.target.longitude + targetWidth * targetScreenWidth));
                                        targetPoints.add(new LatLng(cameraPosition.target.latitude - targetWidth * currScreenHeight,
                                                        cameraPosition.target.longitude + targetWidth * targetScreenWidth));
                                        targetRectangle.setPoints(targetPoints);
                                        readyToAnimate = true;
                                        if (!idling) {
                                            //Log.i("animateByTable", "now");
                                            //retriggered=true;
                                            animationHandler.post(animateByTable);
                                        }
                                        profile(Sections.POSTANIMATEMAP, Profilestates.FINISH);
                                    }

                                    @Override
                                    public void onCancel() {
                                        readyToAnimate = true;
                                        Log.w("animateByTable", "hmm animation got canceled");
                                    }
                                });
                            }
                        });
                    } else {
                        Log.d("animatebytable", "wasnt ready to animate");

                    }
                    if (idling) emergeFromIdle();
                    lastInteractionTime = uptimeMillis();
                    blockEndTime = System.nanoTime();
                    profile(Sections.ANIMATEMAP, Profilestates.FINISH);
                } else {
                   // Log.d("animatebytable", "not ready for doanimate");
                    animationHandler.postDelayed(animateByTable, nullAnimationClockTick);

                }
            }
            else {
                //Log.i("animateByTable", "in the future");
                animationHandler.postDelayed(animateByTable, nullAnimationClockTick);

            }

        }
    };

    private void showAHotSpot() {
        hotSpotActive = true;
        int nextSpot = (lastHotSpotShown + 1) % hotspots.size();
        lastHotSpotShown = nextSpot;
        // ToDo: move the camera
        liveHotSpot = hotspots.get(nextSpot);
        ImageHotspot.activate(liveHotSpot);
        //View mapView =  (View) findViewById(R.id.map);
        //mapView.setVisibility(View.INVISIBLE);
        Log.w("hotspot loading ", nextSpot + ":" + liveHotSpot.baseUri.toString());
        liveHotSpot.setImageByLanguage(hotspotLanguage);
        liveHotSpot.open();
    }

    private void closeTheHotSpot() {
        if (liveHotSpot != null) {
            liveHotSpot.close();
            hotSpotActive = false;
            liveHotSpot = null;

            animationHandler.post(animateByTable);
        }
    }

    protected void doIdle() {
        Log.i("Idle", "going into idle");
        idling = true;
        if (hotSpotActive) {
            closeTheHotSpot();
        }
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(idleHome, (float) (zoomer.idleZoom)), animateToHomeMS, null);
            readyToAnimate = true;
        }
        idleMessageTopView.setText(idleMessageTop);
        idleMessageBottomView.setText(idleMessageBottom);
        if (hotSpotActive) {
            // close it
            hotSpotActive = false;
        }
        hotspotLanguage = ImageHotspot.Languages.ENGLISH;
// TODO: add animation rotating text
        // TODO: after long idle save maxZoom data
    }

    protected void emergeFromIdle() {
        if (mMap != null) {
            Log.i("Idle", "emerging from idle");

            idling = false;
            if (hotSpotActive) {
            } // needs to switch to map mode
            animationHandler.post(animateByTable);
            idleMessageTopView.setText("");
            idleMessageBottomView.setText("");
            asyncTaskHandler.postAtTime(idleMonitor, uptimeMillis() + idleCheckTime);
        }
    }

    private String readJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open(hotspotsJSONFile);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
    protected void loadHotspots() {
        try {
            JSONArray jArray = new JSONArray(readJSONFromAsset());
            ImageView hotSpotImageView = (ImageView) findViewById(R.id.hotSpotImageView);
            //String urlPrefix = "https://botherconsulting.com/GlobalMagic/";
            String urlPrefix = "www/GlobalMagic/";
            for (int i = 0; i < jArray.length(); ++i) {
                String title = jArray.getJSONObject(i).getString("title");// name of the country
                Double latitude = jArray.getJSONObject(i).getDouble("latitude"); // dial code of the country
                Double longitude = jArray.getJSONObject(i).getDouble("longitude"); // code of the country
                Double maxZoom = jArray.getJSONObject(i).getDouble("maxZoom"); // code of the country
                Double minZoom = jArray.getJSONObject(i).getDouble("minZoom"); // code of the country
                hotspots.add(i, new ImageHotspot(mMap, hotSpotImageView, this.getApplicationContext(), MapsActivity.this));
                //hotspots.get(i).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                hotspots.get(i).setIcon(BitmapDescriptorFactory.fromAsset("www/GlobalMagic/globe32x32.png"));
                hotspots.get(i).setPosition(new LatLng(latitude,longitude));
                hotspots.get(i).setBaseName(urlPrefix, title);
                hotspots.get(i).setZoomRange(minZoom, maxZoom);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
    protected void eatHotspots() {
        PreferenceManager.setDefaultValues(this, R.xml.hotspoteditfields, true);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        /*
        get hotspots array
        kill the old array
        parse the new array
         */
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
            if (bws != null) bws.cancel(true);
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

        panner.configure(Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TILT_SENSOR_SCALE_FACTOR,
                            Double.toString(panner.TiltScaleX))),
                    Double.valueOf(sharedPref.getString(SettingsActivity.KEY_PREF_TILT_SENSOR_SCALE_FACTOR,
                            Double.toString(panner.TiltScaleY))),
                    idleHome,
                maxPanPercent);

        useHybridMap = sharedPref.getBoolean(SettingsActivity.KEY_PREF_USE_HYBRID_MAP, useHybridMap);
        if (mMap != null) {
            if (useHybridMap) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                mMap.setMinZoomPreference(0.0f);
                mMap.setMaxZoomPreference(22.0f);
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
            case EAT_HOTSPOTS:
                eatHotspots();
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_E:
                hotspotLanguage = ImageHotspot.Languages.ENGLISH;
                break;
            case KeyEvent.KEYCODE_S:
                hotspotLanguage = ImageHotspot.Languages.SPANISH;
                break;
            case KeyEvent.KEYCODE_K:
                hotspotLanguage = ImageHotspot.Languages.KOREAN;
                break;
            case KeyEvent.KEYCODE_J:
                hotspotLanguage = ImageHotspot.Languages.JAPANESE;
                break;
            case KeyEvent.KEYCODE_C:
                hotspotLanguage = ImageHotspot.Languages.CHINESE;
                break;
            case KeyEvent.KEYCODE_H:
                showAHotSpot();
                break;
            case KeyEvent.KEYCODE_L:
                closeTheHotSpot();
                break;
            default:
                return super.onKeyUp(keyCode, event);
        }
        signalWebSocket(hotspotLanguage);
        ImageHotspot.setLanguage(hotspotLanguage);
        return true;
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

         DownloadKmlFile(String url) {
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
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) mWebSocketClient.close();
        if (bws==null) bws = new BackgroundWebSocket();
        if (bws.getStatus()== AsyncTask.Status.RUNNING) return;
        if (bws.getStatus()== AsyncTask.Status.FINISHED) {
            BackgroundWebSocket deadBWS  =  bws;
            deadBWS.cancel(true);
            bws = new BackgroundWebSocket();
        }
       Log.i("starting websocket", sensorServerAddress +":"+sensorServerPort);
       bws.execute("ws://"+ sensorServerAddress + ":" + sensorServerPort);

    }

    private void signalWebSocket(ImageHotspot.Languages language) {
        if (mWebSocketClient != null && mWebSocketClient.isOpen()){
            mWebSocketClient.send("{language: " + language.toString().substring(0,1) +"}");
        }

    }

    private  void bwsComplain(String... message) {
        Toast.makeText(MapsActivity.this, message[0] + ":" + message[1], Toast.LENGTH_LONG).show();
        if (message[0].equals("Connection closed")) asyncTaskHandler.postDelayed(sensorConnectionLauncher,15000);
    }


       /*animateByTable();

       TextView latDisplay  = findViewById(R.id.currentLatitude);
       latDisplay.setText("Latitude: "+ String.format ("%.2f", mMap.getCameraPosition().target.latitude));
       TextView lonDisplay  = findViewById(R.id.currentLongitude);
       lonDisplay.setText("Longitude: "+ String.format ("%.2f", mMap.getCameraPosition().target.longitude));
       */
       //TextView altDisplay  = (TextView)findViewById(R.id.currentAltitude);
       //latDisplay.setText("Altitude: "+ Double.toString(mMap.getCameraPosition().target.));


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
            mWebSocketClient = new WebSocketClient(uri, new org.java_websocket.drafts.Draft_6455()) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i("Websocket", "Opened");
                    mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
                }

                @Override
                public void onMessage(String message) {

                    bwsHandleMessage(message);
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

                    Log.e("Websocket", "Error " + e.getMessage());
                    publishProgress("Connection Error", e.getMessage());

                }
            };
            mWebSocketClient.connect();
            return null;
        }

        protected void onProgressUpdate(String... progress) {

            //update the progress
            if (progress.length >= 2) {
                if (progress[0].equals("message")) {
                    bwsHandleMessage(progress[1]);
                } else {
                    //onMessage(progress[0]);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            bwsComplain(progress);
                        }
                    });
                }
            }

        }

        //this will call after finishing the doInBackground function
        protected void onPostExecute(String result) {

            // Update the ui elements
            //show some notification
            //showDialog("Task done " + result);

        }


        private void bwsHandleMessage(String messageString) {

            if (mMap == null) return;


            JSONObject message;
            try {
                message = new JSONObject(messageString);
            } catch (org.json.JSONException e) {
                Log.i("odd JSON", messageString);
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
                Log.i("no gesture message", message.toString());
                return;
            }

            //LatLngBounds curScreen = mMap.getProjection()
            //       .getVisibleRegion().latLngBounds;

            if (!hotSpotActive) {
                getUIObjects();
                if (gestureType.equals("pan")) {
                    panner.setMessage(message);
                    panner.setMap(mMap);
                    panner.setLogging(logSensors || logTilt);
                    panner.setScreenBounds(currScreenWidth, currScreenHeight);
                    panner.setMapPosition(cameraPosition);
                    panner.handleJSON.run();

                    //paintTarget();
                } else if (gestureType.equals("zoom")) {
                    zoomer.setMessage(message);
                    zoomer.setLogging(logSensors || logZoom);
                    zoomer.handleJSON.run();
                }

            } else {
                //liveHotSpot.setImageByLanguage(hotspotLanguage);
                if (gestureType.equals("pan")) {
                    liveHotSpot.setMessage(message);
                    liveHotSpot.setLogging(logSensors || logTilt);
                    asyncTaskHandler.post(liveHotSpot.handleJSON);
                } else if (gestureType.equals("zoom")) {
                    liveHotSpot.setMessage(message);
                    liveHotSpot.setLogging(logSensors || logZoom);
                    asyncTaskHandler.post(liveHotSpot.handleJSON);
                } else if (gestureType.equals("switchCode")) {
                    liveHotSpot.setMessage(message);
                    liveHotSpot.setLogging(logSensors);
                    asyncTaskHandler.post(liveHotSpot.handleJSON);
                } else {
                    return;
                }
                // liveHotSpot.handleJSON.run();

            }

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
        WebView.setWebContentsDebuggingEnabled(true);
        mMap = googleMap;
        if (useHybridMap) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        mMap.setOnCameraMoveListener(this);
        mMap.setOnCameraIdleListener(this);
        //mMap.setOnCameraChangeListener(this);
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
       // mMap.addMarker(new MarkerOptions().position(idleHome).title(idleTitle).icon(BitmapDescriptorFactory.fromResource(R.drawable.clark_planetarium_logo_50)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(idleHome,(float) zoomer.idleZoom));
        LatLngBounds curScreen = mMap.getProjection()
                .getVisibleRegion().latLngBounds;
        //LatLng radius = new LatLng(curScreen.northeast.latitude, curScreen.getCenter().longitude);
        //Log.i("mask radius", radius.toString());
        //mMap.addPolygon(MapMask.createPolygonWithCircle(this, sydney, radius));
        //mMap.addPolygon(MapMask.createPolygonWithCircle(this, sydney, 100));
        //Intent i = new Intent(this,TablePreferencesActivity.class);
        //startActivityForResult(i, SHOW_PREFERENCES);

        //WebView hotSpotWebView = (WebView) findViewById(R.id.hotSpotWebView);
        loadHotspots();
        emergeFromIdle();
    }



}
