package com.botherconsulting.geoconnectable;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface HotspotDao {
    @Query("SELECT * FROM HotspotSchema")
    List<HotspotSchema> getAll();

    @Query("SELECT * FROM HotspotSchema WHERE uid IN (:userIds)")
    List<HotspotSchema> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM HotspotSchema WHERE latitude LIKE :lat AND longitude LIKE :lng  LIMIT 1")
    HotspotSchema findByLatLon(double lat, double lng);

    @Insert
    void insertAll(HotspotSchema... hotspots);

    @Delete
    void delete(HotspotSchema user);
}
