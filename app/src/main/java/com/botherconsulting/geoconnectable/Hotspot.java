package com.botherconsulting.geoconnectable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by dwm160130 on 3/22/18.
 */

public class Hotspot {
    public boolean enabled;
    public String set;
    public java.net.URL URL;
    public Marker marker;

    public Hotspot(GoogleMap map) {
        this.enabled = false;
        this.set = "default";
        this.marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(0.0,0.0))
                .title("some pithy name")
                .snippet("Even pithier label")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

    }

    public void setIcon(BitmapDescriptor icon) {
        this.marker.setIcon(icon);
    }
    public void setTitle(String title) {
        this.marker.setTitle(title);
    }
    public void setSnippet(String snippet) {
        this.marker.setSnippet(snippet);
    }
    public void setPosition(LatLng position) {
        this.marker.setPosition(position);
    }
}
