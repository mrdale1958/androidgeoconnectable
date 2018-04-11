package com.botherconsulting.geoconnectable;

import android.util.Log;
import android.webkit.WebView;

import com.google.android.gms.maps.GoogleMap;

/**
 * Created by dalemacdonald on 4/6/18.
 */

public class MapZoomer extends ZoomLens {
    private static final int GESTURE_TIME = 1;
    protected GoogleMap zoomObject;
    static final int ZOOM = 0;
    private int[][] maxZoomCache;
    WebView webView;
    public MapZoomer (GoogleMap _zoomObject,
                      int[][]                  _maxZoomCache,
                      WebView _webView,
                      int _clicksPerRev,
                      int _revsPerFullZoom,
                      double _maxZoom,
                      double _minZoom,
                      double _idleZoom) {
        maxZoomCache = _maxZoomCache;
        zoomObject = _zoomObject;
        webView = _webView;
    }

    public void doZoom(int delta, boolean doLog) {
        currentSpinPosition += delta;
        currentSpinPosition = Math.max(minSpin, Math.min(currentSpinPosition, maxSpin));

        double proposedZoom = idleZoom + (double) currentSpinPosition / (double) clicksPerZoomLevel;
        if (doLog) {
            Log.i("zoom update", "delta:" + Integer.toString(delta) +
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
        updateStats(delta);

        if (proposedZoom != currentZoom) {
            zoom = currentZoom;
            currentZoom = proposedZoom;
            newData = true;

        }

    }

    public boolean needToAnimate() {
        Object[] zoomData = this.getCurrentZoom();
        float newZoom = (float) zoomData[ZOOM];
        long lastZoomTime = (long) zoomData[GESTURE_TIME];
        int latIndex = (int) Math.round(zoomObject.getCameraPosition().target.latitude) + 90;
        int lonIndex = (int) Math.round(zoomObject.getCameraPosition().target.longitude) + 180;
        if (this.maxZoomCache[latIndex][lonIndex] > 0) {
            this.setZoomBounds(this.minZoom, maxZoomCache[latIndex][lonIndex]);
        } else if(Math.floor(newZoom) > zoomObject.getCameraPosition().zoom)  {
            checkMaxZoom( newZoom);
        }
    }

    public void checkMaxZoom(float newZoom) {
        Log.w("zoom checking", Double.toString(Math.floor(newZoom)) +" > " + Float.toString(zoomObject.getCameraPosition().zoom) +
                "\n reported minZoom: " + zoomObject.getMinZoomLevel() + " max: " + zoomObject.getMaxZoomLevel());

        //String mzsURL = "http://192.168.1.64/mzs.html?"+
        String mzsURL = "file:///android_asset/www/index.html?"+
                zoomObject.getCameraPosition().target.latitude +
                "," +
                zoomObject.getCameraPosition().target.longitude;
        Log.w("mzs", mzsURL);
        webView.loadUrl(mzsURL);
    }


}
