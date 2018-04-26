package com.botherconsulting.geoconnectable;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by dalemacdonald on 4/26/18.
 */

public class Hotspot {
    private LatLng latLng;
    private HotSpotMarker icon;
    private URL URL;
    private HotspotSchema data;
    private GoogleMap mmap;

    public Hotspot() {
        this(new HotspotSchema());
    }
    public Hotspot(GoogleMap map) {
        this(new HotspotSchema(), map);
    }
    public Hotspot(HotspotSchema _data) {
        this(_data, null);
    }
    public Hotspot(HotspotSchema _data,GoogleMap map) {
        this.data = _data;
        this.mmap = map;
    }


    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(double lat, double lng) {
        this.latLng = new LatLng(lat,lng);
        this.data.setLatitude(latLng.latitude);
        this.data.setLongitude(latLng.longitude);
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
        this.data.setLatitude(latLng.latitude);
        this.data.setLongitude(latLng.longitude);
    }

    public HotSpotMarker getIcon() {
        return icon;
    }

    public void setIcon(HotSpotMarker icon) {
        this.icon = icon;
        this.data.setIcon(icon.toString());
    }

    public void setIcon(String icon) {
        this.icon = new HotSpotMarker(this.mmap, this);
        if (icon.startsWith("+")) this.icon.setResourceName(icon);
        else if (icon.startsWith("/")) this.icon.setPathName(icon);
        else this.icon.setFileName(icon);
        this.data.setIcon(icon.toString());
    }

    public java.net.URL getURL() {
        return URL;
    }

    public void setURL(String _URL) {
        try {
            this.URL = new java.net.URL(_URL);
            this.data.setURL(this.URL.toString());
        }
        catch (MalformedURLException e) {
            Log.e("hotspot creation: Bad URL", _URL);
        }
    }

    public void setURL(java.net.URL URL) {
        this.URL = URL;
        this.data.setURL(this.URL.toString());
    }

    public boolean inTarget(LatLng position, double diameter) {
        boolean retVal = false;
        if ((Math.abs(this.latLng.latitude  - position.latitude)  < diameter) &&
                (Math.abs(this.latLng.latitude  - position.latitude)  < diameter)) {
            retVal = true;
            this.icon.setTargeted();
        } else {
            this.icon.unsetTargeted();
        }
        return retVal;
    }
}
