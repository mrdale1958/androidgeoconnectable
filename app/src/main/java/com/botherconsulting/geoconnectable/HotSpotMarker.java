package com.botherconsulting.geoconnectable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class HotSpotMarker  {
    private Marker marker;

    public HotSpotMarker(GoogleMap googleMap, Hotspot hotspot) {
        googleMap.addMarker(new MarkerOptions()
        .position(hotspot.get_LatLng())
        )
    }
}
