package com.botherconsulting.geoconnectable;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static android.os.SystemClock.uptimeMillis;

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
    static int zoomInTime = 2000; // milliseconds
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
    static MediaPlayer mediaPlayer = null;
    private boolean serviceBound = false;
    private Uri imageUri, soundUri;
    private int resumePosition;
    private ArrayList<String> audioList;
    private static Uri lastLanguage = null;
    private Handler asyncTaskHandler;

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
        this.displaySurface.setScaleX(0.0f);
        this.displaySurface.setScaleY(0.0f);
        this.enabled = false;
        this.set = "default";
        this.marker=null;
        this.mMap = map;
        this.context = context;
        this.minSpin = -20;
        this.maxSpin = 2000;
        this.asyncTaskHandler = new Handler();

/*        this.marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(0.0,0.0))
                .title("some pithy name")
                .snippet("Even pithier label")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
*/
    }



//    private void updateAudioSource(String mediaFile) {
//        try {
//            // Set the data source to the mediaFile location
//            mediaPlayer.setDataSource(mediaFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//            stopMedia();
//        }
//    }



    public void setBaseName(String location, String name) {
        this.baseUri = Uri.parse(location+name+'/');
        this.setTitle(name);
        this.soundUri = Uri.withAppendedPath(this.baseUri, this.getTitle()+'_'+"English.m4a");

    }


    public void setImageByLanguage(Languages language) {
        Uri imageUri, soundUri;

        switch (language) {
            case KOREAN:
                imageUri = Uri.withAppendedPath(this.baseUri, this.getTitle()+'_'+"Korean.png");
                soundUri = Uri.withAppendedPath(this.baseUri, this.getTitle()+'_'+"Korean.m4a");
                break;
            case CHINESE:
                imageUri = Uri.withAppendedPath(this.baseUri, this.getTitle()+'_'+"Chinese.png");
                soundUri = Uri.withAppendedPath(this.baseUri, this.getTitle()+'_'+"Chinese.m4a");
                break;
            case JAPANESE:
                imageUri = Uri.withAppendedPath(this.baseUri, this.getTitle()+'_'+"Japanese.png");
                soundUri = Uri.withAppendedPath(this.baseUri, this.getTitle()+'_'+"Japanese.m4a");
                break;
            case SPANISH:
                imageUri = Uri.withAppendedPath(this.baseUri, this.getTitle()+'_'+"Spanish.png");
                soundUri = Uri.withAppendedPath(this.baseUri, this.getTitle()+'_'+"Spanish.m4a");
                break;
            case ENGLISH:
            default:
                imageUri = Uri.withAppendedPath(this.baseUri, this.getTitle()+'_'+"English.png");
                soundUri = Uri.withAppendedPath(this.baseUri, this.getTitle()+'_'+"English.m4a");
                break;

        }
        try {

            // get input stream
            String path = imageUri.getPath();
            InputStream ims = this.displaySurface.getContext().getAssets().open(path);

            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            ims.close();
            // set image to ImageView
            Drawable backgrounds[] = new Drawable[2];
            backgrounds[0] =  this.displaySurface.getDrawable();
            if (backgrounds[0] != null) {
                path = ImageHotspot.lastLanguage.getPath();
                ims  = this.displaySurface.getContext().getAssets().open(path);
                backgrounds[0] = Drawable.createFromStream(ims, null);
                ims.close();
                backgrounds[1] = d;

                TransitionDrawable crossfader = new TransitionDrawable(backgrounds);

                this.displaySurface.setImageDrawable(crossfader);
                crossfader.setCrossFadeEnabled(true);

                crossfader.startTransition(300);
            } else {
                this.displaySurface.setImageDrawable(d);

            }
        }
        catch(IOException ex) {

            Log.e("I/O ERROR","Failed when ...");
        }       //this.displaySurface.setImageURI(imageUri);
       // Bitmap bmImg = BitmapFactory.decodeFile(imageUri.getEncodedPath());
       // this.displaySurface.setImageBitmap(bmImg);
        this.soundUri = soundUri;
        ImageHotspot.lastLanguage = imageUri;
        if (state == States.OPEN) {
            playAudio();
        }
    }

    public void open() {
        if (state != States.CLOSED) return;
        currentSpinPosition = 0;
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(this.displaySurface, "scaleX", 1.0f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(this.displaySurface, "scaleY", 1.0f);
        scaleUpX.setDuration(zoomOutTime);
        scaleUpX.setAutoCancel(true);
        scaleUpY.setDuration(zoomOutTime);
        scaleUpY.setAutoCancel(true);

        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.play(scaleUpX).with(scaleUpY);

        scaleUp.start();
        state = States.OPENING;

        this.asyncTaskHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                android.os.Debug.waitForDebugger();
                state = States.OPEN;
                //stopAudio();
                playAudio();
            }

        }, zoomOutTime + 500); // 500ms delay after zoom complete
    }

    public void close() {
        if (state != States.OPEN) return;
        state = States.CLOSING;
        //stopAudio();
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(this.displaySurface, "scaleX", 0.1f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(this.displaySurface, "scaleY", 0.1f);
        scaleDownX.setDuration(zoomInTime);
        scaleDownX.setAutoCancel(true);
        scaleDownY.setDuration(zoomInTime);
        scaleDownY.setAutoCancel(true);

        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.play(scaleDownX).with(scaleDownY);

        scaleDown.start();
        this.asyncTaskHandler.postAtTime(new Runnable() {

            @Override
            public void run() {
                android.os.Debug.waitForDebugger();
                state = States.CLOSED;
                displaySurface.setScaleX(0.0f);
                displaySurface.setScaleY(0.0f);
                displaySurface.setImageDrawable(null);
            }

        }, uptimeMillis()  + 5000); // 500ms delay after zoom complete
        //this.mediaPlayer.release();
    }

    public void playAudio(){
        //try {
        if (ImageHotspot.mediaPlayer != null) // && ImageHotspot.mediaPlayer.isPlaying())
            stopAudio();
        if (ImageHotspot.mediaPlayer == null)
        {
            ImageHotspot.mediaPlayer = new MediaPlayer();

            ImageHotspot.mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp1) {
                    android.os.Debug.waitForDebugger();
                    ImageHotspot.mediaPlayer.start();
                    Log.d("audio start", "mediaplayer  started");
                }
            });

            ImageHotspot.mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    Log.d("audio complete", "media play over");
                }
            });


        }
        try {
            AssetFileDescriptor descriptor = context.getAssets().openFd(this.soundUri.toString());
            ImageHotspot.mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            ImageHotspot.mediaPlayer.prepare();
            ImageHotspot.mediaPlayer.setVolume(1f, 1f);
            ImageHotspot.mediaPlayer.setLooping(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //mediaPlayer.start();
        //}
        //catch (java.io.IOException e) {
        //    Log.e("Hotspot audio", e.getMessage());
        //}
    }

    public void stopAudio() {
        this.mediaPlayer.pause();
        this.mediaPlayer.stop();
        this.mediaPlayer.reset();
        this.mediaPlayer.release();
        this.mediaPlayer=null;
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
                //this.mediaPlayer.release();
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
                // need to cope with different zoom logic so negate the value
                deltaZ = -vector.getInt("delta");
//                int messageID = message.getInt("id");
                //if (messageID > lastMessageID + 1)
                //    Log.w("reading zoom data","got" + Integer.toString(messageID) + " after" + Integer.toString(lastMessageID));
 //               lastZoomMessageID = messageID;
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
