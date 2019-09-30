package com.botherconsulting.geoconnectable;
import android.content.Context;
//import  android.R.color;
//import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dalemacdonald on 11/13/17.
 */

public class MapMask {
    static final LatLng LA_LOCATION = new LatLng(34.052235, -118.243683);

    /**
     * In kilometers.
     */
    private static final int EARTH_RADIUS = 6371;

    private MapMask() {
        //no instance
    }

    static PolygonOptions createPolygonWithCircle(Context context, LatLng center, int radius) {

        return new PolygonOptions()
                .fillColor(0xff000000) //ContextCompat.getColor(context, R.color.colorPrimaryDark))
                .addAll(createOuterBounds())
                .addHole(createHole(center, radius))
                .strokeWidth(0);
    }

    static PolygonOptions createPolygonWithCircle(Context context, LatLng center, LatLng radius) {

        return new PolygonOptions()
                .fillColor(0xff000000) //ContextCompat.getColor(context, R.color.colorPrimaryDark))
                .addAll(createOuterBounds())
                .addHole(createHole(center, radius))
                .strokeWidth(0);
    }

    private static List<LatLng> createOuterBounds() {
        final float delta = 0.01f;

        return new ArrayList<LatLng>() {{
            add(new LatLng(90 - delta, -180 + delta));
            add(new LatLng(0, -180 + delta));
            add(new LatLng(-90 + delta, -180 + delta));
            add(new LatLng(-90 + delta, 0));
            add(new LatLng(-90 + delta, 180 - delta));
            add(new LatLng(0, 180 - delta));
            add(new LatLng(90 - delta, 180 - delta));
            add(new LatLng(90 - delta, 0));
            add(new LatLng(90 - delta, -180 + delta));
        }};
    }

    private static Iterable<LatLng> createHole(LatLng center, int radius) {
        int points = 50; // number of corners of inscribed polygon

        double radiusLatitude = Math.toDegrees(radius / (float) EARTH_RADIUS);
        double radiusLongitude = radiusLatitude / Math.cos(Math.toRadians(center.latitude));

        List<LatLng> result = new ArrayList<>(points);

        double anglePerCircleRegion = 2 * Math.PI / points;

        for (int i = 0; i < points; i++) {
            double theta = i * anglePerCircleRegion;
            double latitude = center.latitude + (radiusLatitude * Math.sin(theta));
            double longitude = center.longitude + (radiusLongitude * Math.cos(theta));

            result.add(new LatLng(latitude, longitude));
        }

        return result;
    }
    private static Iterable<LatLng> createHole(LatLng center, LatLng radius) {
        int points = 50; // number of corners of inscribed polygon

        double radiusLatitude = radius.latitude;
        double radiusLongitude = radius.longitude;

        List<LatLng> result = new ArrayList<>(points);

        double anglePerCircleRegion = 2 * Math.PI / points;

        for (int i = 0; i < points; i++) {
            double theta = i * anglePerCircleRegion;
            double latitude = center.latitude + (radiusLatitude * Math.sin(theta));
            double longitude = center.longitude + (radiusLongitude * Math.cos(theta));
            Log.i("circle point lat", String.valueOf(latitude));
            Log.i("circle point long", String.valueOf(longitude));
            result.add(new LatLng(latitude, longitude));
        }

        return result;
    }
}