package com.botherconsulting.geoconnectable;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
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
    private MapsActivity mapsActivity;

    public enum Languages {
        ENGLISH,
        CHINESE,
        JAPANESE,
        KOREAN,
        SPANISH
    }

    static Languages language = Languages.ENGLISH;
    static ImageHotspot activeHotSpot;
    static ImageHotspot lastHotSpot;

    static final void setLanguage(Languages _language) {
        ImageHotspot.language = _language;
        if (ImageHotspot.activeHotSpot != null) {
            ImageHotspot.activeHotSpot.setImageByLanguage(ImageHotspot.language);
        }
    }


    static final boolean activate(ImageHotspot hs) {
        if (ImageHotspot.lastHotSpot != hs) {
            ImageHotspot.activeHotSpot = hs;
            return true;
        }
        return false;
    }
    static final void deactivate() {
        //TODO: does this need to close() current hotspot?
        ImageHotspot.lastHotSpot = ImageHotspot.activeHotSpot;
        ImageHotspot.activeHotSpot = null;

    }


    public ImageHotspot(GoogleMap map, ImageView imageView, Context context, MapsActivity mapsActivity) {
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
        this.mapsActivity = mapsActivity;

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

                mapsActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        displaySurface.setImageDrawable(crossfader);
                        crossfader.setCrossFadeEnabled(true);

                        crossfader.startTransition(300);
                    }
                });
            } else {
                mapsActivity.runOnUiThread(new Runnable() {
                    public void run() {displaySurface.setImageDrawable(d);}});

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
        ValueAnimator scaleUp = ValueAnimator.ofFloat(0.0f,1.0f);
        scaleUp.addUpdateListener(
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float newScale = (float) animation.getAnimatedValue();
                    displaySurface.setScaleX(newScale);
                    displaySurface.setScaleY(newScale);
                    if (newScale >= 1.0f) {
                        state = States.OPEN;
                        //stopAudio();
                        playAudio();
                    }

                }
        });


        scaleUp.setDuration(zoomOutTime);

        scaleUp.start();
        state = States.OPENING;

     }

    public void close() {
        if (state != States.OPEN) return;
        state = States.CLOSING;
        stopAudio();
        ValueAnimator scaleDown = ValueAnimator.ofFloat(1.0f,0.0f);
        scaleDown.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float newScale = (float) animation.getAnimatedValue();
                        displaySurface.setScaleX(newScale);
                        displaySurface.setScaleY(newScale);
                        if (newScale <= 0.0f) {
                            state = States.CLOSED;
                            ImageHotspot.deactivate();
                            /*displaySurface.setScaleX(0.0f);
                            displaySurface.setScaleY(0.0f);
                            displaySurface.setImageDrawable(null);*/
                        }

                    }
                });


        scaleDown.setDuration(zoomInTime);

        scaleDown.start();
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
                    ImageHotspot.mediaPlayer.setVolume(1f, 1f);
                    ImageHotspot.mediaPlayer.setLooping(false);
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
            Log.d("audio start", "media being prepared");
            ImageHotspot.mediaPlayer.prepareAsync();
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
        mediaPlayer.pause();
        mediaPlayer.stop();
        mediaPlayer.reset(); //TODO: Why can't we sit in this state instead of killing the player
        mediaPlayer.release();
        mediaPlayer=null;
        Log.d("audio stop", "mediaplayer  killed");
    }
    public boolean isClosed() {
        return(state == States.CLOSED);
    }

    public void manageState() {
       /* switch (state) {
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

        } */
    }
    private JSONObject message;
    private boolean doLog;
    private boolean running = false;

    public void setMessage(JSONObject _message) {
        if (! running) this.message = _message;
    }

    public void setMap(GoogleMap _mMap) {
        this.mMap = _mMap;
    }

    public void setLogging(boolean _doLog) {
        this.doLog = _doLog;
    }

    public final Runnable  handleJSON  = new Runnable() {


        public void run() {
            if (! running)
                running = true;
            else
                return;

        //public Boolean handleJSON(JSONObject message, GoogleMap mMap, boolean doLog) {
            String gestureType;
            try {
                gestureType = message.getString("gesture");
                //Log.i("incoming message",message.toString());
            } catch (org.json.JSONException e) {
                Log.i("GCT HS: no gesture msg", message.toString());
                running = false;
                return ;
            }
            double deltaX = 0.0;

            double deltaY = 0.0;
            deltaZ = 0;
            if (gestureType.equals("switchCode")) {
                String keyCode;
                JSONObject switchObj = new JSONObject();
                try {
                    switchObj = message.getJSONObject("vector");
                } catch (org.json.JSONException e) {
                    Log.e("GCT HS error: switch msg", "no switch " + message.toString());
                    running = false;
                    return ;
                }
                try {
                    keyCode = switchObj.getString("code");
                } catch (org.json.JSONException e) {
                    Log.e("GCT HS error: switch msg", "invalid switch " + switchObj.toString());
                    running = false;
                    return ;
                }

                switch (keyCode) {
                    case "e":
                        ImageHotspot.setLanguage(ImageHotspot.Languages.ENGLISH);
                        break;
                    case "s":
                        ImageHotspot.setLanguage(ImageHotspot.Languages.SPANISH);
                        break ;
                    case "k":
                        ImageHotspot.setLanguage(ImageHotspot.Languages.KOREAN);
                        break ;
                    case "j":
                        ImageHotspot.setLanguage(ImageHotspot.Languages.JAPANESE);
                        break ;
                    case "c":
                        ImageHotspot.setLanguage(ImageHotspot.Languages.CHINESE);
                        break ;
                }
                running = false;
                return ;
            } else if (gestureType.equals("zoom")) {

                deltaZ = 0;
                JSONObject vector = new JSONObject();
                try {
                    vector = message.getJSONObject("vector");
                } catch (org.json.JSONException e) {
                    Log.e("GCT HS error: zoom msg", "no vector " + message.toString());
                    running = false;
                    return ;
                }
                try {
                    // need to cope with different zoom logic so negate the value
                    deltaZ = -vector.getInt("delta");
    //                int messageID = message.getInt("id");
                    //if (messageID > lastMessageID + 1)
                    //    Log.w("reading zoom data","got" + Integer.toString(messageID) + " after" + Integer.toString(lastMessageID));
     //               lastZoomMessageID = messageID;
                } catch (org.json.JSONException e) {
                    Log.e("GCT HS error: zoom msg", "invalid vector " + vector.toString() + " for type " + gestureType);
                    running = false;
                    return ;
                }
                currentSpinPosition += deltaZ;
                if (currentSpinPosition > maxSpin || currentSpinPosition < minSpin) {
                    if (ImageHotspot.activeHotSpot != null)
                        ImageHotspot.activeHotSpot.close();
                }
            }
            running = false;
        }
    };

}
