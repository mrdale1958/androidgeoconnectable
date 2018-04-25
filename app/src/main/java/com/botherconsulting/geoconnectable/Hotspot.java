package com.botherconsulting.geoconnectable;

import android.arch.persistence.room.*;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by dwm160130 on 3/22/18.
 */

@Entity
public class Hotspot {
    @PrimaryKey(autoGenerate = true)
    private int uid;

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "longitude")
    private double longitude;


    @ColumnInfo(name = "enabled")
    private  boolean enabled;
    @ColumnInfo(name = "set")
    private  String set;
    @ColumnInfo(name = "URL")
    private  java.net.URL URL;
    private HotSpotMarker icon;

    public Hotspot(GoogleMap _mmap) {
        this.latitude = 0.0;
        this.longitude = 0.0;

        this.enabled = false;
        this.set = "default";
        this.icon = new HotSpotMarker(_mmap, this);


    }

    public LatLng get_LatLng() {
        return new LatLng(latitude, longitude);
    }

    public boolean inTarget(LatLng position, double diameter) {
        boolean retVal = false;
        if ((Math.abs(this.latitude  - position.latitude)  < diameter) &&
                (Math.abs(this.latitude  - position.latitude)  < diameter)) {
            retVal = true;
            this.icon.setTargeted();
        } else {
            this.icon.unsetTargeted();
        }
        return retVal;
    }

}
