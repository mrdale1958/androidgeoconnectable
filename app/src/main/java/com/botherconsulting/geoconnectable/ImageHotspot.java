package com.botherconsulting.geoconnectable;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.media.AudioAttributes;
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
*/  static int zoomOutTime = 2000; // milliseconds
    static int zoomInTime = 1000; // milliseconds
    private States state;
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
    private Uri imageUri, soundUri;



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
        this.minSpin = -20;
        this.maxSpin = 2000;
/*        this.marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(0.0,0.0))
                .title("some pithy name")
                .snippet("Even pithier label")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
*/
    }

    public void setBaseName(String location, String name) {
        this.baseUri = Uri.parse(location+'/'+name+'/'+name+'_');
        this.setTitle(name);
    }


    public void setImageByLanguage(Languages language) {
        Uri imageUri, soundUri;
        this.mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes
                        .Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());

        switch (language) {
            case KOREAN:
                imageUri = Uri.withAppendedPath(this.baseUri, "Korean.png");
                soundUri = Uri.withAppendedPath(this.baseUri, "Korean.m4a");
                break;
            case CHINESE:
                imageUri = Uri.withAppendedPath(this.baseUri, "Chinese.png");
                soundUri = Uri.withAppendedPath(this.baseUri, "Chinese.m4a");
                break;
            case JAPANESE:
                imageUri = Uri.withAppendedPath(this.baseUri, "Japanese.png");
                soundUri = Uri.withAppendedPath(this.baseUri, "Japanese.m4a");
                break;
            case SPANISH:
                imageUri = Uri.withAppendedPath(this.baseUri, "Spanish.png");
                soundUri = Uri.withAppendedPath(this.baseUri, "Spanish.m4a");
                break;
            case ENGLISH:
            default:
                imageUri = Uri.withAppendedPath(this.baseUri, "English.png");
                soundUri = Uri.withAppendedPath(this.baseUri, "English.m4a");
                break;

        }
        this.displaySurface.setImageURI(imageUri);
        // if open playAudio();
    }

    public void open() {
        if (state != States.CLOSED) return;
        currentSpinPosition = 0;
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(this.displaySurface, "scaleX", 1.0f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(this.displaySurface, "scaleY", 1.0f);
        scaleUpX.setDuration(zoomOutTime);
        scaleUpY.setDuration(zoomOutTime);

        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.play(scaleUpX).with(scaleUpY);

        scaleUp.start();
        state = States.OPENING;

        this.displaySurface.postDelayed(new Runnable() {

            @Override
            public void run() {
                state = States.OPEN;
                playAudio();
            }

        }, zoomOutTime + 500); // 500ms delay after zoom complete
    }

    public void close() {
        if (state != States.OPEN) return;
        state = States.CLOSING;
        stopAudio();
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(this.displaySurface, "scaleX", 1.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(this.displaySurface, "scaleY", 1.0f);
        scaleDownX.setDuration(zoomInTime);
        scaleDownY.setDuration(zoomInTime);

        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.play(scaleDownX).with(scaleDownY);

        scaleDown.start();
        this.displaySurface.postDelayed(new Runnable() {

            @Override
            public void run() {
                state = States.CLOSED;
            }

        }, zoomInTime + 500); // 500ms delay after zoom complete

    }

    public void playAudio(){
        try {
            // if audio playing stopit
            this.mediaPlayer.setDataSource(this.context, soundUri);
            this.mediaPlayer.prepare();
            this.mediaPlayer.start();
        }
        catch (java.io.IOException e) {
            Log.e("Hotspot audio", e.getMessage());
        }
    }

    public void stopAudio() {
        this.mediaPlayer.stop();

    }

    public boolean isClosed() {
        return(state == States.CLOSED);
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
    public Boolean handleJSON(JSONObject message, GoogleMap mMap, boolean doLog) {
        String gestureType;
        try {
            gestureType = message.getString("gesture");
            //Log.i("incoming message",message.toString());
        } catch (org.json.JSONException e) {
            Log.i("GCT HS: no gesture msg", message.toString());
            return false;
        }
        double deltaX = 0.0;

        double deltaY = 0.0;
        deltaZ = 0;
        if (gestureType.equals("switch")) {
            String keyCode;
            JSONObject switchObj = new JSONObject();
            try {
                switchObj = message.getJSONObject("switch");
            } catch (org.json.JSONException e) {
                Log.e("GCT HS error: switch msg", "no switch " + message.toString());
                return false;
            }
            try {
                keyCode = switchObj.getString("switchCode");
            } catch (org.json.JSONException e) {
                Log.e("GCT HS error: switch msg", "invalid switch " + switchObj.toString());
                return false;
            }
            switch (keyCode) {
                case "e":
                    this.setImageByLanguage(ImageHotspot.Languages.ENGLISH);
                    return true;
                case "s":
                    this.setImageByLanguage(ImageHotspot.Languages.SPANISH);
                    return true;
                case "k":
                    this.setImageByLanguage(ImageHotspot.Languages.KOREAN);
                    return true;
                case "j":
                    this.setImageByLanguage(ImageHotspot.Languages.JAPANESE);
                    return true;
                case "c":
                    this.setImageByLanguage(ImageHotspot.Languages.CHINESE);
                    return true;
            }
        } else if (gestureType.equals("zoom")) {

            deltaZ = 0;
            JSONObject vector = new JSONObject();
            try {
                vector = message.getJSONObject("vector");
            } catch (org.json.JSONException e) {
                Log.e("GCT HS error: zoom msg", "no vector " + message.toString());
                return false;
            }
            try {
                deltaZ = vector.getInt("delta");
                int messageID = message.getInt("id");
                //if (messageID > lastMessageID + 1)
                //    Log.w("reading zoom data","got" + Integer.toString(messageID) + " after" + Integer.toString(lastMessageID));
                lastZoomMessageID = messageID;
            } catch (org.json.JSONException e) {
                Log.e("GCT HS error: zoom msg", "invalid vector " + vector.toString());
                return false;
            }
            currentSpinPosition += deltaZ;
            if (currentSpinPosition > maxSpin || currentSpinPosition < minSpin) {
                this.close();
            }
        }
        return true;
    }
}
