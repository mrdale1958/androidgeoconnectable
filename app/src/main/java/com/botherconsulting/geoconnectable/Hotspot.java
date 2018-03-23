package com.botherconsulting.geoconnectable;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by dwm160130 on 3/22/18.
 */

public class Hotspot {
    public LatLng latlng;
    public boolean enabled;
    public String set;
    public java.net.URL URL;

    public Hotspot() {
        this.latlng = new LatLng(0.0,0.0);
        this.enabled = false;
        this.set = "default";


    }
}
