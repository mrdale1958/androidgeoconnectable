package com.botherconsulting.geoconnectable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class HotSpotMarker  {
    private Marker untargetedMarker;
    private Marker targetedMarker;
    private MarkerOptions targetedMarkerDescription;
    private MarkerOptions untargetedMarkerDescription;
    private boolean targeted = false;
    private GoogleMap mmap;
    private Hotspot hotspot;
    private String fileName;
    private String resourceName;
    private String pathName;

    public HotSpotMarker(GoogleMap googleMap, Hotspot _hotspot) {
        mmap = googleMap;
        hotspot = _hotspot;
        targetedMarkerDescription = new MarkerOptions()
                .position(hotspot.getLatLng());
        untargetedMarkerDescription = new MarkerOptions()
                .position(hotspot.getLatLng());
        untargetedMarker = googleMap.addMarker(untargetedMarkerDescription
        );
    }



    public String toString() {
        if (!this.resourceName.isEmpty()) return this.resourceName;
        if (!this.fileName.isEmpty()) return this.fileName;
        if (!this.pathName.isEmpty()) return this.pathName;
        return "";
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setPathName(String pathName) {
        this.pathName = pathName;
    }

    public void setTargeted() {
        targeted = true;
        // change to targeted marker
        untargetedMarker.remove();
        targetedMarker = mmap.addMarker(new MarkerOptions()
                .position(hotspot.getLatLng())
        );


    }
    public void unsetTargeted() {
        targeted = false;
        // change to untargeted marker
        targetedMarker.remove();
        untargetedMarker = mmap.addMarker(new MarkerOptions()
                .position(hotspot.getLatLng())
        );

    }
}
