package com.botherconsulting.geoconnectable;

import android.arch.persistence.room.RoomDatabase;

public abstract class HotspotDatabase extends RoomDatabase {
    public abstract HotspotDao hotspotDao();

}
