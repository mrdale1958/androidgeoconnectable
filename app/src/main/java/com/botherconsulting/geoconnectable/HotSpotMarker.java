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

    public HotSpotMarker(GoogleMap googleMap, Hotspot _hotspot) {
        mmap = googleMap;
        hotspot = _hotspot;
        targetedMarkerDescription = new MarkerOptions()
                .position(hotspot.get_LatLng());
        untargetedMarkerDescription = new MarkerOptions()
                .position(hotspot.get_LatLng());
        untargetedMarker = googleMap.addMarker(untargetedMarkerDescription
        );
    }

    public void setTargeted() {
        targeted = true;
        // change to targeted marker
        untargetedMarker.remove();
        targetedMarker = mmap.addMarker(new MarkerOptions()
                .position(hotspot.get_LatLng())
        );


    }
    public void unsetTargeted() {
        targeted = false;
        // change to untargeted marker
        targetedMarker.remove();
        untargetedMarker = mmap.addMarker(new MarkerOptions()
                .position(hotspot.get_LatLng())
        );

    }
}
