package com.botherconsulting.geoconnectable;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

@Dao
public interface HotspotDao {
    @Query("SELECT * FROM Hotspot")
    List<Hotspot> getAll();

    @Query("SELECT * FROM Hotspot WHERE uid IN (:userIds)")
    List<Hotspot> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM Hotspot WHERE latitude LIKE :lat AND longitude LIKE :lng  LIMIT 1")
    Hotspot findByLatLon(double lat, double lng);

    @Insert
    void insertAll(Hotspot... hotspots);

    @Delete
    void delete(Hotspot user);
}
