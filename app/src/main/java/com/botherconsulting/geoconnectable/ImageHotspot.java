package com.botherconsulting.geoconnectable;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONObject;

/**
 * Created by dwm160130 on 3/22/18.
 */

public class ImageHotspot extends Hotspot {
    // tilt stuff
/*    public double TiltScaleX = 0.04; // in settings
    public double TiltScaleY = 0.04; // in settings
    private double panMax = 0.01;
    private int minSpin = -100;
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
    private int clicksPerZoomLevel = 1000;
    private int idleSpin = 0;
    public double idleZoom = 13.5; // in settings
    public double zoom = 0d;
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
    public Double[] hotSpotZoomTriggerRange = {15.0, 19.0};
    public Double[] currentTilt = {0.0, 0.0};
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
*/    private States state;
    long lastTiltMessageTime = System.nanoTime();
    int eventTiltCount = 0;
    long[] eventTiltWindow = new long[eventTiltWindowLength];
    long sumTiltElapsedTimes = 0;
    long sumTiltSquaredElapsedTimes = 0;
    int lastTiltMessageID = 0;
    int currentSpinPosition = 0;
    int deltaZ = 0;
    long lastZoomMessageTime = System.nanoTime();
    int eventZoomCount = 0;
    long[] eventZoomWindow = new long[eventZoomWindowLength];
    long sumZoomElapsedTimes = 0;
    long sumZoomSquaredElapsedTimes = 0;
    int lastZoomMessageID = 0;
    GoogleMap mMap;

    Uri baseUri;

    private ImageView displaySurface;
    private Context context;
    private MediaPlayer mediaPlayer;


    public enum Languages {
        ENGLISH,
        CHINESE,
        JAPANESE,
        KOREAN,
        SPANISH
    }


    public ImageHotspot(GoogleMap map, ImageView imageView, Context context) {
        super(map);
        state = States.CLOSED;
        this.displaySurface = imageView;
        this.enabled = false;
        this.set = "default";
        this.marker=null;
        this.mMap = map;
        this.context = context;
/*        this.marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(0.0,0.0))
                .title("some pithy name")
                .snippet("Even pithier label")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
*/
    }

    public void setBaseName(String location, String name) {
        this.baseUri = Uri.parse(location+'/'+name);
        this.setTitle(name);
    }


    public void setImageByLanguage(Languages language) {
        Uri imageUri, soundUri;
        this.mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        switch (language) {
            case KOREAN:
                imageUri = Uri.withAppendedPath(this.baseUri, "-Korean.png");
                soundUri = Uri.withAppendedPath(this.baseUri, "-Korean.m4a");
                break;
            case CHINESE:
                imageUri = Uri.withAppendedPath(this.baseUri, "-Chinese.png");
                soundUri = Uri.withAppendedPath(this.baseUri, "-Chinese.m4a");
                break;
            case JAPANESE:
                imageUri = Uri.withAppendedPath(this.baseUri, "-Japanese.png");
                soundUri = Uri.withAppendedPath(this.baseUri, "-Japanese.m4a");
                break;
            case SPANISH:
                imageUri = Uri.withAppendedPath(this.baseUri, "-Spanish.png");
                soundUri = Uri.withAppendedPath(this.baseUri, "-Spanish.m4a");
                break;
            case ENGLISH:
            default:
                imageUri = Uri.withAppendedPath(this.baseUri, "-English.png");
                soundUri = Uri.withAppendedPath(this.baseUri, "-English.m4a");
                break;

        }
        this.displaySurface.setImageURI(imageUri);
        try {
            this.mediaPlayer.setDataSource(this.context, soundUri);
            this.mediaPlayer.prepare();
            this.mediaPlayer.start();
        }
        catch (java.io.IOException e) {
            Log.e("Hotspot audio", e.getMessage());
        }
    }

    public void manageState() {
        switch (state) {
            case CLOSED:
                break;
            case OPEN:
                break;
            case OPENING:
                this.setImageByLanguage(Languages.ENGLISH);
                state = States.OPEN;
                break;
            case CLOSING:
                this.mediaPlayer.release();
                state = States.CLOSED;
                break;

        }
    }

@Override
    public Boolean handleJSON(JSONObject message, GoogleMap mMap, boolean doLog)
        {
            String gestureType;
            try {
                gestureType = message.getString("gesture");
                //Log.i("incoming message",message.toString());
            } catch (org.json.JSONException e) {
                Log.i("GCT HS: no gesture msg",message.toString());
                return false;
            }
            double deltaX = 0.0;

            double deltaY = 0.0;
            deltaZ = 0;
            if (gestureType.equals("pan")) {
                JSONObject vector = new JSONObject();
                try {
                    vector = message.getJSONObject("vector");
                } catch (org.json.JSONException e) {
                    Log.e("GCT HS: reading pan msg", "no vector " + message.toString());
                }
                try {
                    //double screenWidthDegrees = Math.abs(curScreen.southwest.longitude - curScreen.northeast.longitude);
                    //double screenHeightDegrees = Math.abs(curScreen.southwest.latitude - curScreen.northeast.latitude);
                    double rawX = vector.getDouble("x");
                    double percentChangeInX = 0;
                    double percentChangeInY = 0;
                    double rawY = vector.getDouble("y");

                    if (doLog) {
                        Log.i("GCT HS : pan update", " raw x: " + Double.toString(rawX) +
                                " raw y: " + Double.toString(rawY));
                    }
                    if (Math.abs(rawX) < validTiltThreshold && Math.abs(rawY) < validTiltThreshold) {
                        if (doLog) {
                            Log.d("GCT HS: rejected pan", " raw x: " + Double.toString(rawX) +
                                    " raw y: " + Double.toString(rawY) + " validTiltThreshold: " + Double.toString(validTiltThreshold));
                        }
                        return false;
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
                        Log.i("GCT HS: pan hs update", " deltax: " + Double.toString(deltaX) +
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
                    Log.e("GCT HS error: pan msg", "invalid vector " + vector.toString());
                }
            }
            else
            {
                deltaZ = 0;
                JSONObject vector = new JSONObject();
                try {
                    vector = message.getJSONObject("vector");
                } catch (org.json.JSONException e) {
                    Log.e("GCT HS error: zoom msg", "no vector " + message.toString());
                }
                try {
                    deltaZ = vector.getInt("delta");
                    int messageID = message.getInt("id");
                    //if (messageID > lastMessageID + 1)
                    //    Log.w("reading zoom data","got" + Integer.toString(messageID) + " after" + Integer.toString(lastMessageID));
                    lastZoomMessageID = messageID;
                } catch (org.json.JSONException e) {
                    Log.e("GCT HS error: zoom msg", "invalid vector " + vector.toString());
                }
                currentSpinPosition += deltaZ;
                currentSpinPosition = Math.max(minSpin,Math.min(currentSpinPosition,maxSpin));

                double proposedZoom = idleZoom + (double)currentSpinPosition / (double)clicksPerZoomLevel;
                if (doLog) {
                    Log.i("GCT HS: zoom update", "delta:" + Integer.toString(deltaZ) +
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
 /*           if (newData)
            {
                displaySurface.setVisibility(View.VISIBLE);
                String updateURL = "javascript://table.update(" + currentTilt + " , " + currentZoom + ")";
                if (doLog) {
                    Log.i("GCT HotSpot: notify webView" , updateURL);
                }
                displaySurface.loadUrl(updateURL);
                return true;
            }
*/
            return false;
        }
}
