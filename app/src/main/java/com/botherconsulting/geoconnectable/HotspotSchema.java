package com.botherconsulting.geoconnectable;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by dwm160130 on 3/22/18.
 */

@Entity
public class HotspotSchema {
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
    //private  java.net.URL URL;
    private String URL;
    @ColumnInfo(name = "icon")
    //private HotSpotMarker icon;
    private String icon;

    public HotspotSchema() {
        this.latitude = 0.0;
        this.longitude = 0.0;

        this.enabled = false;
        this.set = "default";
        //this.icon = new HotSpotMarker(_mmap, this);


    }
    public int getUid() {
        return uid;
    }

    public void setUid(int _uid) {
        this.uid = _uid;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getSet() {
        return set;
    }

    public void setSet(String set) {
        this.set = set;
    }



}
